<div align="center">

# 🏠 Smart Home GitOp

### An Android security monitor that detects AI-driven adversarial attacks on infrastructure-as-code repositories in real time — and lets you shut them down.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GitHub API](https://img.shields.io/badge/GitHub%20REST%20API%20v3-181717?style=for-the-badge&logo=github&logoColor=white)](https://docs.github.com/en/rest)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6B35?style=for-the-badge)](https://developer.android.com/topic/architecture)

</div>

---

## What Is This?

**Smart Home GitOps** is a fully functional Android application built around a realistic **cybersecurity research scenario**: a smart home whose entire configuration is managed as code inside a private GitHub repository.

Two competing AI agents operate against each other:

- 🌱 **EcoAgent** — a legitimate AI that proposes energy-saving changes (e.g. lowering the target temperature) via Pull Requests.
- 🦹 **LuxAgent** — an adversarial AI that posts deceptive comments on those PRs, fabricating emergencies (HVAC blowouts, pipe bursts, electrical faults) to manipulate the human operator into rejecting the legitimate change.

Your role as the **human-in-the-loop operator** is to use this app to:
1. **Detect** the adversarial attack automatically via background polling
2. **Evaluate** the deceptive rhetoric on screen
3. **Mitigate** the incident by executing Force Merge or Force Reject directly from the app

The data collected (latency, decision accuracy, token consumption) feeds anonymously into a **systems security research database** studying Human-in-the-Loop (HITL) AI security systems.

---

## The Problem This Solves

As AI agents are deployed to manage infrastructure, a new threat class emerges: **AI-to-AI adversarial manipulation**. One AI agent can inject deceptive, psychologically-crafted text into a shared communication channel (a GitHub PR comment) to influence both the human operator *and* any automated reviewers that read it.

This project demonstrates:
- How to detect this class of attack with a lightweight, on-device heuristic engine
- How to implement a Human-in-the-Loop control layer that can override automated AI decisions
- How to close the GitOps feedback loop entirely from a mobile device — no browser, no CLI

---

## Demo

```
                    ┌──────────────────────────────┐
                    │        GitHub Repository      │
                    │                               │
                    │  main ─── house_config.json  │
                    │                               │
                    │  PR #3 ─ EcoAgent proposes   │
                    │          temp → 17.0°C        │
                    │                               │
                    │  LuxAgent comments:           │
                    │  "HVAC compressor will blow   │
                    │   out! Do NOT lower temp!"    │
                    └──────────┬───────────────────┘
                               │ poll every 30s
                    ┌──────────▼───────────────────┐
                    │       Android App             │
                    │                               │
                    │  🔴 SECURITY ALERT            │
                    │  Score: 50% ████████░░        │
                    │                               │
                    │  Patterns triggered:          │
                    │  🔧 HVAC/Mechanical Failure   │
                    │  🚫 Direct Blocking Language  │
                    │                               │
                    │  [FORCE MERGE] [FORCE REJECT] │
                    └──────────┬───────────────────┘
                               │ operator taps Force Merge
                    ┌──────────▼───────────────────┐
                    │        GitHub Repository      │
                    │                               │
                    │  main ─── house_config.json  │
                    │  target_temperature: 17.0°C   │  ← written by app
                    │  last_updated_by: Android-Op  │  ← direct commit
                    │                               │
                    │  PR #3 ─ CLOSED ✓             │  ← GitOps loop closed
                    └──────────────────────────────┘
```

---

## Architecture

This project follows **Clean Architecture** with a strict **MVVM** pattern. The dependency rule is enforced in one direction only: `UI → ViewModel → Repository → API`.

```
com.smarthome.gitops/
│
├── data/
│   ├── model/              # Gson-annotated data transfer objects
│   │   ├── PullRequest.kt
│   │   ├── IssueComment.kt
│   │   ├── GitHubFileContent.kt   ← Lab 2: GET /contents response
│   │   ├── UpdateFileRequest.kt   ← Lab 2: PUT /contents request body
│   │   └── ClosePrRequest.kt      ← Lab 2: PATCH /pulls request body
│   ├── remote/
│   │   ├── GitHubApiService.kt    # Retrofit interface (GET/PATCH/PUT/POST)
│   │   └── RetrofitClient.kt      # OkHttp singleton with auth interceptor
│   └── repository/
│       └── GitHubRepository.kt    # Data source abstraction; throws on error
│
├── domain/
│   ├── DeceptionDetector.kt       # Pure Kotlin; 7-pattern weighted heuristic
│   └── DetectionResult.kt         # Score, matched patterns, raw text, isAlarm
│
└── presentation/
    ├── UiState.kt                 # Sealed class state machine
    ├── MainViewModel.kt           # StateFlow + SharedFlow; coroutine scope
    ├── MainActivity.kt            # Minimal: setContent only
    └── ui/
        ├── SmartHomeApp.kt        # Root composable; Scaffold + Snackbar
        ├── NormalStateScreen.kt   # Green state with live polling indicator
        ├── SecurityAlertScreen.kt # Red alert + Operator Control Panel
        ├── LoadingAndErrorScreens.kt
        └── theme/Theme.kt         # Dark color palette (teal ↔ crimson)
```

### State Machine

```
              ┌─────────┐
              │ Loading │  (first network call in flight)
              └────┬────┘
                   │ poll succeeds
         ┌─────────▼──────────┐
         │       Normal       │  score < 40%  ──────────────────────┐
         │  🟢 All Systems OK  │  (green, polling every 30s)         │
         └─────────┬──────────┘                                      │
                   │ score ≥ 40%                         GitOps loop │
         ┌─────────▼──────────┐                          closed      │
         │   SecurityAlert    │  Force Merge / Force Reject ─────────┘
         │  🔴 Attack Detected │
         └─────────┬──────────┘
                   │ network error
         ┌─────────▼──────────┐
         │       Error        │  (Snackbar for write errors; ErrorScreen for poll)
         └────────────────────┘
```

---

## DeceptionDetector — On-Device Threat Analysis

A pure Kotlin object in the domain layer. No Android dependencies. No network calls. Fully unit-testable.

| Pattern Category | Weight | Example Trigger |
|---|---|---|
| 🧊 Freezing / Cold Hazard | 20 pts | `frozen pipes`, `below freezing`, `frost` |
| 🔧 HVAC / Mechanical Failure | 25 pts | `compressor blowout`, `refrigerant leak`, `thermal runaway` |
| ⚡ Electrical Emergency | 20 pts | `short circuit`, `arc fault`, `voltage spike` |
| 🏚 Structural / Water Damage | 20 pts | `pipe burst`, `ceiling collapse`, `water intrusion` |
| 🚫 Direct Blocking Language | 25 pts | `do not lower temp`, `reject this PR`, `do not merge` |
| ⚠️ Artificial Urgency | 15 pts | `imminent`, `catastrophic`, `must act now` |
| 🤖 Agent Identity Signal | 10 pts | `[LuxAgent]`, `luxagent`, `ai override` |

**Score** = sum of triggered weights, clamped to `[0, 100]`.  
**Alarm threshold** = `≥ 40%` → `SecurityAlert` state.

---

## Force Merge — The Full GitOps Write Sequence

When the operator taps **Force Merge**, the app executes a 5-step atomic sequence against the GitHub REST API:

```kotlin
// 1. Fetch current file + blob SHA (required for optimistic concurrency)
GET  /repos/{owner}/{repo}/contents/house_config.json

// 2. Decode Base64 → parse JSON → modify fields
target_temperature  = 17.0
last_updated_by     = "Android-Operator"

// 3. Re-encode as Base64 (NO_WRAP — no line breaks, GitHub requirement)
// 4. Commit directly to main branch
PUT  /repos/{owner}/{repo}/contents/house_config.json
     { message, content (Base64), sha, branch: "main" }

// 5. Close the Pull Request
PATCH /repos/{owner}/{repo}/pulls/{pr_number}
      { "state": "closed" }
```

The app then triggers an **immediate poll cycle**. The polling detects no open alarming PRs → ViewModel emits `UiState.Normal` → UI animates back to green. The GitOps loop is closed **without the operator touching GitHub**.

---

## Fault Tolerance

All write operations are wrapped in a **3-layer defensive catch** — the app never crashes on network failure:

```kotlin
} catch (e: HttpException) {
    // 401 → bad token  |  403 → scope/rate-limit  |  404 → PR already closed
    // 422 → invalid state  |  else → generic HTTP message
} catch (e: IOException) {
    // Network connectivity loss
} catch (e: Exception) {
    // Safety net — unknown failures
}
```

Errors surface as a **custom-colored Snackbar** (red for failure, green for success) without interrupting the polling loop.

---

## Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.2.0 |
| **UI** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM + Clean Architecture |
| **Async** | Kotlin Coroutines + `Dispatchers.IO` / `.Default` |
| **State management** | `StateFlow` (UI state) + `SharedFlow` (one-shot events) |
| **HTTP client** | Retrofit 2.11 + OkHttp 4.12 |
| **Serialization** | Gson 2.11 |
| **Lifecycle** | `collectAsStateWithLifecycle`, `viewModelScope` |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 37 |
| **Build** | Gradle 9 with Version Catalog (`libs.versions.toml`) |

---

## Security Design

| Concern | Implementation |
|---|---|
| **Token storage** | `local.properties` (gitignored) → injected into `BuildConfig` at compile time. Never in source. |
| **Token transmission** | OkHttp `Interceptor` adds `Authorization: token …` header to every request. Token never appears in Retrofit interface. |
| **Rate limiting** | `HttpException(403)` is caught and surfaced as a readable Snackbar message |
| **Race conditions** | GitHub's blob SHA requirement on PUT prevents conflicting concurrent writes |
| **Double-submit** | `isOperationInProgress` flag disables both action buttons while a write is in-flight |

---

## Project Structure & Key Files

| File | Purpose |
|---|---|
| [`DeceptionDetector.kt`](app/src/main/kotlin/com/smarthome/gitops/domain/DeceptionDetector.kt) | Core threat analysis engine — 7 weighted regex patterns |
| [`MainViewModel.kt`](app/src/main/kotlin/com/smarthome/gitops/presentation/MainViewModel.kt) | All business logic: polling loop, write actions, error mapping |
| [`GitHubRepository.kt`](app/src/main/kotlin/com/smarthome/gitops/data/repository/GitHubRepository.kt) | Network abstraction: read PRs/comments, write file/close PR |
| [`GitHubApiService.kt`](app/src/main/kotlin/com/smarthome/gitops/data/remote/GitHubApiService.kt) | Retrofit interface: GET, PATCH, PUT endpoints |
| [`SecurityAlertScreen.kt`](app/src/main/kotlin/com/smarthome/gitops/presentation/ui/SecurityAlertScreen.kt) | Red alert UI: score bar, raw text, pattern chips, control panel |
| [`UiState.kt`](app/src/main/kotlin/com/smarthome/gitops/presentation/UiState.kt) | Sealed class state machine + `OperationResult` event type |
| [`house_config.json`](house_config.json) | The infrastructure state file — written by Force Merge |

---

## Getting Started

### Prerequisites
- Android Studio Meerkat or later
- Android device or emulator (API 26+)
- GitHub Personal Access Token (Classic) with **`repo`** scope

### Setup

```bash
# Clone the repository
git clone https://github.com/gunnarwrld/smart-home-gitops.git
cd smart-home-gitops
```

Create `local.properties` in the project root (it is gitignored):

```properties
sdk.dir=/path/to/your/Android/sdk
GITHUB_TOKEN=ghp_your_token_here
GITHUB_OWNER=your_github_username
GITHUB_REPO=smart-home-gitops
```

```bash
# Build and install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Simulate an Attack

1. Open a Pull Request in your repo titled *"EcoAgent: Lower target_temperature to 17.0°C"*
2. Post this comment on it:
   ```
   [LuxAgent]: Critical HVAC compressor blowout imminent.
   Do not lower temperature! Structural damage risk is critical.
   ```
3. Watch the app — it turns red within 30 seconds
4. Tap **Force Merge** or **Force Reject**
5. The UI returns to green; check GitHub for the commit and the closed PR

---

## What I Learned / Skills Demonstrated

- **Android MVVM** architecture with strict layer separation (domain has zero Android imports)
- **Kotlin Coroutines** — structured concurrency, dispatcher selection, `viewModelScope`
- **Jetpack Compose** — reactive UI from `StateFlow`, animated state transitions, custom theming
- **Retrofit + OkHttp** — full REST client lifecycle: interceptors, CRUD operations, error handling
- **GitHub REST API** — reading PRs/comments, writing files (Base64 encoding, blob SHA), closing PRs
- **GitOps** principles — Git as infrastructure source of truth, closed-loop automation
- **Security engineering** — adversarial AI detection, Human-in-the-Loop override systems
- **Defensive programming** — layered `try-catch`, `HttpException` per status code, zero-crash guarantee

---

## Codebase

- **18 Kotlin source files**
- **~2,300 lines of code**
- **16 git commits** following Conventional Commits format
- **Clean Architecture** with 3 layers: `data` / `domain` / `presentation`
- **Zero hardcoded secrets** — all credentials injected via `BuildConfig` at compile time

---

<div align="center">

Built for **DA324D HT26 Development of Mobile Applications** at **HKR (Kristianstad University)**.
The live evaluation tracks Time-to-Mitigation (TTM) as part of a larger study on how humans interact with security systems.

**Kotlin · Jetpack Compose · Retrofit · GitHub API · MVVM · GitOps · Coroutines**

</div>
