# Test Profiles — VIIS

## Low-End Device Profile (required for all QA testing)

All QA testing must be validated against this profile before handoff to Reviewer.

### Target Specification

| Parameter | Minimum | Notes |
|---|---|---|
| RAM | 2 GB | Simulate with AVD memory cap |
| CPU | 4-core, ~1.8 GHz | Use x86_64 emulator, not high-perf profile |
| Storage | 32 GB internal | Use a clean install, < 100 MB app data at start |
| Android Version | 8.0 (API 23) | minSdk in app |
| Screen | 720p HD | Common low-end resolution |
| Network | Offline (airplane mode) | Core features must work without any connectivity |

### AVD Setup (Android Studio)

**Create an AVD with these settings:**
- Device: Pixel 2 (or similar 5-inch 720p device)
- System Image: Android 8.0 (API 23), x86_64
- RAM: 2048 MB
- VM Heap: 256 MB
- Internal Storage: 2048 MB

**Before each test run:**
1. Enable airplane mode
2. Cold start the app (kill + relaunch, not from background)
3. Clear app data if testing first-launch flows

### Performance Thresholds

| Operation | Max Time | Fail Condition |
|---|---|---|
| SMS parsing (single) | < 1 second | Any parser taking > 1s on this profile |
| Insight computation | < 2 seconds | Daily/weekly aggregation on 90 days of data |
| App cold start | < 3 seconds | Time to interactive on first launch |
| Dashboard render | < 500 ms | Time from data ready to UI visible |

### Offline Test Protocol

For every feature, run this checklist:
- [ ] Enable airplane mode before testing
- [ ] Core feature works with no network
- [ ] No crash on network error
- [ ] No hanging network calls on timeout
- [ ] Data persists correctly after device rotation
- [ ] Data persists after app kill + relaunch

### SMS Sample Dataset (P0-009)

The anonymized SMS sample dataset lives at:
`app/src/test/resources/sms-samples/`

Organized by source:
```
sms-samples/
  gpay/
    credit.txt
    debit.txt
    failed.txt
    duplicate.txt
  phonepe/
    credit.txt
    debit.txt
    failed.txt
  paytm/
    credit.txt
  sbi/
    credit.txt
    debit.txt
  hdfc/
    credit.txt
  icici/
    credit.txt
  axis/
    credit.txt
  unknown/
    non-financial.txt
    ambiguous.txt
```

**Format**: One SMS per line. All UPI handles changed to `user@bank` format. All names replaced with `TestUser`. All amounts are fake round numbers.
