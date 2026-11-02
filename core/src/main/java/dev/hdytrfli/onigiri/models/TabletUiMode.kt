package dev.hdytrfli.onigiri.models

/** Where the navigation suite is drawn: the vertical rail on wide layouts or the bottom bar. */
enum class TabletUiMode(val storageKey: String) {
    AUTOMATIC("auto"),
    ALWAYS("always"),
    LANDSCAPE("landscape"),
    NEVER("never");

    companion object {
        fun fromStorageKey(key: String?): TabletUiMode =
            entries.firstOrNull { it.storageKey == key } ?: AUTOMATIC
    }
}
