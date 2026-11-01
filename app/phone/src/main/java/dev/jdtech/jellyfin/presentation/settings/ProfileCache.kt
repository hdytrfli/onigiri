package dev.jdtech.jellyfin.presentation.settings

import android.app.Activity
import android.content.Context
import coil3.SingletonImageLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.jdtech.jellyfin.swr.SwrStore
import dev.jdtech.jellyfin.utils.restart

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SwrEntryPoint {
    fun swrStore(): SwrStore
}

/** Clears user-scoped caches and relaunches the task so every ViewModel is rebuilt. */
fun Context.clearProfileCachesAndRestart() {
    val imageLoader = SingletonImageLoader.get(this)
    imageLoader.memoryCache?.clear()
    imageLoader.diskCache?.clear()
    cacheDir.resolve("books").deleteRecursively()
    EntryPointAccessors.fromApplication(applicationContext, SwrEntryPoint::class.java)
        .swrStore()
        .clear()
    (this as? Activity)?.restart()
}
