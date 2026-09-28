# Changelog

## v8 — Auto-login, monthly allowance input, edit/delete transactions

**Auto-login**
- The app used to always start at the Welcome screen, forcing a fresh login every
  time even though Firebase Auth already remembers your session. Fixed with a new
  `SplashScreen` (shown briefly on launch) + `AppViewModel.tryAutoLogin()`, which
  checks for an existing signed-in, verified user and routes straight to the
  Dashboard (loading their profile/transactions/goals first). Unverified sessions go
  to the email-verify screen; no session goes to Welcome as before.
  New `NavigationKeys.SPLASH` as the actual `NavHost` start destination.
- `navigateToMainGraph()` / `navigateToWelcomeGraph()` rewritten to clear the entire
  back stack via `popUpTo(graph.id)` instead of a specific route, since the app can
  now enter the main graph from more than one place (splash vs. normal login).

**Monthly allowance**
- This was previously unreachable — the allowance card only rendered *if* an
  allowance was already set, with no UI anywhere to set one. Fixed: Insights now
  always shows either the allowance card (tap to edit) or a "Set a monthly
  allowance" prompt (tap to set for the first time), both opening the same
  `SetAllowanceDialog`.

**Transactions**
- Every row in the Activity list is now tappable, opening an edit dialog (type
  toggle, title, amount, category dropdown) with **Save** and **Delete** — no more
  no-fix-possible typos or bad statement-scan reads.
  New `AppViewModel.updateTransaction()` / `deleteTransaction()`, and
  `TransactionRepository.update()`.

**Security**
- Cleared the (now-revoked) leaked Gemini key out of this project's `local.properties`
  — the field is blank; paste your new key in before building.

## v7 — Logout button, fixed dark-mode contrast bug

**Added**
- Logout button (top-right of the Dashboard) — signs out of Firebase, clears all
  in-memory state (`AppViewModel.logOut()`), and returns to the Welcome screen with
  the entire app back stack cleared so there's no way to swipe/back into stale data.

**Fixed**
- Real contrast bug: the app auto-followed the device's system dark mode setting,
  switching to a dark scheme (near-black background, light/cream text). But several
  cards — the "Recommended habits" tip, the Dashboard balance card, the Insights
  allowance card, and others — use a fixed **light** beige/gold background regardless
  of theme. In dark mode, that meant near-white text sitting directly on a bright
  light-beige card: exactly the "beige background too bright against the white text"
  issue reported.
  Fix: `SmartStudentTheme` no longer follows `isSystemInDarkTheme()` — the app always
  uses the light (beige background, dark text) scheme, which is what the whole visual
  design (all those light-tinted accent cards) was actually built against. The dark
  color scheme still exists in `Theme.kt` for future use, just not auto-selected.

## v6 — Categorized activity filters, goal descriptions, centered insights, AI habit tips

**Activity / Transactions**
- New filter chips at the top: **All, Income, Expenses**, plus a dynamic chip for
  every category that's actually appeared in your transactions (from manual entries
  or statement scans) — tap any chip to filter the list
- Manual "Add transaction" now has:
  - An **Income / Expense toggle** (previously hardcoded to expense-only)
  - A **category dropdown** (`CategoryDropdown`, Material3 `ExposedDropdownMenuBox`)
    instead of a free-text field, pre-populated with built-in categories + anything
    you've used before, but still lets you type a new one
  - **Local auto-suggestion**: as you type a title like "Uber" or "Checkers", the
    category field auto-fills using a new keyword rule set (`DefaultCategoryRules`)
    — no Gemini call needed for this, keeps it instant and free of rate limits
- New `TransactionTypeToggle` component

**Goals**
- `SavingsGoal` now has a `description` field (added to Firestore DTO too)
- New dedicated `GoalCreateScreen` (name, target amount, description) — replaces
  the old placeholder that reused the transaction-entry form
- Goal cards are now **tap-to-expand**: tapping a card with a description toggles
  it open/closed (animated), with a chevron indicating state. No separate "goal
  detail" screen — this replaces that half-built stub entirely.

**Insights**
- **Fixed the off-center pie chart** — it was a real layout bug: the donut+legend
  Row was never centered within the screen width, so it hugged the left edge.
  Redesigned `CategoryPieChart` as a centered vertical layout (donut on top, legend
  below) rather than side-by-side, which reads better on phone widths anyway.
- New **"Recommended habits"** card: on-demand AI tip (Gemini) on how to spend less
  in your top expense category — tap "Get a tip" (not automatic, to stay easy on
  the free-tier rate limit). Loading/error states follow the same sanitized-error
  policy as everything else (technical detail logged, friendly text shown).
  New `GeminiRepository.suggestSpendingHabitTip()`, `AppViewModel.habitTipState` /
  `fetchHabitTip()`.

## v5 — Fix Gemini 404, no raw errors shown to users, sound + animated result splash

**Fixed (the actual bug)**
- Gemini API calls were returning a 404: Google deprecated `gemini-2.5-flash` for new
  users. `GeminiRepository.kt` now targets `gemini-3.6-flash` (current GA model).

**Fixed (the real issue behind the screenshot)**
- The statement scan error screen was showing the **raw Gemini API JSON error body**
  directly to the user — never acceptable, and specifically what you flagged.
  - New `util/AppLogger.kt` — every catch block in the app should log the real
    exception here (visible in Logcat, tag "SmartStudent"), never bind it to a
    `Text()` composable.
  - `AppViewModel.kt` — every single catch block audited and rewritten: technical
    exceptions are now logged via `AppLogger.e(...)`, and only short, generic,
    user-safe strings are ever assigned to `errorMessage` or `StatementScanState.Error`.
  - Added `friendlyAuthMessage()` to map common Firebase Auth failures (wrong
    password, email already in use, bad email format, network issues) to
    consistent, friendly wording instead of passing through Firebase's raw message.

**Added — animated success/failure splash**
- New `ui/components/ResultSplash.kt` — full-screen animated outcome screen:
  - **Success**: mascot bounces in (spring/back-ease scale-in) with a small Canvas
    confetti burst radiating outward, green theme, checkmark badge
  - **Failure**: mascot bounces in then does a quick "shake" (keyframe animation),
    red theme, exclamation badge, generic friendly message only
- Wired into the statement scan flow:
  - Failure → full-screen `ResultSplash` instead of inline red error text
  - Success → brief (1.4s) celebration splash, then auto-reveals the parsed
    transaction results underneath

**Added — sound**
- Three short sound effects, synthesized locally (sine-wave tones, no external
  audio assets/licensing): `res/raw/success_chime.wav`, `error_tone.wav`,
  `mascot_chirp.wav`
- New `util/SoundPlayer.kt` (SoundPool-based, correct tool for short UI sounds)
- New `SmartStudentApplication.kt` registered in the manifest, initializes
  `SoundPlayer` once at app startup
- `ResultSplash` auto-plays the success chime or error tone when it appears
- `MascotBird` now chirps whenever tapped (any screen it appears on)

## v4.1 — Fix Gradle sync failure in app/build.gradle.kts

**Fixed**
- `app/build.gradle.kts` failed to sync with `Unresolved reference: util` (line 12)
  and `Unresolved reference: load` (line 15). Root cause: using the fully-qualified
  `java.util.Properties()` combined with an implicit-receiver `load(it)` call nested
  inside `.apply { }` and `.use { }` blocks confuses the Gradle Kotlin DSL script
  compiler — the second error was actually a cascade from the first failing to
  resolve, not two separate bugs.
  Fixed by adding explicit `import java.util.Properties` / `import java.io.FileInputStream`
  at the very top of the file (imports must come before the `plugins {}` block in a
  build.gradle.kts) and calling `localProperties.load(stream)` explicitly instead of
  relying on an implicit receiver.

## v4 — Bank statement scanning (on-device OCR + Gemini analysis)

**Added**
- **Statement scanning flow**: new "Scan statement" button on the Transactions screen
  opens a file picker (images or PDFs) → on-device OCR (ML Kit, free, no API key) →
  extracted text sent to **Gemini 2.5 Flash** (free tier) for categorization and
  analysis → review screen → "Import all" adds everything to Firestore.
  - New `data/ocr/OcrHelper.kt` — handles both image files and multi-page PDFs
    (rendered via Android's built-in `PdfRenderer`, no extra PDF library needed)
  - New `data/repository/GeminiRepository.kt` — builds the analysis prompt (including
    current goals + monthly allowance as context), calls the Gemini REST API directly
    via `HttpURLConnection` (no Retrofit/OkHttp dependency added), parses the model's
    JSON reply
  - New `data/remote/GeminiDto.kt` — REST request/response shapes + the JSON schema
    the model is instructed to reply in
  - New `domain/model/StatementAnalysis.kt` — `ParsedTransaction`, `BudgetStatus`,
    `GoalInsight`, `StatementAnalysis`
  - New `ui/ingestion/StatementScanScreen.kt` — idle/loading/error/results states,
    shows budget status banner, overspending categories, per-goal insights, and the
    list of parsed transactions before import
  - `AppViewModel`: new `scanState` (`StatementScanState`), `scanStatement()`,
    `importScannedTransactions()`, `resetScanState()`

**Gemini API key setup**
- Added `gemini.api.key` to `local.properties` (gitignored, machine-local — same
  pattern as `sdk.dir`)
- `app/build.gradle.kts` now reads `local.properties` at build time and exposes the
  key as `BuildConfig.GEMINI_API_KEY`, so it's never hardcoded in a source file
- `buildFeatures.buildConfig = true` enabled to support this

**Transactions screen**
- Added a second FAB ("Scan statement", gold) alongside the existing manual-add FAB
- Manual add now navigates via a callback instead of being the screen's only action

**Note on this being a school project**: the person building this app opted to keep
the API key handling simple (local.properties + BuildConfig, no backend proxy) since
this isn't shipping to real users. See the "Known placeholders" section below for
what a production version would need instead.

## v3 — Black & beige redesign, goals actions, analytics, mascot

**Theme**
- Replaced the stark black/white/green palette with a warmer **black + beige**
  palette (`StudentBlack`, `StudentBeigeBg`, `StudentCream`, `StudentGold` as the new
  primary accent). Old `StudentGray*` names kept as aliases onto the new warm-neutral
  scale so existing components didn't need touching.
- Added an earth-tone `CategoryChartPalette` (terracotta, olive, mustard, clay, sage,
  plum) for distinguishing pie chart slices.
- Updated `themes.xml` status bar / window background to match.

**Mascot**
- New `MascotBird` component (`ui/components/MascotBird.kt`) — a small bird drawn
  entirely with Canvas paths (no image assets), with an idle bob + wing-flap loop
  animation and a tap-to-hop interaction. Mood-tinted (neutral/happy/concerned).
  Placed on the Welcome screen (replacing the old plain center circle) and the
  Dashboard header (reacts to whether balance is negative).

**Goals**
- **Add money**: tap "Add money" on any goal card → dialog → adds to `savedAmount`,
  persisted via `GoalRepository.update`
- **Delete goal**: trash icon on any goal card → confirm dialog → removes from
  Firestore via `GoalRepository.delete`
- New `AppViewModel.addMoneyToGoal()` / `deleteGoal()` functions

**Analytics (new "Insights" tab)**
- New `AnalyticsScreen` + 4th bottom nav tab, showing:
  - Monthly allowance card (set via `AppViewModel.setMonthlyAllowance`, persisted to
    the user's Firestore profile) with remaining/over-budget state
  - Average expense amount this month
  - Income-to-expense ratio this month
  - Income vs. spend totals this month
  - Category breakdown **pie chart** (new `CategoryPieChart` component, hand-drawn
    with Canvas arcs — no external chart library dependency)
- New computed properties on `AppViewModel`: `monthlyIncome`, `monthlyExpenses`,
  `incomeToExpenseRatio`, `averageExpenseAmount`, `categorySpendTotals`,
  `monthlyAllowance`, `allowanceRemaining`
- `User.monthlyAllowance` field added; `UserRepository` updated to read/write it

**Not yet done (next round, per your request)**
- Bank statement scanning + AI categorization — confirmed direction is the free
  **Gemini API** (Google AI Studio, no credit card, rate-limited). Needs its own API
  key from you (same pattern as the Firebase setup) before wiring in.
- Habit-insight metrics on the Dashboard still return an empty list — good candidate
  to compute from real transaction history once statement scanning feeds more data in.

## v2.2 — Fix missing gradle-wrapper.jar

**Fixed**
- The project shipped `gradle/wrapper/gradle-wrapper.properties` but was missing the
  actual `gradle-wrapper.jar` binary, which `gradlew`/`gradlew.bat` need to run at all.
  This caused `Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain`
  whenever running any `gradlew` command (e.g. `signingReport`) from a terminal.
  Added the correct jar (matching Gradle 8.9, same version referenced in
  `gradle-wrapper.properties`).

## v2.1 — Updated google-services.json (web OAuth client added)

**Changed**
- Replaced `app/google-services.json` with an updated download that now includes a
  **web** OAuth client (`client_type: 3`). This means `default_web_client_id` will
  exist as a generated resource, so the "Continue with Google" button will show as
  enabled instead of the "not configured" note.

**Still outstanding**
- No **Android** OAuth client (`client_type: 1`) is present yet — that only gets
  created once a SHA-1 fingerprint is registered against this Firebase Android app.
  Without it, tapping "Continue with Google" will likely fail at runtime with a
  `DEVELOPER_ERROR` (ApiException code 10), even though the button itself now works.
  Run `./gradlew signingReport`, copy the debug `SHA1`, add it under **Firebase
  console → Project settings → your Android app → Add fingerprint**, then
  re-download `google-services.json` and replace `app/google-services.json` again.

## v2 — Real Firebase auth + Firestore database, email-only onboarding

**Added**
- Firebase Authentication wired in for real:
  - Email/password sign-up, which sends a **real verification email** via Firebase
  - Email/password log-in
  - Google Sign-In (gracefully disables itself with an explanatory note if the
    Firebase project doesn't have a SHA-1 fingerprint registered yet — see README)
- Firestore as the real database:
  - `users/{uid}` — profile doc (first/last name, email, notification preference)
  - `users/{uid}/transactions/{id}` — each transaction
  - `users/{uid}/goals/{id}` — each savings goal
  - New `data/remote/` DTOs (`TransactionDto`, `SavingsGoalDto`) mapping between
    Firestore-safe types and the domain models
  - New `data/repository/` layer: `AuthRepository`, `UserRepository`,
    `TransactionRepository`, `GoalRepository`
- New `AuthScreen` — single screen handling both sign-up and log-in (toggle link
  between modes), with a "Continue with Google" button above an email/password form
- New `EmailVerificationScreen` — shown after sign-up until the user confirms their
  email; has Resend + "I've verified my email" (re-checks Firebase Auth state)
- `AppViewModel` rewritten to call real repositories instead of in-memory sample data;
  added `isLoading` / `errorMessage` state for the UI to react to

**Removed**
- Phone number entry screen
- OTP / SMS verification screen
- Face ID enrollment screen (and `faceIdEnabled` field on `User`)
- `PersonalInfoScreen.kt` / `PasswordScreen.kt` (merged into `AuthScreen.kt`)

**Changed**
- `NavigationKeys` — onboarding routes trimmed to `WELCOME → AUTH → EMAIL_VERIFY →
  ENABLE_NOTIFICATIONS → DASHBOARD` (Google Sign-In skips straight to Dashboard)
- Dashboard's habit-insights section currently receives an empty list — habit metrics
  aren't computed from real Firestore data yet (still a placeholder feature)

**Requires manual setup (see README)**
- A `google-services.json` from your own Firebase project (already added in this zip
  using the one you provided)
- A SHA-1 fingerprint registered with that Firebase project for Google Sign-In to work
- Firestore security rules tightened before shipping (currently open "test mode")

## v1.1 — Fix missing import in MainScaffold.kt

**Fixed**
- `MainScaffold.kt` used `by navController.currentBackStackEntryAsState()` without importing
  `androidx.compose.runtime.getValue`, which Kotlin needs in scope to resolve the `by` delegate
  on a `State<T>`. This caused an unresolved reference build error at MainScaffold.kt:35.
  Audited every other file using `by` delegates — all already had correct imports.

## v1 — Initial scaffold (Oportun-inspired onboarding + core shell)

Built from scratch using the uploaded Gradle config as the base, plus the Mobbin/Oportun
screenshots as the visual reference.

**Added**
- Full Gradle project setup (version catalog, wrapper, app module config)
- Design system: black/white/green theme, pill buttons, rounded cards/inputs (`theme/`)
- Domain models: `User`, `Transaction`, `SavingsGoal`, `HabitMetric`, `CategoryRule`
- Reusable components: `PrimaryPillButton`, `SecondaryPillButton`, `TextLinkButton`,
  `StudentTextField`, `OutlinedRowCard`, `GoalProgressCard`, `IconBadge`
- Onboarding flow matching the reference screens:
  - Welcome (hero art + Log in / Sign up)
  - Phone number entry
  - Personal info (name, email, consent checkbox)
  - Password creation (live requirement checklist)
  - OTP verification (6-box code entry)
  - Enable Face ID prompt
  - Enable notifications prompt
- Core app shell:
  - Bottom nav (Home / Activity / Goals)
  - Dashboard (balance, spending habit insights, recent transactions)
  - Transactions list (grouped by date)
  - Goals list + "What do you want to save for?" type picker (Savings goal / Smart bill)
  - Manual transaction entry (ingestion) form
- `AppViewModel` with in-memory sample data wiring the whole flow together end-to-end
- Adaptive launcher icon (placeholder mark)

**Not yet done**
- No real backend / persistence
- No dedicated goal-builder screen (reuses ingestion form)
- OTP resend/voice call are stubbed no-ops
- No automated tests
