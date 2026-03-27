# Branch Strategy

## Overview

One branch per task. Developer implements on the branch, QA tests on the branch, Reviewer raises the PR. **Rishabh reviews and merges all PRs** — agents never merge to main.

---

## Branch Naming Convention

```
feature/{task-id}-{short-description}
```

**Examples:**
```
feature/P1-001-sms-broadcast-receiver
feature/P1-005-gpay-sms-parser
feature/P2-020-main-dashboard-ui
feature/P3-003-eod-notification
feature/P4-001-qr-generator
```

Rules:
- All lowercase, hyphens only (no underscores, no spaces)
- Task ID first — makes it easy to find branches by task
- Short description: 2-4 words max
- Always branch from `main` (after ensuring it's up to date)

---

## Full Workflow

```
main
 └── feature/P1-005-gpay-sms-parser
      │
      ├── Developer: implement + commit(s)
      ├── QA: test on branch (no new commits unless fixing bugs found in review)
      ├── Reviewer: code review → if approved → raise PR to main
      └── Rishabh: reviews PR → merges (or requests changes)
```

### Step-by-Step

**1. Developer creates branch**
```bash
git checkout main && git pull origin main
git checkout -b feature/{task-id}-{short-description}
```

**2. Developer implements and commits**
```bash
git add <files>
git commit -m "{task-id}: {what was done}"
git push -u origin feature/{task-id}-{short-description}
```

**3. QA tests on the branch** — checks out branch, runs tests. If bugs found, Developer fixes on same branch with new commits.

**4. Reviewer raises PR** (after QA passes)
- PR: `feature/... → main`
- Title: `[{task-id}] {feature name}`
- Body: alignment check results, test coverage, any decisions made

**5. Rishabh reviews and merges** — agents wait. Notion updated to "Awaiting Merge".

**6. After merge** — branch deleted, Notion updated to "Done ✅".

---

## Commit Message Format

```
{task-id}: {short description}

{optional body with more context}
```

**Examples:**
```
P1-005: Implement GPay SMS parser with credit/debit detection
P1-013: Add deduplication logic with 5-minute time window
P2-020: Main dashboard UI with income card and comparison widget
```

---

## PR Template

```markdown
## {task-id}: {Feature Name}

### What was built
- {bullet point summary}

### Alignment check
- Modules checked: {list from alignment-spec.md}
- Items: X/Y passed
- Architecture: Pass/Fail

### Test coverage
- Coverage: X%
- Edge cases tested: {list key ones}
- Events verified: {list events produced}

### Decisions made
- {any 2-way door decisions}

### How to test
1. {step}
2. {step}
```

---

## Notion Updates (by milestone)

| Milestone | Who | Notion Status |
|---|---|---|
| Branch created | Developer | In Progress |
| Dev complete, pushed | Developer | Dev Complete |
| QA passed | QA | QA Passed |
| PR raised | Reviewer | In Review |
| Rishabh merges PR | — | Done ✅ |
| QA failed | QA | QA Failed → back to Dev |
| Reviewer requests changes | Reviewer | Rework Needed |
