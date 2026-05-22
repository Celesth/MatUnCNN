# MatUnCNN

Android native image upscaler powered by ncnn. Uses AI models to upscale images with high quality on-device, with Vulkan GPU acceleration.

**Forked from** [Tumuyan/RealSR-NCNN-Android](https://github.com/tumuyan/RealSR-NCNN-Android)

## Features

- **AI Image Upscaling** — select from multiple ncnn models (Real-ESRGAN, etc.)
- **Vulkan GPU Acceleration** — automatic Vulkan detection; falls back gracefully
- **Progress Tracking** — real-time ETA/percentage from ncnn stdout
- **Output Preview** — view and compare inline; save or share the result
- **Result Caching** — in-memory LRU cache avoids redundant processing
- **Noexec Bypass** — runs binaries on noexec-mounted filesystems via `/proc/self/fd`
- **Dynamic Asset Download** — model files fetched from GitHub Releases on first launch
- **Debug Log** — 500-entry ring buffer with copy-to-clipboard and file save
- **Customizable** — tile size, thread count, save format/name, notification mode, custom command arguments
- **Material 3 UI** — dark theme, bottom navigation, scrollable settings tabs

## Requirements

- Android 8.0+ (API 26+)
- Vulkan-compatible GPU recommended (CPU fallback available)

## Installation

Download the latest APK from the [Releases](https://github.com/Celesth/MatUnCNN/releases) page and install it.

Model assets are downloaded automatically on first launch.

## Building

```shell
./gradlew assembleRelease -Dorg.gradle.java.home=/path/to/jdk-17
```

Requires JDK 17 and Android SDK (command-line tools).

## Usage

1. Launch the app — assets download on first run
2. Tap the image card to select an image
3. Choose a model from the dropdown
4. Tap **Run** to upscale
5. View the result in the output card; save or share from there

Tweak settings in the **Settings** tab: tile size, thread count, save format, naming scheme, custom arguments, and notification mode.

## Tech Stack

- **ncnn** — neural network inference framework
- **ImageMagick** — image pre/post processing
- **Jetpack Compose / Material 3** — UI
- **Coil** — image loading
- **DataStore** — preferences persistence

## License

MIT
