# Alignment Specification

## Purpose

This is the **single source of truth** for the Reviewer Agent. Every feature, component, and behavior must be checked against this spec before approval. The Reviewer uses this document as a checklist during every review cycle.

## How to Use

1. Identify which modules are affected by the PR/feature
2. Check **every item** in the relevant sections
3. Mark as: ✅ Pass | ❌ Fail | ⚠️ Partial
4. Any ❌ → **NEEDS REWORK** (mandatory)
5. Any ⚠️ → requires documented justification
6. Produce a review report (template at bottom)

---

## Module 1: SMS Listener & Permission Handling

### Functional Alignment
- [ ] App requests SMS read permission on first launch (after onboarding screens)
- [ ] Permission request includes clear Hinglish explanation of why needed
- [ ] Graceful handling when permission denied first time (show explanation, re-request)
- [ ] Handles permanent denial (guides user to system Settings)
- [ ] SMS listener activates immediately after permission granted
- [ ] Filters ONLY financial SMS by sender ID (AD-SBIINB, AD-GPAY, etc.)
- [ ] Does NOT read personal/non-financial SMS
- [ ] Produces `SMSReceived` event with raw body, sender ID, timestamp
- [ ] Works on Android 6.0+ (API 23+)
- [ ] Handles boot complete (re-registers listener after device restart)

### Event Model Alignment
- [ ] `SMSReceived` event is stored BEFORE any parsing begins
- [ ] Event is immutable — never modified after creation
- [ ] Event contains sufficient data to re-parse if needed

### Non-Functional Alignment
- [ ] Permission flow completes in < 5 taps (happy path)
- [ ] No confusing technical language in permission dialogs
- [ ] Trust messaging present: "Hum sirf payment messages padhte hain"
- [ ] Battery-efficient (BroadcastReceiver, not polling)
- [ ] Works on low-end devices (2-3GB RAM)

---

## Module 2: Parsing Engine

### Functional Alignment
- [ ] Parser registry pattern implemented (one parser per SMS source)
- [ ] Supports GPay SMS format (sender: AD-GPAY, VD-GPAY, VM-GPAY, JD-GPAY, BZ-GPAY)
- [ ] Supports PhonePe SMS format (sender: AD-PHONEPE)
- [ ] Supports Paytm SMS format (sender: AD-PAYTM)
- [ ] Supports SBI bank SMS format (sender: AD-SBIINB, AD-SBIPSG)
- [ ] Supports HDFC bank SMS format (sender: AD-HDFCBK)
- [ ] Supports ICICI bank SMS format (sender: AD-ICICIB)
- [ ] Supports Axis bank SMS format (sender: AD-AXISBK)
- [ ] Fallback regex parser for unknown formats
- [ ] Fallback chain: exact sender match → regex → heuristic → ParseFailed event
- [ ] Extracts: amount (mandatory)
- [ ] Extracts: sender name / UPI handle (when available)
- [ ] Extracts: timestamp (mandatory)
- [ ] Extracts: payment source — UPI/NEFT/IMPS/BANK (mandatory)
- [ ] Extracts: transaction reference ID (when available)
- [ ] Extracts: status — SUCCESS/FAILED/PENDING (mandatory)
- [ ] Handles multilingual SMS (English/Hinglish)
- [ ] Unknown format SMS produces `ParseFailed` event (not crash, not silent drop)
- [ ] Only CREDIT transactions detected (not debits)

### Event Model Alignment
- [ ] Successful parse produces `TransactionDetected` event
- [ ] Failed/declined SMS produces `TransactionFailed` event
- [ ] Unparseable SMS produces `ParseFailed` event
- [ ] Duplicate SMS produces `DuplicateDetected` event
- [ ] All events are immutable and contain full context
- [ ] Events link back to originating `SMSReceived` event

### Deduplication Alignment
- [ ] Duplicate detection uses: same amount + same sender + within time window
- [ ] Time window is configurable (default: 5 minutes)
- [ ] If reference_id available, uses exact match on reference_id
- [ ] Duplicates produce `DuplicateDetected` event (auditable, not silent)
- [ ] No double counting of same transaction in any read model

### Failed Transaction Alignment
- [ ] Failed/declined/pending keywords detected correctly
- [ ] Failed transactions excluded from income totals in all projections
- [ ] Failed transactions produce their own event type
- [ ] Failed transactions visible in UI with clear "failed" label

### Accuracy Alignment
- [ ] ≥ 90% correct detection for supported SMS sources
- [ ] Graceful degradation for unknown formats (ParseFailed, not crash)
- [ ] Parsing latency < 1 second per message
- [ ] Parser test suite covers real SMS samples from each supported source

---

## Module 3: Event Store

### Data Model Alignment
- [ ] `events` table: event_id, event_type, timestamp, payload (JSON), version, created_at
- [ ] Events are immutable — no UPDATE or DELETE operations on event table
- [ ] `transactions` projection: id, event_id, amount, timestamp, sender_name, upi_handle, source, reference_id, status
- [ ] `customer_profiles` projection: customer_id (hashed), display_name, upi_handle, first_seen, last_seen, transaction_count, total_amount
- [ ] `daily_summaries` projection: date, total_income, transaction_count, avg_transaction_value, peak_hour, first_txn_time, last_txn_time, expected_income, run_rate_projection, consistency_score, new_customers, returning_customers
- [ ] `hourly_stats` projection: date, hour_block, txn_count, total_amount
- [ ] Proper indexing on: events(event_type, timestamp), transactions(timestamp, status), customer_profiles(transaction_count)
- [ ] Data stored locally in Room/SQLite only
- [ ] No data sent to external servers without explicit user consent
- [ ] Projections can theoretically be rebuilt from event log

### Event Model Alignment
- [ ] Event store is the single source of truth
- [ ] Read model projections are derived from events
- [ ] Events use JSON serialization for payloads
- [ ] Event IDs are unique (UUID or similar)

---

## Module 4: Aggregation Automation

### Functional Alignment
- [ ] Triggers on every `TransactionDetected` event (real-time update)
- [ ] Calculates total daily income (SUCCESS transactions only, digital only)
- [ ] Calculates hourly distribution (by 1-hour blocks)
- [ ] Calculates day-over-day comparison
- [ ] Updates read model projections incrementally (not full rebuild per transaction)
- [ ] Produces `DailySummaryComputed` event
- [ ] Produces `HourlyStatsUpdated` event
- [ ] Computation completes in < 2 seconds

### Event Model Alignment
- [ ] Aggregation is triggered by events (not polling or manual trigger)
- [ ] Results stored as new events AND reflected in read model projections
- [ ] Aggregation is idempotent — reprocessing same event doesn't corrupt state

---

## Module 5: Insights Engine

### 5a: Expected Earnings Baseline
- [ ] Shows "Expected by now" vs "Actual by now"
- [ ] Based on rolling 14-day same-weekday average
- [ ] Computes expected at current hour (not just EOD)
- [ ] Minimum 7 data points before showing (graceful empty state otherwise)
- [ ] Produces `InsightGenerated` event with type "expected_earnings"

### 5b: Live Run Rate Projection
- [ ] Predicts end-of-day earnings using current pace + historical curve
- [ ] Weighted: 60% current pace, 40% historical pattern
- [ ] Updates with every new `TransactionDetected` event
- [ ] Shows meaningful projection only after 2+ transactions today

### 5c: Slow Hour / Drop Detection
- [ ] Detects deviations vs historical hourly averages
- [ ] Flags when current hour is > 30% below historical average for that hour
- [ ] Threshold is dynamic (based on historical variance, not hardcoded)
- [ ] Produces `InsightGenerated` event with type "slow_hour"

### 5d: Transaction Count
- [ ] Shows total number of SUCCESS transactions per day
- [ ] Excludes FAILED and PENDING
- [ ] Updates in real-time with each new transaction

### 5e: Average Sale Value
- [ ] Correctly computes: total_amount / transaction_count
- [ ] Only includes SUCCESS transactions
- [ ] Handles edge case: 0 transactions (show "—" not crash/NaN)

### 5f: Repeat Customer Detection
- [ ] Identifies repeat payers via UPI handle (primary) / name clustering (secondary)
- [ ] Ranks top customers by frequency
- [ ] Ranks top customers by total value
- [ ] Customer classified as "repeat" at 2+ transactions
- [ ] Customer classified as "regular" at 5+ transactions in 30 days

### 5g: New vs Returning Customers
- [ ] Classifies each transaction's customer as new (first_seen == today) or returning
- [ ] Shows daily ratio/breakdown
- [ ] Produces `CustomerIdentified` event with is_new flag

### 5h: Peak Hour Identification
- [ ] Identifies highest earning 1-hour block for today
- [ ] Tie-breaking: earlier hour wins
- [ ] Shows in simple format: "6-7 PM"

### 5i: Idle Time Detection
- [ ] Detects gaps between consecutive transactions exceeding threshold
- [ ] Default threshold: 2 hours
- [ ] Dynamic threshold: avg_gap × 2.5 (after sufficient data)
- [ ] Produces `IdleDetected` event when threshold exceeded

### 5j: First & Last Sale Time
- [ ] Shows earliest transaction timestamp today
- [ ] Shows latest transaction timestamp today
- [ ] Format: "8:15 AM" / "9:30 PM" (simple, 12-hour)

### 5k: Weekly Trend Analysis
- [ ] Compares rolling 7-day total vs previous 7 days
- [ ] Shows trend direction: up / down / flat (within 5% = flat)
- [ ] Requires minimum 7 days of data to show
- [ ] Shows absolute difference and percentage

### 5l: Best/Worst Day of Week
- [ ] Computes average earnings per weekday (using 28-day history)
- [ ] Identifies best earning day and worst earning day
- [ ] Shows in Hinglish: "Sabse achha din: Shanivar"

### 5m: Payment Method Split
- [ ] Classifies transactions by source: UPI vs Bank Transfer
- [ ] Shows percentage and absolute amounts
- [ ] Based on SMS source identification

### 5n: Daily Consistency Score
- [ ] Composite score (0-100) based on: expected vs actual (40%), stability (30%), activity (30%)
- [ ] Score explained in simple language: "Achha din" (>75), "Theek hai" (50-75), "Slow din" (<50)
- [ ] Produces `InsightGenerated` event with type "consistency_score"

---

## Module 6: UI / Dashboard

### UX Alignment
- [ ] **Primary view is a single scrollable screen** with expandable cards
- [ ] "Aaj ₹X aaya" format for daily income (prominent, top of screen)
- [ ] "Kal se ₹Y zyada/kam" for day-over-day comparison
- [ ] "Aaj slow chal raha hai" for underperformance (when applicable)
- [ ] "Aapka peak time: X-Y PM" for peak hour
- [ ] **No complex charts** — simple cards, large numbers, color coding only
- [ ] Large text throughout (minimum 16sp body, 24sp+ headers)
- [ ] Minimal navigation (dashboard → detail, max 2 levels deep)
- [ ] Works on low-end Android devices (2-3GB RAM)
- [ ] No setup or configuration required from user to see dashboard
- [ ] No financial jargon anywhere in the UI
- [ ] Indian number formatting: ₹1,23,456 (with lakhs/crores commas)
- [ ] 12-hour time format: "8:15 AM" not "08:15"
- [ ] Empty states are helpful, not confusing ("Jaise hi payment aayega, yahan dikhega")

### Reactivity Alignment
- [ ] Dashboard updates in real-time when new `TransactionDetected` event occurs
- [ ] UI observes read model projections via Flow/LiveData
- [ ] No manual refresh needed — updates are automatic
- [ ] Pull-to-refresh available but not required

---

## Module 7: Notifications

### Functional Alignment
- [ ] **EOD Summary**: Sends at 9 PM with daily total, transaction count, comparison
- [ ] **Mid-Day Alert**: Sends at 2 PM when actual < 70% of expected
- [ ] **Inactivity Alert**: Sends when gap > dynamic threshold during active hours
- [ ] All notifications use Hinglish text
- [ ] EOD template: "Aaj ₹{amount} aaya, {count} transactions. {comparison}"
- [ ] Alert template: "Aaj slow chal raha hai. Ab tak ₹{actual}, ₹{expected} expected tha."
- [ ] Inactivity template: "Koi payment nahi aaya {duration} se."
- [ ] Tapping notification opens relevant app screen
- [ ] Each notification type produces `NotificationSent` event

### Event Model Alignment
- [ ] EOD triggered by `DayEnded` event (not just a timer)
- [ ] Mid-day triggered by `MidDayReached` event + `UnderperformanceDetected`
- [ ] Inactivity triggered by `IdleDetected` event
- [ ] All notifications logged as events for auditability

### Non-Functional
- [ ] Uses WorkManager for reliable scheduling
- [ ] Survives device restart (re-schedules on boot)
- [ ] 3 notification channels: Summary (default), Alerts (high), System (low)
- [ ] Doesn't drain battery excessively
- [ ] Respects DND mode (except high-importance alerts)

---

## Module 8: Privacy & Trust

### Critical Alignment (MUST PASS ALL — any ❌ is a hard blocker)
- [ ] **No access to bank accounts or funds** — app is read-only on SMS
- [ ] **No interception or routing of payments** — money flows directly bank-to-bank
- [ ] **SMS processed entirely on-device** — no raw SMS sent to any server
- [ ] **Clear consent** before any data access (permission dialog + onboarding)
- [ ] **Transparent usage messaging** in Hindi/Hinglish during onboarding
- [ ] **"Paisa seedha aapke account mein aata hai"** messaging present in onboarding
- [ ] **No bank login required** — stated explicitly
- [ ] **Secure local storage** — Room DB with app-private storage
- [ ] **No PII in logs** — amounts, UPI handles, names never in production logs
- [ ] **Customer IDs are hashed** — not storing raw UPI handles as identifiers
- [ ] **Optional sync is opt-in** — no automatic data upload without consent

---

## Module 9: Offline & Connectivity

- [ ] **All core features work completely offline** — SMS processing, parsing, storage, aggregation, insights, UI
- [ ] SMS processing works without internet (SMS is carrier-delivered, not internet)
- [ ] All insights computed locally on-device
- [ ] Notifications work offline (local scheduling)
- [ ] No data loss during offline periods
- [ ] When internet available, sync is optional and user-initiated or explicitly opted-in
- [ ] Sync failure doesn't affect core app functionality
- [ ] App works if user has NEVER had internet since install

---

## Module 10: QR System (V2)

### QR Generation
- [ ] Generates UPI QR using `upi://pay?pa={upi_id}&pn={name}&tr={metadata}` format
- [ ] Vendor enters/confirms their UPI ID
- [ ] QR embeds metadata tag for transaction correlation
- [ ] QR image displayable on screen
- [ ] QR works with all major UPI apps (GPay, PhonePe, Paytm)
- [ ] Produces `QRGenerated` event

### QR Distribution
- [ ] Save QR as PNG to device gallery
- [ ] Share via Android share intent (WhatsApp, Bluetooth, etc.)
- [ ] Printable resolution (minimum 300 DPI equivalent)

### Transaction Matching
- [ ] Matches `TransactionDetected` events with active QR codes
- [ ] Exact match: reference_id contains QR metadata_tag (confidence: 1.0)
- [ ] Fuzzy match: amount + time window correlation (confidence: 0.7-0.9)
- [ ] Unmatched transactions fall back to SMS-only path (V1 behavior)
- [ ] Produces `QRTransactionMatched` event with confidence score

### Customer Patterns (V2 Enhanced)
- [ ] Repeat customers identified via QR-tagged UPI handles
- [ ] Groups: regular (5+ in 30d) vs occasional (2-4) vs one-time (1)
- [ ] Enhanced accuracy over SMS-only identification

---

## Module 11: Performance

- [ ] SMS parsing latency < 1 second per message
- [ ] Daily insight computation (all 14) < 2 seconds
- [ ] App cold start < 3 seconds on low-end device
- [ ] App runs on 2-3GB RAM devices without OOM
- [ ] APK size < 15MB (important for low-storage devices)
- [ ] Battery usage: < 2% per day from background SMS listening
- [ ] Storage growth: < 1MB per month of active use (for event store)
- [ ] UI: 60fps scrolling, no dropped frames on dashboard
- [ ] No ANR (Application Not Responding) in any flow

---

## Cross-Cutting Concerns

### Event Modeling Compliance
- [ ] All state changes captured as immutable events
- [ ] Read models are projections, not primary data
- [ ] Commands produce events (not direct state mutations)
- [ ] Automations react to events (not polling or timers alone)
- [ ] Events contain sufficient context to rebuild projections
- [ ] Event schema is versioned (version field in events table)

### Code Quality
- [ ] Follows Kotlin coding conventions (ktlint compliant)
- [ ] Proper error handling — no silent failures, no swallowed exceptions
- [ ] Logging via Timber — no sensitive data (amounts, names, UPI) in production logs
- [ ] No hardcoded values — thresholds, timings, formats use constants/config
- [ ] Dependency injection via Hilt — no manual instantiation of services
- [ ] Coroutines used correctly — no blocking on main thread

### Testing
- [ ] Unit tests for every parser (with real SMS samples)
- [ ] Unit tests for aggregation logic
- [ ] Unit tests for each of the 14 insight calculations
- [ ] Unit tests for deduplication logic
- [ ] Unit tests for customer clustering
- [ ] Integration tests: SMS → parse → event → projection pipeline
- [ ] Edge case coverage: empty SMS, malformed amounts, unknown formats, boundary dates
- [ ] Test coverage ≥ 80% for core modules (parsing, aggregation, insights)
- [ ] Tests verify event correctness (right events produced with right payloads)

### Architecture
- [ ] Follows event-sourced architecture from `architecture.md`
- [ ] Adheres to all DECIDED 1-way door decisions in `decision-framework.md`
- [ ] No unauthorized architectural deviations
- [ ] Clean layer separation: UI → ViewModel → UseCase → Repository → EventStore
- [ ] No business logic in UI layer
- [ ] No direct database access from ViewModels (must go through repository/usecase)

---

## Review Report Template

```markdown
## Review Report — {Task ID}

**Date**: YYYY-MM-DD
**Reviewer**: Reviewer Agent
**Feature**: {feature name}

### Modules Checked
- [ ] Module X: {name}
- [ ] Module Y: {name}

### Results
| Module | Items Checked | ✅ Pass | ❌ Fail | ⚠️ Partial |
|--------|--------------|---------|---------|------------|
| Module X | X/Y | X | X | X |
| Module Y | X/Y | X | X | X |
| **Total** | **X/Y** | **X** | **X** | **X** |

### Event Model Compliance
- [ ] Correct events produced
- [ ] Events immutable
- [ ] Projections derived correctly
- [ ] Automations trigger correctly

### Architecture Compliance
- Pass / Fail — {details}

### Decision Compliance
- Pass / Fail — {details}

### Verdict
**APPROVED** / **NEEDS REWORK** / **BLOCKED**

### Feedback
1. {specific, actionable item}
2. {specific, actionable item}

### Blocking Issues (if any)
- {what's blocking and what's needed to unblock}
```
