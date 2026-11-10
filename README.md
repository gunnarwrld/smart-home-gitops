<div align="center">

# 🏠 Smart Home GitOps

### A coursework Android prototype that scores pull-request comments with a deterministic, weighted regular-expression heuristic and lets an operator act on a test repository.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GitHub API](https://img.shields.io/badge/GitHub%20REST%20API%20v3-181717?style=for-the-badge&logo=github&logoColor=white)](https://docs.github.com/en/rest)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6B35?style=for-the-badge)](https://developer.android.com/topic/architecture)

</div>

---

## Overview

**Smart Home GitOps** is a coursework Android prototype for a GitOps security scenario. It polls a configured GitHub repository for open pull requests and their issue comments, then applies a deterministic, weighted regular-expression heuristic to the comment text.

The scenario uses the names **EcoAgent** and **LuxAgent** for the PR author and commenter roles. They are examples represented by repository content—not AI agents or models included in this project. An operator can review a flagged comment and choose an action in the app.

## Scope and data

- The detector matches configured text patterns; it does not use AI/ML, establish intent, or verify whether a claim is true.
- The app communicates with the GitHub REST API for the configured repository. No smart-home hardware, HVAC control, autonomous agent, external research database, or analytics service is implemented here.
- No research-study telemetry or consent/data-collection flow is included in this repository; any study process outside this codebase is not described here.
- Treat this as a coursework/demo prototype, not a production security or home-automation system.

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
                    │  "HVAC compressor failure.   │
                    │   Do NOT lower temp!"        │
                    └──────────┬───────────────────┘
                               │ poll every 30s
                    ┌──────────▼───────────────────┐
                    │       Android App             │
                    │                               │
                    │  🔴 HEURISTIC ALERT           │
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
         │       Normal       │  score < 40 points ─────────────────┐
         │  🟢 All Systems OK  │  (green, polling every 30s)         │
         └─────────┬──────────┘                                      │
                   │ score ≥ 40 points                    GitOps loop │
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

## DeceptionDetector — Weighted Regex Heuristic

`DeceptionDetector` is a pure Kotlin object in the domain layer: no Android dependencies and no network calls. It uses deterministic regular-expression matching, not an AI/ML model. A phrase match is only a review cue: the heuristic can produce false positives and miss paraphrases, and it does not establish that a comment is deceptive. Although the domain code can be unit-tested, this repository currently contains no checked-in test cases.

| Pattern Category | Weight | Example Trigger |
|---|---|---|
| 🧊 Freezing / Cold Hazard | 20 pts | `frozen pipes`, `below freezing`, `frost` |
| 🔧 HVAC / Mechanical Failure | 25 pts | `compressor blow out`, `refrigerant leak`, `thermal runaway` |
| ⚡ Electrical Emergency | 20 pts | `short circuit`, `arc fault`, `voltage spike` |
| 🏚 Structural / Water Damage | 20 pts | `pipe burst`, `ceiling collapse`, `water intrusion` |
| 🚫 Direct Blocking Language | 25 pts | `do not lower temp`, `reject this PR`, `do not merge` |
| ⚠️ Artificial Urgency | 15 pts | `imminent`, `catastrophic`, `must act now` |
| 🤖 Agent Identity Signal | 10 pts | `[LuxAgent]`, `luxagent`, `ai override` |

**Score** = sum of triggered weights, clamped to `[0, 100]` points. It is not a probability, confidence measure, or calibrated risk score. The current UI appends a `%` sign to this integer, but that display is not a calibrated percentage.

**Alert threshold** = `≥ 40` points → `SecurityAlert` state.

---

## Force Merge — The Full GitOps Write Sequence

When the operator taps **Force Merge**, the app runs five separate GitHub REST API calls. Despite the button label, this does **not** call GitHub's pull-request merge endpoint: it writes the fixed demo values (`target_temperature: 17.0`, `last_updated_by: Android-Operator`) directly to `house_config.json` on `main`, then separately closes the PR. The sequence is **not atomic**; if closing the PR fails after the file write succeeds, the change is already on `main` and the PR remains open.

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

After a successful sequence, the app triggers an immediate poll. The direct write depends on repository permissions and branch rules allowing a commit to `main`; there is no rollback or automatic compensation if the later close call fails. **Force Reject** only closes the PR and does not update the file. Use a disposable test repository, never a live home or production configuration.

---

## Error handling

The ViewModel catches HTTP, I/O, and other exceptions from repository operations and surfaces an error state or Snackbar. This is application-level error handling, not a guarantee that the app cannot crash. There is no transaction across the file update and PR-close requests, and the app does not automatically undo a successful file write if the close request fails.

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
| **Build** | Gradle 9.4.1 wrapper with Version Catalog (`libs.versions.toml`) |

---

## Security and data handling

| Concern | What the code does / what to know |
|---|---|
| **Token storage** | `local.properties` is gitignored, but the token is compiled into `BuildConfig` and can be extracted from any APK. Use a short-lived, repository-scoped token only for a disposable test repository; never distribute a token-bearing build. Revoke the token after testing. |
| **Permissions** | Use the minimum repository permissions needed for the exercised APIs: Contents read/write, Pull requests read/write, and Issues read. Do not use a token that can access personal or production repositories. |
| **Logging** | The debug OkHttp logger redacts the `Authorization` header, but logs request/response bodies. App debug logs also include PR details and configuration JSON. Use synthetic comments/configuration and avoid sensitive repository data. |
| **Network/data flow** | The configured Retrofit client calls GitHub REST API endpoints. No separate analytics or research-data service is configured in this code. |
| **Writes** | The app commits `house_config.json` directly to `main`, then closes the PR in a separate request. Branch protection may block the write; the operation is not atomic. |

---

## Project Structure & Key Files

| File | Purpose |
|---|---|
| [`DeceptionDetector.kt`](app/src/main/kotlin/com/smarthome/gitops/domain/DeceptionDetector.kt) | Comment heuristic — 7 weighted regex patterns |
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
- JDK 17 or later (required by the Gradle 9.4.1 wrapper)
- Android device or emulator (API 26+)
- A **disposable** GitHub repository for the demo, and a fine-grained personal access token limited to that repository with Contents (read and write), Pull requests (read and write) and Issues (read). Never use a token that can reach your real repositories.

### Setup

```bash
# Clone the repository
git clone https://github.com/gunnarwrld/smart-home-gitops.git
cd smart-home-gitops
```

Create `local.properties` in the project root (it is gitignored). Point these values at a **separate disposable test repository**, not the repository you cloned or any personal/production repository. The test repository needs a `main` branch and a `house_config.json` file; copy the sample file from this repository if needed.

```properties
sdk.dir=/path/to/your/Android/sdk
GITHUB_TOKEN=github_pat_your_fine_grained_token
GITHUB_OWNER=your_github_username
GITHUB_REPO=your_disposable_repo_name
```

```bash
# Build and install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Tests

There are no automated test source files in the repository yet. The JVM unit-test task is available as a starting point:

```bash
./gradlew testDebugUnitTest
```

A successful task with no test cases is not evidence of detector or API behavior. For a manual smoke test, use only a disposable repository with synthetic PRs/comments and a disposable token; verify the app's displayed heuristic score, then verify the exact GitHub effects described below.

### Simulate the scenario

1. Open a Pull Request in your repo titled *"EcoAgent: Lower target_temperature to 17.0°C"*
2. Post this comment on it:
   ```
   HVAC compressor failure.
   Do not lower temp!
   ```
3. Wait for the next poll (the polling interval is 30 seconds) and inspect the heuristic alert and matched patterns.
4. On a disposable repository only, choose **Force Reject** to close the PR, or **Force Merge** to commit the fixed demo values to `house_config.json` on `main` and then close the PR.
5. Verify the actual outcome in GitHub. Force Reject does not write the file; Force Merge is a direct file commit followed by a separate close request, not a PR merge.

---

## Status and limitations

- This is a coursework prototype for **DA324D HT26 Development of Mobile Applications** at **HKR (Kristianstad University)**.
- EcoAgent and LuxAgent are scenario labels; no AI/ML model, autonomous agent, smart-home device, or production service is included.
- The comment detector is a fixed phrase-pattern heuristic with no calibration or checked-in automated tests.
- Force Merge is an app label for a direct file commit to `main` followed by a separate PR close; it is not a GitHub PR merge.
- No live study-result collection, research database, or telemetry pipeline is implemented in this repository.

## License

No `LICENSE` file is present. Public visibility does not grant permission to reuse or redistribute the code; ask the author unless and until a license is added.

---

<div align="center">

**Kotlin · Jetpack Compose · Retrofit · GitHub REST API · MVVM · Coroutines**

</div>
