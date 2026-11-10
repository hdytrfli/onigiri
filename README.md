<p align="center">
  <img src="icons/findroid-plus.svg" width="120" alt="Onigiri logo">
</p>

<h1 align="center">Onigiri</h1>

<p align="center">
  <a href="https://onigiri-project.vercel.app">Onigiri</a> A native Jellyfin client for Android, with a built-in reader.
</p>

<p align="center"><strong>1.0.0-beta.4</strong></p>

Onigiri is an independent Android client for Jellyfin. It began as a fork of
[Findroid](https://github.com/jarnedemeulemeester/findroid) and continues its native-first
approach while adding a reading experience, a fuller profile system, music playback, and a more
consistent interface.

## Reading

- Dedicated Books tab for books and audiobooks
- In-app EPUB reading using Readium
- Reading progress and current position
- Hierarchical table of contents
- Pull-to-chapter navigation in scroll mode
- Reader themes, text alignment, hyphenation, font selection, and maximum line width
- Precise reading-position seeking
- Author pages and book metadata
- Book downloads for offline reading
- Search, sorting, and list-display options for books

## Music

- Dedicated Music tab with artists, albums, songs, and favorites
- Full music player with cover and vinyl-disc artwork modes
- Swipe artwork to change tracks; rotating disc shared across visible artwork
- Reorderable queue page with search and jump-to-item
- Shuffle and repeat modes, including repeat-one
- Favorite tracks directly from the player and queue
- Hold-to-speed-up playback with the same long-press threshold as video
- Background playback with a persistent mini-player

## Video playback

- mpv-based playback with bundled FFmpeg codecs
- Native playback for movies and episodes without app-side transcoding
- Picture-in-picture mode
- Double-tap seeking with tap-versus-hold precision
- Exact and keyframe seek modes
- Long-press speed boost and configurable playback speed
- Volume, brightness, and zoom gestures with gesture locking
- Chapter markers, next/previous item controls, and unavailable-action feedback
- Trickplay thumbnails on supported servers
- Embedded subtitles plus external SRT/ASS subtitle delivery
- Subtitle styling, per-item subtitle offset, and audio/subtitle track selection
- Playback statistics overlay for stream, transcode, codec, and dropped-frame details

## Profiles and onboarding

- Guided first-time setup with server and account details
- Password or Quick Connect sign-in
- Optional four-digit profile PIN
- Profile picker with saved sessions and persistent logout
- Profile switching that clears artwork/query caches and reader state while keeping downloads
- Add and remove local profiles inside the app
- Admin-aware profile controls

## Library and browsing

- Movies, shows, books, audiobooks, and music from one Jellyfin server
- Home, Movies, Shows, Books, and Music tabs with adaptive navigation
- Collections, genres, suggestions, favorites, and downloads
- Scoped search, sorting, and per-list display controls
- Configurable grids, artwork ratios, and content density

## Downloads and offline use

- Download movies, episodes, and books
- Select the storage location for downloads
- Background download progress for video
- Offline mode for browsing and playing downloaded content
- Download state on detail pages and progress for video downloads

## Interface

- Material 3 interface built with Jetpack Compose
- Dark theme with shared color, spacing, button, dialog, and list components
- Overlay top bars with consistent safe-area and content insets across pages
- Immersive detail pages and consistent page transitions
- Reader, video, music, subtitle, download, and interface settings in one settings area

## Requirements

- Android 9.0 or newer
- A Jellyfin server
- Network access to the server unless offline mode is enabled
- Jellyfin 10.9 or newer for trickplay

## Current limitations

- The app connects to one Jellyfin server at a time.
- The app does not transcode media itself; playback uses the server-provided stream.

## Links

- [Onigiri on GitHub](https://github.com/hdytrfli/onigiri)
- [Findroid, the upstream project](https://github.com/jarnedemeulemeester/findroid)
- [Jellyfin](https://jellyfin.org)

## Thanks to the Findroid project

Onigiri would not exist without the work of
[Jarden Demeulemeester](https://github.com/jarnedemeulemeester) and every contributor to the
[Findroid](https://github.com/jarnedemeulemeester/findroid) project.

The upstream project built the foundation this app still relies on: the Jellyfin integration,
library browsing, playback architecture, downloads, offline behavior, and much of the Android
and Compose structure. Their years of work made it possible to focus this fork on reading,
profiles, music, and interface refinements instead of starting from zero.

Thank you to Jarden and to all Findroid contributors, past and present. Your effort and generosity
in sharing the project under an open-source license are the reason Onigiri can exist.

## License

Onigiri is licensed under the GNU General Public License v3.0.

Onigiri is an independent fork and is not affiliated with the Jellyfin project or the Findroid
project. The original Findroid project and its contributors remain the foundation of this work.
Playback, library browsing, music, and much of the application architecture originate there.

This project also depends on the Jellyfin Kotlin SDK, Readium, AndroidX Media3
(player UI and session), mpv, and other open-source libraries.
