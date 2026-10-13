# HisabTrip — Master Architecture & UI Implementation Plan

A production-oriented group travel finance platform unifying upfront wallet contributions, multi-payer expense logging, deterministic who-pays-whom settlements, and audit history.

---

## 1. Technology Stack Audit & Environment Reconciliation

### Current AI Studio Container Environment
* **Runtime & Build System:** Cloud-based native Android environment running Gradle (`app/build.gradle.kts`, `settings.gradle.kts`) and compiling directly into an Android APK for the streaming browser emulator.
* **Current Codebase:** Kotlin + Jetpack Compose + Material 3, with a Room SQLite database (`hisabtrip_ledger.db`).
* **Platform Context:** In AI Studio, the emulator runs native Android APKs. While your master prompt outlines React Native + Expo for multi-platform distribution, this container compiles and previews Kotlin/Compose natively.
* **Resolution Strategy:**
  1. We preserve the running, high-performance native Android (Kotlin + Jetpack Compose) application in this workspace so it builds and previews live in the emulator.
  2. For the multi-user backend, we specify and integrate the **Supabase PostgreSQL + Google OAuth** cloud architecture so data is synchronized across real devices, with Room serving as the robust offline-first local cache.
  3. We provide the complete Supabase SQL schema migrations with Row Level Security (RLS) policies so you can deploy to Supabase immediately.

---

## 2. Multi-User Architecture & Cloud Synchronization Plan

### The Limitation of Local-Only Room
A local Room SQLite database only persists data on the single device running the app. For real group trips, travel companions on different phones must be able to join via trip codes, log expenses, and see live settlements.

### Dual-Layer Storage Model: Offline-First Room + Supabase Cloud
```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose Client                          │
│     All 24 Screens • Warm Neutral Theme • Curved Floating Nav Bar      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Repository & Sync Engine Layer                     │
│  ┌──────────────────────────────┐    ┌──────────────────────────────┐  │
│  │ Local Room Database (SQLite) │◄──►│    Supabase Client / API     │  │
│  │  - Offline read-only cache   │    │  - Google OAuth Auth Token   │  │
│  │  - Instant UI reactivity     │    │  - Postgres REST / RPC API   │  │
│  │  - Zero floating-point paise │    │  - Realtime WebSocket sync   │  │
│  └──────────────────────────────┘    └──────────────┬───────────────┘  │
└─────────────────────────────────────────────────────┼──────────────────┘
                                                      │ HTTPS / WSS
                                                      ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Supabase Cloud Platform                         │
│  ┌────────────────────┐  ┌───────────────────┐  ┌───────────────────┐  │
│  │   Supabase Auth    │  │ PostgreSQL + RLS  │  │  Private Storage  │  │
│  │ (Google OAuth JWT) │  │  (12 Core Tables) │  │ (Receipt Objects) │  │
│  └────────────────────┘  └───────────────────┘  └───────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Multi-User Synchronization Mechanics
1. **Google Authentication:** User authenticates via Google Identity. Supabase issues a verified JWT with `sub = user_id`.
2. **Profile Creation:** On first sign-in, an immutable record is inserted into `public.profiles` (`id = auth.uid()`, unique `username`, `display_name`, `avatar_key`).
3. **Trip Creation & Joining:**
   - Creator inserts a trip into `trips` and is automatically granted `CREATOR` role in `trip_members`.
   - Other users enter the 8-character trip code or scan an invite link; the server function `join_trip_by_code(code)` verifies the invitation and inserts their `trip_members` record.
4. **Row Level Security (RLS):** Every query is gated by `auth.uid() IN (SELECT user_id FROM trip_members WHERE trip_id = ...)`. Non-members cannot read or tamper with another group's expenses.
5. **Realtime Updates:** Client subscribes to Supabase Realtime Postgres Changes on `expenses`, `contributions`, and `settlements` for the active `trip_id`. When any member logs a transaction, all group members' screens update automatically.

---

## 3. Visual Design System (Matching the Reference)

### Exact Palette Tokens
* **Main Canvas (`main_bg`):** `#F7F5F1` (Warm off-white / linen)
* **Card Surface (`card_bg`):** `#FFFFFF` with soft 1.dp elevation and `#E8E5DF` border
* **Soft Neutral Surface:** `#F0EEEA` (For secondary containers, chips, input backgrounds)
* **Subtle Border:** `#E8E5DF` (0.8.dp)
* **Primary Text:** `#191919` (Near black)
* **Secondary Text:** `#777777` (Muted caption grey)
* **Status Colors:**
  * *Approved / Settled / Paid:* Muted green (`#2E7D32` text, `#E8F5E9` container, `#C8E6C9` border)
  * *Pending / Caution:* Soft amber (`#B45309` text, `#FEF3C7` container, `#FDE68A` border)
  * *Rejected / Over-Budget:* Restrained red (`#C62828` text, `#FFEBEE` container, `#FFCDD2` border)
* **Primary Actions:** Graphite `#191919` with pure white text
* **Accent:** Subtle sage green (`#5F7161`)

### Structural & Interaction Elements
* **Cards & Shapes:** 20.dp rounded corners for trip cards; 14.dp for action buttons and metric cards; circular avatars.
* **Curved Bottom Navigation:** Floating rounded navigation surface containing Home, Trips, an elevated circular central **(+) Create Trip** button, and Profile, with full `navigationBarsPadding()` support.
* **Visual Media:** Curated scenic landscape photography cards (beaches, mountain passes, road trips) with soft gradient overlays.
* **Splash Animation:** Softly blurred travel background, centered HisabTrip symbol with route-line animation, and smooth fade-in tagline.

---

## 4. Complete Screen-by-Screen Implementation Checklist (All 24 Screens)

1. **Splash Screen:** Full-bleed blurred travel background, route line node animation, canonical monochrome HisabTrip logo, fading tagline, session check.
2. **Google Sign-In Screen:** Minimal warm `#F7F5F1` background, centered logo and wordmark, prominent graphite "Continue with Google" button with official Google icon, legal links.
3. **Profile Setup Screen:** Prompts new users to confirm their suggested unique username, choose from 8 predefined travel avatars (compass, mountain, tent, backpack, bonfire, camera, map, luggage), and save profile.
4. **Home Screen:** Traveler greeting with avatar, notification bell with unread badge, horizontal recent trip cards with scenic covers, quick-action pills ("+ Create Trip", "Join Trip"), active trip cards with progress meters and personal net balance tags.
5. **Trips List Screen:** Segmented tabs ("Ongoing", "Upcoming", "Past"), searchable list of real trips, cover photos, date ranges, and member count chips.
6. **Create Trip Screen:** Clean form with fields: Trip Name, Description, Start Date, End Date, Currency (default INR), Total Budget (paise), cover selection, and companion invitations.
7. **Join Trip Screen:** Input for 8-character trip code or invitation ID with server-backed code validation and duplicate check.
8. **Trip Overview Screen:** Scenic hero cover with gradient overlay, live trip status pill, compact financial metrics (Total Budget, Spent, Remaining/Over-Budget), utilization progress bar, member avatar row, and quick action buttons.
9. **Wallet / Upfront Contributions Screen:** Central trip kitty card, total collected vs spent, member target vs received progress, and creator/manager "+ Record Contribution" action.
10. **Add Expense Screen:** Form with title, amount (₹), category chips, location, date, single or multiple payers selector, participant split selector (**Equal**, **Exact Custom ₹**, **Percentage %**), and receipt note/photo memo.
11. **Expenses List Screen:** Chronological feed with search, category filters, approval status chips (All, Approved, Pending), payer summaries, and member split counts.
12. **Expense Detail Screen:** Full breakdown of payers, individual participant shares, date, approval state, receipt memo, and edit/delete actions according to permissions.
13. **Expense Approval Queue Screen:** Pending review queue for managers/creator with one-tap "Approve" and "Reject with reason" actions and audit logging.
14. **Budget Settings Screen:** Creator controls to edit trip budget with dialog prompt: "Recalculate member contribution targets" or "Keep current targets".
15. **Members & Permissions Screen:** Roster of travelers showing avatars, usernames, assigned roles (Creator, Manager, Member), and individual balance summaries. Creator controls to promote, demote, or remove members.
16. **Member Detail Screen:** Traveler profile card showing their total expenses paid, total expense share, upfront wallet contributions, and net balance.
17. **Settlement Summary Screen:** Total outstanding group debt, filter pills (All, You Pay, You Receive), and deterministic "Who Pays Whom" transfer cards.
18. **Settlement Detail Screen:** Payer, receiver, transfer amount, "Mark as Paid" action, payment mode/reference notes, and timestamp.
19. **Notifications Screen:** Chronological list of in-app alerts (expense approvals, contribution updates, budget warnings, settlement notices) with unread indicators and "Mark all read".
20. **Profile Screen:** View and edit display name, unique username, travel avatar picker, immutable User ID, and Google account email.
21. **App Settings Screen:** Currency display preferences (INR ₹), language (English), encrypted local database status, privacy policy, and terms of service.
22. **Trip Settings Screen:** Creator controls for budget, permissions, category rules, close trip, and soft-delete with 30-day recovery.
23. **Completed Trip Screen:** Read-only view of finalized financials, complete expense ledger, wallet reconciliation, and export access, with creator reopen option.
24. **Export Report Screen:** Dialog and generator producing structured plaintext and CSV exports containing budget, member contributions, approved expenses, and final settlements.

---

## 5. File Modification Plan

```
app/src/main/
├── java/com/example/
│   ├── MainActivity.kt                      (App navigation, bottom nav, global dialogs)
│   ├── data/
│   │   ├── local/
│   │   │   ├── Entities.kt                  (10 Room entities with integer minor units)
│   │   │   ├── HisabTripDao.kt              (Reactive Flow queries and transactions)
│   │   │   └── HisabTripDatabase.kt         (Room database singleton)
│   │   └── repository/
│   │       ├── HisabTripRepository.kt       (Business logic, Supabase sync hooks, audit logs)
│   │       └── SupabaseConfig.kt            (Supabase REST/RPC client configuration)
│   ├── domain/
│   │   ├── money/
│   │   │   └── MoneyUtils.kt                (Paise formatters, exact remainder split calculators)
│   │   └── settlement/
│   │       └── SettlementCalculator.kt      (Deterministic greedy two-pointer matching)
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt                     (Warm neutral palette: #F7F5F1, #191919, #2E7D32)
│       │   └── Theme.kt                     (HisabTripTheme with status bar integration)
│       ├── components/
│       │   ├── CurvedBottomNavBar.kt        (Floating curved nav bar with central (+) button)
│       │   ├── CommonComponents.kt          (Metric cards, badges, empty states, scenic covers)
│       │   └── MemberAvatar.kt              (Travel icons with circular borders)
│       ├── screens/
│       │   ├── SplashScreen.kt              (Blurred background, animated route, tagline)
│       │   ├── SignInScreen.kt              (Dedicated warm Google Sign-In screen)
│       │   ├── HomeScreen.kt                (Recent scenic trip cards, stats, quick actions)
│       │   ├── TripDashboardScreen.kt       (Overview, Wallet, Expenses, Settle, Members, Settings)
│       │   ├── FinancialSheets.kt           (Add Expense with 3 splits & multi-payer, Contributions, Settle)
│       │   ├── TripDialogs.kt               (Create Trip, Join Trip with validation)
│       │   └── ProfileAndNotificationsScreens.kt (Notifications feed, Profile editor, Preferences)
│       └── viewmodel/
│           └── HisabTripViewModel.kt        (Unified UI state, screen router, calculations)
├── res/
│   ├── drawable/
│   │   ├── ic_hisabtrip_logo.jpg            (Canonical monochrome logo asset)
│   │   ├── ic_launcher_foreground.xml       (Adaptive launcher foreground)
│   │   └── ic_launcher_background.xml       (Adaptive launcher background #121417)
│   └── values/
│       └── strings.xml                      (App name: HisabTrip)
```

---

## 6. Security, RLS & Threat Model Plan

1. **Integer Minor Units (Paise):**
   - All financial numbers (`budget_minor`, `amount_minor`, `share_minor`, `target_minor`) are strictly `BIGINT` in PostgreSQL and `Long` in Kotlin.
   - Zero JavaScript/Kotlin floating-point numbers in financial arithmetic.
2. **Server-Side Authorization & RLS (PostgreSQL):**
   - Tables: `profiles`, `trips`, `trip_members`, `contributions`, `expenses`, `expense_payers`, `expense_participants`, `settlements`, `audit_logs`, `notifications`.
   - Default deny: `ALTER TABLE <table_name> ENABLE ROW LEVEL SECURITY;`.
   - Read policy: A user can only read records belonging to trips where `auth.uid()` exists in `trip_members`.
   - Mutation policy: Only users with `role IN ('CREATOR', 'MANAGER')` can modify budgets, approve expenses, or delete trips.
3. **Private File & Receipt Security:**
   - Receipts are stored in a private storage bucket (`trip-receipts`).
   - Objects are accessed exclusively via short-lived signed URLs generated server-side.
4. **Append-Only Audit Log:**
   - Every financial mutation (`TRIP_CREATED`, `CONTRIBUTION_RECORDED`, `EXPENSE_ADDED`, `EXPENSE_APPROVED`, `EXPENSE_REJECTED`, `SETTLEMENT_PAID`, `BUDGET_UPDATED`) generates an immutable audit record with user ID and timestamp.
5. **No Client Secrets:**
   - Only the public Supabase Anon key and project URL are stored in client configuration. The service-role key is never packaged in the mobile app.

---

## 7. Comprehensive Testing & Verification Plan

We will implement a rigorous test suite spanning unit, integration, and Robolectric JVM tests:

### A. Financial & Minor-Unit Math Tests (`MoneyUtilsTest`)
- Minor-unit conversion (`₹500.50` -> `50050` paise, negative formatting, zero cases).
- Equal split remainder conservation (e.g. ₹100 split among 3 members: exactly 3334, 3333, 3333 paise, asserting $\sum \text{shares} \equiv 10000$ paise).
- Percentage split remainder conservation across uneven distributions.
- Multiple-payer sum validation (ensuring $\sum \text{payers} \equiv \text{total}$).

### B. Settlement Algorithm Tests (`SettlementCalculatorTest`)
- 2-member single-payer balance.
- 3-member multi-payer balance with unequal spending.
- 4+ member complex groups with zero-balance travelers.
- Mathematical assertion: $\sum \text{outgoing} \equiv \sum \text{incoming} \equiv \frac{\text{total absolute imbalance}}{2}$.
- Minimization of transfer count via greedy matching.

### C. Role & Permission Tests (`PermissionsTest`)
- Creator can change budget; normal Member cannot.
- Manager can approve expenses; normal Member cannot.
- Non-member cannot access trip data.

### D. Robolectric JVM End-to-End Tests (`HisabTripRobolectricTest`)
- Context string verification (`R.string.app_name` == `"HisabTrip"`).
- Profile initialization with Google session email.
- Trip creation flow with initial member assignment.
- Expense submission, approval transition, and settlement generation cycle.

---

## 8. Separation: Immediate Code Implementation vs. Human Setup

### What We Implement Immediately in the Codebase
* Complete 24-screen UI system with warm neutral styling (`#F7F5F1`), rounded cards, and scenic photography banners.
* Elevated curved bottom navigation bar with circular central **(+) Create Trip** button.
* Dedicated Google Sign-In screen with session persistence.
* Full Room SQLite database schema with offline caching and reactive `Flow` repositories.
* Supabase configuration client hooks, data models, and synchronization engine.
* Complete SQL migration file (`supabase_schema.sql`) containing the 10 tables, RLS policies, and RPC functions.
* Complete unit, financial, and Robolectric test suite.

### Human Setup Required Later by App Owner
1. **Supabase Project:** Create a project at [supabase.com](https://supabase.com) and run `supabase_schema.sql` in the SQL Editor.
2. **Google Cloud Console:** Create an OAuth 2.0 Web Client ID and Android Client ID matching package name `com.aistudio.hisabtrip.vzkpqm`.
3. **Configure Google OAuth in Supabase:** Add the Google Client ID & Secret in Supabase Auth -> Providers -> Google.
4. **Environment Secrets:** Add `SUPABASE_URL` and `SUPABASE_ANON_KEY` to `.env` or AI Studio Secrets panel.
