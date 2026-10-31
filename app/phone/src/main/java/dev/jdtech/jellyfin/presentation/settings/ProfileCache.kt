package dev.jdtech.jellyfin.presentation.settings

import android.app.Activity
import android.content.Context
import coil3.SingletonImageLoader
import dev.jdtech.jellyfin.utils.restart

/** Clears user-scoped caches and relaunches the task so every ViewModel is rebuilt. */
fun Context.clearProfileCachesAndRestart() {
    val imageLoader = SingletonImageLoader.get(this)
    imageLoader.memoryCache?.clear()
    imageLoader.diskCache?.clear()
    cacheDir.resolve("books").deleteRecursively()
    (this as? Activity)?.restart()
}
