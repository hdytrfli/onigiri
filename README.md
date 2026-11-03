<p align="center">
  <img src="icons/findroid-plus.svg" width="120" alt="Findroid+ logo">
</p>

<h1 align="center">Findroid+</h1>

<p align="center">A native Jellyfin client for Android, with a built-in reader.</p>

<p align="center"><strong>1.0.0-beta.2</strong></p>

Findroid+ is an independent Android client for Jellyfin. It began as a fork of
[Findroid](https://github.com/jarnedemeulemeester/findroid) and continues its native-first
approach while adding a reading experience, a fuller profile system, and a more consistent
interface.

## What this fork adds

### Reading

- A dedicated Books tab for books and audiobooks
- In-app EPUB reading using Readium
- Reading progress and current position
- Hierarchical table of contents
- Pull-to-chapter navigation in scroll mode
- Reader themes, text alignment, hyphenation, font selection, and maximum line width
- Author pages and book metadata
- Book downloads for offline reading
- Search, sorting, and list-display options for books

### Profiles

- Guided first-time setup with server and account details
- Password or Quick Connect sign-in
- Optional four-digit profile PIN
- Profile picker with saved sessions and persistent logout
- Profile switching that clears artwork/query caches and reader state while keeping downloads
- Add and remove local profiles inside the app
- Admin-aware profile controls

### Interface

- Material 3 interface built with Jetpack Compose
- Adaptive bottom navigation and navigation rail
- Immersive detail pages and consistent page transitions
- Search, sorting, and display controls for media and book lists
- Shared dialogs, fields, buttons, and menus across screens
- Configurable grids and artwork ratios

## Library and playback

- Movies, series, books, and audiobooks from one Jellyfin server
- Native playback for movies and episodes
- The app does not transcode media itself; playback uses the server-provided stream
- mpv-based playback with bundled FFmpeg codecs
- Picture-in-picture mode
- Chapter markers and chapter navigation gestures
- Trickplay thumbnails on supported servers
- Embedded subtitles plus external SRT/ASS subtitle delivery
- Preferred audio and subtitle language selection

## Downloads and offline use

- Download movies, episodes, and books
- Select the storage location for downloads
- Background download progress for video
- Offline mode for browsing and playing downloaded content
- Download state on detail pages and progress for video downloads

## Requirements

- Android 9.0 or newer
- A Jellyfin server
- Network access to the server unless offline mode is enabled
- Jellyfin 10.9 or newer for trickplay

## Current limitations

- Audiobooks are listed in the library, but audiobook playback is not yet available.
- The app connects to one Jellyfin server at a time.
- The app does not transcode media itself; playback uses the server-provided stream.

## Links

- [Findroid+ on GitHub](https://github.com/hdytrfli/findroid)
- [Findroid, the upstream project](https://github.com/jarnedemeulemeester/findroid)
- [Jellyfin](https://jellyfin.org)

## License

Findroid+ is licensed under the GNU General Public License v3.0.

Findroid+ is an independent fork and is not affiliated with the Jellyfin project or the Findroid
project. The original Findroid project and its contributors remain the foundation of this work.
Playback, library browsing, and much of the application architecture originate there.

This project also depends on the Jellyfin Kotlin SDK, Readium, AndroidX Media3
(player UI and session), mpv, and other open-source libraries.
