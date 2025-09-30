package me.bipul.blueshare.network.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import me.bipul.blueshare.core.Result
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.TransferError
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.domain.datasource.NetworkDataSource

/**
 * Manager for Bluetooth operations.
 * Implements NetworkDataSource for Bluetooth Classic communication.
 * Follows Single Responsibility Principle - handles only Bluetooth functionality.
 *
 * @property context Android application context
 */
class BluetoothManager(private val context: Context) : NetworkDataSource {

    private val bluetoothManager: AndroidBluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private var connectedDevice: Device? = null

    override fun isAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    override fun isEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override fun startDiscovery(): Flow<Result<Device>> = callbackFlow {
        if (!hasPermissions()) {
            trySend(Result.Error(TransferError.PermissionDenied("Bluetooth permissions not granted")))
            close()
            return@callbackFlow
        }

        if (!isEnabled()) {
            trySend(Result.Error(TransferError.NotSupported("Bluetooth is not enabled")))
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AndroidBluetoothDevice.ACTION_FOUND -> {
                        val device: AndroidBluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                        }

                        device?.let {
                            trySend(Result.Success(it.toDevice()))
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        close()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        if (!bluetoothAdapter!!.startDiscovery()) {
            trySend(Result.Error(TransferError.DeviceNotFound("Failed to start Bluetooth discovery")))
            close()
        }

        awaitClose {
            context.unregisterReceiver(receiver)
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    override suspend fun stopDiscovery() {
        if (hasPermissions()) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    override suspend fun connect(device: Device): Result<Unit> {
        if (!hasPermissions()) {
            return Result.Error(TransferError.PermissionDenied())
        }

        // In a real implementation, we would create a BluetoothSocket
        // and connect to the device here
        connectedDevice = device
        return Result.Success(Unit)
    }

    override suspend fun disconnect() {
        // Close socket connection
        connectedDevice = null
    }

    override fun getConnectedDevice(): Device? = connectedDevice

    /**
     * Checks if required Bluetooth permissions are granted.
     */
    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Converts Android BluetoothDevice to our Device model.
     */
    private fun AndroidBluetoothDevice.toDevice(): Device {
        val deviceName = if (hasPermissions()) {
            name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }

        return Device(
            id = address,
            name = deviceName,
            address = address,
            type = TransferMethod.Bluetooth,
            isConnected = false
        )
    }

    // Placeholder implementations for file transfer
    override suspend fun sendFile(
        file: me.bipul.blueshare.core.model.TransferFile,
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<Unit> {
        return Result.Error(TransferError.NotSupported("Send not yet implemented"))
    }

    override suspend fun receiveFile(
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<me.bipul.blueshare.core.model.TransferFile> {
        return Result.Error(TransferError.NotSupported("Receive not yet implemented"))
    }
}