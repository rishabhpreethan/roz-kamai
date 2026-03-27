# Roz Kamai — Vendor Income Intelligence System (VIIS)

Zero-input income tracking for small Indian vendors (kirana stores, street vendors, carts). Passively detects payments via SMS and delivers daily insights — no manual entry, no bank login required.

> "Aaj ₹12,350 aaya, 42 transactions. Kal se ₹2,100 zyada."

---

## What It Does

- **Reads financial SMS** from UPI apps (GPay, PhonePe, Paytm) and banks (SBI, HDFC, ICICI, Axis)
- **Parses transactions automatically** — amount, sender, timestamp, source
- **Computes 14 daily insights** — expected vs actual earnings, run rate projection, peak hours, repeat customers, weekly trends, consistency score, and more
- **Sends Hinglish notifications** — end-of-day summary, mid-day alerts, inactivity alerts
- **Works completely offline** — SMS doesn't need internet; all processing is on-device
- **V2: QR-based tracking** — vendor-specific UPI QR with embedded metadata for enhanced accuracy

---

## Architecture

Event-modeled, offline-first Android app:

```
SMS arrives → SMSReceived event → ParseSMS command → TransactionDetected event
    → AggregationAutomation → ComputeInsights → Read Model Projections → UI
```

- **Events** are the source of truth (immutable SQLite log)
- **Read models** (Dashboard, Hourly, Customers) are projections derived from events
- **Automations** react to events to trigger downstream processing
- **Local-first** — no internet dependency for any core feature

See [`docs/architecture.md`](docs/architecture.md) for full HLD, LLD, event catalog, swimlane diagrams.

---

## Project Structure

```
roz-kamai/
├── srs.md                        # Product requirements
├── docs/
│   ├── architecture.md           # HLD + LLD + event model + swimlanes
│   ├── decision-framework.md     # 1-way / 1.5-way / 2-way door decisions
│   ├── alignment-spec.md         # Reviewer checklist (11 modules)
│   ├── agents.md                 # 3-agent system (Developer, QA, Reviewer)
│   ├── status.md                 # Task board + activity log (134 tasks)
│   └── user-flows/               # 24 user flow docs with mermaid diagrams
│       ├── 01-onboarding.md
│       ├── 02-sms-permission-grant.md
│       ├── ...
│       └── 24-app-settings.md
└── .claude/
    └── agents/
        ├── developer.md          # Developer agent definition
        ├── qa.md                 # QA agent definition
        └── reviewer.md          # Reviewer agent definition
```

---

## Development System

This project uses a **3-agent development workflow**:

| Agent | Role | Works On |
|---|---|---|
| **Developer** | Implements features, writes code | Implementation tasks |
| **QA** | Tests features, validates spec compliance | Testing tasks |
| **Reviewer** | Reviews code, checks alignment | Review tasks |

Flow: `Developer → QA → Reviewer → ✅ Done` (or `→ 🔁 Back to Developer`)

See [`docs/status.md`](docs/status.md) for the full task board.

---

## Key Documents

| Document | Purpose |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | System design — event model, components, SQL schema, mermaid diagrams |
| [`docs/decision-framework.md`](docs/decision-framework.md) | All 42 architectural decisions with reversal cost classification |
| [`docs/alignment-spec.md`](docs/alignment-spec.md) | Reviewer checklist — 11 modules, every functional requirement |
| [`docs/status.md`](docs/status.md) | 134 tasks across 6 phases, agent assignments, activity log |
| [`docs/user-flows/`](docs/user-flows/) | 24 user flows with acceptance criteria and edge cases |

---

## Tech Stack (planned)

- **Platform**: Android (Kotlin), min API 23 (Android 6.0)
- **UI**: Jetpack Compose
- **Storage**: Room (SQLite) — event store + read model projections
- **Background**: WorkManager
- **DI**: Hilt
- **Architecture**: MVVM + Event Sourcing + Clean Architecture
- **V2 QR**: ZXing

---

## Versions

- **V1 (MVP)**: SMS parsing → daily income summary → basic notifications
- **V2**: QR code generation → enhanced transaction matching → repeat customer detection

---

## Target Users

Small vendors with ₹5k–₹50k/day revenue, low tech literacy, Android devices (2–3GB RAM), unreliable internet, WhatsApp-native UX expectations.
