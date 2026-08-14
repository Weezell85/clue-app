# Clue Companion

Clue Companion replaces the physical evidence cards used while playing **Clue**.

- `backend/` is a Spring Boot service that owns lobbies, deals cards, validates turns, and keeps private information private.
- `android/` is a Jetpack Compose client with a name/create/join landing screen and lobby experience.

Games accept 3–6 players. Only the host can start. One card of each category is selected as the solution and the rest are dealt. Suggestions proceed in turn order, reveals remain private, and an incorrect final accusation removes that player from taking turns while they remain able to reveal cards.

## Run

Run the backend with `cd backend && mvn spring-boot:run`. Open `android/` in Android Studio and run the app; an emulator connects to the host service at `http://10.0.2.2:8080/`.

The in-memory service is intentionally ephemeral for this first version.
