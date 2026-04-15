# Copilot Instructions

## Repository Overview

**ZeroProbe** is a Maven-based JavaFX 21 desktop application that provides a lightweight monitoring interface for embedded Linux devices. It connects via SSH or serial port to collect CPU, memory, and process data by reading the /proc filesystem. Features include real-time charts, data recording to CSV, internationalization (i18n), theme management (AtlantaFX), and user preferences (PreferencesFX). The project targets Java 21 on Ubuntu, Windows, and macOS.

- **Language:** Java 21
- **Build tool:** Maven 3.9+

## Commit Message Convention

**All commits must follow the Angular Commit Message Convention:**

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

Valid types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

Examples:
- `feat(ssh): add SSH key-based authentication`
- `fix(parser): correct CPU usage calculation`
- `docs: update README with new build steps`
- `chore(deps): upgrade jsch to 0.2.25`

## Build & Validation

Always use Java 21 (Temurin recommended). Set `JAVA_HOME` if needed.

```bash
# Build and run all tests (required before any PR)
mvn -B clean verify

# Build only (skip tests, produces target/zeroprobe.jar)
mvn -B clean package -DskipTests

# Run the application (requires a display)
mvn javafx:run

# Run tests only
mvn test
```

- `mvn clean verify` is the canonical CI command. Always run it before submitting changes.
- Build output is `target/zeroprobe.jar` + `target/lib/` (all runtime deps). Both are required to run the JAR.
- The JAR manifest entry point is `com.tlcsdm.zeroprobe.Launcher`; `mvn javafx:run` also uses `Launcher`.
- There is no linting step beyond Java compilation (`-Xlint:-module` suppresses module-related warnings).

## Project Layout

```
.github/
  CODEOWNERS                 # Code ownership assignments
  dependabot.yml             # Weekly Dependabot updates for Maven + GitHub Actions
  workflows/
    test.yml       # CI: mvn -B clean verify on push/PR to master (Ubuntu, Windows, macOS)
    package.yml    # Manual: build + stage JAR/lib/scripts/JRE, uploads per-OS artifact
    release.yml    # On release created: packages per-OS ZIP and uploads to GitHub release
    app-image.yml  # Manual: build Windows app image via jpackage

scripts/
  jre.sh           # Linux: downloads JDK 21, builds custom JRE via jlink
  jre_mac.sh       # macOS: same as jre.sh for macOS
  jre.ps1          # Windows: same as jre.sh for Windows (PowerShell)
  package.sh       # Linux/macOS: stages JAR, libs, scripts, and JRE into staging/
  package.ps1      # Windows: stages JAR, libs, scripts, and JRE into staging/
  app-image.ps1    # Windows: builds app image via jpackage
  linux/start.sh   # Linux launcher script (bundled in staging)
  mac/start.sh     # macOS launcher script (bundled in staging)
  win/             # Windows launcher scripts: start.bat, console.bat, start.vbs

pom.xml            # Single-module Maven project (groupId=com.tlcsdm, artifactId=zeroprobe)

src/main/java/com/tlcsdm/zeroprobe/
  Launcher.java                   # Main class for JAR (delegates to ZeroProbeApplication)
  ZeroProbeApplication.java       # JavaFX Application: loads main.fxml, wires i18n/theme/prefs
  config/
    AppSettings.java              # Preferences (PreferencesFX) – persists theme/locale/user prefs
    AppTheme.java                 # Theme enum and application logic (AtlantaFX Primer/Nord, light/dark)
    I18N.java                     # ResourceBundle helper – supports en, zh, ja; call I18N.get(key)
  connection/
    ConnectionProvider.java       # Transport abstraction interface
    SshConnectionProvider.java    # SSH implementation (JSch)
    SerialConnectionProvider.java # Serial port implementation (jSerialComm)
  controller/
    MainController.java           # FXML controller wired to main.fxml
  export/
    DataExporter.java             # Export abstraction interface
    CsvExporter.java              # CSV export implementation
  model/
    DisplayLocale.java            # Wraps Locale for ComboBox display
  parser/
    CpuParser.java                # Parses /proc/stat for CPU usage
    MemParser.java                # Parses /proc/meminfo for memory usage
    ProcessParser.java            # Parses /proc/[pid]/stat and /proc/[pid]/status
  service/
    MonitoringService.java        # Scheduled polling service for device metrics

src/main/resources/com/tlcsdm/zeroprobe/
  main.fxml                       # Main UI layout (loaded by ZeroProbeApplication)
  i18n/
    messages.properties           # English (default)
    messages_zh.properties        # Chinese (Simplified)
    messages_ja.properties        # Japanese
  logback.xml                     # Logback configuration (src/main/resources/logback.xml)

src/test/java/com/tlcsdm/zeroprobe/
  config/AppThemeTest.java        # Unit tests for AppTheme
  config/I18NTest.java            # Unit tests for I18N
  model/                          # Unit tests for model classes

.gitignore         # Ignores: target/, .idea/, .vscode/, *.class, staging/, dist/
```

## CI Checks

The **Test** workflow (`.github/workflows/test.yml`) runs on every push and pull request to `master`:

1. Checks out the code
2. Sets up Temurin JDK 21
3. Caches `~/.m2/repository`
4. Runs `mvn -B clean verify --no-transfer-progress`

The build must pass on Ubuntu, Windows, and macOS. Replicate locally with:

```bash
mvn -B clean verify --no-transfer-progress
```

## Code Comments and Documentation Style

- Write natural, concise text that reads like a developer wrote it — no AI tone.
- Avoid excessive politeness, over-explanation, or listing too many options.
- Use direct wording and short sentences.
- Code and comments must be in English.
- PR titles, descriptions, and result summaries should be in Chinese.
- PR replies should be in Chinese.

## Key Facts

- The `Launcher` class must remain the JAR manifest entry point; do **not** change the manifest `mainClass` or the classpath-based launch will fail.
- The packaging produces `target/zeroprobe.jar` + `target/lib/`; both must be kept together to run the application.
- When adding i18n keys, add them to **all three** properties files (`messages.properties`, `messages_zh.properties`, `messages_ja.properties`).
- Tests do **not** start the JavaFX runtime; keep unit tests headless. UI changes must be tested manually.
- `AppSettings` uses PreferencesFX and persists data to the OS preferences store; it is a singleton accessed via `AppSettings.getInstance()`.
- All device data is collected via `/proc` filesystem (zero agent on device).
- `ConnectionProvider` is the transport abstraction; SSH and Serial implementations exist.
- Trust these instructions first; only search the codebase if something here appears incomplete or incorrect.
