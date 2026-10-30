package dev.jdtech.jellyfin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.databinding.ActivityReaderBinding
import dev.jdtech.jellyfin.presentation.reader.ContinuousProgress
import dev.jdtech.jellyfin.presentation.reader.ContinuousReader
import dev.jdtech.jellyfin.presentation.reader.ContinuousReaderController
import dev.jdtech.jellyfin.presentation.reader.ReaderChapter
import dev.jdtech.jellyfin.presentation.reader.ReaderOverlay
import dev.jdtech.jellyfin.presentation.reader.ReaderSettings
import dev.jdtech.jellyfin.presentation.reader.ReaderTocScreen
import dev.jdtech.jellyfin.presentation.reader.ReaderUiState
import dev.jdtech.jellyfin.presentation.reader.readerSettings
import dev.jdtech.jellyfin.presentation.reader.toEpubPreferences
import dev.jdtech.jellyfin.presentation.reader.toReaderChapters
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.utils.LocalHideStatusBar
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.settings.domain.SubtitleFonts
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.Length
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import timber.log.Timber

/**
 * Reads a Jellyfin book in the app. The publication is downloaded once, opened with Readium and
 * rendered by the navigator matching its format: EPUB (reflowable, with the reader's typography
 * settings), PDF or CBZ/Divina (fixed layout). The controls overlay matches the player's chrome.
 */
@AndroidEntryPoint
@OptIn(
    ExperimentalReadiumApi::class,
    InternalReadiumApi::class,
    DelicateReadiumApi::class,
)
class ReaderActivity : AppCompatActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    @Inject lateinit var jellyfinApi: JellyfinApi

    @Inject lateinit var repository: JellyfinRepository

    private lateinit var binding: ActivityReaderBinding

    private val uiState = mutableStateOf(ReaderUiState())
    private val controlsVisible = mutableStateOf(true)
    private val showContents = mutableStateOf(false)
    private val chapters = mutableStateOf<List<ReaderChapter>>(emptyList())
    private val continuousController = mutableStateOf<ContinuousReaderController?>(null)
    private val readerSettings = mutableStateOf(ReaderSettings())

    private val handler = Handler(Looper.getMainLooper())
    private val hideControls = Runnable { controlsVisible.value = false }
    private val tapListener =
        object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val scrollMode = appPreferences.getValue(appPreferences.readerMode) == "scroll"
                if (!scrollMode && appPreferences.getValue(appPreferences.readerTapTurn)) {
                    val width = binding.readerNavigator.width
                    if (width > 0) {
                        when {
                            event.point.x < width * 0.3f ->
                                return overflowableNavigator?.goBackward() == true
                            event.point.x > width * 0.7f ->
                                return overflowableNavigator?.goForward() == true
                        }
                    }
                }
                toggleControls()
                return false
            }
        }

    private var bookId: UUID? = null
    private var navigator: Navigator? = null
    private var visualNavigator: VisualNavigator? = null
    private var overflowableNavigator: OverflowableNavigator? = null
    private var epubNavigator: EpubNavigatorFragment? = null
    private var positions: List<Locator> = emptyList()
    private var publication: Publication? = null
    private var lastLocator: Locator? = null
    private var locatorJob: Job? = null
    private var appliedMaxWidth: Double? = null
    private var maxWidthHref: String? = null
    private var startFromBeginning = false
    private var markedPlayed = false
    private var currentTitle = ""
    private var currentId: UUID? = null
    private var lastSavedPage = 0
    private var lastSavedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        val id =
            intent.getStringExtra(EXTRA_BOOK_ID)?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            }
        val title = intent.getStringExtra(EXTRA_BOOK_TITLE).orEmpty()
        val overview = intent.getStringExtra(EXTRA_OVERVIEW).orEmpty()
        if (id == null) {
            finish()
            return
        }
        bookId = id
        startFromBeginning = intent.getBooleanExtra(EXTRA_START_FROM_BEGINNING, false)

        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readerSettings.value = appPreferences.readerSettings()
        binding.readerDim.alpha = brightnessOverlay(readerSettings.value.brightness)

        binding.readerContinuous.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            isVisible = false
            setContent {
                FindroidTheme {
                    continuousController.value?.let { controller ->
                        ContinuousReader(
                            controller = controller,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        binding.readerOverlay.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FindroidTheme {
                    BackHandler(enabled = showContents.value) { showContents.value = false }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showContents.value) {
                            CompositionLocalProvider(LocalHideStatusBar provides true) {
                                ReaderTocScreen(
                                    chapters = chapters.value,
                                    onBack = { showContents.value = false },
                                    onChapterClick = { chapter ->
                                        showContents.value = false
                                        val continuous = continuousController.value
                                        if (continuous != null) {
                                            continuous.goTo(chapter.link)
                                        } else {
                                            navigator?.go(chapter.link)
                                        }
                                    },
                                )
                            }
                        } else {
                            ReaderOverlay(
                                state = uiState.value,
                                controlsVisible = controlsVisible.value,
                                onBack = { finish() },
                                onOpenContents = { showContents.value = true },
                                onOpenSettings = {
                                    startActivity(
                                        Intent(
                                            this@ReaderActivity,
                                            ReaderSettingsActivity::class.java,
                                        )
                                    )
                                },
                                onSeek = { page -> seekTo(page) },
                                onInteraction = { showControls() },
                            )
                        }
                    }
                }
            }
        }

        uiState.value = uiState.value.copy(title = title, overview = overview, loading = true)

        val isAudiobook = intent.getBooleanExtra(EXTRA_AUDIOBOOK, false)
        if (isAudiobook) {
            uiState.value =
                uiState.value.copy(
                    loading = false,
                    error = "Audiobooks aren't supported in the reader yet.",
                )
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finish()
                }
            },
        )

        lifecycleScope.launch { if (!isAudiobook) openBook(id, title) }
    }

    override fun onResume() {
        super.onResume()
        // Settings opened from the reader are visible behind this activity when we come back.
        if (::binding.isInitialized) {
            val settings = appPreferences.readerSettings()
            readerSettings.value = settings
            binding.readerDim.alpha = brightnessOverlay(settings.brightness)

            val continuous = continuousController.value
            val continuousExpected =
                publication?.let { supportsContinuousScroll(it, settings) } == true
            if (continuous != null && !continuousExpected) {
                reopenCurrentPublication()
                return
            }

            if (continuous != null) {
                continuous.updateSettings(settings)
            } else {
                epubNavigator?.submitPreferences(settings.toEpubPreferences())
                lifecycleScope.launch { applyMaxWidth(settings.maxLineLengthRem, force = true) }
                if (continuousExpected) {
                    reopenCurrentPublication()
                }
            }
        }
    }

    private fun reopenCurrentPublication() {
        val id = currentId ?: return
        val publication = publication ?: return
        if (uiState.value.loading) return
        showPublication(publication, id, currentTitle)
    }

    private fun supportsContinuousScroll(
        publication: Publication,
        settings: ReaderSettings,
    ): Boolean =
        publication.conformsTo(Publication.Profile.EPUB) &&
            settings.scroll &&
            (publication.metadata.layout == null ||
                publication.metadata.layout == Layout.REFLOWABLE)

    private fun showControls() {
        controlsVisible.value = true
        handler.removeCallbacks(hideControls)
        handler.postDelayed(hideControls, 4000L)
    }

    private fun toggleControls() {
        if (controlsVisible.value) {
            handler.removeCallbacks(hideControls)
            controlsVisible.value = false
        } else {
            showControls()
        }
    }

    private fun seekTo(page: Int) {
        positions.getOrNull(page - 1)?.let { locator ->
            val continuous = continuousController.value
            if (continuous != null) {
                continuous.seekTo(locator)
            } else {
                epubNavigator?.go(locator)
            }
        }
    }

    private suspend fun openBook(id: UUID, title: String) {
        try {
            val publication =
                withContext(Dispatchers.IO) {
                    val file = downloadBook(id)
                    openPublication(file)
                }
            if (isFinishing || isDestroyed) return
            showPublication(publication, id, title)
        } catch (e: Exception) {
            Timber.e(e)
            uiState.value = uiState.value.copy(loading = false, error = readableError(e))
        }
    }

    private suspend fun downloadBook(id: UUID): File {
        val file = File(cacheDir, "books/$id")
        if (file.exists() && file.length() > 0) return file

        file.parentFile?.mkdirs()

        // Let the SDK make the request: it authenticates exactly like every other API call.
        val bytes = jellyfinApi.libraryApi.getDownload(id).content
        if (bytes.isEmpty()) throw IOException("Empty response")
        file.writeBytes(bytes)
        return file
    }

    private suspend fun openPublication(file: File): Publication {
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(contentResolver, httpClient)
        val parser =
            DefaultPublicationParser(
                this,
                httpClient,
                assetRetriever,
                pdfFactory = PdfiumDocumentFactory(this),
            )
        val opener = PublicationOpener(parser)

        val asset =
            assetRetriever.retrieve(file).getOrNull()
                ?: throw IOException("Unsupported book format")
        return opener.open(asset, allowUserInteraction = false).getOrNull()
            ?: throw IOException("This book could not be opened")
    }

    private fun showPublication(publication: Publication, id: UUID, title: String) {
        this.publication = publication
        currentId = id
        currentTitle = title
        chapters.value = publication.tableOfContents.toReaderChapters()

        val initialLocator =
            if (startFromBeginning) {
                null
            } else {
                lastLocator ?: storedLocator(id)
            }
        startFromBeginning = false
        val settings = readerSettings.value
        val epubPreferences = settings.toEpubPreferences()

        // Switching between paged and continuous scroll reuses the same activity. Tear down the
        // renderer that was active before creating the new one.
        locatorJob?.cancel()
        locatorJob = null
        continuousController.value?.release()
        continuousController.value = null
        supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { previous ->
            supportFragmentManager.beginTransaction().remove(previous).commitNow()
        }
        navigator = null
        visualNavigator = null
        overflowableNavigator = null
        epubNavigator = null
        positions = emptyList()
        lastLocator = initialLocator
        lastSavedPage = 0
        lastSavedAt = 0L
        maxWidthHref = null
        binding.readerContinuous.isVisible = false

        if (supportsContinuousScroll(publication, settings)) {
            val controller =
                ContinuousReaderController(
                    context = this,
                    publication = publication,
                    chapters = publication.readingOrder,
                    initialSettings = settings,
                    initialLocator = initialLocator,
                )
            controller.onTap = { toggleControls() }
            controller.onProgress = { progress -> onContinuousProgress(progress) }
            controller.onError = { error ->
                uiState.value = uiState.value.copy(loading = false, error = readableError(error))
            }
            continuousController.value = controller
            binding.readerContinuous.isVisible = true

            uiState.value =
                uiState.value.copy(
                    title = title,
                    loading = false,
                    error = null,
                    page = initialLocator?.locations?.position ?: 1,
                    pageCount = positions.size,
                )
            showControls()

            lifecycleScope.launch {
                val locators = runCatching { publication.positions() }.getOrNull().orEmpty()
                positions = locators
                appPreferences.sharedPreferences
                    .edit()
                    .putInt("reader_page_count_$id", locators.size)
                    .apply()
                uiState.value = uiState.value.copy(pageCount = locators.size)
                continuousController.value?.lastProgress?.let { progress ->
                    onContinuousProgress(progress, force = true)
                }
            }
            return
        }

        val fragmentClass: Class<out Fragment>
        val fragmentFactory: androidx.fragment.app.FragmentFactory

        when {
            publication.conformsTo(Publication.Profile.EPUB) -> {
                fragmentClass = EpubNavigatorFragment::class.java
                val configuration = EpubNavigatorFragment.Configuration {
                    // Serve the bundled font files and declare them, so the same families as
                    // the subtitle font setting can be selected for the book text.
                    servedAssets += "subtitle-fonts/.*"
                    // Scroll mode should never change chapters on a horizontal swipe.
                    disablePageTurnsWhileScrolling = true
                    readiumCssRsProperties =
                        RsProperties(maxLineLength = Length.Rem(settings.maxLineLengthRem))
                    SubtitleFonts.bundledFamilyAssets(this@ReaderActivity).forEach { (family, asset)
                        ->
                        addFontFamilyDeclaration(FontFamily(family)) {
                            addFontFace {
                                addSource("subtitle-fonts/$asset")
                                setFontWeight(100..900)
                            }
                        }
                    }
                }
                fragmentFactory =
                    EpubNavigatorFactory(publication)
                        .createFragmentFactory(
                            initialLocator = initialLocator,
                            initialPreferences = epubPreferences,
                            configuration = configuration,
                        )
            }

            publication.conformsTo(Publication.Profile.PDF) -> {
                fragmentClass = PdfNavigatorFragment::class.java
                fragmentFactory =
                    PdfNavigatorFactory(publication, PdfiumEngineProvider())
                        .createFragmentFactory(
                            initialLocator = initialLocator,
                            initialPreferences = PdfiumPreferences(),
                        )
            }

            else -> {
                fragmentClass = ImageNavigatorFragment::class.java
                fragmentFactory =
                    ImageNavigatorFragment.createFactory(
                        publication = publication,
                        initialLocator = initialLocator,
                    )
            }
        }

        supportFragmentManager.fragmentFactory = fragmentFactory
        supportFragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .add(R.id.reader_navigator, fragmentClass, null, FRAGMENT_TAG)
            .commitNow()

        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
        navigator = fragment as? Navigator
        visualNavigator = fragment as? VisualNavigator
        overflowableNavigator = fragment as? OverflowableNavigator
        epubNavigator = fragment as? EpubNavigatorFragment

        visualNavigator?.addInputListener(tapListener)
        appliedMaxWidth = settings.maxLineLengthRem

        uiState.value =
            uiState.value.copy(
                title = title,
                loading = false,
                error = null,
                page = initialLocator?.locations?.position ?: 1,
                pageCount = positions.size,
            )
        showControls()

        lifecycleScope.launch {
            val locators = runCatching { publication.positions() }.getOrNull().orEmpty()
            positions = locators
            appPreferences.sharedPreferences
                .edit()
                .putInt("reader_page_count_$id", locators.size)
                .apply()
            uiState.value = uiState.value.copy(pageCount = locators.size)
        }

        locatorJob = lifecycleScope.launch {
            navigator?.currentLocator?.collect { locator ->
                val href = locator.href.toString()
                if (href.isEmpty()) return@collect
                lastLocator = locator
                appPreferences.sharedPreferences
                    .edit()
                    .putString(locatorKey(id), locator.toJSON().toString())
                    .apply()
                uiState.value =
                    uiState.value.copy(
                        page = locator.locations.position ?: uiState.value.page,
                        pageCount = positions.size.takeIf { it > 0 } ?: uiState.value.pageCount,
                    )
                if (href != maxWidthHref) {
                    maxWidthHref = href
                    val maxWidth = appPreferences.readerSettings().maxLineLengthRem
                    lifecycleScope.launch { applyMaxWidth(maxWidth, force = true) }
                }
                val atEnd =
                    (locator.locations.totalProgression ?: 0.0) >= 0.999 ||
                        (positions.isNotEmpty() && locator.locations.position == positions.size)
                if (!markedPlayed && atEnd) {
                    markedPlayed = true
                    lastLocator = null
                    lifecycleScope.launch {
                        runCatching { repository.markAsPlayed(id) }
                        appPreferences.sharedPreferences
                            .edit()
                            .remove(locatorKey(id))
                            .remove("reader_page_count_$id")
                            .apply()
                    }
                }
            }
        }
    }

    private fun onContinuousProgress(progress: ContinuousProgress, force: Boolean = false) {
        val id = currentId ?: return
        val pageCount = positions.size
        val page =
            if (pageCount > 1) {
                (progress.totalProgression * (pageCount - 1)).roundToInt() + 1
            } else {
                1
            }
        val position = page.coerceIn(1, pageCount.coerceAtLeast(1))
        uiState.value =
            uiState.value.copy(
                page = position,
                pageCount = pageCount.takeIf { it > 0 } ?: uiState.value.pageCount,
            )

        val atEnd = progress.totalProgression >= 0.999
        if (atEnd) {
            lastLocator = null
            if (!markedPlayed) {
                markedPlayed = true
                lifecycleScope.launch { runCatching { repository.markAsPlayed(id) } }
            }
            appPreferences.sharedPreferences
                .edit()
                .remove(locatorKey(id))
                .remove("reader_page_count_$id")
                .apply()
            return
        }

        val locator = continuousLocator(progress, position, pageCount)
        if (locator != null) {
            lastLocator = locator
        }

        val now = SystemClock.elapsedRealtime()
        if (locator != null && (force || position != lastSavedPage || now - lastSavedAt > 750L)) {
            appPreferences.sharedPreferences
                .edit()
                .putString(locatorKey(id), locator.toJSON().toString())
                .apply()
            lastSavedPage = position
            lastSavedAt = now
        }
    }

    private fun continuousLocator(
        progress: ContinuousProgress,
        page: Int,
        pageCount: Int,
    ): Locator? {
        val publication = publication ?: return null
        val link = publication.readingOrder.getOrNull(progress.chapterIndex) ?: return null
        val base = publication.locatorFromLink(link) ?: return null
        return base.copy(
            locations =
                base.locations.copy(
                    progression = progress.chapterProgression,
                    position = page.takeIf { pageCount > 0 },
                    totalProgression = progress.totalProgression,
                )
        )
    }

    /**
     * Applies the page-width cap to the resource currently on screen. Readium's CSS property is
     * injected at creation, but changing the setting while the reader is open needs a live update.
     */
    private suspend fun applyMaxWidth(rem: Double, force: Boolean) {
        if (!force && rem == appliedMaxWidth) return
        appliedMaxWidth = rem
        val script =
            """
            (function() {
              var width = '${rem}rem';
              document.documentElement.style.setProperty('--RS__maxLineLength', width);
              if (document.body) {
                document.body.style.setProperty('max-width', width, 'important');
              }
            })();
            """
                .trimIndent()
        runCatching { epubNavigator?.evaluateJavascript(script) }
    }

    private fun storedLocator(id: UUID): Locator? {
        val json = appPreferences.sharedPreferences.getString(locatorKey(id), null) ?: return null
        return runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
    }

    private fun locatorKey(id: UUID) = "reader_locator_$id"

    private fun brightnessOverlay(brightness: Float): Float = (1f - brightness).coerceIn(0f, 0.8f)

    private fun readableError(error: Throwable): String =
        error.message ?: "This book could not be opened"

    override fun onPause() {
        continuousController.value?.lastProgress?.let { progress ->
            onContinuousProgress(progress, force = true)
        }
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideControls)
        locatorJob?.cancel()
        continuousController.value?.release()
        continuousController.value = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_BOOK_TITLE = "book_title"
        private const val EXTRA_AUDIOBOOK = "audiobook"
        private const val EXTRA_OVERVIEW = "overview"
        private const val EXTRA_START_FROM_BEGINNING = "start_from_beginning"
        private const val FRAGMENT_TAG = "reader_navigator_fragment"

        fun intent(
            context: Context,
            bookId: UUID,
            title: String,
            overview: String = "",
            audiobook: Boolean = false,
            startFromBeginning: Boolean = false,
        ): Intent =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId.toString())
                .putExtra(EXTRA_BOOK_TITLE, title)
                .putExtra(EXTRA_OVERVIEW, overview)
                .putExtra(EXTRA_AUDIOBOOK, audiobook)
                .putExtra(EXTRA_START_FROM_BEGINNING, startFromBeginning)
    }
}
