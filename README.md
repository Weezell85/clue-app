# Clue Companion

Clue Companion replaces the physical evidence cards used while playing **Clue**.

- `backend/` is a Spring Boot service that owns lobbies, deals cards, validates turns, and keeps private information private.
- `android/` is a Jetpack Compose client with a name/create/join landing screen and lobby experience.

Games accept 3–6 players. Only the host can start. One card of each category is selected as the solution and the rest are dealt. Suggestions proceed in turn order, reveals remain private, and an incorrect final accusation removes that player from taking turns while they remain able to reveal cards.

## Run

Run the backend with `cd backend && mvn spring-boot:run`. Open `android/` in Android Studio and run the app. The Android client uses `https://jubilant-doodle-4gjj5qx9pj7h7rwp-8080.app.github.dev/` as its base URL.

The in-memory service is intentionally ephemeral for this first version.

## Download a CI-built APK

The **Build Android APK** GitHub Actions workflow runs when Android files change on a push or pull request, and it can also be started manually from the Actions tab. After a successful run, download the `clue-companion-debug-apk` artifact from the workflow summary. The artifact contains `app-debug.apk` and is retained for 14 days.
