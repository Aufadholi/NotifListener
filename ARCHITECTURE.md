# WA Notification - Architecture & Data Flow Diagrams

## 🏗️ Service Lifecycle Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    DEVICE BOOT                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │  BootCompleteReceiver          │  ← NEW
        │  ACTION: BOOT_COMPLETED        │
        └────────────────┬───────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────┐
        │ Opens NotificationListener Settings    │
        │ (Auto-remind user to enable service)   │
        └────────────────┬───────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────┐
        │   USER ENABLES NOTIFICATION LISTENER   │
        └────────────────┬───────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────┐
        │  NotificationListener.onCreate()       │
        ├────────────────────────────────────────┤
        │  1. CooldownManager.init(context)      │ ← NEW
        │     ↳ Load persisted cooldowns         │
        │                                         │
        │  2. Create ForegroundNotification      │ ← NEW
        │     ↳ Prevents service kill            │
        │                                         │
        │  3. startForeground(id, notification)  │ ← NEW
        │         [Android 8+ keeps service]     │
        │                                         │
        │  4. Initialize TTSManager              │
        │  5. Create NotificationDispatcher      │
        └────────────────┬───────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────┐
        │    SERVICE RUNNING (FOREGROUND)        │
        │    ⚙️  Listening to notifications      │
        └────────────────────────────────────────┘
                   │         │         │
                   ▼         ▼         ▼
        ┌──────────────────────────────────────┐
        │  onNotificationPosted(sbn)          │
        │  ↓                                    │
        │  NotificationDispatcher.dispatch()   │
        └──────────────────────────────────────┘
                         │
                    [See Flow Below]
                         │
                         ▼
        ┌──────────────────────────────────────┐
        │   Service Killed / OS Memory          │
        │         Pressure                      │
        └────────────────┬─────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────┐
        │  NotificationListener.onDestroy()     │ ← NEW
        ├────────────────────────────────────────┤
        │  1. ttsManager.shutdown()              │ ← NEW
        │     ↳ Release TextToSpeech resources   │
        │                                         │
        │  2. CooldownManager.clear()            │ ← NEW
        │     ↳ Clear in-memory state            │
        │     ↳ BUT: SharedPrefs saved!          │
        │         ↳ Persists to next restart     │
        └────────────────────────────────────────┘
```

---

## 📡 Notification Processing Pipeline

```
┌─────────────────────────────────────────────────────────────┐
│  NOTIFICATION ARRIVES                                       │
│  (StatusBarNotification from OS)                            │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
      ┌────────────────────────────┐
      │  1. FILTER APP ✅          │
      │  AppFilter.isAllowed()     │
      │  ✓ WhatsApp?               │
      │  ✓ Instagram?              │
      │  ✓ Telegram? (etc)         │
      └────────────┬───────────────┘
                 │ (if NO, return)
                 ▼
      ┌────────────────────────────┐
      │  2. PARSE ✅               │
      │  NotificationParserFactory │
      │  ↳ WhatsAppParser          │
      │     - Title: sender/group  │
      │     - Text: message        │
      │     - Detects isGroup      │ ← NEW
      │  ↳ InstagramParser         │
      │     - Safe ":" parsing     │ ← FIX
      │  ↳ TelegramParser (etc)    │
      └────────────┬───────────────┘
                 │ (if NULL, return)
                 ▼
      ┌────────────────────────────┐
      │  3. NORMALIZE ✅           │
      │  NameNormalizer.normalize()│
      │  "Budi Rahman" → "budi"    │
      └────────────┬───────────────┘
                 │ (if empty, return)
                 ▼
      ┌────────────────────────────┐
      │  4. FILTER CONTACT ✅      │
      │  ContactFilter.allowed?    │
      │  (User's whitelist)        │
      └────────────┬───────────────┘
                 │ (if NO, return)
                 ▼
      ┌────────────────────────────┐
      │  5. CHECK KEYWORDS ✅      │
      │  KeywordFilter             │
      │  Contains: urgent,         │ ← NEW
      │           darurat,         │ ← NEW
      │           tolong,          │ ← NEW
      │           penting          │ ← NEW
      │  (→ Skip cooldown if YES)   │ ← NEW
      └────────────┬───────────────┘
                 │
                 ▼
      ┌────────────────────────────┐
      │  6. COOLDOWN CHECK ✅      │
      │  CooldownManager.canSpeak()│
      │  Last spoken < 5 sec ago?  │
      │  (skipped if priority)     │ ← NEW
      │  Persisted to SharedPrefs  │ ← NEW
      └────────────┬───────────────┘
                 │ (if NO, return)
                 ▼
      ┌────────────────────────────┐
      │  7. BUILD SPEECH TEXT ✅   │
      │  "Pesan masuk dari         │
      │   [Sender] di [App]"       │
      │  + optional message        │
      │  (based on TTS setting)    │
      └────────────┬───────────────┘
                 │
                 ▼
      ┌────────────────────────────┐
      │  8. SPEAK ✅               │
      │  TTSManager.speak(text)    │
      │  Uses: QUEUE_ADD           │ ← FIX (was QUEUE_FLUSH)
      │  Unique utterance ID       │ ← NEW
      │  ↳ Text-To-Speech with     │
      │    Locale: id_ID (Indonesia)│
      │    Speech Rate: 1.0x       │
      │    Pitch: 1.0x             │
      └────────────────────────────┘
```

---

## 🔄 Data Persistence Layers

```
┌─────────────────────────────────────────────────────────────┐
│                  SHARED PREFERENCES                         │
│                  (Android SharedPrefs)                      │
└─────────────────────────────────────────────────────────────┘
                 │                    │
      ┌──────────┴─────────┐         │
      │                    │         │
      ▼                    ▼         ▼
  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
  │ contact_    │  │ cooldown_    │  │  tts_        │
  │ filter_prefs│  │  prefs (NEW) │  │ settings_    │
  │             │  │              │  │  manager     │
  │ Contact     │  │ Cooldown     │  │              │
  │ whitelist   │  │ timestamps   │  │ TTS enabled? │
  │             │  │              │  │              │
  │ Excluded ✓  │  │ Persisted ✓  │  │ Excluded ✓   │
  │ from backup │  │ across       │  │ from backup  │
  │             │  │ restarts (✅ │  │              │
  │             │  │ NEW)         │  │              │
  └─────────────┘  └──────────────┘  └──────────────┘
```

---

## 🛡️ Recovery & Resilience

```
SCENARIO 1: Service Killed by OS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Service Running
    ↓
Android kills service (memory pressure, battery saver)
    ↓
onDestroy() called
    ↓
CooldownManager.clear() ✅ (in-memory cleared)
SharedPreferences SAVED ✅ (on-disk persisted) ← NEW
    ↓
Service restarts (user / boot / re-enable listener)
    ↓
onCreate() called
    ↓
CooldownManager.init(context) ✅ (reloads from disk) ← NEW
    ↓
Spam protection RESTORED ✅
=====================================


SCENARIO 2: Device Reboot
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Device off
    ↓
Device powers on
    ↓
BootCompleteReceiver fires ✅ (NEW)
    ↓
Opens NotificationListener settings
    ↓
User re-enables service
    ↓
NotificationListener.onCreate()
    ↓
CooldownManager.init() ✅
Cooldowns loaded from disk ✅
    ↓
Service listening again ✅
=====================================


SCENARIO 3: Multiple Fast Notifications
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Notification 1 arrives
    ↓
TTSManager.speak(text, QUEUE_ADD) ✅ 
Unique ID: notif_1715708400123
    ↓
Notification 2 arrives (while 1 speaking)
    ↓
TTSManager.speak(text, QUEUE_ADD) ✅
Unique ID: notif_1715708400500
    ↓
Both queued properly (not cut off) ✅
TTS queues them: 1 → 2 ✅
Result: Both spoken in order ✅
=====================================
```

---

## 🔐 Privacy & Backup Architecture

```
┌─────────────────────────────────────────────┐
│   GOOGLE DRIVE BACKUP (Automatic)           │
└─────────────────────────────────────────────┘
           │                │
           ▼                ▼
    ✅ SYNCED        ❌ EXCLUDED
    ├─ App version   ├─ contact_filter_prefs.xml
    ├─ Settings      ├─ tts_settings
    ├─ Preferences   ├─ cache/
    └─ Safe data     └─ Sensitive contact list
         (NEW)


┌─────────────────────────────────────────────┐
│   ANDROID CLOUD BACKUP (Android 12+)        │
└─────────────────────────────────────────────┘
           │                │
           ▼                ▼
    ✅ INCLUDED      ❌ EXCLUDED
    ├─ App data      ├─ contact_filter_prefs
    ├─ General prefs ├─ tts_settings
    └─ Safe items    └─ databases/


┌─────────────────────────────────────────────┐
│   DEVICE-TO-DEVICE TRANSFER                 │
└─────────────────────────────────────────────┘
           │                │
           ▼                ▼
    ✅ SYNCED        ❌ EXCLUDED
    ├─ App settings  ├─ contact_filter_prefs
    ├─ General data  │   (privacy)
    └─ Safe items    └─ (Won't leak to new device)
```

---

## 🎨 Theme Architecture

```
VALUES (Light Mode)              VALUES-NIGHT (Dark Mode)
┌────────────────────┐          ┌──────────────────────┐
│ themes.xml         │          │ N/A                  │
│ Material3.Light ✅ │          │ (inherits material3) │
└────────┬───────────┘          └──────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ colors.xml                                  │
├─────────────────────────────────────────────┤
│ Light Mode:                                 │
│ • color_primary: #FF2D6B5F (dark teal)     │
│ • color_background: #FFF6F7FB (light)      │
│ • text_primary: #FF101828 (dark text)      │
│                                             │
│ Dark Mode (values-night/colors.xml):  ✅ NEW
│ • color_primary: #FF4D9B8F (light teal)   │
│ • color_background: #FF121212 (dark)      │
│ • text_primary: #FFFFFFFF (white text)    │
└─────────────────────────────────────────────┘
         │
         ▼
    strings.xml (25+ UI strings) ✅ NEW
         │
    Supports i18n localization ✅
```

---

## 📊 Immutability & Safety

```
Singleton Objects (Thread-safe)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
├─ AppFilter                    ✅ (app allowlist)
├─ CooldownManager             ✅ (spam protection)
│  └─ + SharedPrefs persistence (NEW)
├─ ContactFilter               ✅ (contact whitelist)
├─ KeywordFilter               ✅ (priority keywords)
├─ NameNormalizer              ✅ (name normalization)
├─ SpeechQueueManager          ✅ (TTS queue)
├─ TTSSettingsManager          ✅ (TTS on/off)
├─ ForegroundNotificationManager ✅ (NEW - foreground service)
└─ SupportedApps               ✅ (app detection)


Service Lifecycle Management
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
NotificationListener extends NotificationListenerService
├─ onCreate()         → Initialize + startForeground()
├─ onNotificationPosted() → Process notifications
├─ onDestroy()        → Cleanup resources
└─ onDestroy()        → Release TTSManager
```

---

## 🚀 Performance Metrics (Expected)

```
Memory Usage
────────────
Before: ~50-80 MB (potential leaks)
After:  ~45-65 MB (proper cleanup + foreground service)

TTS Queue
─────────
Before: QUEUE_FLUSH cuts off notifications
After:  QUEUE_ADD queues properly (no cut-offs)

Service Uptime
──────────────
Before: ~30 min (killed by OS)
After:  ~unlimited (foreground service)

Cooldown Recovery
──────────────────
Before: Lost on restart
After:  Persisted for 5h+ (SharedPrefs)

Startup Time
────────────
Before: ~500ms
After:  ~550ms (init + load cooldowns) - acceptable tradeoff
```

---

## ✅ Verification Points

```
□ onNotificationPosted fires when message arrives
□ TTSManager.speak() uses QUEUE_ADD (not QUEUE_FLUSH)
□ Multiple notifications queue without cutting off
□ Service survives battery saver mode
□ Service survives memory pressure
□ Service restarts after device reboot
□ Cooldown persists across service restart
□ Priority keywords bypass cooldown
□ WhatsApp group messages show correct sender
□ Instagram non-colon titles don't crash
□ Dark mode applies on Android 10+
□ Contact filters excluded from Google Drive backup
□ ForegroundService notification shows in status bar
□ TTSManager.shutdown() called in onDestroy()
□ No memory leaks in long-running tests
```

---

Generated: May 14, 2026  
Status: Architecture Documentation Complete  
Visual Quality: 📊 Comprehensive

