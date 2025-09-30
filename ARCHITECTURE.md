# BlueShare Architecture Documentation

## Overview

BlueShare follows **Clean Architecture** principles with clear separation of concerns across multiple modules. The architecture is designed to be maintainable, testable, and loosely coupled.

## Architecture Layers

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│        (app module - MVVM)             │
│   ViewModels, Activities, Fragments     │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│           Domain Layer                  │
│    (domain module - Business Logic)     │
│   Use Cases, Repository Interfaces      │
└───────────┬─────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────┐
│            Data Layer                   │
│  (data module - Data Management)        │
│   Repository Implementations            │
└───────────┬─────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────┐
│          Network Layer                  │
│   (network module - Platform APIs)      │
│  Wi-Fi Direct & Bluetooth Managers      │
└─────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────┐
│            Core Layer                   │
│    (core module - Shared Code)          │
│  Models, Interfaces, Utilities          │
└─────────────────────────────────────────┘
```

## Module Breakdown

### 1. Core Module (`:core`)

**Purpose:** Shared code used across all modules

**Contents:**
- `model/` - Data models (Device, TransferFile, TransferMethod, etc.)
- `Result.kt` - Generic result wrapper for success/failure
- `util/` - Utility classes (FileUtils)

**Key Principles:**
- No Android dependencies (except Android SDK classes like Uri)
- Pure Kotlin where possible
- Immutable data classes
- No business logic

**Example:**
```kotlin
// Core model - no dependencies on other modules
data class Device(
    val id: String,
    val name: String,
    val address: String,
    val type: TransferMethod,
    val isConnected: Boolean = false
)
```

### 2. Domain Module (`:domain`)

**Purpose:** Business logic and use case definitions

**Dependencies:** `core`

**Contents:**
- `usecase/` - Business logic implementations
  - `DiscoverDevicesUseCase.kt`
  - `SendFilesUseCase.kt`
  - `ReceiveFilesUseCase.kt`
- `repository/` - Repository interface definitions
- `datasource/` - Data source interfaces

**Key Principles:**
- **Dependency Inversion:** Depends on abstractions (interfaces), not implementations
- **Single Responsibility:** Each use case has one clear purpose
- **Interface Segregation:** Small, focused interfaces
- Platform-agnostic where possible

**Example:**
```kotlin
// Use case - orchestrates business logic
class SendFilesUseCase(
    private val repository: TransferRepository
) {
    operator fun invoke(
        device: Device,
        files: List<TransferFile>
    ): Flow<FileTransferState> {
        return repository.sendFiles(files)
    }

    suspend fun cancel() = repository.cancelTransfer()
}
```

### 3. Data Module (`:data`)

**Purpose:** Implementation of repository interfaces

**Dependencies:** `core`, `domain`

**Contents:**
- `repository/` - Repository implementations
  - `TransferRepositoryImpl.kt`

**Key Principles:**
- Implements interfaces from domain layer
- Coordinates between multiple data sources
- **Strategy Pattern:** Selects appropriate data source (Wi-Fi/Bluetooth)
- Handles data transformation

**Example:**
```kotlin
class TransferRepositoryImpl(
    private val wifiDirectDataSource: NetworkDataSource,
    private val bluetoothDataSource: NetworkDataSource
) : TransferRepository {

    // Strategy pattern for selecting data source
    private fun selectDataSource(method: TransferMethod): NetworkDataSource? {
        return when (method) {
            TransferMethod.WifiDirect -> wifiDirectDataSource
            TransferMethod.Bluetooth -> bluetoothDataSource
            TransferMethod.Auto -> /* auto-select logic */
        }
    }
}
```

### 4. Network Module (`:network`)

**Purpose:** Platform-specific network implementations

**Dependencies:** `core`, `domain`

**Contents:**
- `wifidirect/` - Wi-Fi Direct implementation
  - `WifiDirectManager.kt`
- `bluetooth/` - Bluetooth implementation
  - `BluetoothManager.kt`

**Key Principles:**
- Implements `NetworkDataSource` interface
- Platform-specific Android APIs
- **Single Responsibility:** Each manager handles one protocol
- Reactive with Kotlin Flows

**Example:**
```kotlin
class WifiDirectManager(
    private val context: Context
) : NetworkDataSource {

    override fun startDiscovery(): Flow<Result<Device>> = callbackFlow {
        // Platform-specific implementation
        val manager = context.getSystemService(WIFI_P2P_SERVICE)
        // ... discovery logic

        awaitClose { /* cleanup */ }
    }
}
```

### 5. App Module (`:app`)

**Purpose:** UI and presentation logic

**Dependencies:** `core`, `domain`, `data`, `network`

**Contents:**
- `ui/` - UI components
  - `transfer/TransferViewModel.kt`
- `MainActivity.kt`
- Layouts, resources

**Key Principles:**
- **MVVM Pattern:** ViewModel + LiveData/StateFlow
- View Binding for type-safe view access
- Reactive UI updates via Flow/StateFlow
- No business logic in UI layer

**Example:**
```kotlin
class TransferViewModel(
    private val discoverDevicesUseCase: DiscoverDevicesUseCase,
    private val sendFilesUseCase: SendFilesUseCase
) : ViewModel() {

    private val _transferState = MutableStateFlow<FileTransferState>(FileTransferState.Idle)
    val transferState: StateFlow<FileTransferState> = _transferState.asStateFlow()

    fun startDiscovery(method: TransferMethod) {
        viewModelScope.launch {
            discoverDevicesUseCase(method).collect { state ->
                _transferState.value = state
            }
        }
    }
}
```

## SOLID Principles Application

### Single Responsibility Principle (SRP)
- **Each class has one reason to change**
- `WifiDirectManager` - Only manages Wi-Fi Direct
- `BluetoothManager` - Only manages Bluetooth
- `FileUtils` - Only file operations
- `DiscoverDevicesUseCase` - Only device discovery

### Open/Closed Principle (OCP)
- **Open for extension, closed for modification**
- `TransferMethod` sealed class - Can add new methods without modifying existing code
- `FileTransferState` sealed class - New states can be added
- Strategy pattern in repository allows new data sources

### Liskov Substitution Principle (LSP)
- **Subtypes must be substitutable for base types**
- Any `NetworkDataSource` implementation can replace another
- Both `WifiDirectManager` and `BluetoothManager` implement `NetworkDataSource` correctly

### Interface Segregation Principle (ISP)
- **Clients shouldn't depend on interfaces they don't use**
- `NetworkDataSource` - Focused on network operations
- `TransferRepository` - Focused on transfer operations
- No "God interfaces"

### Dependency Inversion Principle (DIP)
- **Depend on abstractions, not concretions**
- Domain layer defines `TransferRepository` interface
- Data layer implements the interface
- Presentation layer depends on use cases (abstractions)
- Network implementations hidden behind `NetworkDataSource`

## Design Patterns

### 1. Repository Pattern
**Purpose:** Abstract data sources from business logic

```kotlin
interface TransferRepository {
    fun discoverDevices(method: TransferMethod): Flow<FileTransferState>
    fun sendFiles(files: List<TransferFile>): Flow<FileTransferState>
}

class TransferRepositoryImpl(...) : TransferRepository {
    // Implementation
}
```

### 2. Strategy Pattern
**Purpose:** Select algorithm at runtime

```kotlin
// Repository selects appropriate data source based on method
private fun selectDataSource(method: TransferMethod): NetworkDataSource? {
    return when (method) {
        TransferMethod.WifiDirect -> wifiDirectDataSource
        TransferMethod.Bluetooth -> bluetoothDataSource
        TransferMethod.Auto -> autoSelectBestDataSource()
    }
}
```

### 3. Observer Pattern
**Purpose:** Reactive state updates

```kotlin
// ViewModel exposes StateFlow
val transferState: StateFlow<FileTransferState>

// UI observes state changes
viewModel.transferState.collect { state ->
    updateUI(state)
}
```

### 4. Factory Pattern (Implicit)
**Purpose:** Object creation abstraction

```kotlin
// Use cases act as factories for repository operations
class SendFilesUseCase(private val repository: TransferRepository) {
    operator fun invoke(...): Flow<FileTransferState> {
        return repository.sendFiles(...)
    }
}
```

## Data Flow

### Device Discovery Flow

```
User Action (UI)
    ↓
ViewModel.startDiscovery()
    ↓
DiscoverDevicesUseCase.invoke()
    ↓
TransferRepository.discoverDevices()
    ↓
NetworkDataSource.startDiscovery()
    ↓
Android Platform APIs (Wi-Fi/Bluetooth)
    ↓
Result<Device> ← Flow emissions
    ↓
FileTransferState.DevicesFound(devices)
    ↓
ViewModel._transferState.value = state
    ↓
UI updates (collect from StateFlow)
```

### File Transfer Flow

```
User selects files and device
    ↓
ViewModel.sendFiles()
    ↓
SendFilesUseCase.invoke(device, files)
    ↓
Repository.sendFiles(files)
    ↓
NetworkDataSource.sendFile(file) for each file
    ↓
Socket I/O (Platform-specific)
    ↓
Progress callbacks
    ↓
FileTransferState.Transferring(progress...)
    ↓
UI shows progress
    ↓
FileTransferState.Completed(...)
    ↓
UI shows success
```

## State Management

### Sealed Classes for Type Safety

```kotlin
sealed class FileTransferState {
    object Idle : FileTransferState()
    object Discovering : FileTransferState()
    data class DevicesFound(val devices: List<Device>) : FileTransferState()
    // ... more states
}

// Exhaustive when expression
when (state) {
    is FileTransferState.Idle -> { /* handle */ }
    is FileTransferState.Discovering -> { /* handle */ }
    is FileTransferState.DevicesFound -> { /* handle */ }
    // Compiler ensures all cases are covered
}
```

## Error Handling

### Result Wrapper

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: TransferError) : Result<Nothing>()
}

// Usage
when (val result = dataSource.connect(device)) {
    is Result.Success -> { /* success */ }
    is Result.Error -> { /* handle error */ }
}
```

### Typed Errors

```kotlin
sealed class TransferError {
    data class PermissionDenied(...) : TransferError()
    data class DeviceNotFound(...) : TransferError()
    data class ConnectionFailed(...) : TransferError()
    // ... more error types
}
```

## Dependency Injection

Currently using **constructor injection** (manual DI):

```kotlin
// Manual DI in app initialization
val wifiManager = WifiDirectManager(context)
val bluetoothManager = BluetoothManager(context)
val repository = TransferRepositoryImpl(wifiManager, bluetoothManager)
val useCase = DiscoverDevicesUseCase(repository)
val viewModel = TransferViewModel(useCase, ...)
```

**Future:** Can easily migrate to Hilt or Koin without changing business logic.

## Testing Strategy

### Unit Tests
- **Core:** Test models and utilities
- **Domain:** Test use cases with mocked repositories
- **Data:** Test repository logic with mocked data sources

### Integration Tests
- Test full flow from ViewModel to Repository
- Mock Android platform APIs

### Instrumentation Tests
- Test UI components
- Test permission handling
- Test actual Bluetooth/Wi-Fi operations on device

## Reusability

### As a Library

To use BlueShare in another project:

1. Copy modules: `:core`, `:domain`, `:data`, `:network`
2. Add dependencies in `build.gradle.kts`
3. Initialize components in your app
4. Use ViewModels or create custom UI

### Public API

Core classes you'll use:
- `TransferViewModel` - Main entry point
- Use cases - `DiscoverDevicesUseCase`, `SendFilesUseCase`
- Models - `Device`, `TransferFile`, `FileTransferState`
- Utilities - `FileUtils`

## Best Practices Followed

1. **Immutability** - All models are immutable (`data class` with `val`)
2. **Null Safety** - Leverage Kotlin null safety
3. **Coroutines** - Asynchronous operations with structured concurrency
4. **Flow** - Reactive streams for state updates
5. **ViewBinding** - Type-safe view access (no `findViewById`)
6. **Material Design 3** - Modern UI components
7. **Sealed Classes** - Exhaustive state/error handling
8. **Extension Functions** - Cleaner utility code
9. **Scope Functions** - Readable initialization blocks
10. **Documentation** - KDoc comments on public APIs

## Performance Considerations

1. **Lazy Initialization** - Managers initialized on first use
2. **Coroutine Scopes** - Proper lifecycle management
3. **Flow Cancellation** - Clean up resources with `awaitClose`
4. **Background Threading** - I/O operations off main thread
5. **Memory Management** - No memory leaks from listeners/receivers

## Security Considerations

1. **File Name Sanitization** - Prevent path traversal
2. **Permission Checks** - Runtime permission validation
3. **Scoped Storage** - Use Android 10+ scoped storage
4. **No Sensitive Logging** - Don't log user data
5. **Secure Defaults** - Fail closed on errors

---

This architecture provides a solid foundation for a production-ready file sharing solution. It's extensible, testable, and follows Android best practices.