# BlueShare Quick Start Guide

Get started with BlueShare in 5 minutes!

## 🚀 Quick Setup

### 1. Clone and Build (2 minutes)

```bash
cd /path/to/Blueshare
./gradlew assembleDebug
```

### 2. Install on Device (1 minute)

```bash
./gradlew installDebug
```

### 3. Grant Permissions

When the app launches, it will request necessary permissions. Grant all for full functionality:
- Location (for Wi-Fi Direct discovery)
- Bluetooth (for device scanning)
- Nearby Devices (Android 12+)
- Storage (for file access)
- Notifications (for transfer updates)

## 📱 Using the App

### Current Features (v0.1)

1. **Main Screen** ✅
   - Welcome card with app info
   - Send Files button (placeholder)
   - Receive Files button (placeholder)
   - Settings FAB

2. **Architecture** ✅
   - Complete multi-module structure
   - MVVM with StateFlow
   - Clean architecture layers
   - Wi-Fi Direct & Bluetooth managers

## 💻 Development Quick Reference

### Project Structure

```
:app        → UI and ViewModels
:core       → Models and utilities
:domain     → Use cases (business logic)
:data       → Repository implementations
:network    → Wi-Fi Direct & Bluetooth
```

### Key Classes

| Class | Purpose | Location |
|-------|---------|----------|
| `TransferViewModel` | Main ViewModel | `app/ui/transfer/` |
| `TransferRepository` | Repository interface | `domain/repository/` |
| `WifiDirectManager` | Wi-Fi Direct impl | `network/wifidirect/` |
| `BluetoothManager` | Bluetooth impl | `network/bluetooth/` |
| `FileUtils` | File operations | `core/util/` |

### Adding a New Feature

**Example: Add a new transfer method**

1. **Add to sealed class** (`core/model/TransferMethod.kt`):
```kotlin
sealed class TransferMethod {
    object WifiDirect : TransferMethod()
    object Bluetooth : TransferMethod()
    object Nfc : TransferMethod()  // ← New method
    object Auto : TransferMethod()
}
```

2. **Create manager** (`network/nfc/NfcManager.kt`):
```kotlin
class NfcManager(context: Context) : NetworkDataSource {
    // Implement interface methods
}
```

3. **Update repository** (`data/repository/TransferRepositoryImpl.kt`):
```kotlin
private fun selectDataSource(method: TransferMethod): NetworkDataSource? {
    return when (method) {
        TransferMethod.WifiDirect -> wifiDirectDataSource
        TransferMethod.Bluetooth -> bluetoothDataSource
        TransferMethod.Nfc -> nfcDataSource  // ← Handle new method
        TransferMethod.Auto -> autoSelectBestDataSource()
    }
}
```

That's it! The architecture handles the rest.

## 🧪 Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run on Device

```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] App launches without crashes
- [ ] Permissions are requested
- [ ] UI renders correctly
- [ ] Buttons are responsive
- [ ] No ANR (Application Not Responding)

## 🐛 Common Issues

### Build Fails

**Issue:** Gradle sync fails
```
Solution:
1. File → Invalidate Caches / Restart
2. ./gradlew clean build
```

### Permission Denied

**Issue:** Wi-Fi Direct or Bluetooth not working
```
Solution:
1. Check AndroidManifest.xml has all permissions
2. Verify runtime permissions are granted
3. Check device supports the feature
```

### Module Not Found

**Issue:** Cannot resolve module reference
```
Solution:
1. Verify settings.gradle.kts includes the module
2. Sync Gradle files
3. Check module build.gradle.kts exists
```

## 📝 Next Steps for Development

### Priority 1: Complete File Transfer

**Location:** `network/wifidirect/WifiDirectManager.kt` and `network/bluetooth/BluetoothManager.kt`

Implement these placeholder methods:
```kotlin
override suspend fun sendFile(
    file: TransferFile,
    onProgress: (Int, Long, Long) -> Unit
): Result<Unit> {
    // TODO: Implement socket-based file transfer
    // 1. Get WifiP2pInfo for socket address
    // 2. Create ServerSocket/Socket
    // 3. Transfer file in chunks
    // 4. Call onProgress() for updates
}

override suspend fun receiveFile(
    onProgress: (Int, Long, Long) -> Unit
): Result<TransferFile> {
    // TODO: Implement socket-based file reception
}
```

**Reference Implementation Pattern:**
```kotlin
// Wi-Fi Direct send example
val socket = Socket()
socket.connect(InetSocketAddress(connectionInfo.groupOwnerAddress, PORT), 5000)

val outputStream = socket.getOutputStream()
val inputStream = context.contentResolver.openInputStream(file.uri)

val buffer = ByteArray(8192)
var bytesTransferred = 0L
var lastProgressTime = System.currentTimeMillis()

while (true) {
    val read = inputStream?.read(buffer) ?: break
    if (read == -1) break

    outputStream.write(buffer, 0, read)
    bytesTransferred += read

    // Update progress every 100ms
    val now = System.currentTimeMillis()
    if (now - lastProgressTime > 100) {
        val progress = ((bytesTransferred * 100) / file.size).toInt()
        val speed = calculateSpeed(bytesTransferred, elapsedTime)
        onProgress(progress, bytesTransferred, speed)
        lastProgressTime = now
    }
}

inputStream?.close()
outputStream.close()
socket.close()
```

### Priority 2: Add UI Screens

**Create:**
1. `DeviceListFragment` - Show discovered devices
2. `FilePickerFragment` - Select files to send
3. `TransferProgressFragment` - Show transfer status

**Use ViewBinding:**
```kotlin
class DeviceListFragment : Fragment() {
    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(...): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }
}
```

### Priority 3: Implement Foreground Service

**Location:** `app/service/FileTransferService.kt`

```kotlin
class FileTransferService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start transfer in background
        return START_STICKY
    }

    private fun createNotification(): Notification {
        // Create notification with progress updates
    }
}
```

## 🔧 Useful Commands

### Gradle

```bash
# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Build specific module
./gradlew :core:build

# Run tests for specific module
./gradlew :domain:test

# Check dependencies
./gradlew :app:dependencies
```

### ADB

```bash
# List devices
adb devices

# Install APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n me.bipul.blueshare/.MainActivity

# View logs
adb logcat | grep BlueShare

# Clear app data
adb shell pm clear me.bipul.blueshare

# Grant permission
adb shell pm grant me.bipul.blueshare android.permission.ACCESS_FINE_LOCATION
```

## 📚 Learning Resources

### Kotlin Coroutines
- [Official Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- Flow: Reactive streams for state updates

### Android Wi-Fi Direct
- [P2P API Guide](https://developer.android.com/guide/topics/connectivity/wifip2p)
- Socket programming for data transfer

### Bluetooth Classic
- [Bluetooth Guide](https://developer.android.com/guide/topics/connectivity/bluetooth)
- RFCOMM sockets

### Clean Architecture
- [Uncle Bob's Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- Separation of concerns

## 💡 Pro Tips

1. **Use Coroutine Scopes Properly**
   ```kotlin
   // ✅ Good - tied to lifecycle
   viewModelScope.launch { }

   // ❌ Bad - memory leak risk
   GlobalScope.launch { }
   ```

2. **Handle State Exhaustively**
   ```kotlin
   when (state) {
       is FileTransferState.Idle -> { }
       is FileTransferState.Transferring -> { }
       // Compiler ensures all cases handled
   }
   ```

3. **Test Use Cases**
   ```kotlin
   @Test
   fun `sendFiles emits Transferring state`() = runTest {
       val useCase = SendFilesUseCase(mockRepository)
       useCase(device, files).test {
           assertThat(awaitItem()).isInstanceOf<Transferring>()
       }
   }
   ```

4. **Use Type-Safe Navigation** (when adding more screens)
   ```kotlin
   // Consider Jetpack Compose Navigation or Navigation Component
   ```

## 🎯 Success Criteria

✅ App builds without errors
✅ Runs on Android 10+ devices
✅ Permissions handled correctly
✅ Clean architecture maintained
✅ SOLID principles followed
✅ Code is documented

## 🤝 Contributing

1. Follow existing code style
2. Write tests for new features
3. Update documentation
4. Use meaningful commit messages:
   - `feat: add NFC support`
   - `fix: correct permission check`
   - `docs: update README`

## 📞 Support

- **Documentation:** See `README.md` and `ARCHITECTURE.md`
- **Issues:** Check code comments for TODOs
- **Questions:** Review inline KDoc comments

---

**Happy Coding! 🚀**

Remember: The architecture is already solid. Focus on implementing the core features (file transfer sockets, UI screens) and the app will be production-ready!