<div align="center">

# 🎮 Wrestling Empire Save Editor

**A powerful Android save editor for Wrestling Empire — edit every stat, every attribute, every wrestler. Right from your phone.**

[![Release](https://img.shields.io/github/v/release/Mrakbrlalhzar/Wrestling-empire-patcher?style=for-the-badge&color=FFC857&labelColor=0E1116)](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/releases)
[![Platform](https://img.shields.io/badge/Platform-Android%208.1%2B-4DD0E1?style=for-the-badge&labelColor=0E1116)](https://www.android.com/)
[![Made with Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-CE93D8?style=for-the-badge&labelColor=0E1116)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-FF6B6B?style=for-the-badge&labelColor=0E1116)](LICENSE)

**Made with ♥ in Iran 🇮🇷**

[Download Latest APK](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/releases/latest) · [Report a Bug](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/issues) · [Request a Feature](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/issues)

</div>

---

## 📖 About

Wrestling Empire stores all wrestler data in a `Save.bytes` file using **.NET BinaryFormatter** serialization. Tweaking it usually meant pulling the file to a PC, running Node.js scripts, and manually editing a 50MB JSON file with a text editor.

**Not anymore.**

This app brings the entire workflow to your phone — parse the save file, edit stats, attributes, and even bulk-modify entire rosters in a few taps. All the heavy lifting (byte-level binary parsing, offset mapping, patching) happens right on your device. No PC. No Termux. No `nano`. Just you and your phone.

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎯 Stat Editing
- Edit **all 9 stats** of any wrestler
- One-tap **⚡ All stats → -1000** for the entire roster
- One-tap **⚡ All stats → 1000** for your favorite
- Per-stat manual editing with type-aware input (Int vs Float)

### 📊 Attribute Editing
- **age**, **height**, **armMass**, **muscleMass**, **bodyMass**, **legMass**
- Same one-tap and manual editing support

</td>
<td width="50%">

### 🔍 Search & Filter
- Real-time search by wrestler name
- Case-insensitive matching

### ☑️ Multi-Select
- Checkboxes on every character
- **✓ Select All** visible wrestlers
- **⇄ Invert** selection
- **Bulk edit** for many characters at once

</td>
</tr>
<tr>
<td width="50%">

### 💾 Smart Saving
- **Save** — overwrite the original `Save.bytes` file
- **Save As** — create a copy (SAF picker)
- Automatic reload after save so you see changes instantly
- Fallback to Save As if the provider denies write access

</td>
<td width="50%">

### 🎨 Beautiful UI
- Persian-inspired dark theme
- Gold & turquoise palette
- Material 3 design language
- Custom app icon
- Fully in **English**

</td>
</tr>
</table>

---

## 📱 Screenshots

<div align="center">

| Character List | Character Detail | Bulk Edit |
|:---:|:---:|:---:|
| <img src="docs/screenshots/list.png" width="240"/> | <img src="docs/screenshots/detail.png" width="240"/> | <img src="docs/screenshots/bulk.png" width="240"/> |

| Search & Select | About & Guide | Empty State |
|:---:|:---:|:---:|
| <img src="docs/screenshots/search.png" width="240"/> | <img src="docs/screenshots/about.png" width="240"/> | <img src="docs/screenshots/welcome.png" width="240"/> |

</div>

> 📸 *Screenshots coming soon. Want to help? Open a PR with your own!*

---

## 🚀 Installation

### Requirements
- Android **8.1 (Oreo)** or newer
- ~10 MB free storage

### Steps
1. Head to the [**Releases**](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/releases) page
2. Download the latest `SaveEditor-vX.X.X.apk`
3. Allow installation from unknown sources if prompted
4. Install & open the app

> 💡 **Note:** The APK is signed with the debug keystore. If you're upgrading from an older build, install over it (no need to uninstall).

---

## 🎮 How to Use

### Step 1 — Find your save file
Locate `Save.bytes` on your device. Common locations:
```
/Android/data/com.MDickie.WrestlingEmpire/files/Save.bytes
/storage/emulated/0/Android/data/com.MDickie.WrestlingEmpire/files/Save.bytes
```

> ⚠️ **Backup first!** Copy `Save.bytes` to another folder before editing. The app is safe, but games can behave weirdly when you nerf the entire roster.

### Step 2 — Open the file
1. Launch the app
2. Tap **📂 Pick file**
3. Navigate to `Save.bytes` and select it
4. Wait for parsing (usually 1–3 seconds)

### Step 3 — Edit
**Option A: Edit everyone at once**
- Tap **⚡ All stats → -1000.0** to zero out every wrestler in the game

**Option B: Buff yourself**
- Search for your character's name
- Tap it → tap **⚡ All stats of this character → 1000.0**

**Option C: Surgical editing**
- Tap any character
- Tap **Edit** next to any stat or attribute
- Enter the new value → OK

**Option D: Bulk edit selected**
- Check the box next to multiple wrestlers
- Tap **⚡ N selected → Bulk edit**
- Fill in stat and/or extra field values
- Tap **Apply**

### Step 4 — Save
- Tap **💾 Save** to overwrite the original file
- Or tap **Save As** to create a `.patched` copy

### Step 5 — Load in game
- Open Wrestling Empire
- Load your save
- Enjoy your new stats ✨

---

## ⚙️ How It Works

Wrestling Empire uses **.NET `BinaryFormatter.Serialize()`** to store its save data. That's a binary format with typed records:

- `SerializationHeader` → root object id
- `ClassWithMembersAndTypes` → typed class definition
- `ClassWithId` → cached class instance
- `BinaryObjectString` → length-prefixed string
- `BinaryArray` / `ArraySinglePrimitive` / `ArraySingleString` → arrays
- `MemberReference` → back-references to other objects

The app:

1. **Parses** the entire binary file into a typed in-memory tree (`BinValue` sealed class)
2. **Extracts** the fields you care about — `name`, `stat[]`, `age`, `height`, etc. — along with their **byte offset** and **length** in the original file
3. Shows them in a clean UI
4. On save, **patches only the modified offsets** in a byte-for-byte copy of the original
5. Writes the result back — preserving 100% of the file structure

This means:
- ✅ No risk of corrupting fields you didn't touch
- ✅ No need to rebuild the entire binary
- ✅ Works with any version of the save format the parser understands
- ✅ NaN/Infinity float values are preserved correctly

---

## 🛠️ Tech Stack

| Layer | Tech |
|---|---|
| **Language** | Kotlin 1.9 |
| **UI** | Jetpack Compose + Material 3 |
| **Binary parsing** | Custom `ByteBuffer` (little-endian) |
| **Architecture** | MVVM (ViewModel + Compose state) |
| **File I/O** | Android Storage Access Framework (SAF) |
| **Build** | Gradle 8.5 + GitHub Actions |
| **Min SDK** | 27 (Android 8.1) |
| **Target SDK** | 34 (Android 14) |

---

## 🏗️ Project Structure

```
.
├── .github/workflows/
│   └── release.yml              # Build + release automation
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/savetool/
│       │   ├── MainActivity.kt   # Compose UI
│       │   ├── SaveModel.kt      # Binary tree data model
│       │   ├── SaveParser.kt     # BinaryFormatter parser
│       │   ├── SavePatcher.kt    # Byte-level patcher
│       │   ├── SaveSession.kt    # High-level session API
│       │   ├── SaveViewModel.kt  # UI state
│       │   └── Theme.kt          # Material 3 theme
│       └── res/
│           ├── drawable/         # Vector icons
│           └── mipmap-*/         # App icons
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🐛 Known Limitations

- **ObjectArray** records are not yet implemented (uncommon in `Save.bytes`)
- **Very large saves** (>100 MB) may hit memory limits on low-end devices
- **Cloud providers** (Google Drive, OneDrive) sometimes deny direct writes → use **Save As**
- **Encrypted saves** (if the game ever adds encryption) won't parse

---

## 🗺️ Roadmap

- [ ] Export full save as `clean.json` (like the Node.js reference dump)
- [ ] Editor for **companies**, **Maidan**, **install date**, and other root-level fields
- [ ] Batch import: apply a JSON patch to a save file
- [ ] Save history / undo
- [ ] Dark & light theme toggle
- [ ] Turkish / Persian localization

Want a feature? [Open an issue](https://github.com/Mrakbrlalhzar/Wrestling-empire-patcher/issues).

---

## 🤝 Contributing

Contributions are welcome! Whether it's:

- 🐛 **Bug reports** — open an issue with steps to reproduce
- ✨ **Feature requests** — describe what you'd like
- 🌍 **Translations** — add your language to `strings.xml`
- 📸 **Screenshots** — PR them to `docs/screenshots/`
- 💻 **Code** — fork, branch, PR

Please keep PRs focused and add a short description of the change.

---

## ❤️ Credits

**Created by [Mrakbrlalhzar](https://github.com/Mrakbrlalhzar)**

Original workflow (Termux + Node.js) → Android app — full port of:
- `csharp-assembly-dump.js` → `SaveParser.kt`
- `patch-save.js` → `SavePatcher.kt`

**Special thanks:**
- [Mat Dickie](https://www.mdickie.com/) — for making Wrestling Empire (and never patching it, which keeps us busy 😄)
- The **.NET BinaryFormatter** spec — still the best (and worst) source of headaches
- Everyone who tests, reports bugs, and keeps this project alive

---

## 📄 License

```
MIT License

Copyright (c) 2026 Mrakbrlalhzar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

**⭐ If this app helped you, drop a star — it means a lot! ⭐**

**Made with ♥ in Iran 🇮🇷**

`0x3b45` — that's where the magic begins.

</div>
