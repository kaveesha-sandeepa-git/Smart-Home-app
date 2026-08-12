# Smart Home Automation System

A robust Android application for monitoring and controlling smart home devices. Built with modern Android architecture components, the app features real-time synchronization, safety monitoring, and detailed usage reporting.

## 🚀 Features Implemented So Far

### 📱 User Interface
*   **Dashboard:** Real-time overview of all connected devices across different floors. Includes quick stats for active devices.
*   **Device Control:** Granular control for specialized devices (e.g., Brightness for Lights, Safety Cutoffs for Irons).
*   **Floor Plans:** Visual representation of device layouts on a per-floor basis.
*   **Usage Reporting:** Detailed analytics and energy consumption logs with time-based filtering (Today, Week, Month).
*   **Settings:** Centralized configuration for notifications, safety alerts, and app-wide preferences.

### 🏗️ Architecture & Data Layer
*   **Repository Pattern:** Implemented as a single source of truth, coordinating between local storage (Room) and remote sync (Firebase).
*   **Room Database:** Persistent local storage for Floors, Devices, and Usage Reports with type converters for complex data.
*   **ViewModels:** Reactive UI state management using `LiveData` and `switchMap` for efficient data transformations.
*   **Firebase Integration:** 
    *   **Realtime Database:** Bidirectional sync for device statuses.
    *   **Cloud Messaging (FCM):** Push notifications for safety alerts and schedule events.

### ⚙️ Background Services & Safety
*   **WorkManager:** 
    *   `DeviceSyncWorker`: Ensures data stays updated with the cloud even when the app is in the background.
    *   `SafetyCheckWorker`: Periodically monitors high-power devices to prevent hazards.
*   **Safety Engine:** Automatic safety cutoff logic for devices like Irons based on maximum allowed "ON" duration.
*   **Scheduling Service:** Time-based automation for lights and other smart devices.

### 🛠️ Utilities & Monitoring
*   **ErrorHandler:** Centralized system for managing network, database, and safety errors with user-facing feedback.
*   **AppLogger:** Dual-mode logging system that writes to both the system console and a persistent local file (`app_logs.txt`) for debugging.
*   **PreferencesManager:** Singleton wrapper around `SharedPreferences` for persisting user settings like Dark Mode and sync intervals.

## 🛠️ Technologies Used
*   **Language:** 100% Kotlin
*   **Jetpack Components:** ViewModel, LiveData, Room, WorkManager, Navigation.
*   **Firebase:** Realtime Database, Cloud Messaging.
*   **UI:** Material Design 3, RecyclerView, CardView, ViewModels by KTX.
*   **Concurrency:** Kotlin Coroutines & Flow.

## 📁 Project Structure
```
com.example.smart_home/
├── activities/     # Activity classes for main navigation
├── adapters/       # RecyclerView adapters for devices and reports
├── database/       # Room DB definitions, DAOs, and Converters
├── fragments/      # UI Fragments for main screens
├── models/         # Data classes and Entity definitions
├── repository/     # Central SmartHomeRepository
├── services/       # Firebase Sync, Scheduling, and Notification services
├── utils/          # Logger, ErrorHandler, Preferences, and Constants
└── workers/        # WorkManager background tasks
```

## 🛠️ Getting Started
1.  **Firebase Setup:** Connect the project to a Firebase console and add `google-services.json`.
2.  **Permissions:** Ensure the app has Notification and Internet permissions (handled in Manifest).
3.  **Sync:** On first launch, the app will automatically trigger a background sync to fetch floor and device data.
