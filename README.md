# AppPair — Persistent Dual-App Tab Switcher for Android

**AppPair** is a production-ready Android application built with **Kotlin** and **Jetpack Compose** (Material Design 3 / Material You) that turns any two installed apps into instant, persistent tabs.

Unlike standard task switching or split screen, AppPair runs a lightweight floating dual-tab pill (`SYSTEM_ALERT_WINDOW`) overlaying all your apps. Tapping Tab A brings App A directly to the foreground via `ActivityManager.moveTaskToFront()`, and tapping Tab B instantly brings App B to the front—preserving their exact states without reloading or triggering splash screens.

---

## 🚀 Key Features

### 1. **Step-by-Step System Permissions Flow**
- **Overlay (`SYSTEM_ALERT_WINDOW`)**: Required to draw the persistent floating dual-tab widget across the system.
- **Battery Optimization (`IGNORE_BATTERY_OPTIMIZATION`)**: Prevents system memory cleaners and aggressive OEM battery savers from killing the background service and your paired apps.
- **Notifications (`POST_NOTIFICATIONS`)**: Required on Android 13+ (`Tiramisu`) for the persistent foreground service notification and quick control actions.
- Automatically detects status on resume and unlocks the app once requirements are met.

### 2. **App Selection & Slot Assignment**
- Lists all installed launchable apps (filtering out headless internal packages).
- Search bar with instant real-time filtering by app label or package name.
- Single-tap or dedicated `[Set A]` / `[Set B]` quick slot buttons to assign your pair.
- Top Sticky Header providing clear visibility and instant removal of assigned slots.

### 3. **Persistent Floating Widget (Overlay)**
- Built with Jetpack Compose inside `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- **Dual Tabs**: Displays active icons for App A and App B side-by-side with distinct colored rings (`Tab A: Blue`, `Tab B: Rose`).
- **Draggable**: Move any position on screen (`detectDragGestures`). Position is persisted automatically across sessions via `DataStore`.
- **Control Panel (Long-Press / 3-dots)**:
  - `Change apps`: Opens App Selection screen directly.
  - `Minimize widget`: Collapses the pill into a tiny 44dp circular dot to clear screen real estate when watching full-screen content.
  - `Stop switcher`: Safely removes overlay and shuts down foreground service.
- **Spring Animations**: Smooth transitions (`animateContentSize` with spring physics) when expanding, collapsing, or toggling the control panel.

### 4. **Instant Task Switching Mechanism (`moveTaskToFront`)**
- Tracks running task IDs using `ActivityManager.appTasks` and `getRunningTasks()`.
- When switching to an already running task, `ActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)` is invoked, bringing the application instantly to the foreground without reloading.
- If the task was evicted from memory or launched for the first time, `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_RESET_TASK_IF_NEEDED` guarantees a clean launch and captures the new task ID.

### 5. **Anti-Kill Watchdog & OEM Guides**
- **Foreground Service**: Runs with `specialUse` type and a low-priority persistent notification displaying currently paired app names.
- **Notification Quick Controls**:
  - `Stop`: Stops service and widget.
  - `Reset Widget`: Immediately snaps the floating widget back to `(x=100, y=300)` if it got stuck off-screen or rotated out of view.
  - `Open App`: Returns to settings.
- **Coroutines Watchdog Loop**: Checks app health every 10 seconds. Warns the user if a selected application was uninstalled.
- **AlarmManager Fallback**: Wakes up periodically to verify and restore the foreground service if stopped unexpectedly.
- **OEM Battery Guide**: Tailored instructions for **Xiaomi/MIUI/HyperOS**, **Samsung One UI**, **Huawei/EMUI**, **OPPO/ColorOS**, and **Vivo/FuntouchOS** with direct system shortcut buttons (`Auto-Start Management`, `Battery Optimization`, and `App Details`).

---

## 🏗️ Architecture & Tech Stack

| Component | Choice | Description |
| :--- | :--- | :--- |
| **Language** | Kotlin 1.9.22 | 100% Kotlin code |
| **UI Framework** | Jetpack Compose | Material Design 3 (`Material You` with dynamic colors) |
| **Architecture** | MVVM + Repository + Flow | Unidirectional Data Flow (UDF) & Coroutines (`StateFlow`) |
| **Dependency Injection** | Hilt (`2.50`) | Standard `@HiltAndroidApp`, `@HiltViewModel`, `@Inject` |
| **Preferences Storage** | Jetpack DataStore | Asynchronous, reactive (`AppPairPreferences`) |
| **Service Layer** | `LifecycleService` | Foreground service hosting `FloatingWidgetController` |
| **Overlay Rendering** | `ComposeView` inside `WindowManager` | Configured with `ViewTreeLifecycleOwner` & `SavedStateRegistry` |
| **Min SDK / Target SDK** | Min SDK `26` (Android 8.0) / Target SDK `34` (Android 14) | Fully compliant with Android 14 foreground service policies |

---

## 📁 Project Structure

```text
AppPair/
├── build.gradle.kts (Project level)
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts (App level)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/apppair/
        │   ├── AppPairApplication.kt             # Hilt Application entry point
        │   ├── data/
        │   │   ├── model/
        │   │   │   ├── AppInfo.kt                # Package metadata & icon wrapper
        │   │   │   ├── SelectedAppPair.kt        # Active pair selection model
        │   │   │   └── PermissionStatus.kt       # System permissions verification data
        │   │   ├── preferences/
        │   │   │   └── AppPairPreferences.kt     # DataStore wrapper for positions & state
        │   │   └── repository/
        │   │       └── AppRepository.kt          # Single source of truth repository
        │   ├── di/
        │   │   └── AppModule.kt                  # Hilt DI providers
        │   ├── service/
        │   │   ├── AppPairForegroundService.kt   # Persistent background service & watchdog loop
        │   │   ├── FloatingWidgetController.kt   # WindowManager overlay with Jetpack Compose UI
        │   │   ├── AppSwitchHelper.kt            # moveTaskToFront() & task ID management
        │   │   └── WatchdogAlarmReceiver.kt      # Periodic AlarmManager resilience check
        │   ├── ui/
        │   │   ├── MainActivity.kt               # Single Activity host with uninstalled app cleanup
        │   │   ├── navigation/
        │   │   │   └── AppNavigation.kt          # Compose Navigation routes
        │   │   ├── permissions/
        │   │   │   ├── PermissionsScreen.kt      # Step-by-step permissions onboarding
        │   │   │   └── PermissionsViewModel.kt
        │   │   ├── selection/
        │   │   │   ├── AppSelectionScreen.kt     # App picker grid, search & slot assignment
        │   │   │   └── AppSelectionViewModel.kt
        │   │   ├── guide/
        │   │   │   ├── OemBatteryGuideScreen.kt  # Xiaomi/Samsung/Huawei auto-start guides
        │   │   │   └── OemHelper.kt              # Manufacturer detection & direct intent shortcuts
        │   │   └── theme/
        │   │       ├── Color.kt
        │   │       ├── Theme.kt                  # Dynamic Material You color schemes
        │   │       └── Type.kt
        │   └── utils/
        │       ├── NotificationHelper.kt         # Low-priority persistent notification & actions
        │       ├── PackageUtils.kt               # PackageManager query & launch intent builder
        │       └── PermissionUtils.kt            # Overlay, battery & notification check utilities
        └── res/
            ├── drawable/                         # Adaptive launcher icons
            ├── mipmap-anydpi-v26/
            ├── values/                           # Strings, colors, and themes
            └── xml/                              # Backup rules & data extraction rules
```

---

## 🛠️ Building and Running

1. **Open in Android Studio**:
   - Launch **Android Studio Iguana** (or newer).
   - Select **Open** and choose the `AppPair/` directory.
2. **Gradle Sync**:
   - Android Studio will automatically sync dependencies (`Compose BOM 2024.02.00`, `Hilt 2.50`, `DataStore`, `Accompanist`).
3. **Run on Device / Emulator**:
   - Select a device running Android 8.0 (`API 26`) up to Android 14 (`API 34`).
   - Click **Run (`Shift + F10`)**.

---

## 🔍 Verification & Edge Case Handling

- **Uninstalled Apps**: If an app assigned to Tab A or Tab B is uninstalled while the service is running, the monitoring watchdog triggers an alert notification (`"AppPair Alert: One or both selected apps were uninstalled!"`) and opening `MainActivity` automatically clears the missing slot.
- **Stuck / Off-Screen Widget**: If screen rotation or a resolution change causes the floating widget to become inaccessible, tap the **`Reset Widget`** action inside the persistent status bar notification to immediately snap the pill back to `(100, 300)`.
- **Force Stop Recovery**: The service registers an `AlarmManager` wake-up intent (`WatchdogAlarmReceiver`) to check whether the service was stopped unexpectedly and re-launch it if the user has `isServiceActive` enabled.
