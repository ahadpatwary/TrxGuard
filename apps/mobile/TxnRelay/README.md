# TxnRelay

Android app (Kotlin, Jetpack Compose, Material 3) that listens to **bKash** and **Nagad** notifications, stores
them in a local Room queue, and POSTs lightweight JSON to your backend. Nothing is lost when offline: events stay
`PENDING` and WorkManager retries with exponential backoff once the network is back.

## Get an APK (pick one)

**A. GitHub Actions (no Android Studio needed)**
1. Create a new GitHub repo, push this folder to it (`main` branch).
2. Open the repo → **Actions → Build debug APK** (runs on push, or press *Run workflow*).
3. When it is green, open the run → **Artifacts → txnrelay-debug-apk** → download, unzip, copy `app-debug.apk` to your phone.

**B. Android Studio**
1. *File → Open* this folder (Studio will sync Gradle; JDK 17).
2. *Build → Build APK(s)*, or press Run with your phone connected (USB debugging).
   APK path: `app/build/outputs/apk/debug/app-debug.apk`

**C. Command line** (needs JDK 17 + Android SDK): `./gradlew assembleDebug`

## First run on the phone
1. Install the APK (allow "install unknown apps" for your file manager/browser).
2. Open **TxnRelay → Enable notification access** and switch TxnRelay on.
   - Android 13+ and sideloaded APKs: if the switch is greyed out → *App info → ⋮ → Allow restricted settings*, then retry.
3. Enter your endpoint URL (+ optional API key) → **Save**.
4. Tap **Test event** to see a fake bKash/Nagad event go through the whole pipeline.

Debug builds accept `http://` endpoints (handy for a LAN server); release builds are HTTPS-only.

## Project structure
```
app/src/main/java/com/txnrelay/
├── config/    AppConfig.kt (tunables), TargetApps.kt (bKash/Nagad package names  ← edit here)
├── listener/  PaymentNotificationListener.kt (service), NotificationParser.kt
├── data/
│   ├── db/        PendingEventEntity, PendingEventDao, AppDatabase   (Room, table `pending_events`)
│   ├── model/     EventStatus (PENDING/SENT/FAILED_PERMANENT), CapturedNotification, StatusCounts
│   ├── settings/  SettingsStore.kt (EncryptedSharedPreferences: endpoint, API key, auto-sync, theme)
│   └── EventRepository.kt   (dedup + queue operations)
├── domain/    EventIdGenerator.kt (eventId / fingerprint hashing)
├── network/   EventPayload.kt (JSON fields ← edit here), EventApi.kt, ApiClient.kt, EventUploader.kt
├── sync/      SyncEngine.kt (how), SyncManager.kt (when), SyncWorker.kt (WorkManager)
├── ui/        MainActivity, MainViewModel, MainScreen, MainUiState, components/, theme/
├── di/        AppContainer.kt (manual DI)
└── util/      NotificationAccess.kt, UrlValidator.kt
```

## How it works
- `onNotificationPosted` + one sweep of `getActiveNotifications()` in `onListenerConnected`.
  Non-target packages are dropped first; target ones are parsed null-safely (title, longest of big-text/text/lines, post time, key).
- **Dedup**: `eventId = sha256(package|postTime|title|body)` is the Room primary key (`OnConflictStrategy.IGNORE`).
  A second check ignores the same package+title+body within `DEDUP_WINDOW_MS` (2 min) to absorb re-posted/updated notifications.
- **Sync**: new event → immediate upload attempt; on failure a unique WorkManager job (`NetworkType.CONNECTED`, exponential backoff from 30 s) takes over.
  A mutex makes sure only one upload pass runs at a time. `eventId` is sent in the body and as `Idempotency-Key`.
- **HTTP handling** (`EventUploader.kt`): 2xx and 409 → `SENT`; network error/408/425/429/5xx → stays `PENDING` (retry);
  401/403/404/bad URL → stays `PENDING`, sync stops and the UI shows the reason; other 4xx → `FAILED_PERMANENT`
  ("Sync now" re-queues failed ones). `SENT` rows are purged after 7 days.

### Request
```
POST <your endpoint>
Authorization: Bearer <api key>     (only if set)
Idempotency-Key: <eventId>
Content-Type: application/json

{"eventId":"…","source":"bkash","packageName":"com.bkash.customerapp","title":"…","body":"…",
 "postedAt":1732400000000,"capturedAt":1732400000500,"deviceId":"…"}
```
Respond with any 2xx. Use `eventId` to de-duplicate on the server.

## Customising
| Want to change | Edit |
|---|---|
| Package names / add apps | `config/TargetApps.kt` |
| Dedup window, retry cap, backoff, timeouts | `config/AppConfig.kt` |
| JSON fields | `network/EventPayload.kt` |
| Auth header name/scheme | `network/EventApi.kt` |
| HTTP status → retry/permanent rules | `network/EventUploader.kt` (`classify`) |
| Colours / dark theme | `ui/theme/Color.kt` |

## Verify the package names
Defaults: bKash `com.bkash.customerapp`, Nagad `com.konasl.nagad`. Confirm on your device:
`adb shell pm list packages | grep -i -E "bkash|nagad"` (business/other builds may differ – add them in `TargetApps.kt`).
To see what a real notification looks like: `adb shell dumpsys notification --noredact | grep -A15 bkash`.

## Testing checklist (real device)
1. **Pipeline without bKash**: *Test event* → appears as Pending → flips to Sent (or Pending with a reason if the endpoint fails).
2. **Real notification**: receive a small bKash/Nagad transaction (or a promo notification) → new row appears.
3. **Offline/retry**: turn on airplane mode → trigger an event → stays *Pending* → turn network on → becomes *Sent* within ~1 min
   (backoff: 30 s, 1 m, 2 m …; *Sync now* resets the delay).
4. **Dedup**: same notification updated repeatedly within 2 minutes → single row. Toggle notification access off/on → no duplicates.
5. **Bad config**: wrong URL → 404 message, events stay Pending; fix URL + Save → they flush.
6. **Idempotency**: make the backend return 500 *after* storing the event, then 200 → backend must see the same `eventId` twice.
7. **Process death**: force-stop the app with pending items → open it / receive next event → queue resumes (`scheduleIfPending`).

## Edge cases & OEM notes
- **Battery optimisation** (Xiaomi/MIUI, Oppo/ColorOS, Vivo, Huawei, Samsung "sleeping apps"): set TxnRelay to *No restrictions* / enable *Autostart*,
  lock it in Recents. Otherwise the system may stop the listener. The service calls `requestRebind()` when disconnected, but some OEMs still need the manual setting.
- Notification access is revoked on some devices after app updates or reboots – the status card turns red when that happens.
- bKash/Nagad may hide sensitive text on the lock screen or send generic notifications; you can only capture what the notification contains.
- Notification text is financial data: use HTTPS, an API key, and keep the backend endpoint private.
- Release build: configure signing, then consider enabling R8 (`isMinifyEnabled`) and adding keep rules for kotlinx-serialization/Retrofit.
