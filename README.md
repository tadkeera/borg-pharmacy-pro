# Borg Pharmacy Pro

This workspace contains the re-architected Android implementation under `app/src/main/java/com/borgpharmacy/pro`.

## Architecture

- Room-backed offline-first entities and reactive DAO flows in `core/database`.
- Supabase Kotlin SDK with Ktor Android in `core/network`; URL, anon key, and sync token are Gradle properties/environment values.
- PBKDF2-HMAC-SHA256 password hashing with a random 128-bit salt and 120,000 iterations.
- Pure deterministic 1–4 visit cycle generation and non-destructive reconciliation in `domain/scheduler`.
- Canvas receipt rendering and ESC/POS `GS v 0` raster output in `core/printer`.
- Feature-scoped Compose screens and ViewModels in `ui/onboarding`, `home`, `weekly`, `companies`, `settings`, and `dashboard`.

## Configuration

Create `local.properties` or pass Gradle properties:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SYNC_TOKEN=your-sync-token
```

No production credentials are stored in source. The original public repository was cloned to `../borg-pharmacy-source`; the requested target repository was not anonymously cloneable, so this target was bootstrapped from that source tree.
