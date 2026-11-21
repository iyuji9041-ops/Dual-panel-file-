# DualFiles Material

<div align="center">
  <h3>Modern Material You Dual-Pane File Manager for Android</h3>
</div>

DualFiles Material is a fast, versatile dual-pane file manager built with Jetpack Compose and Material 3 design. It enables seamless side-by-side or top-and-bottom file management, drag-and-drop transfers, storage analytics, and archive operations.

---

## ✨ Features

- 📂 **Dual-Pane Interface**: Side-by-side (landscape/tablet) or top-and-bottom (portrait) dual-pane view for effortless file organization.
- 🔄 **Drag and Drop**: Real-time cross-pane drag-and-drop support for fast copying and moving.
- 🗜️ **Archive Management**: Built-in ZIP compression and extraction.
- 📊 **Storage Analytics**: Visualize internal and external storage usage with intuitive category breakdowns.
- 🎨 **Material You / Dynamic Theming**: Fully embraces Android Material 3 with dynamic colors and smooth transitions.
- 🛡️ **Safe & Secure**: Keystore and private credentials are kept strictly out of version control.

---

## 🚀 Building & Publishing

### Local Build (Debug)
```bash
./gradlew assembleDebug
```

### Release Build
```bash
KEYSTORE_PATH="/path/to/my-upload-key.jks" \
STORE_PASSWORD="your-store-password" \
KEY_ALIAS="upload" \
KEY_PASSWORD="your-key-password" \
./gradlew assembleRelease
```

### Automated CI/CD
This repository includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds and signs the release APK and publishes it to GitHub Releases upon tag creation or manual workflow dispatch.

---

## 📄 License
This project is licensed under the Apache 2.0 License.
