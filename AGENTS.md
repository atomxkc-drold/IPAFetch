AGENTS.md
Project
Dev Stash is a premium, privacy-first Android developer utility designed for securely storing and managing API keys, JSON snippets, and developer tokens using a strictly local, AES-256-GCM + PBKDF2 encrypted vault.
Build & run
./gradlew assembleDebug (to build the debug APK)
./gradlew test (to run unit tests)
./gradlew lint (to run standard Android lint checks)
Conventions
Language & UI: Use Kotlin and Jetpack Compose. Follow Material Design 3 guidelines, respecting the app's dynamic color palettes (Vibrant, Expressive, etc.) and pure black dark theme support.
Architecture: Use MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF). Keep UI state immutable.
Security Logic: Isolate all cryptography and key derivation (AES/PBKDF2) within dedicated security repositories or modules. Keep it decoupled from UI code.
Styling: Rely on existing theme variables for surface shading, corner radii (e.g., pill chips), and typography rather than hardcoding values.
Constraints
NO LOGGING SENSITIVE DATA: You must NEVER log vault contents, API keys, user passwords, or decrypted secrets to Logcat, console, or any crash reporting tool.
NO NETWORK LEAKS: Do not introduce any third-party SDKs, telemetry, analytics, or network requests that could compromise the local-first, zero-trust security model.
MEMORY MANAGEMENT: Never store decrypted vault items in global variables or persistent singletons. Clear decrypted secrets from memory as soon as they are no longer actively displayed or copied to the clipboard.
NO CLOUD SYNC: Do not write features for unencrypted cloud backups or syncing. All data must remain strictly on-device unless explicitly requested by the user via an encrypted export workflow.