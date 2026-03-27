# Software Requirements Specification (SRS)

## Product Name

Vendor Income Intelligence System (VIIS)

---

# 1. Overview

## 1.1 Purpose

This document defines the requirements for a system that provides zero-input income tracking and insights for small vendors (kirana stores, street vendors, carts) using passive data sources.

## 1.2 Scope

The system will be built in two phases:

* **Version 1 (V1): SMS-Based Income Detection**
* **Version 2 (V2): QR-Based Enhanced Tracking Layer**

The product focuses on:

* Zero manual input
* Passive data extraction
* Daily actionable insights

---

# 2. Target Users

## 2.1 Primary Users

* Small vendors (₹5k–₹50k/day revenue)
* Kirana stores (low-tech usage)
* Street vendors (chai, snacks, carts)

## 2.2 User Constraints

* Low tech literacy
* No time for manual input
* Prefer WhatsApp-like simplicity
* High sensitivity to trust (money-related)

---

# 3. Version 1: SMS-Based System

## 3.1 Objective

Automatically detect incoming payments via SMS and provide daily income insights.

---

## 3.2 Functional Requirements

### 3.2.1 SMS Access

* System shall request permission to read SMS (Android compliant categories)
* System shall filter financial SMS from banks, UPI apps (GPay, PhonePe, Paytm), and wallets

### 3.2.2 Transaction Detection

* Detect incoming payment messages (credit only)
* Extract:

  * Amount
  * Sender name / UPI handle (if available)
  * Timestamp
  * Payment source (UPI / bank)
  * Transaction reference (if present)

### 3.2.3 Parsing Engine (India-Specific)

* Support heterogeneous SMS formats from major Indian banks and UPI apps
* Maintain parser library per source (e.g., GPay, PhonePe, Paytm, SBI, HDFC, ICICI, Axis)
* Use rule-based + heuristic + fallback regex matching
* Handle multilingual SMS where applicable (English/Hinglish primarily in V1)

### 3.2.4 Income Aggregation

* Calculate:

  * Total daily income (digital only)
  * Hourly distribution
  * Day-over-day comparison

### 3.2.5 Deduplication

* Detect duplicate SMS (same amount, timestamp window, reference id)
* Avoid double counting

### 3.2.6 Failed Transaction Detection

* Identify failed/declined/pending transactions
* Exclude from income totals

### 3.2.7 Derived Business Insights (Zero-Input)

System shall compute the following insights using only passive SMS data:

#### a) Expected Earnings (Baseline Model)

* “Expected by now” vs “Actual by now”
* Based on historical same-day/time patterns

#### b) Live Run Rate Projection

* Predict end-of-day earnings using current pace and historical curves

#### c) Slow Hour / Drop Detection

* Detect deviations vs historical hourly averages
* Flag unusually low activity windows

#### d) Transaction Count

* Total number of successful transactions per day

#### e) Average Sale Value

* Avg = Total amount / Transaction count

#### f) Repeat Customer Detection

* Identify repeat payers via UPI handle/name clustering
* Rank top customers by frequency and value

#### g) New vs Returning Customers

* Classify payers as new (first seen) or returning (seen before)

#### h) Peak Hour Identification

* Highest earning hour block (rolling windows supported)

#### i) Idle Time Detection

* Detect gaps with no transactions exceeding dynamic threshold

#### j) First & Last Sale Time

* Earliest and latest transaction timestamps per day

#### k) Weekly Trend Analysis

* Compare rolling 7-day performance vs previous 7 days

#### l) Best/Worst Day of Week

* Compute average earnings by weekday

#### m) Payment Method Split (when inferable)

* UPI vs bank transfer classification based on SMS source

#### n) Daily Consistency Score

* Composite score based on expected vs actual, volatility, and activity

### 3.2.8 Daily Summary Output (India-Optimized UX)

* Provide concise, Hindi/Hinglish-friendly statements:

  * “Aaj ₹X aaya”
  * “Kal se ₹Y zyada/kam”
  * “Aaj slow chal raha hai”
  * “Aapka peak time: 6–8pm”
* Avoid complex charts; prioritize simple cards/messages

### 3.2.9 Notifications

* Send daily end-of-day summary notification
* Optional mid-day alert for:

  * Underperformance vs expected
  * Unusual inactivity

---

## 3.3 Non-Functional Requirements

### 3.3.1 Performance

* SMS parsing latency < 1 second per message
* Daily insight computation < 2 seconds

### 3.3.2 Accuracy

* ≥ 90% correct transaction detection for supported sources
* Graceful degradation for unknown SMS formats

### 3.3.3 Privacy & Trust (Critical for India SMBs)

* No access to bank accounts or funds
* No interception or routing of payments
* SMS processed on-device where feasible
* Clear consent and transparent usage messaging (Hindi/Hinglish support)

### 3.3.4 Usability (Low-Literacy Friendly)

* No setup or configuration required
* Single-screen primary experience
* Large text, minimal navigation
* Works on low-end Android devices (2–3GB RAM)

### 3.3.5 Connectivity

* Core functionality works offline (SMS-based)
* Sync optional when internet available

---

## 3.4 System Architecture (V1)

Components:

* SMS Listener
* Parsing Engine
* Transaction Store
* Aggregation Engine
* Notification Engine

---

## 3.5 Data Model

Transaction:

* id
* amount
* timestamp
* sender_name
* upi_handle (optional)
* source (UPI/bank)
* reference_id (optional)
* status (success/failed)

Customer Profile (derived):

* customer_id (hashed from name/UPI)
* display_name
* frequency
* total_spend
* last_seen

Daily Summary:

* date
* total_income
* transaction_count
* avg_transaction_value
* peak_hour
* first_txn_time
* last_txn_time
* expected_vs_actual_delta
* run_rate_prediction
* consistency_score

Hourly Stats:

* hour_block
* txn_count
* total_amount

---

## 3.6 Limitations

* Cash transactions not tracked (critical for kirana segment)
* SMS format inconsistency across Indian banks/UPI apps
* Dependency on Android SMS permissions and policies
* Sender name ambiguity (e.g., common names, merchant accounts)
* Shared devices may mix personal and business transactions

---

# 3.7 India-Specific Design Considerations

### 3.7.1 Language & UX

* Support Hinglish/Hindi simple phrasing in UI and notifications
* Avoid financial jargon; use everyday language (“aaya”, “kam/zyaada”)

### 3.7.2 Device Constraints

* Optimize for low-cost Android devices
* Minimal battery and storage usage

### 3.7.3 Payment Ecosystem

* Prioritize UPI SMS formats (GPay, PhonePe, Paytm)
* Handle small-value, high-frequency transactions (₹10–₹200)

### 3.7.4 Trust Building

* Explicitly communicate: “Paisa seedha aapke account mein aata hai”
* No login to bank accounts required

---

* Cash transactions not tracked
* SMS format inconsistency
* Dependency on Android SMS permissions

---

# 4. Version 2: QR-Based System

## 4.1 Objective

Enhance tracking accuracy using vendor-specific QR codes without altering money flow.

---

## 4.2 Functional Requirements

### 4.2.1 QR Generation

* Generate UPI QR linked to vendor UPI ID
* Embed metadata (transaction reference)

### 4.2.2 QR Distribution

* Provide printable QR
* Allow sticker format

### 4.2.3 Transaction Tagging

* Attach identifier to each QR transaction
* Map payments more accurately

### 4.2.4 Hybrid Detection

* Combine:

  * SMS parsing (baseline)
  * QR metadata (enhanced accuracy)

### 4.2.5 Customer Pattern Detection

* Identify repeat customers via UPI handle

### 4.2.6 Income Classification

* Group transactions:

  * Regular customers
  * One-time customers

---

## 4.3 Non-Functional Requirements

### 4.3.1 Trust

* Ensure money goes directly to vendor account
* No payment routing through system

### 4.3.2 Reliability

* QR must work with all major UPI apps

---

## 4.4 System Architecture (V2)

Additional Components:

* QR Generator Service
* Metadata Tagging Layer
* Enhanced Matching Engine

---

## 4.5 Data Model Additions

QR Transaction:

* qr_id
* metadata_tag
* linked_transaction_id

Customer Profile:

* name
* upi_id
* frequency

---

## 4.6 Limitations

* Requires vendor QR adoption
* Partial coverage (not all customers will use QR)

---

# 5. Security Considerations

* No handling of actual funds
* Secure storage of transaction data
* User consent for data usage

---

# 6. Future Enhancements

* Cash estimation models
* Voice interface
* WhatsApp integration

---

# 7. Success Metrics

* Daily active users
* SMS parsing accuracy
* User retention (7-day, 30-day)
* Insight engagement rate

---

# 8. MVP Definition

## V1 MVP:

* SMS parsing
* Daily income summary
* Basic notifications

## V2 MVP:

* QR generation
* Enhanced matching
* Repeat customer detection

---

# End of Document
