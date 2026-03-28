# Project Status — VIIS (Vendor Income Intelligence System)

**Overall Status**: 🟡 In Progress
**Current Phase**: Phase 0 — Project Setup & Architecture
**Last Updated**: 2026-03-28

---

## Phase Overview

| Phase | Name | Status | Target |
|---|---|---|---|
| 0 | Project Setup & Architecture | 🟡 In Progress | Foundation |
| 1 | Core SMS Infrastructure (V1 MVP) | 🔴 Not Started | MVP |
| 2 | Insights & Dashboard (V1) | 🔴 Not Started | MVP |
| 3 | Notifications (V1) | 🔴 Not Started | V1 Complete |
| 4 | QR System (V2) | 🔴 Not Started | V2 |
| 5 | Polish & Launch | 🔴 Not Started | Launch |

---

## Task Board

### Phase 0 — Project Setup & Architecture

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P0-001 | Android project scaffolding (Kotlin, Gradle) | Developer | ✅ Done | P0 | — |
| P0-002 | Hilt DI module setup | Developer | ✅ Done | P0 | P0-001 |
| P0-003 | Room database setup (event store + projections) | Developer | ✅ Done | P0 | P0-001 |
| P0-004 | CI/CD pipeline (GitHub Actions) | Developer | ✅ Done | P1 | P0-001 |
| P0-005 | Linting & code style (ktlint) | Developer | ✅ Done | P1 | P0-001 |
| P0-006 | Architecture review | Reviewer | Not Started | P0 | P0-003 |
| P0-007 | Test framework setup (JUnit5, MockK, Turbine) | QA | ✅ Done | P0 | P0-001 |
| P0-008 | Low-end device test profile setup | QA | ✅ Done | P1 | P0-007 |
| P0-009 | SMS sample dataset collection (anonymized) | QA | ✅ Done | P0 | — |
| P0-010 | Event store schema review | Reviewer | Not Started | P0 | P0-003 |
| P0-011 | Project documentation review | Reviewer | 🟡 In Progress | P0 | — |

### Phase 1 — Core SMS Infrastructure (V1 MVP)

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P1-001 | SMS BroadcastReceiver implementation | Developer | Dev Complete | P0 | P0-001 |
| P1-002 | SMS permission handling (request, explain, retry, settings redirect) | Developer | Dev Complete | P0 | P0-001 |
| P1-003 | SMS sender ID filter list | Developer | Dev Complete | P0 | P1-001 |
| P1-004 | Parser registry architecture (interface, registry, fallback chain) | Developer | Dev Complete | P0 | P0-001, P0-003 |
| P1-005 | GPay SMS parser | Developer | Dev Complete | P0 | P1-004 |
| P1-006 | PhonePe SMS parser | Developer | Dev Complete | P0 | P1-004 |
| P1-007 | Paytm SMS parser | Developer | Dev Complete | P0 | P1-004 |
| P1-008 | SBI bank SMS parser | Developer | Dev Complete | P1 | P1-004 |
| P1-009 | HDFC bank SMS parser | Developer | Dev Complete | P1 | P1-004 |
| P1-010 | ICICI bank SMS parser | Developer | Dev Complete | P1 | P1-004 |
| P1-011 | Axis bank SMS parser | Developer | Dev Complete | P1 | P1-004 |
| P1-012 | Fallback regex parser (heuristic) | Developer | Dev Complete | P1 | P1-004 |
| P1-013 | Transaction deduplication logic | Developer | Dev Complete | P0 | P1-004, P0-003 |
| P1-014 | Failed transaction detection | Developer | Dev Complete | P0 | P1-004 |
| P1-015 | Transaction DAO + Room entities | Developer | Dev Complete | P0 | P0-003 |
| P1-016 | Event store DAO (append-only event log) | Developer | Dev Complete | P0 | P0-003 |
| P1-017 | SMSReceived event production | Developer | Dev Complete | P0 | P1-001, P1-016 |
| P1-018 | TransactionDetected event production | Developer | Dev Complete | P0 | P1-004, P1-016 |
| P1-019 | ParseFailed / DuplicateDetected event production | Developer | Dev Complete | P1 | P1-004, P1-016 |
| P1-020 | Boot complete receiver (re-register SMS listener) | Developer | Dev Complete | P1 | P1-001 |
| P1-021 | Unit tests — GPay parser | QA | Dev Complete | P0 | P1-005 |
| P1-022 | Unit tests — PhonePe parser | QA | Dev Complete | P0 | P1-006 |
| P1-023 | Unit tests — Paytm parser | QA | Dev Complete | P0 | P1-007 |
| P1-024 | Unit tests — SBI parser | QA | Dev Complete | P1 | P1-008 |
| P1-025 | Unit tests — HDFC parser | QA | Dev Complete | P1 | P1-009 |
| P1-026 | Unit tests — ICICI parser | QA | Dev Complete | P1 | P1-010 |
| P1-027 | Unit tests — Axis parser | QA | Dev Complete | P1 | P1-011 |
| P1-028 | Unit tests — Fallback parser | QA | Dev Complete | P1 | P1-012 |
| P1-029 | Unit tests — Deduplication logic | QA | Dev Complete | P0 | P1-013 |
| P1-030 | Unit tests — Failed transaction detection | QA | Dev Complete | P0 | P1-014 |
| P1-031 | Integration test — SMS → Parse → Event → Store pipeline | QA | Dev Complete | P0 | P1-018 |
| P1-032 | Parser accuracy validation (against SMS sample dataset) | QA | Dev Complete | P0 | P1-005 to P1-012, P0-009 |
| P1-033 | Low-end device testing — SMS module | QA | Not Started | P1 | P1-031 |
| P1-034 | Code review — SMS listener + permission | Reviewer | Not Started | P0 | P1-021, P1-031 |
| P1-035 | Code review — Parser registry + all parsers | Reviewer | Not Started | P0 | P1-032 |
| P1-036 | Alignment check — Module 1 & 2 (SMS + Parsing) | Reviewer | Not Started | P0 | P1-034, P1-035 |
| P1-037 | Event model review — correct events, immutability | Reviewer | Not Started | P0 | P1-018, P1-019 |

### Phase 2 — Insights & Dashboard (V1)

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P2-001 | Aggregation automation (trigger on TransactionDetected) | Developer | Not Started | P0 | P1-018 |
| P2-002 | Daily income total computation | Developer | Not Started | P0 | P2-001 |
| P2-003 | Hourly distribution computation | Developer | Not Started | P0 | P2-001 |
| P2-004 | Day-over-day comparison | Developer | Not Started | P0 | P2-002 |
| P2-005 | Expected earnings baseline (14-day rolling weekday avg) | Developer | Not Started | P0 | P2-002 |
| P2-006 | Live run rate projection | Developer | Not Started | P1 | P2-003, P2-005 |
| P2-007 | Slow hour / drop detection | Developer | Not Started | P1 | P2-003 |
| P2-008 | Transaction count insight | Developer | Not Started | P0 | P2-001 |
| P2-009 | Average sale value insight | Developer | Not Started | P0 | P2-008 |
| P2-010 | Customer identification (UPI handle + name clustering) | Developer | Not Started | P0 | P1-018 |
| P2-011 | Repeat customer detection | Developer | Not Started | P1 | P2-010 |
| P2-012 | New vs returning classification | Developer | Not Started | P1 | P2-010 |
| P2-013 | Peak hour identification | Developer | Not Started | P1 | P2-003 |
| P2-014 | Idle time detection | Developer | Not Started | P1 | P1-018 |
| P2-015 | First & last sale time | Developer | Not Started | P0 | P1-018 |
| P2-016 | Weekly trend analysis (7d vs 7d) | Developer | Not Started | P1 | P2-002 |
| P2-017 | Best/worst day of week | Developer | Not Started | P2 | P2-002 |
| P2-018 | Payment method split (UPI vs bank) | Developer | Not Started | P2 | P1-018 |
| P2-019 | Consistency score computation | Developer | Not Started | P1 | P2-005, P2-003 |
| P2-020 | Main dashboard UI (Compose) — income card, comparison, score | Developer | Not Started | P0 | P2-002, P2-004 |
| P2-021 | Hourly breakdown UI | Developer | Not Started | P1 | P2-003 |
| P2-022 | Customer insights UI | Developer | Not Started | P1 | P2-011, P2-012 |
| P2-023 | Weekly trends UI | Developer | Not Started | P2 | P2-016 |
| P2-024 | Expected vs actual UI widget | Developer | Not Started | P1 | P2-005 |
| P2-025 | Empty state UI (no transactions yet) | Developer | Not Started | P0 | P2-020 |
| P2-026 | Unit tests — Aggregation engine | QA | Not Started | P0 | P2-001 |
| P2-027 | Unit tests — All 14 insight calculations | QA | Not Started | P0 | P2-002 to P2-019 |
| P2-028 | Unit tests — Customer clustering | QA | Not Started | P1 | P2-010 |
| P2-029 | UI tests — Dashboard layout and content | QA | Not Started | P1 | P2-020 |
| P2-030 | Integration test — Event → Aggregation → Projection → UI | QA | Not Started | P0 | P2-020 |
| P2-031 | Edge case testing — 0 transactions, 1 transaction, first day | QA | Not Started | P0 | P2-027 |
| P2-032 | Low-end device testing — Dashboard performance | QA | Not Started | P1 | P2-020 |
| P2-033 | Code review — Aggregation + Insights | Reviewer | Not Started | P0 | P2-026, P2-030 |
| P2-034 | Code review — Dashboard UI | Reviewer | Not Started | P1 | P2-029 |
| P2-035 | Alignment check — Modules 4, 5, 6 (Aggregation, Insights, UI) | Reviewer | Not Started | P0 | P2-033, P2-034 |
| P2-036 | Event model review — automation triggers, projections | Reviewer | Not Started | P0 | P2-001 |

### Phase 3 — Notifications (V1)

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P3-001 | Notification engine (WorkManager scheduling) | Developer | Not Started | P0 | P0-002 |
| P3-002 | Notification channel setup (3 channels) | Developer | Not Started | P0 | P0-001 |
| P3-003 | EOD summary notification | Developer | Not Started | P0 | P3-001, P2-002 |
| P3-004 | Mid-day underperformance alert | Developer | Not Started | P1 | P3-001, P2-005 |
| P3-005 | Inactivity alert | Developer | Not Started | P1 | P3-001, P2-014 |
| P3-006 | Hinglish notification templates | Developer | Not Started | P0 | P3-003 |
| P3-007 | Notification → app deep linking | Developer | Not Started | P1 | P3-003, P2-020 |
| P3-008 | NotificationSent event production | Developer | Not Started | P0 | P3-003, P1-016 |
| P3-009 | Boot complete re-scheduling | Developer | Not Started | P1 | P3-001, P1-020 |
| P3-010 | Unit tests — Notification scheduling | QA | Not Started | P0 | P3-001 |
| P3-011 | Unit tests — Notification content generation | QA | Not Started | P0 | P3-006 |
| P3-012 | Integration test — Event → Automation → Notification | QA | Not Started | P0 | P3-003 |
| P3-013 | Test — Notifications after device restart | QA | Not Started | P1 | P3-009 |
| P3-014 | Test — Battery impact of notification scheduling | QA | Not Started | P1 | P3-001 |
| P3-015 | Code review — Notification engine | Reviewer | Not Started | P0 | P3-010, P3-012 |
| P3-016 | Alignment check — Module 7 (Notifications) | Reviewer | Not Started | P0 | P3-015 |

### Phase 4 — QR System (V2)

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P4-001 | QR generator service (ZXing + UPI deep link) | Developer | Not Started | P0 | P0-001 |
| P4-002 | QR metadata tagging (viis-{id}-{date}-{rand}) | Developer | Not Started | P0 | P4-001 |
| P4-003 | QR generation UI (enter UPI ID, generate, display) | Developer | Not Started | P0 | P4-001 |
| P4-004 | QR save to gallery | Developer | Not Started | P1 | P4-001 |
| P4-005 | QR share via Android intent | Developer | Not Started | P1 | P4-001 |
| P4-006 | QR print support (high-res output) | Developer | Not Started | P2 | P4-001 |
| P4-007 | Enhanced matching engine (SMS ↔ QR correlation) | Developer | Not Started | P0 | P4-002, P1-018 |
| P4-008 | Exact match (reference_id ↔ metadata_tag) | Developer | Not Started | P0 | P4-007 |
| P4-009 | Fuzzy match (amount + time window) | Developer | Not Started | P1 | P4-007 |
| P4-010 | QR transaction store (qr_codes, qr_matches tables) | Developer | Not Started | P0 | P0-003, P4-002 |
| P4-011 | QRGenerated event production | Developer | Not Started | P0 | P4-001, P1-016 |
| P4-012 | QRTransactionMatched event production | Developer | Not Started | P0 | P4-007, P1-016 |
| P4-013 | Unit tests — QR generation + UPI format | QA | Not Started | P0 | P4-001 |
| P4-014 | Unit tests — Matching engine (exact + fuzzy) | QA | Not Started | P0 | P4-007 |
| P4-015 | Integration test — QR generate → customer pays → SMS → match | QA | Not Started | P0 | P4-012 |
| P4-016 | UPI app compatibility testing (GPay, PhonePe, Paytm) | QA | Not Started | P0 | P4-001 |
| P4-017 | Code review — QR system | Reviewer | Not Started | P0 | P4-013, P4-015 |
| P4-018 | Alignment check — Module 10 (QR System) | Reviewer | Not Started | P0 | P4-017 |

### Phase 5 — Polish & Launch

| Task ID | Task Name | Agent | Status | Priority | Dependencies |
|---|---|---|---|---|---|
| P5-001 | Onboarding flow UI (welcome screens, trust messaging) | Developer | Not Started | P0 | P2-020 |
| P5-002 | Hindi/Hinglish string localization (all UI text) | Developer | Not Started | P0 | P2-020 |
| P5-003 | Settings screen (notifications, language, data) | Developer | Not Started | P1 | P2-020, P3-001 |
| P5-004 | Low-end device optimization (memory, battery) | Developer | Not Started | P0 | All Phase 1-4 |
| P5-005 | Offline reliability hardening | Developer | Not Started | P0 | All Phase 1-3 |
| P5-006 | App icon & branding | Developer | Not Started | P1 | — |
| P5-007 | ProGuard/R8 configuration | Developer | Not Started | P1 | P5-004 |
| P5-008 | Full regression test suite | QA | Not Started | P0 | All Phase 1-4 |
| P5-009 | Performance benchmarks (parse latency, insight compute, cold start) | QA | Not Started | P0 | P5-004 |
| P5-010 | Low-end device full test pass | QA | Not Started | P0 | P5-004 |
| P5-011 | Security review (no PII in logs, secure storage) | QA | Not Started | P0 | All |
| P5-012 | Alignment check — Module 8 (Privacy & Trust) | Reviewer | Not Started | P0 | P5-011 |
| P5-013 | Alignment check — Module 9 (Offline) | Reviewer | Not Started | P0 | P5-005 |
| P5-014 | Alignment check — Module 11 (Performance) | Reviewer | Not Started | P0 | P5-009 |
| P5-015 | Final architecture review | Reviewer | Not Started | P0 | All |
| P5-016 | Launch checklist verification | Reviewer | Not Started | P0 | All P5 tasks |

---

## Summary

| Phase | Developer | QA | Reviewer | Total |
|---|---|---|---|---|
| Phase 0 | 5 | 3 | 3 | 11 |
| Phase 1 | 20 | 13 | 4 | 37 |
| Phase 2 | 25 | 7 | 4 | 36 |
| Phase 3 | 9 | 5 | 2 | 16 |
| Phase 4 | 12 | 4 | 2 | 18 |
| Phase 5 | 7 | 4 | 5 | 16 |
| **Total** | **78** | **36** | **20** | **134** |

---

## Activity Log

```
[2026-03-28 00:05] [SYSTEM] Project initialized. SRS document created.
[2026-03-28 00:10] [SYSTEM] Architecture planning begins. Event modeling approach adopted.
[2026-03-28 00:30] [SYSTEM] Documentation framework created:
  - architecture.md (HLD, LLD, event model, swimlanes)
  - decision-framework.md (42 decisions across 3 door types)
  - alignment-spec.md (11-module review checklist)
  - agents.md (3-agent system with workflow)
  - status.md (134 tasks across 6 phases)
  - 24 user flow documents
[2026-03-28 00:30] [REVIEWER] P0-011: Project documentation review — In Progress
[2026-03-28 10:00] [DEVELOPER] COMPLETED P0-001, P0-002, P0-003: Android project scaffolding
  Branch: main (scaffolding committed directly to main before feature branch workflow activated)
  Commit: bd1e812
  Deliverables: Gradle project structure, domain event sealed classes (16 types), Room entities (5),
    DAOs (5), ViisDatabase, Hilt DatabaseModule, SmsReceiver/BootReceiver stubs, AndroidManifest,
    Hinglish strings, proguard rules
  Notes: All architectural decisions followed — append-only events, hashed customer IDs, no PII in logs
[2026-03-28 14:30] [DEVELOPER] COMPLETED P1-013, P1-014: Deduplication + failed transaction detection
  Branch: feature/P1-013-014-dedup-failed-detection
  Deliverables:
    - FailedTransactionDetector: 12 failure keyword patterns (object, no injection)
    - DeduplicationChecker: 5-minute window, amount+type+upiHash matching (or source fallback)
    - EventDao.getTransactionDetectedInWindow(): query for dedup window
    - EventRepository + EventRepositoryImpl: new method added
    - ParseSmsUseCase: rewritten as 4-step pipeline (failed → parse → dedup → emit)
    - FailedTransactionDetectorTest: 14 tests
    - DeduplicationCheckerTest: 7 tests
    - ParseSmsUseCaseTest: updated — DeduplicationChecker mock added, 2 new tests
[2026-03-28 15:30] [DEVELOPER] COMPLETED P1-015–P1-020: Transaction projection, event pipeline, boot receiver
  Branch: feature/P1-015-020-transaction-projection-events-boot
  Deliverables:
    - TransactionEntity: fixed schema — added type, renamed upiHandle→upiHandleHash,
      senderName→rawSenderMasked, removed status index (now indexed by type)
    - TransactionDao: removed findDuplicate (superseded by EventDao dedup), queries updated to type='CREDIT'
    - ViisDatabase: bumped to version 2; DatabaseModule uses fallbackToDestructiveMigration()
    - TransactionProjector: projects ParsedTransaction → TransactionEntity after TransactionDetected event
    - ParseSmsUseCase: wires TransactionProjector in step 4; event ID shared between event + projection
    - SmsProcessingWorker: produces SMSReceived event (audit trail) before calling ParseSmsUseCase
    - BootReceiver: fully implemented — initializes WorkManager, guards on BOOT_COMPLETED action
    - TransactionProjectorTest: 10 tests covering all mapped fields
    - ParseSmsUseCaseTest: updated with TransactionProjector mock
[2026-03-28 16:30] [QA] COMPLETED P1-021–P1-028: Parser unit tests for all 8 parsers
  Branch: feature/P1-021-028-parser-unit-tests
  Deliverables (expanded existing stubs with full coverage):
    - GPaySmsParserTest: +3 tests — lowercase hash verification, debit UPI hash, timestamp preserved (14 total)
    - PhonePeSmsParserTest: +8 tests — hash exact values for all 3 patterns, declined, ref ID, comma amount, masking (15 total)
    - PaytmSmsParserTest: +7 tests — canParse, failed/declined, decimal, masking, null upiHandleHash (12 total)
    - BankSmsParserTest: +9 tests — HDFC/ICICI/Axis debit, ICICI/Axis failed/declined, comma amounts, canParse (23 total)
    - FallbackSmsParserTest: +7 tests — ₹ symbol, comma amount, paid/added keywords, declined, masking (16 total)
[2026-03-28 17:30] [QA] COMPLETED P1-031, P1-032: Integration + accuracy tests
  Branch: feature/P1-031-032-integration-accuracy-tests
  Deliverables:
    - SmsPipelineIntegrationTest: 20 tests — full pipeline with real parsers + real registry,
      mocked persistence only. Covers all 8 sources, failed pre-check, dedup window,
      ParseFailed event, payload privacy (no raw UPI handle), TransactionProjector wiring.
    - ParserAccuracyTest: 16 tests — loads every SMS sample file from test/resources,
      validates correct source/type/amount per dataset. Non-financial and ambiguous samples
      confirmed null. Duplicate sample confirmed parseable (dedup is use-case concern).
      Acts as regression guard against regex changes.
[2026-03-28 10:30] [DEVELOPER] COMPLETED P0-004: CI/CD pipeline (GitHub Actions)
  Branch: main
  Deliverables: .github/workflows/ci.yml — 3 jobs: lint (ktlint), unit-tests, build debug APK
[2026-03-28 10:30] [DEVELOPER] COMPLETED P0-005: Linting & code style (ktlint)
  Branch: main
  Deliverables: ktlint plugin added to libs.versions.toml + app/build.gradle.kts; .editorconfig with 120-char limit
[2026-03-28 10:30] [QA] COMPLETED P0-007: Test framework setup (JUnit5, MockK, Turbine)
  Branch: main
  Deliverables: BaseUnitTest, MainDispatcherRule, EventTestUtils, FlowTestUtils in test/util/
[2026-03-28 10:30] [QA] COMPLETED P0-008: Low-end device test profile setup
  Branch: main
  Deliverables: docs/test-profiles.md with AVD spec (2GB RAM, API 23), performance thresholds, offline checklist
[2026-03-28 10:30] [QA] COMPLETED P0-009: SMS sample dataset collection
  Branch: main
  Deliverables: app/src/test/resources/sms-samples/ — GPay, PhonePe, Paytm, SBI, HDFC, ICICI, Axis, unknown
    All samples anonymized (fake UPI handles, fake names, fake amounts)
[2026-03-28 12:00] [DEVELOPER] COMPLETED P1-001, P1-003: SMS BroadcastReceiver + sender filter
  Branch: feature/P1-001-sms-broadcast-receiver
  Handoff to: QA
  Notes: SmsReceiver filters by sender ID, enqueues SmsProcessingWorker via WorkManager.
    SmsSenderFilter covers 15 known financial senders (covers P1-003).
    EventRepository created for append-only writes.
    SMSReceived event produced for every financial SMS.
[2026-03-28 13:30] [DEVELOPER] COMPLETED P1-004: Parser registry architecture
  Branch: feature/P1-004-parser-registry
  Handoff to: QA
  Notes: SmsParser interface, ParserRegistry (priority-ordered, exception-safe), ParseResult sealed class,
    ParseSmsUseCase (orchestrates parse + TransactionDetected/ParseFailed events), HashUtils (SHA-256),
    ParserModule (empty set, ready for P1-005+). SmsProcessingWorker updated to call ParseSmsUseCase.
[2026-03-28 13:00] [DEVELOPER] COMPLETED P1-002: SMS permission handling
  Branch: feature/P1-002-sms-permission-handling
  Handoff to: QA
  Notes: SmsPermissionScreen (3 states: NeedsRequest, NeedsRationale, PermanentlyDenied).
    SmsPermissionViewModel produces PermissionGranted/PermissionDenied events.
    Settings redirect implemented for permanently denied state. Hinglish UI strings added.
```

---

## Completed Tasks

| Task ID | Completed At | Notes |
|---|---|---|
| P0-001 | 2026-03-28 | Android project scaffolding — 29 files, 1027 lines |
| P0-002 | 2026-03-28 | Hilt DI module with all 5 DAOs |
| P0-003 | 2026-03-28 | Room DB: EventEntity (append-only), 4 projection entities |
| P0-004 | 2026-03-28 | GitHub Actions CI: lint + test + build |
| P0-005 | 2026-03-28 | ktlint + .editorconfig |
| P0-007 | 2026-03-28 | Test base classes + event utilities + Flow test utils |
| P0-008 | 2026-03-28 | Low-end device profile doc (2GB RAM, API 23) |
| P0-009 | 2026-03-28 | SMS sample dataset — 7 sources, credit/debit/failed/duplicate |
| P1-001 | 2026-03-28 | SmsReceiver + SmsProcessingWorker + EventRepository + DI modules |
| P1-003 | 2026-03-28 | SmsSenderFilter — 15 financial sender IDs, case-insensitive |

---

## Blocked Tasks

_None yet._
