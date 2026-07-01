<div align="center">

# RoutineAI

**An offline-first Android app that turns natural language into a structured, self-enforcing daily schedule.**

Describe your day in plain English → Gemini 2.5 Flash converts it into typed tasks → a fully normalized on-device database schedules, tracks, and audits execution — with zero backend server.

[![Java](https://img.shields.io/badge/Java-11-orange)](#)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84)](#)
[![Room](https://img.shields.io/badge/Persistence-Room%20%2F%20SQLite-blue)](#)
[![Gemini](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-4285F4)](#)

</div>

---

## Overview

RoutineAI is a conversational scheduling assistant built as a systems-first alternative to typical to-do apps. Instead of manually adding tasks, users talk to it — *"I want to study DSA for two hours and hit the gym before 7"* — and the app produces a structured, conflict-checked schedule, backed by native Android alarms and a genuinely normalized relational database, not a flattened key-value store dressed up as one.

The project was built to demonstrate production-grade database and Android fundamentals: 3NF schema design, trigger-based audit logging, diff-based state reconciliation, and native OS-level scheduling — the kind of engineering that's easy to describe and hard to actually get right.

## Why this project stands out

**Real database engineering, not a SQLite wrapper.** Most student projects store tasks as one flat table with a category string. RoutineAI's schema is fully normalized: tasks, categories, and their many-to-many relationship live in separate tables joined through a mapping table, recurrence and alarm state are decoupled from the task itself, and every AI-generated schedule is preserved as an immutable history row rather than overwriting the last one.

**Audit logging at the database layer, not the app layer.** Task completion history isn't written by application code that could have a bug or a missed code path — it's captured by a native SQL `TRIGGER` that fires on any `status` transition. That means the audit trail is correct by construction, independent of which screen or code path changed the task.

**A real reconciliation algorithm, not "delete and re-insert."** When the AI returns an updated schedule, the app diffs it against existing tasks by normalized key, patches only the fields that changed, preserves anything the user already completed, and cleans up orphaned alarms and mappings for anything removed — a materially harder problem than the naive approach, solved correctly.

**Correctly handled Android platform edge cases.** Exact-alarm permission gating for Android 12+, notification-permission requests for Android 13+, rotation-safe chat state, and API-key hygiene via `local.properties` → `BuildConfig` rather than a hardcoded secret — the details that separate a working demo from a working app.

## How it works

```
 User (natural language)
        │
        ▼
 Gemini 2.5 Flash  ──►  strict JSON contract { mode, message, tasks[] }
        │
        ▼
 Diff engine  ──►  reconciles against existing tasks (patch / insert / retire)
        │
        ▼
 Room / SQLite  ──►  7-table normalized schema + audit trigger
        │
        ▼
 AlarmManager + BroadcastReceiver  ──►  exact, permission-aware native reminders
```

**Schema:** `Tasks` · `Daily_Routine` · `Categories` · `Task_Category_Mapping` · `Task_Recurrence` · `Alarms` · `Execution_Logs`

## Feature highlights

- 🗣️ **Conversational scheduling** — free-form input parsed into structured, typed tasks by Gemini 2.5 Flash
- 🔁 **Recurring habit support** with automatic daily rollover (completed one-offs purged, recurring tasks reset)
- 🔔 **Native push reminders** via `AlarmManager`, with inline mark-done/missed actions
- 🧾 **Immutable execution history**, generated automatically at the database engine level
- 📊 **Productivity analytics** — per-category completion rates computed with multi-table SQL aggregation, no client-side post-processing
- 📴 **Fully offline data layer** — scheduling, history, and analytics work with zero network dependency beyond the initial AI call

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 11 |
| Persistence | Room (SQLite) — 7 tables, native trigger-based logging |
| Networking | Retrofit2 + Gson |
| AI | Google Gemini 2.5 Flash (REST) |
| Scheduling | `AlarmManager` + `BroadcastReceiver` |
| Min / Target SDK | 26 / 36 |

## Getting started

```bash
git clone https://github.com/godly-AK/routine.ai.git
```

Add your key to `local.properties`:
```
GEMINI_API_KEY=your_key_here
```

Build and run:
```bash
./gradlew assembleDebug
```
Requires Android API 26+ (device or emulator).

## Roadmap

The next iteration is scoped around three upgrades: a full **Kotlin + Coroutines/Flow** migration for structured concurrency, an **automated test suite** covering the reconciliation and DAO logic, and a **behavioral profiling engine** that classifies user execution patterns from `Execution_Logs` (e.g. procrastinator vs. sprint-worker) to drive adaptive reminder timing.

---

<div align="center">
<sub>Built solo as a database systems mini-project — schema design, trigger logic, and the diff-based sync engine included.</sub>
</div>
