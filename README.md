# SmartStudent

A student-focused personal finance tracker for Android, built with Kotlin + Jetpack Compose.
Visual design is inspired by Oportun's onboarding flow (Mobbin reference): black/white/green
palette, full-width pill buttons, rounded outlined inputs and cards.

## Opening the project

1. Open this folder in Android Studio (Ladybug or newer recommended).
2. Let Gradle sync — it will pull Compose BOM 2024.12.01, Navigation Compose, Kotlin 2.0.21,
   and Firebase (Auth + Firestore) from Google's Maven + Maven Central.
3. Edit `local.properties` if needed to point `sdk.dir` at your Android SDK install.
4. Run on an emulator or device with API 26+.

## Firebase setup (required — auth and database won't work without this)

This project already includes `app/google-services.json` pointing at your Firebase project.
Two things to finish on your end, both free:

### 1. Enable sign-in providers (if not already done)
Firebase console → **Build → Authentication → Sign-in method** → enable **Email/Password**
and **Google**.

### 2. Register a SHA-1 fingerprint (required for Google Sign-In only — email/password
   works without this)
Google Sign-In needs a SHA-1 certificate fingerprint registered with your Firebase app,
which generates a `default_web_client_id` resource this app looks up at runtime. Until you
do this, the "Continue with Google" button will show a disabled note explaining why — it
won't crash the app.

To get your debug SHA-1:
```
cd SmartStudent
./gradlew signingReport
```
Look for the `SHA1` line under `Variant: debug`. Then in the Firebase console:
**Project settings → your Android app → Add fingerprint** → paste it in → save.
Re-download `google-services.json` and replace `app/google-services.json` with the new one
(the new file will include a populated `oauth_client` array).

### 3. Firestore security rules
The database was created in **test mode**, which is open to anyone for 30 days — fine for
development, not for shipping. Before release, go to **Firestore → Rules** and restrict
access to signed-in users' own data, e.g.:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Gemini API setup (required for bank statement scanning)

This project already has a Gemini API key wired in via `local.properties` →
`BuildConfig.GEMINI_API_KEY` (used by `GeminiRepository`). If you ever need to
replace it: get a free key from **[aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)**
(no credit card required), then edit `local.properties`:
```
gemini.api.key=YOUR_KEY_HERE
```
This file is gitignored and never committed. Note this is a client-side key with no
backend proxy — fine for a personal/school project, but a real production app should
route Gemini calls through a server so the key isn't embedded in the shipped APK.

## Architecture

```
com.example.smartstudent
├── MainActivity.kt          # Entry point, sets Compose content
├── Navigation.kt            # NavHost wiring every screen together
├── NavigationKeys.kt        # Centralized route constants
├── data/remote/             # Firestore DTOs (TransactionDto, SavingsGoalDto)
├── data/repository/         # AuthRepository, UserRepository, TransactionRepository, GoalRepository
├── domain/model/            # Plain data models: User, Transaction, SavingsGoal, HabitMetric, CategoryRule
├── theme/                   # Color.kt, Type.kt, Shape.kt, Theme.kt — design tokens
├── ui/components/           # Reusable pieces: buttons, text fields, cards
├── ui/onboarding/           # Welcome → AuthScreen (sign up/log in + Google) → email verify → notifications
├── ui/main/                 # AppViewModel (real Firebase-backed state), AppViewModelFactory, MainScaffold
├── ui/dashboard/            # Home screen: balance, recent transactions
├── ui/transactions/         # Full transaction list, grouped by date
├── ui/goals/                # Goals list + "what do you want to save for" type picker
└── ui/ingestion/            # Manual transaction entry form
```

## State

`AppViewModel` now reads/writes real data via `data/repository/`:
- Auth state comes from Firebase Authentication
- Transactions and goals are stored per-user in Firestore under `users/{uid}/...`
- Habit-insight metrics are not yet computed from real data (still returns an empty list
  on the Dashboard) — a good next feature to build once there's enough transaction history.

## Known placeholders / next steps

- Habit-insight metrics aren't computed from real transaction data yet.
- "Learn about Smart bills" and goal detail screens are not yet built.
- Goal creation currently reuses the ingestion form; a dedicated goal builder (amount, due date,
  emoji picker) is a good next iteration.
- Firestore is fetched once per screen load (no live listeners yet) — fine for now, upgrade to
  `addSnapshotListener` + `callbackFlow` for real-time updates later.
- Firestore security rules are in open test mode — lock down before shipping (see above).
- Launcher icon is a placeholder vector mark, not final branding.
- No Room/offline-first layer — app requires an internet connection (per your request to keep
  this simple for now; can be layered in later per the original spec).

## Iteration log

See `CHANGELOG.md`.
