# 🧩 Material Theme Preview

**Material Theme Preview** is an IntelliJ plugin that enhances the Android and Kotlin development experience by providing real-time visual previews of Material Design components directly in your editor.

---

## ✨ Features

- 🎨 **Color Previews** — Inline color squares for `MaterialTheme.colorScheme.*`
- 🅰️ **Typography Previews** — Show font size and line height for `MaterialTheme.typography.*`
- 🧱 **Shape Previews** — Display shape corner radii from `MaterialTheme.shapes.*`
- 🧩 **Completion Enhancements** — Smarter autocompletion with color chips and style hints
- 💡 **Documentation Hints** — Quick inline docs for colors, typography, and shapes
- ⚡ **K2-Mode Compatible** — Fully supports Kotlin’s new compiler frontend

---

## 🖼️ Screenshots

| Color & Typography Inlay | Gutter Color Marker | Autocomplete |
|:--------------------------:|:-------------------:|:-------------:|
| ![color-preview](docs/color-preview.png) | ![gutter-icon](docs/gutter-icon.png) | ![completion](docs/completion.png) |

---

## 🧠 How It Works

Material Theme Preview scans your Compose or Android Kotlin files for `MaterialTheme` references, such as:

```kotlin
color = MaterialTheme.colorScheme.primary
style = MaterialTheme.typography.titleMedium
shape = MaterialTheme.shapes.small
