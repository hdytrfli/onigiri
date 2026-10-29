package dev.jdtech.jellyfin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.api.JellyfinApi
import dev.jdtech.jellyfin.databinding.ActivityReaderBinding
import dev.jdtech.jellyfin.presentation.reader.ReaderOverlay
import dev.jdtech.jellyfin.presentation.reader.ReaderSettings
import dev.jdtech.jellyfin.presentation.reader.ReaderUiState
import dev.jdtech.jellyfin.presentation.reader.readerSettings
import dev.jdtech.jellyfin.presentation.reader.setReaderSettings
import dev.jdtech.jellyfin.presentation.reader.toEpubPreferences
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.image.ImageNavigatorFragment
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
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
@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : AppCompatActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    @Inject lateinit var jellyfinApi: JellyfinApi

    private lateinit var binding: ActivityReaderBinding

    private val uiState = mutableStateOf(ReaderUiState())
    private val settingsState = mutableStateOf(ReaderSettings())
    private val showSettings = mutableStateOf(false)

    private var bookId: UUID? = null
    private var navigator: Navigator? = null
    private var epubNavigator: EpubNavigatorFragment? = null
    private var locatorJob: Job? = null

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
        if (id == null) {
            finish()
            return
        }
        bookId = id

        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsState.value = appPreferences.readerSettings()
        binding.readerDim.alpha = brightnessOverlay(settingsState.value.brightness)

        binding.readerOverlay.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FindroidTheme {
                    ReaderOverlay(
                        state = uiState.value,
                        settings = settingsState.value,
                        showSettings = showSettings.value,
                        onBack = { finish() },
                        onOpenSettings = { showSettings.value = true },
                        onDismissSettings = { showSettings.value = false },
                        onSettingsChange = { settings -> updateSettings(settings) },
                    )
                }
            }
        }

        uiState.value = uiState.value.copy(title = title, loading = true)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (showSettings.value) {
                        showSettings.value = false
                    } else {
                        finish()
                    }
                }
            },
        )

        lifecycleScope.launch { openBook(id, title) }
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

    private fun downloadBook(id: UUID): File {
        val file = File(cacheDir, "books/$id")
        if (file.exists() && file.length() > 0) return file

        file.parentFile?.mkdirs()

        val baseUrl =
            jellyfinApi.api.baseUrl?.trimEnd('/') ?: throw IOException("No server configured")
        val requestBuilder =
            Request.Builder().url("$baseUrl/Items/$id/Download").apply {
                jellyfinApi.api.accessToken?.let { token -> header("X-Emby-Token", token) }
            }

        OkHttpClient().newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed with HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty response")
            file.outputStream().use { output ->
                body.byteStream().use { input -> input.copyTo(output) }
            }
        }
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
        val initialLocator = storedLocator(id)
        val epubPreferences = settingsState.value.toEpubPreferences()

        val fragmentClass: Class<out Fragment>
        val fragmentFactory: androidx.fragment.app.FragmentFactory

        when {
            publication.conformsTo(Publication.Profile.EPUB) -> {
                fragmentClass = EpubNavigatorFragment::class.java
                val configuration = EpubNavigatorFragment.Configuration {
                    // Serve the bundled font files and declare them, so the same families as
                    // the subtitle font setting can be selected for the book text.
                    servedAssets += "subtitle-fonts/.*"
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
        epubNavigator = fragment as? EpubNavigatorFragment

        uiState.value =
            uiState.value.copy(
                title = title,
                loading = false,
                error = null,
                progress = initialLocator?.progressLabel().orEmpty(),
            )

        locatorJob?.cancel()
        locatorJob = lifecycleScope.launch {
            navigator?.currentLocator?.collect { locator ->
                if (locator.href.toString().isEmpty()) return@collect
                appPreferences.sharedPreferences
                    .edit()
                    .putString(locatorKey(id), locator.toJSON().toString())
                    .apply()
                uiState.value = uiState.value.copy(progress = locator.progressLabel())
            }
        }
    }

    private fun updateSettings(settings: ReaderSettings) {
        settingsState.value = settings
        appPreferences.setReaderSettings(settings)
        binding.readerDim.alpha = brightnessOverlay(settings.brightness)
        epubNavigator?.submitPreferences(settings.toEpubPreferences())
    }

    private fun storedLocator(id: UUID): Locator? {
        val json = appPreferences.sharedPreferences.getString(locatorKey(id), null) ?: return null
        return runCatching { Locator.fromJSON(JSONObject(json)) }.getOrNull()
    }

    private fun locatorKey(id: UUID) = "reader_locator_$id"

    private fun brightnessOverlay(brightness: Float): Float = (1f - brightness).coerceIn(0f, 0.8f)

    private fun Locator.progressLabel(): String {
        val percent = locations.totalProgression?.let { "${(it * 100).roundToInt()}%" }
        return listOfNotNull(title, percent).joinToString(" · ")
    }

    private fun readableError(error: Throwable): String =
        error.message ?: "This book could not be opened"

    override fun onDestroy() {
        locatorJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_BOOK_TITLE = "book_title"
        private const val FRAGMENT_TAG = "reader_navigator_fragment"

        fun intent(context: Context, bookId: UUID, title: String): Intent =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId.toString())
                .putExtra(EXTRA_BOOK_TITLE, title)
    }
}
