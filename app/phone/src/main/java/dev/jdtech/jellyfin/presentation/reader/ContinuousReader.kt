package dev.jdtech.jellyfin.presentation.reader

import android.content.Context
import android.net.Uri
import android.util.LruCache
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.jdtech.jellyfin.settings.domain.SubtitleFonts
import java.io.File
import java.net.URI
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url

/**
 * A continuous, vertically scrolling EPUB reader.
 *
 * Readium's reflowable navigator treats every spine resource as its own page. That is why scroll
 * mode only scrolls inside one chapter at a time. This reader assembles the reading order into one
 * HTML document and lets a single WebView scroll through it natively, so chapters flow into each
 * other with the platform's normal scrolling and fling behavior. Readium still supplies the
 * publication, TOC and locations.
 */
class ContinuousReaderController(
    context: Context,
    private val publication: Publication,
    private val chapters: List<Link>,
    initialSettings: ReaderSettings,
    private val initialLocator: Locator?,
) {
    private val context = context.applicationContext

    var settings by mutableStateOf(initialSettings)
        private set

    var onTap: (() -> Unit)? = null

    var onProgress: ((ContinuousProgress) -> Unit)? = null

    var onError: ((Throwable) -> Unit)? = null

    var lastProgress: ContinuousProgress? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var webView: WebView? = null

    private var loadJob: Job? = null

    private var loaded = false

    private var pendingTarget: ContinuousTarget? = initialTarget()

    private var pendingFraction: Double? = initialFraction()

    private val resourceCache =
        object : LruCache<String, ByteArray>(16 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = value.size
        }

    private val fontFiles: Map<String, File> by lazy {
        runCatching {
            SubtitleFonts.available(context)
                .mapNotNull { font -> font.file?.let { font.family to it } }
                .toMap()
        }
            .getOrElse { emptyMap() }
    }

    fun updateSettings(settings: ReaderSettings) {
        if (this.settings == settings) return
        this.settings = settings
        webView?.let { view ->
            view.setBackgroundColor(settings.readerBackgroundColor())
            applyStyle(view)
        }
    }

    fun goTo(link: Link) {
        val raw = link.href.toString()
        val index = findIndex(raw) ?: return
        pendingFraction = null
        navigateTo(ContinuousTarget(index, Url(raw)?.fragment))
    }

    fun seekTo(locator: Locator) {
        val fraction = locator.locations.totalProgression
        if (fraction != null) {
            seekToFraction(fraction)
            return
        }
        val index = findIndex(locator.href.toString()) ?: return
        pendingFraction = null
        navigateTo(
            ContinuousTarget(
                index = index,
                anchor = locator.locations.fragments.firstOrNull(),
            )
        )
    }

    fun seekToFraction(fraction: Double) {
        pendingTarget = null
        pendingFraction = fraction.coerceIn(0.0, 1.0)
        if (loaded) {
            scrollToFraction(pendingFraction ?: 0.0)
            pendingFraction = null
        }
    }

    internal fun createWebView(): WebView {
        val view =
            WebView(context).apply {
                setBackgroundColor(this@ContinuousReaderController.settings.readerBackgroundColor())
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = true
                settings.blockNetworkImage = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.textZoom = 100
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = ContinuousWebViewClient(this@ContinuousReaderController)
                addJavascriptInterface(ChapterBridge(this@ContinuousReaderController), BRIDGE_NAME)
            }
        webView = view
        loadJob?.cancel()
        loadJob = null
        loaded = false
        loadCombined()
        return view
    }

    internal fun releaseWebView(view: WebView) {
        if (webView === view) {
            webView = null
            loaded = false
            loadJob?.cancel()
            loadJob = null
        }
        view.destroy()
    }

    internal fun onPageFinished(view: WebView) {
        if (webView !== view) return
        loaded = true
        val target = pendingTarget
        val fraction = pendingFraction
        pendingTarget = null
        pendingFraction = null
        when {
            target != null -> scrollToSection(target)
            fraction != null -> scrollToFraction(fraction)
            lastProgress != null -> scrollToFraction(lastProgress!!.totalProgression)
            else -> report()
        }
        applyStyle(view)
    }

    internal fun postScroll(index: Int, chapterProgression: Double, totalProgression: Double) {
        scope.launch { onScroll(index, chapterProgression, totalProgression) }
    }

    internal fun postTap() {
        scope.launch { onTap?.invoke() }
    }

    internal fun postLink(url: String) {
        scope.launch { onLink(url) }
    }

    internal fun onScroll(index: Int, chapterProgression: Double, totalProgression: Double) {
        if (chapters.isEmpty()) return
        val progress =
            ContinuousProgress(
                chapterIndex = index.coerceIn(0, chapters.lastIndex),
                chapterProgression = chapterProgression.coerceIn(0.0, 1.0),
                totalProgression = totalProgression.coerceIn(0.0, 1.0),
            )
        lastProgress = progress
        onProgress?.invoke(progress)
    }

    internal fun onLink(url: String) {
        val index = findIndex(url) ?: return
        pendingFraction = null
        navigateTo(ContinuousTarget(index, Url(url)?.fragment))
    }

    internal fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        if (uri.host != HOST) return null

        val path = uri.encodedPath?.removePrefix("/") ?: return null
        if (path.startsWith(FONT_PREFIX)) {
            val family = Uri.decode(path.removePrefix(FONT_PREFIX))
            val file = fontFiles[family] ?: return null
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
            return webResponse(fontMimeType(file.name), bytes)
        }

        val href = Url(path) ?: return null
        val resource = publication.get(href) ?: return null
        val bytes =
            resourceCache.get(path)
                ?: runCatching { runBlocking { resource.read().getOrNull() } }
                    .getOrNull()
                    ?.also { resourceCache.put(path, it) }
                ?: return null
        val mediaType =
            publication.linkWithHref(href)?.mediaType?.toString()?.substringBefore(';')
                ?: mimeTypeFor(path)
        return webResponse(mediaType, bytes)
    }

    private fun navigateTo(target: ContinuousTarget) {
        if (loaded) {
            scrollToSection(target)
        } else {
            pendingTarget = target
        }
    }

    private fun loadCombined() {
        if (loadJob != null) return
        val target = webView ?: return
        loadJob = scope.launch {
            val currentSettings = settings
            val html = runCatching {
                withContext(Dispatchers.IO) { buildCombinedHtml(currentSettings) }
            }
                .getOrElse { error ->
                    withContext(Dispatchers.Main) { onError?.invoke(error) }
                    return@launch
                }
            withContext(Dispatchers.Main) {
                if (webView !== target) return@withContext
                loaded = false
                target.loadDataWithBaseURL(
                    "$HOST/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        }
    }

    private suspend fun buildCombinedHtml(settings: ReaderSettings): String {
        val stylesheets = linkedSetOf<String>()
        val inlineStyles = linkedSetOf<String>()
        val body = StringBuilder()

        chapters.forEachIndexed { index, link ->
            val raw = readChapter(link) ?: return@forEachIndexed
            val document = Jsoup.parse(raw, "$HOST/${link.href}")
            document.outputSettings().prettyPrint(false)
            document.select("script, base, noscript, meta[http-equiv]").remove()

            document.select("link").forEach { element ->
                val rels = element.attr("rel").lowercase().split(Regex("\\s+"))
                if (rels.contains("stylesheet")) {
                    element.absUrl("href").takeIf { it.isNotBlank() }?.let(stylesheets::add)
                }
                element.remove()
            }

            document.select("style").forEach { element ->
                val css = rewriteCssUrls(element.data(), link.href.toString())
                if (css.isNotBlank()) inlineStyles.add(css)
                element.remove()
            }

            val chapterBody = document.body()
            val chapterHref = link.href.toString()
            rewriteBodyUrls(chapterBody, chapterHref)
            val sectionClasses = buildString {
                append("findroid-chapter")
                val bodyClass = chapterBody.className().trim()
                if (bodyClass.isNotBlank()) append(' ').append(bodyClass)
            }
            body.append("<section")
            body.append(" class=\"${escapeAttribute(sectionClasses)}\"")
            body.append(" id=\"findroid-chapter-$index\"")
            body.append(" data-findroid-index=\"$index\"")
            chapterBody
                .attr("dir")
                .takeIf { it.isNotBlank() }
                ?.let { body.append(" dir=\"${escapeAttribute(it)}\"") }
            chapterBody
                .attr("lang")
                .takeIf { it.isNotBlank() }
                ?.let { body.append(" lang=\"${escapeAttribute(it)}\"") }
            chapterBody
                .attr("style")
                .takeIf { it.isNotBlank() }
                ?.let {
                    body.append(" style=\"${escapeAttribute(rewriteCssUrls(it, chapterHref))}\"")
                }
            body.append(">")
            body.append(chapterBody.html())
            body.append("</section>")
        }

        val cssLinks =
            stylesheets.joinToString("") { href ->
                """<link rel="stylesheet" href="${escapeAttribute(href)}">"""
            }
        val cssBlocks = inlineStyles.joinToString("") { css -> "<style>$css</style>" }

        return buildString {
            append("<!DOCTYPE html><html><head>")
            append(HEAD_META)
            append(cssLinks)
            append(cssBlocks)
            append("""<style id="findroid-reader-style">""")
            append(readerCss(settings))
            append("</style></head><body>")
            append(body)
            append("<script>")
            append(READER_SCRIPT)
            append("</script></body></html>")
        }
    }

    private suspend fun readChapter(link: Link): String? {
        val bytes = publication.get(link)?.read()?.getOrNull() ?: return null
        val charset = link.mediaType?.charset ?: Charsets.UTF_8
        return bytes.toString(charset)
    }

    private fun rewriteBodyUrls(body: Element, chapterHref: String) {
        listOf("src", "poster", "data", "background").forEach { attribute ->
            body.select("[$attribute]").forEach { element ->
                val raw = element.attr(attribute)
                if (raw.isNotBlank()) {
                    element.attr(attribute, resolveUrl(chapterHref, raw))
                }
            }
        }

        body.select("[href]").forEach { element ->
            val raw = element.attr("href")
            when {
                raw.isBlank() -> Unit
                raw.startsWith("javascript:", ignoreCase = true) -> element.attr("href", "#")
                else -> element.attr("href", resolveUrl(chapterHref, raw))
            }
        }

        body.select("[srcset]").forEach { element ->
            element.attr("srcset", rewriteSrcset(element.attr("srcset"), chapterHref))
        }

        body.select("[style]").forEach { element ->
            element.attr("style", rewriteCssUrls(element.attr("style"), chapterHref))
        }

        body.allElements.forEach { element ->
            if (element.hasAttr("xlink:href")) {
                element.attr(
                    "xlink:href",
                    resolveUrl(chapterHref, element.attr("xlink:href")),
                )
            }
        }
    }

    private fun rewriteSrcset(value: String, chapterHref: String): String {
        if (value.isBlank() || value.contains("data:")) return value
        return value.split(',').joinToString(", ") { candidate ->
            val trimmed = candidate.trim()
            if (trimmed.isEmpty()) return@joinToString trimmed
            val separator = trimmed.indexOfFirst { it.isWhitespace() }
            if (separator < 0) {
                resolveUrl(chapterHref, trimmed)
            } else {
                resolveUrl(chapterHref, trimmed.substring(0, separator)) +
                    trimmed.substring(separator)
            }
        }
    }

    private fun rewriteCssUrls(css: String, chapterHref: String): String {
        var result =
            css.replace(CSS_IMPORT_REGEX) { match ->
                val quote = match.groupValues[1]
                val importUrl = match.groupValues[2]
                val resolved =
                    if (importUrl.startsWith("#")) importUrl else resolveUrl(chapterHref, importUrl)
                "@import $quote$resolved$quote"
            }
        result =
            result.replace(CSS_URL_REGEX) { match ->
                val quote = match.groupValues[1]
                val url = match.groupValues[2].trim()
                val resolved = if (url.startsWith("#")) url else resolveUrl(chapterHref, url)
                "url($quote$resolved$quote)"
            }
        return result
    }

    private fun resolveUrl(chapterHref: String, raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return value
        val lower = value.lowercase()
        if (
            lower.startsWith("data:") ||
                lower.startsWith("mailto:") ||
                lower.startsWith("tel:") ||
                lower.startsWith("javascript:")
        ) {
            return value
        }
        if (value.startsWith("//")) return "https:$value"
        if (value.startsWith("#")) return "$HOST/${chapterHref.substringBefore('#')}$value"
        if (SCHEME_REGEX.containsMatchIn(value)) return value

        val base = chapterHref.substringBefore('#').replace(" ", "%20")
        val resolved =
            runCatching {
                URI(base).resolve(value.replace(" ", "%20")).toString()
            }
                .getOrNull() ?: value
        return if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            resolved
        } else {
            "$HOST/${resolved.removePrefix("/")}"
        }
    }

    private fun scrollToFraction(fraction: Double) {
        val view = webView ?: return
        view.evaluateJavascript(
            "window.__findroidScrollToFraction&&window.__findroidScrollToFraction(${fraction.coerceIn(0.0, 1.0)})",
            null,
        )
    }

    private fun scrollToSection(target: ContinuousTarget) {
        val view = webView ?: return
        val anchor = JSONObject.quote(target.anchor.orEmpty())
        view.evaluateJavascript(
            "window.__findroidScrollToSection&&window.__findroidScrollToSection(${target.index}, $anchor)",
            null,
        )
    }

    private fun report() {
        webView?.evaluateJavascript(
            "window.__findroidReport&&window.__findroidReport()",
            null,
        )
    }

    private fun applyStyle(view: WebView) {
        val css = readerCss(settings)
        val script =
            """
            (function() {
              var style = document.getElementById('findroid-reader-style');
              var doc = document.documentElement;
              var max = Math.max(1, doc.scrollHeight - window.innerHeight);
              var fraction = (window.scrollY || doc.scrollTop || 0) / max;
              if (style) style.textContent = ${JSONObject.quote(css)};
              requestAnimationFrame(function() {
                var newMax = Math.max(0, doc.scrollHeight - window.innerHeight);
                window.scrollTo(0, Math.max(0, Math.min(1, fraction)) * newMax);
                if (window.__findroidReport) window.__findroidReport();
              });
            })();
            """
                .trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun initialFraction(): Double? = initialLocator?.locations?.totalProgression

    private fun initialTarget(): ContinuousTarget? {
        val locator = initialLocator ?: return null
        if (locator.locations.totalProgression != null) return null
        val index = findIndex(locator.href.toString()) ?: return null
        return ContinuousTarget(index, locator.locations.fragments.firstOrNull())
    }

    private fun findIndex(href: String): Int? {
        val url = Url(href) ?: return null
        val target = chapterKey(url.removeFragment().toString())
        chapters
            .indexOfFirst { chapterKey(it.href.toString()) == target }
            .takeIf { it >= 0 }
            ?.let {
                return it
            }

        chapters
            .indexOfFirst { chapter ->
                val key = chapterKey(chapter.href.toString())
                key.endsWith("/$target") || target.endsWith("/$key")
            }
            .takeIf { it >= 0 }
            ?.let {
                return it
            }

        val link = publication.linkWithHref(url.removeFragment()) ?: return null
        val linkedHref = chapterKey(link.href.toString())
        return chapters
            .indexOfFirst { chapterKey(it.href.toString()) == linkedHref }
            .takeIf { it >= 0 }
    }

    private fun chapterKey(href: String): String {
        val parsed = Uri.parse(href)
        val localHref =
            if (parsed.host == HOST) {
                parsed.encodedPath?.removePrefix("/") ?: href
            } else {
                href
            }
        return Url(localHref)?.removeFragment()?.toString()?.substringBefore('?')
            ?: localHref.substringBefore('#').substringBefore('?')
    }

    private fun readerCss(settings: ReaderSettings): String {
        val background = settings.readerBackgroundColor()
        val foreground = settings.readerTextColor()
        val link = settings.readerLinkColor()
        val maxWidth =
            when (settings.maxWidth) {
                "narrow" -> "32rem"
                "wide" -> "60rem"
                "none" -> "none"
                else -> "40rem"
            }
        val margin = settings.pageMargins
        val horizontalPadding = (margin * 28).roundToInt()
        val verticalPadding = (margin * 24).roundToInt()
        val textAlignRule =
            when (settings.textAlign) {
                "start" -> "text-align: start !important;"
                "justify" -> "text-align: justify !important;"
                else -> ""
            }
        val hyphens = if (settings.hyphenation) "auto" else "manual"
        val font = fontCss(settings)

        return """
            html {
              background: #${colorHex(background)} !important;
              font-size: ${(16f * settings.fontSize).roundToInt()}px !important;
              height: auto !important;
              min-height: 0 !important;
              overflow-x: hidden !important;
            }
            body {
              background: #${colorHex(background)} !important;
              color: #${colorHex(foreground)} !important;
              margin: 0 auto !important;
              padding: ${verticalPadding}px ${horizontalPadding}px !important;
              width: 100% !important;
              height: auto !important;
              min-height: 0 !important;
              max-width: $maxWidth !important;
              box-sizing: border-box !important;
              display: block !important;
              font-size: 1rem !important;
              font-weight: ${(400f * settings.fontWeight).roundToInt()} !important;
              line-height: ${settings.lineHeight} !important;
              $textAlignRule
              word-spacing: ${settings.wordSpacing}em !important;
              letter-spacing: ${settings.letterSpacing}em !important;
              hyphens: $hyphens !important;
              -webkit-hyphens: $hyphens !important;
              overflow-x: hidden !important;
              overflow-wrap: break-word !important;
              column-count: 1 !important;
              column-width: auto !important;
            }
            section.findroid-chapter {
              display: block !important;
            }
            section.findroid-chapter + section.findroid-chapter {
              margin-top: 2.5em !important;
            }
            p {
              margin: 0 0 ${settings.paragraphSpacing}em !important;
              padding: 0 !important;
            }
            h1, h2, h3, h4, h5, h6 {
              color: #${colorHex(foreground)} !important;
              line-height: 1.25 !important;
            }
            body *:not(a) {
              color: #${colorHex(foreground)} !important;
            }
            p, div, span, li, td, th, blockquote, section, article, main, aside {
              background-color: transparent !important;
            }
            a, a:visited, a * {
              color: #${colorHex(link)} !important;
            }
            img, svg, video, canvas {
              max-width: 100% !important;
              height: auto !important;
            }
            table {
              max-width: 100% !important;
            }
            pre, code {
              white-space: pre-wrap !important;
              overflow-wrap: break-word !important;
            }
            ::selection {
              background: #${colorHex(link)}44 !important;
            }
            $font
        """
            .trimIndent()
    }

    private fun fontCss(settings: ReaderSettings): String {
        val family = settings.fontFamily.ifBlank { "sans-serif" }
        if (family == "sans-serif" || family == "serif" || family == "monospace") {
            return "body { font-family: $family, serif !important; }"
        }
        val familyLiteral = "'$family'"
        val file = fontFiles[family]
        if (file == null) {
            return "body { font-family: $familyLiteral, serif !important; }"
        }
        val familyUrl = "$HOST/$FONT_PREFIX${Uri.encode(family)}"
        return """
            @font-face {
              font-family: $familyLiteral;
              src: url('$familyUrl');
              font-weight: 100 900;
              font-style: normal;
            }
            body { font-family: $familyLiteral, serif !important; }
        """
            .trimIndent()
    }

    fun release() {
        loadJob?.cancel()
        scope.cancel()
        webView = null
        loaded = false
    }

    private fun webResponse(mediaType: String, bytes: ByteArray): WebResourceResponse =
        WebResourceResponse(
            mediaType,
            null,
            200,
            "OK",
            mapOf("Access-Control-Allow-Origin" to "*"),
            bytes.inputStream(),
        )

    private class ChapterBridge(private val controller: ContinuousReaderController) {
        @JavascriptInterface
        fun onScroll(index: Int, chapterProgression: Double, totalProgression: Double) {
            controller.postScroll(index, chapterProgression, totalProgression)
        }

        @JavascriptInterface
        fun onTap() {
            controller.postTap()
        }

        @JavascriptInterface
        fun onLink(url: String) {
            controller.postLink(url)
        }
    }

    private class ContinuousWebViewClient(private val controller: ContinuousReaderController) :
        WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            controller.onPageFinished(view)
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? = controller.intercept(request)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (request.url.host == HOST) {
                controller.onLink(request.url.toString())
            }
            return true
        }
    }

    private companion object {
        const val HOST = "reader.local"
        const val FONT_PREFIX = "__findroid_fonts/"
        const val BRIDGE_NAME = "FindroidReader"

        val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
        val CSS_IMPORT_REGEX = Regex("""@import\s+(['"])([^'"]+)\1""", RegexOption.IGNORE_CASE)
        val CSS_URL_REGEX = Regex("""url\(\s*(['"]?)([^'")]*)\1\s*\)""", RegexOption.IGNORE_CASE)

        const val HEAD_META =
            """<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">"""

        const val READER_SCRIPT =
            """
            (function() {
              var sections = null;
              var ticking = false;

              function getSections() {
                if (!sections) {
                  sections = document.querySelectorAll('section.findroid-chapter');
                }
                return sections;
              }

              function reportNow() {
                var doc = document.documentElement;
                var y = window.scrollY || doc.scrollTop || 0;
                var max = Math.max(1, doc.scrollHeight - window.innerHeight);
                var overall = Math.min(1, Math.max(0, y / max));
                var nodes = getSections();
                var point = document.elementFromPoint(window.innerWidth / 2, 1);
                var current = point && point.closest
                  ? point.closest('section.findroid-chapter')
                  : null;
                if (!current) {
                  current = nodes.length ? nodes[0] : null;
                  for (var i = 0; i < nodes.length; i++) {
                    var rect = nodes[i].getBoundingClientRect();
                    if (rect.top <= 2) {
                      current = nodes[i];
                    } else {
                      break;
                    }
                  }
                }
                var index = current
                  ? parseInt(current.getAttribute('data-findroid-index') || '0', 10)
                  : 0;
                var top = current ? current.getBoundingClientRect().top + y : 0;
                var height = current ? Math.max(1, current.getBoundingClientRect().height) : 1;
                var local = Math.min(1, Math.max(0, (y - top) / height));
                try {
                  FindroidReader.onScroll(index, local, overall);
                } catch (error) {}
              }

              function report() {
                if (ticking) return;
                ticking = true;
                requestAnimationFrame(function() {
                  ticking = false;
                  reportNow();
                });
              }

              window.__findroidReport = report;
              window.__findroidScrollToFraction = function(fraction) {
                var doc = document.documentElement;
                var max = Math.max(0, doc.scrollHeight - window.innerHeight);
                window.scrollTo(0, Math.max(0, Math.min(1, fraction)) * max);
                report();
              };
              window.__findroidScrollToSection = function(index, anchor) {
                var section = document.getElementById('findroid-chapter-' + index);
                if (!section) return;
                var target = section;
                if (anchor) {
                  var ids = section.querySelectorAll('[id]');
                  for (var i = 0; i < ids.length; i++) {
                    if (ids[i].id === anchor) {
                      target = ids[i];
                      break;
                    }
                  }
                  if (target === section) {
                    var names = section.querySelectorAll('[name]');
                    for (var j = 0; j < names.length; j++) {
                      if (names[j].getAttribute('name') === anchor) {
                        target = names[j];
                        break;
                      }
                    }
                  }
                }
                target.scrollIntoView(true);
                report();
              };

              window.addEventListener('scroll', report, { passive: true });
              window.addEventListener('resize', report);
              window.addEventListener('load', report);
              setTimeout(report, 50);
              setTimeout(report, 300);
              setTimeout(report, 1200);

              document.addEventListener('click', function(event) {
                var target = event.target;
                var link = target && target.closest ? target.closest('a') : null;
                if (link) {
                  var href = link.getAttribute('href');
                  if (href) {
                    event.preventDefault();
                    event.stopPropagation();
                    try {
                      FindroidReader.onLink(link.href);
                    } catch (error) {}
                    return;
                  }
                }
                try {
                  FindroidReader.onTap();
                } catch (error) {}
              }, true);
            })();
            """
    }
}

data class ContinuousProgress(
    val chapterIndex: Int,
    val chapterProgression: Double,
    val totalProgression: Double,
)

private data class ContinuousTarget(
    val index: Int,
    val anchor: String?,
)

private fun colorHex(color: Int): String = String.format("%06X", color and 0xFFFFFF)

private fun mimeTypeFor(path: String): String =
    when (path.substringAfterLast('.', "").lowercase()) {
        "css" -> "text/css"
        "html",
        "xhtml",
        "htm" -> "text/html"
        "js" -> "application/javascript"
        "json" -> "application/json"
        "png" -> "image/png"
        "jpg",
        "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        else -> "application/octet-stream"
    }

private fun fontMimeType(name: String): String =
    when (name.substringAfterLast('.', "").lowercase()) {
        "otf" -> "font/otf"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        else -> "font/ttf"
    }

fun ReaderSettings.readerBackgroundColor(): Int =
    when (theme) {
        "light" -> 0xFFFFFFFF.toInt()
        "sepia" -> 0xFFF4ECD8.toInt()
        else -> 0xFF121212.toInt()
    }

fun ReaderSettings.readerTextColor(): Int =
    when (theme) {
        "light" -> 0xFF1A1A1A.toInt()
        "sepia" -> 0xFF5B4636.toInt()
        else -> 0xFFE6E6E6.toInt()
    }

private fun ReaderSettings.readerLinkColor(): Int =
    when (theme) {
        "light" -> 0xFF1A73E8.toInt()
        "sepia" -> 0xFF8B5E3C.toInt()
        else -> 0xFF8AB4F8.toInt()
    }

private fun escapeAttribute(value: String): String =
    value.replace("&", "&amp;").replace("\"", "&quot;")

@Composable
fun ContinuousReader(
    controller: ContinuousReaderController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { controller.createWebView() },
        modifier = modifier.fillMaxSize(),
        onRelease = { controller.releaseWebView(it) },
    )
}
