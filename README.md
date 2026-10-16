# Excel Manager (Android)

Upload an Excel sheet, store and analyze it offline, edit it, search/filter it, and export it back in the original layout.
Kotlin · Jetpack Compose (Material 3) · MVVM + Repository · Hilt · Room · Apache POI · Storage Access Framework (no storage permissions).

> **Status:** the source was written without an Android SDK available, so it has **not been compiled or run**. Expect to fix a few small compile errors on first sync. The APK must be built on your machine (steps below).

## Build the APK (Android Studio — easiest)
1. Install Android Studio (Ladybug or newer, JDK 17+ is bundled).
2. **File ▸ Open** and choose this folder. Let Gradle sync (it downloads Gradle 8.9 and the SDK 35 platform when prompted).
3. **Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)** gives a *debug* APK:
   `app/build/outputs/apk/debug/app-debug.apk`
4. For the signed *release* APK use **Build ▸ Generate Signed App Bundle / APK ▸ APK**, or the command line below.

## Build from the command line
Generate the Gradle wrapper once (needs Gradle installed, or just use Android Studio's terminal after sync):
```
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # -> app/build/outputs/apk/release/app-release.apk  (signed, R8-minified)
./gradlew test               # unit tests: parser, filter logic, validator, analyzer
```
`assembleRelease` signs automatically using `keystore.properties` + `release.keystore` (included).
If either file is missing the release build is produced **unsigned** (`app-release-unsigned.apk`), which phones will not install; use debug instead.

### Signing key
A keystore is already generated for personal sideloading (password `excelmgr2026`, alias `excelmanager`). To make your own:
```
keytool -genkeypair -v -keystore release.keystore -alias excelmanager \
  -keyalg RSA -keysize 2048 -validity 10000
```
then put the values in `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). The signing config lives in `app/build.gradle.kts`.
**Do not publish to Play Store with this keystore or commit real passwords**; create a private one and keep it safe — losing it means you cannot update the app.

## How to install the APK on my phone
1. Copy `app-release.apk` (or `app-debug.apk`) to the phone via USB, Google Drive or WhatsApp.
2. Settings ▸ allow **Install unknown apps** for the file manager or browser you will open it from.
3. Tap the APK and choose **Install** (Android 8.0 / API 26 or newer).
4. Open **Excel Manager**, tap **Upload Excel**, and choose `sample/sample-employees.xlsx` (copy it to the phone too). Try Analysis, Search (e.g. salary greater than 1000000, hired after 2022-01-01), edit a row, then **Export / Sync**.

## Architecture
```
ui/        Compose screens (Files, Data, Analysis, Search) + single MainViewModel (StateFlow)
domain/    Col, RowItem, FilterEngine, Analyzer, Validator   (pure Kotlin, unit-tested)
data/
  excel/   ExcelParser (header + type detection), ExcelExporter (format-preserving write-back)
  db/      Room: FileEntity, SheetEntity, ColumnEntity, RowEntity
  repo/    ExcelRepository, SelectionStore (persists selection across process death)
di/        Hilt module
```
**Room schema:** `FileEntity(id, name, sourceUri, storedName, uploadedAt)` → `SheetEntity(id, fileId, name, orderIndex, headerRowIndex, rowCount)` → `ColumnEntity(id, sheetId, position, name, type)` and `RowEntity(id, sheetId, rowIndex, cellsJson)`; foreign keys cascade on delete.

**Format preservation:** on upload the original file is copied into app storage. Export re-opens that copy, keeps the sheets, header row, column widths and styles, clears the data rows, and rewrites them from Room using each column's original cell style and detected type. It then writes to the original document through SAF; if write access is denied you are asked to save a new copy.

## Implemented vs. not implemented
Implemented: multi-file upload and switching, multi-sheet parsing, header/type detection, analysis cards (counts, empties, duplicates, numeric stats, top values), add/edit/delete with type validation, export/sync, auto-sync toggle, debounced search, text/number/date filters, combined filters, sorting, highlighting, result count, clear filters, horizontally scrollable table, light/dark theme.
**Not implemented:** merge-import by key column, charts (progress bars only), repository/instrumented tests, search/filter state surviving process death (selected file/sheet and auto-sync do survive).

## Known limitations / things to check first
- **Apache POI on Android** is the riskiest part. It relies on the Aalto StAX setup in `ExcelApp.kt` and broad R8 keep rules in `proguard-rules.pro`; if the release build crashes on import, test the debug build first, then loosen R8 rules. The APK is large (~30 MB+) because POI is kept whole.
- Date-times are reduced to dates; cells with errors read as blank; formulas are written back as their values; charts, images and merged cells beyond the data area are left as in the original but rows are rewritten sequentially.
- Header row is assumed to be the first all-text row in the first 10 rows; columns beyond the header width are ignored.
- All rows of the current sheet are held in memory for filtering (fine for tens of thousands of rows).
