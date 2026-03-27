# Decision Framework

## Overview

Every technical and product decision in VIIS is classified by **reversibility cost** — how long it takes to undo the decision if it turns out to be wrong. This framework ensures we move fast on easy-to-reverse decisions while being deliberate about hard-to-reverse ones.

| Type | Reversal Cost | Process | Approval |
|---|---|---|---|
| 🚪 1-Way Door | 2-3 weeks | Full analysis, documented trade-offs | Developer + Reviewer must agree |
| 🚪↔️ 1.5-Way Door | 3-5 days | Brief rationale, documented | Developer proposes, Reviewer acknowledges |
| 🚪🚪 2-Way Door | 1-2 days | Developer decides, documents briefly | Batch review by Reviewer |

---

## Decision Log Template

```
### D-XXX: [Title]
- **Type**: 1-way / 1.5-way / 2-way
- **Status**: PROPOSED / DECIDED / REVERSED
- **Date**: YYYY-MM-DD
- **Proposed by**: [Agent]
- **Approved by**: [Agent] (if applicable)
- **Context**: Why this decision needs to be made
- **Options**: List of alternatives considered
- **Decision**: What was chosen
- **Rationale**: Why
- **Trade-offs**: What we're giving up
- **Reversal cost**: Time/effort to undo
```

---

## 🚪 1-Way Door Decisions (2-3 Weeks to Reverse)

These are hard-to-reverse architectural and strategic decisions. They shape the foundation of the system. Require full analysis, documented trade-offs, and Reviewer Agent sign-off before implementation begins.

**When to classify as 1-Way Door**: Changes to core architecture, data models, platform, external dependencies, security model, or anything that creates significant migration cost if reversed.

---

### D-001: Platform — Android Native (Kotlin)
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Need to choose mobile platform for an app that requires deep SMS access
- **Options**: (A) React Native, (B) Flutter, (C) Android Native Kotlin, (D) KMP
- **Decision**: Android Native (Kotlin)
- **Rationale**: SMS BroadcastReceiver, ContentObserver, and background processing require deep Android OS integration. Cross-platform frameworks add an abstraction layer that complicates permission handling and background reliability. Target users are 95%+ Android.
- **Trade-offs**: No iOS support. Limits hiring pool to Kotlin/Android developers.
- **Reversal cost**: 3+ weeks — complete rewrite of all platform-specific code

### D-002: Local-First / Offline-First Architecture
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Vendors operate in areas with unreliable internet. Money data is sensitive.
- **Options**: (A) Cloud-first with offline cache, (B) Local-first with optional sync, (C) Fully local, no backend
- **Decision**: Local-first with optional sync (B)
- **Rationale**: Core value proposition (SMS parsing + insights) needs zero internet dependency. Trust is paramount — vendors must feel their money data stays on their phone. Optional sync allows backup/analytics later.
- **Trade-offs**: Harder to do cross-device sync. Limited server-side analytics in V1. More complex eventual consistency when sync is added.
- **Reversal cost**: 2-3 weeks to move processing server-side, redesign data flow

### D-003: Event Sourcing as Core Architecture Pattern
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Need a data architecture that supports passive data ingestion, multiple derived views, and auditability
- **Options**: (A) Traditional CRUD, (B) Event sourcing with projections, (C) CQRS without event sourcing
- **Decision**: Event sourcing — events are immutable source of truth, read models are projections
- **Rationale**: SMS events are naturally immutable facts. Multiple views (dashboard, hourly, customer, weekly) are all derived from the same events. Event log provides full audit trail. Projections can be rebuilt if schema changes.
- **Trade-offs**: More complex than simple CRUD. Higher storage (events never deleted). Learning curve for team.
- **Reversal cost**: 3+ weeks — fundamental data layer redesign

### D-004: SQLite (Room) as Event Store + Read Model Store
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Need local storage for event log and read model projections
- **Options**: (A) Room/SQLite, (B) Realm, (C) ObjectBox, (D) SharedPreferences + files
- **Decision**: Room (SQLite)
- **Rationale**: Mature, well-supported by Google, great with Kotlin coroutines/Flow, SQL queries for complex aggregations, no additional binary size. Perfect for both event storage and read model projections.
- **Trade-offs**: SQL can be verbose. No built-in sync (need to build). Schema migrations need care.
- **Reversal cost**: 2-3 weeks to migrate data layer, rewrite all DAOs

### D-005: Parser Registry Pattern (One Parser Per SMS Source)
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: SMS formats vary dramatically across banks/UPI apps. Need a parsing strategy.
- **Options**: (A) Single universal regex, (B) ML/NLP-based parser, (C) Registry pattern (one parser per source), (D) Template-based matching
- **Decision**: Registry pattern with fallback chain
- **Rationale**: Each bank/UPI app has distinct SMS format. Registry is predictable, testable, debuggable. Each parser can be unit tested independently with real SMS samples. ML is overkill for V1 and hard to debug when wrong. Fallback chain (exact → regex → heuristic) handles unknown formats gracefully.
- **Trade-offs**: Need to maintain N parsers. New bank = new parser code. More code than single regex.
- **Reversal cost**: 2-3 weeks — parsing is core to the system, affects all downstream events

### D-006: UPI Handle as Primary Customer Identity Key
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Need to identify repeat customers from SMS data for customer insights
- **Options**: (A) Phone number, (B) UPI handle, (C) Sender name only, (D) Composite key (UPI handle + name clustering)
- **Decision**: UPI handle primary, name clustering secondary
- **Rationale**: UPI handle is the most reliable identifier in SMS data. Phone numbers aren't always present. Names are ambiguous (common names). Composite approach: use UPI handle when available, fall back to normalized name matching.
- **Trade-offs**: Not all SMS contain UPI handles. Name clustering can have false positives. Privacy considerations with storing UPI handles.
- **Reversal cost**: 2+ weeks — customer profile table redesign, re-process all historical events

### D-007: Hinglish-First UI Language Strategy
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Target users have low tech literacy, speak Hindi, interact in Hinglish
- **Options**: (A) English-first with Hindi translation, (B) Hinglish-first, (C) Pure Hindi (Devanagari), (D) Multi-language from start
- **Decision**: Hinglish-first (Roman script Hindi mixed with simple English)
- **Rationale**: Target audience types in Roman script. Devanagari feels formal/governmental. Hinglish is WhatsApp-native for this demographic. "Aaj ₹X aaya" is more natural than "Today's income: ₹X" or "आज की कमाई: ₹X".
- **Trade-offs**: Hinglish is non-standard, hard to get consistent. May feel unprofessional to some. Harder to add formal Hindi later.
- **Reversal cost**: 2+ weeks — all UI strings, notification templates, onboarding content

### D-008: No Cloud Dependency for Core Features
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Should any core feature require internet?
- **Options**: (A) Some insights computed server-side, (B) All computation on-device
- **Decision**: All computation on-device. Zero cloud dependency for any user-facing feature.
- **Rationale**: Target users have inconsistent internet. Trust requires data staying on device. Server costs must be near-zero for viability. If the server goes down, the app must still fully function.
- **Trade-offs**: Limited to device CPU/memory for computations. Can't do cross-vendor analytics. Can't push parser updates remotely without app update.
- **Reversal cost**: 3+ weeks — need to build API, handle offline/online states, rewrite computation layer

### D-009: Single-Screen Primary UX
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: Target users have minimal tech literacy, WhatsApp-like simplicity expected
- **Options**: (A) Tab-based navigation, (B) Single scrollable screen with expandable cards, (C) Dashboard + detail screens
- **Decision**: Single scrollable screen with expandable cards, secondary detail screens accessible via tap
- **Rationale**: Minimizes navigation confusion. Main insight ("Aaj kitna aaya") is always visible. Details are discoverable but not required. Matches mental model of "one place to check my money."
- **Trade-offs**: Screen can get long with many insights. Limited information density. Harder to organize V2 features.
- **Reversal cost**: 2-3 weeks — complete UI/navigation redesign

### D-010: SMS Permission as Hard Requirement
- **Type**: 1-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Approved by**: Reviewer
- **Context**: App is useless without SMS access. How to handle permission denial?
- **Options**: (A) App works in limited mode without SMS, (B) SMS permission is hard requirement, (C) Alternative input (manual entry fallback)
- **Decision**: SMS permission is a hard requirement. App cannot function without it.
- **Rationale**: The entire value proposition is zero-input tracking via SMS. A manual entry fallback defeats the purpose and would never be used by the target audience. Better to invest in persuasive permission explanation than build a degraded mode nobody wants.
- **Trade-offs**: Lose users who don't grant permission. Google Play policy risk around SMS permissions. No fallback mode.
- **Reversal cost**: 2-3 weeks to build alternative input mechanisms

---

## 🚪↔️ 1.5-Way Door Decisions (3-5 Days to Reverse)

Moderately reversible decisions. Require some migration or refactoring effort to undo. Developer proposes with brief rationale, Reviewer must acknowledge before implementation begins.

**When to classify as 1.5-Way Door**: API/data contracts, database schema specifics, third-party library choices, notification strategies, data retention policies, algorithm choices.

---

### D-201: Room Database Schema — Separate Tables for Events and Projections
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: How to organize the SQLite schema for event sourcing
- **Decision**: One `events` table (immutable log) + separate projection tables (transactions, daily_summaries, hourly_stats, customer_profiles). Projections are denormalized for fast reads.
- **Reversal cost**: 3-5 days — schema migration, DAO rewrites, projection rebuild logic

### D-202: WorkManager for Background Scheduling
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: Need reliable scheduling for EOD notifications, mid-day checks, and periodic sync
- **Options**: (A) WorkManager, (B) AlarmManager, (C) Foreground Service
- **Decision**: WorkManager — handles doze mode, battery optimization, guaranteed execution
- **Reversal cost**: 3-4 days to switch scheduling mechanism

### D-203: Jetpack Compose for UI
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: UI framework choice for simple card-based interface
- **Options**: (A) Jetpack Compose, (B) XML Views, (C) Compose + XML hybrid
- **Decision**: Pure Jetpack Compose — simpler for card-based UI, less boilerplate, reactive state management
- **Reversal cost**: 4-5 days to rewrite to XML (UI is simple, so limited scope)

### D-204: Notification Channel Strategy — 3 Channels
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: Android requires notification channels. How many and which?
- **Decision**: 3 channels: (1) Daily Summary (default importance), (2) Alerts (high importance), (3) System (low importance). Users can independently control each.
- **Reversal cost**: 3 days — channel IDs are sticky on Android, need migration strategy

### D-205: Data Retention — 90 Days on Device, Events Never Deleted
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: How long to keep data on device? Storage is limited on low-end phones.
- **Decision**: Events are kept forever (they're small). Read model projections for detailed data (hourly, individual transactions) retained for 90 days. Daily summaries kept forever. Old projection data archived to compressed format.
- **Reversal cost**: 3-4 days — need data migration, policy change, possible data loss

### D-206: ZXing for QR Generation (V2)
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: Library choice for generating UPI QR codes
- **Options**: (A) ZXing, (B) QRGen, (C) Custom implementation
- **Decision**: ZXing — mature, widely used, supports UPI format
- **Reversal cost**: 3 days — swap library, adjust generation code

### D-207: Customer Clustering — UPI Handle Exact Match + Levenshtein Name Similarity
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: How to identify same customer across transactions when UPI handle is missing
- **Decision**: Primary: exact UPI handle match. Secondary: Levenshtein distance on normalized names (threshold: 0.85 similarity). Cluster assignment is an event (CustomerIdentified) so it can be corrected.
- **Reversal cost**: 3-5 days — re-run clustering, update customer profiles

### D-208: Kotlin Coroutines + Flow for Reactive Pipeline
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: Need async processing for SMS parsing and reactive UI updates
- **Decision**: Kotlin Coroutines for async work, Flow for reactive streams from Room to UI
- **Reversal cost**: 4-5 days — significant refactor of async code

### D-209: Moshi for JSON Serialization of Event Payloads
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Options**: (A) Moshi, (B) Kotlinx Serialization, (C) Gson
- **Decision**: Moshi — good Kotlin support, codegen adapter, smaller than Gson
- **Reversal cost**: 3 days — swap serialization across event payloads

### D-210: MVVM + Clean Architecture Layers
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: App architecture pattern within the event-sourced system
- **Decision**: MVVM with Clean Architecture layers: UI → ViewModel → UseCase → Repository → EventStore/DAO
- **Reversal cost**: 4-5 days — structural refactor

### D-211: Hilt for Dependency Injection
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Options**: (A) Hilt, (B) Koin, (C) Manual DI
- **Decision**: Hilt — compile-time safety, official Jetpack integration, good for Android
- **Reversal cost**: 3-4 days — swap DI framework, re-annotate modules

### D-212: Delta Sync Protocol for Optional Backend
- **Type**: 1.5-way
- **Status**: PROPOSED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: How to sync local events to optional backend
- **Decision**: Delta sync based on `last_sync_timestamp`. Client sends events after last sync. Server acknowledges with new timestamp. Idempotent (events have unique IDs).
- **Reversal cost**: 4-5 days — sync protocol change, potential data inconsistency

### D-213: Timber for Logging
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Decision**: Timber library for logging. Production tree filters sensitive data (amounts, UPI handles). Debug tree logs everything.
- **Reversal cost**: 3 days — swap logging calls across codebase

### D-214: SharedPreferences for User Settings
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Context**: Where to store user preferences (language, notification timing, etc.)
- **Decision**: Jetpack DataStore (Preferences) — type-safe, coroutine-based, successor to SharedPreferences
- **Reversal cost**: 3 days — migrate preferences store

### D-215: Proguard/R8 for Release Builds
- **Type**: 1.5-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Proposed by**: Developer
- **Decision**: R8 with aggressive minification for small APK size. Important for low-end devices with limited storage.
- **Reversal cost**: 3 days — adjust proguard rules, test for reflection issues

---

## 🚪🚪 2-Way Door Decisions (1-2 Days to Reverse)

Easily reversible decisions. Developer Agent can make these independently and document briefly. Reviewer reviews in batch.

**When to classify as 2-Way Door**: UI tweaks, notification timing/copy, threshold values, feature toggles, sorting orders, display formats, default values.

---

### D-101: EOD Notification Time — 9:00 PM
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: End-of-day summary notification triggers at 9:00 PM local time
- **Rationale**: Most kirana stores close by 9 PM. Late enough to capture full day, early enough to not disturb sleep.
- **Reversal cost**: 1 hour — change constant

### D-102: Deduplication Time Window — 5 Minutes
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Two SMS with same amount + sender within 5 minutes are considered duplicates
- **Reversal cost**: 1 hour — change constant, reprocess recent events

### D-103: Idle Time Default Threshold — 2 Hours
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Default idle threshold is 2 hours. Dynamic adjustment: threshold = avg_gap × 2.5 once enough data.
- **Reversal cost**: 1 hour

### D-104: Historical Baseline Window — 14-Day Rolling Average
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Expected earnings baseline uses 14-day rolling average of same weekday
- **Reversal cost**: 1 hour — change window size, recompute

### D-105: Consistency Score Weights
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: alignment=0.4, stability=0.3, activity=0.3
- **Reversal cost**: < 1 hour

### D-106: Mid-Day Alert Trigger — 2:00 PM
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Mid-day check runs at 2:00 PM. Alerts if actual < 70% of expected.
- **Reversal cost**: 1 hour

### D-107: Minimum Data for Weekly Trends — 7 Days
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Weekly trend insight requires minimum 7 days of data to show
- **Reversal cost**: < 1 hour

### D-108: Splash Screen Duration — 2 Seconds
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Splash screen shows for 2 seconds (or until init completes, whichever is longer)
- **Reversal cost**: < 1 hour

### D-109: Dashboard Currency Format — "₹X,XXX"
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Indian number format with commas (₹12,50,000). No decimal for amounts > ₹100, two decimals for < ₹100.
- **Reversal cost**: 1 hour

### D-110: Onboarding Screen Count — 3 Screens
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: 3 onboarding screens (Trust → Features → How It Works). Minimal — don't overwhelm.
- **Reversal cost**: 1 hour

### D-111: Peak Hour Window — 1-Hour Blocks
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Hourly breakdown uses 1-hour blocks (6-7 AM, 7-8 AM, ...). Not 30-min or 2-hour.
- **Reversal cost**: < 1 hour

### D-112: Repeat Customer Threshold — 2+ Transactions
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Customer is "repeat" after 2+ transactions. "Regular" after 5+ in 30 days.
- **Reversal cost**: < 1 hour

### D-113: Underperformance Alert Threshold — 30% Deficit
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Mid-day alert fires when actual < 70% of expected (30% deficit)
- **Reversal cost**: < 1 hour

### D-114: Daily Summary Card Order
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Card order: (1) Total income, (2) vs yesterday, (3) transaction count, (4) peak hour, (5) expected vs actual, (6) consistency score
- **Reversal cost**: < 1 hour

### D-115: QR Metadata Tag Format (V2)
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Format: `viis-{vendorShortId}-{yyyyMMdd}-{random4}`
- **Reversal cost**: 1 hour

### D-116: Maximum SMS Length for Parsing — 500 Characters
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Reject SMS > 500 characters as likely non-transactional
- **Reversal cost**: < 1 hour

### D-117: Weekly Summary Day — Sunday 9 PM
- **Type**: 2-way
- **Status**: DECIDED
- **Date**: 2026-03-28
- **Decision**: Weekly summary notification sent Sunday at 9 PM, alongside daily summary
- **Reversal cost**: < 1 hour

---

## Decision-Making Guidelines

1. **When in doubt, classify higher** — if unsure between 1.5-way and 2-way, treat as 1.5-way
2. **All 1-way doors require documentation BEFORE implementation begins** — no "decide as we go"
3. **2-way doors can be documented after implementation** — in batch reviews
4. **If a 2-way door causes a bug in production, reclassify as 1.5-way** — it's harder than we thought
5. **Reversal cost includes data migration** — changing a schema is not just changing code
6. **The Developer Agent can escalate** — if they realize a decision is harder than classified, pause and re-classify
7. **Decisions are events too** — every decision is logged, timestamped, and attributed. They can be reversed with a new decision event referencing the original.
