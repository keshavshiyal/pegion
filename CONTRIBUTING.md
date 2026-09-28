# Contributing to Pegion 🕊️

First off, thank you for considering contributing to Pegion! It's people like you that make Pegion such a great tool for everyone.

Following these guidelines helps to communicate that you respect the time of the developers managing and developing this open source project. In return, they should reciprocate that respect in addressing your issue, assessing changes, and helping you finalize your pull requests.

---

## Code of Conduct

This project and everyone participating in it is governed by the expectation of positive, constructive, and respectful collaboration. Please be kind, courteous, and respectful to all contributors.

---

## How Can I Contribute?

### 1. Reporting Bugs
- **Search existing issues** to make sure the bug hasn’t already been reported.
- **Provide clear reproduction steps**, including:
  - Android OS version (e.g. Android 14 / OneUI 6).
  - Device model.
  - Expected vs actual behavior.
  - URL (if public) or type of server tested against.
  - Logcat output if applicable.

### 2. Suggesting Enhancements
- Explain why this enhancement would be useful to most Pegion users.
- Provide mockups, sketches, or reference implementations where possible.

### 3. Submitting Pull Requests
1. Fork the repo and create your branch from `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Keep your code clean, readable, and follow **Modern Android Development (MAD)** standards:
   - 100% Jetpack Compose for UI.
   - Kotlin Coroutines & Flow for asynchronous tasks.
   - Material 3 design tokens.
3. Verify that the app compiles and all unit tests pass:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Format and commit your changes following [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat: Add multi-segment parallel download support`
   - `fix: Handle HTTP 416 Range Not Satisfiable correctly`
   - `perf: Reduce SQLite write frequency during active downloads`
   - `docs: Update README with IzzyOnDroid badge`
5. Push to your fork and submit a Pull Request targeting `main`.

---

## Development Setup

- **IDE:** Android Studio Ladybug (2024.2+) or later.
- **JDK:** OpenJDK or Eclipse Temurin JDK 17+.
- **Android SDK:** Target API 36, Min API 26.
- Run tests via CLI:
  ```bash
  # Linux / macOS
  ./gradlew testDebugUnitTest

  # Windows PowerShell
  .\gradlew.bat testDebugUnitTest
  ```

Thank you for helping make Pegion the best open-source download manager on Android!
