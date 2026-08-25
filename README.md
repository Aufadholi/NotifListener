# WaNotification

WaNotification is an Android application designed to enhance your messaging experience by providing Text-to-Speech (TTS) announcements for incoming notifications from supported messaging apps like WhatsApp and Instagram.

## 🚀 Detailed Functions

### 1. Smart Notification Monitoring
The app utilizes a `NotificationListenerService` to monitor incoming alerts in real-time. It is specifically tuned for:
- **WhatsApp**: Detects individual chat messages.
- **Instagram**: Monitors Direct Messages (DMs).

### 2. Intelligent Whitelist Filtering
Unlike basic notification readers, WaNotification uses a **Strict Whitelist** approach:
- **Name Normalization**: The app automatically strips extra spaces and ignores case sensitivity when matching names (e.g., "John Doe" will match "john doe").
- **Privacy-First**: All filtering logic happens locally on your device; no message content or contact names are sent to any external servers.
- **Per-App Configuration**: You can maintain separate lists for WhatsApp and Instagram.

### 3. Text-to-Speech (TTS) Engine
- **Voice Announcements**: When a whitelisted contact sends a message, the app uses the system's TTS engine to announce the sender's name.
- **Focus Management**: The app handles audio focus to ensure announcements are clear even if other media is playing.
- **Customizable**: TTS only activates when the global toggle is ON and the necessary system permissions are granted.

### 4. Reliable Background Operation
- **Foreground Service**: To ensure you never miss a notification, the app runs a high-priority foreground service with a persistent status notification.
- **Auto-Restart**: The service is designed to be resilient and stay active even if the main UI is closed.

## 🏗️ Architecture

The project follows modern Android development best practices:

- **UI Layer**: Jetpack Compose for a declarative UI.
- **ViewModel Layer**: Manages UI state using `StateFlow` and handles user events via Unidirectional Data Flow (UDF).
- **Domain Layer (Use Cases)**: Encapsulates specific business logic (e.g., `AddContactUseCase`, `CheckNotificationAccessUseCase`).
- **Data Layer (Repositories)**: Abstracts data sources (SharedPreferences, System Settings).
- **Dependency Injection**: Simple Service Locator pattern using `AppContainer`.

## 📁 Project Structure

```text
com.example.wanotification
├── di              # Dependency Injection (AppContainer)
├── repository      # Data layer (Repositories & Interfaces)
├── usecase         # Domain layer (Business logic)
├── viewmodel       # Presentation layer (ViewModels & Factories)
├── state           # UI State and Event definitions
├── uiux            # UI Components (MainActivity, Screens, Composables)
├── listener        # NotificationListenerService implementation
├── config          # App configurations and managers
└── filter          # Business logic for contact filtering
```

## 🛠️ How it Works (Step-by-Step)

1. **Permission Granting**: Upon first launch, the app will guide you to the Android **Notification Access** settings. This is a system-level permission required for the app to "see" incoming messages from other apps.
2. **Whitelist Setup**: Navigate to the **Contacts** tab. Select either WhatsApp or Instagram from the dropdown, type the exact name of the contact as it appears in your notifications, and click **Tambah**.
3. **Activation**: On the **Home** tab, ensure the "NotificationListener" button shows **Active**. Then, toggle the **Aktifkan TTS** switch to the "on" position.
4. **Passive Monitoring**: You can now close the app or lock your screen. The foreground service will continue to run in the background.
5. **Announcement**: When a message arrives from a whitelisted name, the app immediately triggers the TTS engine to announce: *"Ada notifikasi dari [Nama Kontak]"*.

## 🔒 Permissions

- `BIND_NOTIFICATION_LISTENER_SERVICE`: Required to read incoming notifications.
- `POST_NOTIFICATIONS`: Required for foreground service status.

## 📄 License

This project is for educational purposes.
