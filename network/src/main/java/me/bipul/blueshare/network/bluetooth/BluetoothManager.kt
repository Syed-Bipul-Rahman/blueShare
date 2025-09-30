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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private var bluetoothSocket: android.bluetooth.BluetoothSocket? = null
    private var serverSocket: android.bluetooth.BluetoothServerSocket? = null

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

    override suspend fun connect(device: Device): Result<Unit> = suspendCoroutine { continuation ->
        if (!hasPermissions()) {
            continuation.resume(Result.Error(TransferError.PermissionDenied()))
            return@suspendCoroutine
        }

        Thread {
            try {
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                if (bluetoothDevice == null) {
                    continuation.resume(Result.Error(TransferError.DeviceNotFound("Device not found")))
                    return@Thread
                }

                // Create RFCOMM socket
                val socket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID)
                bluetoothSocket = socket

                // Cancel discovery to improve connection speed
                bluetoothAdapter?.cancelDiscovery()

                // Connect
                socket.connect()

                connectedDevice = device
                continuation.resume(Result.Success(Unit))
            } catch (e: Exception) {
                bluetoothSocket = null
                continuation.resume(Result.Error(TransferError.ConnectionFailed("Bluetooth connection failed: ${e.message}", e)))
            }
        }.start()
    }

    override suspend fun disconnect() {
        try {
            bluetoothSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        bluetoothSocket = null
        serverSocket = null
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

    override suspend fun sendFile(
        file: me.bipul.blueshare.core.model.TransferFile,
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<Unit> = suspendCoroutine { continuation ->
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            continuation.resume(Result.Error(TransferError.ConnectionFailed("Not connected")))
            return@suspendCoroutine
        }

        Thread {
            try {
                val outputStream = bluetoothSocket!!.getOutputStream()
                val dataOutputStream = java.io.DataOutputStream(outputStream)
                val inputStream = context.contentResolver.openInputStream(file.uri)

                if (inputStream == null) {
                    continuation.resume(Result.Error(TransferError.FileError("Cannot open file")))
                    return@Thread
                }

                // Send file metadata
                dataOutputStream.writeUTF(file.safeName)
                dataOutputStream.writeLong(file.size)
                dataOutputStream.writeUTF(file.mimeType ?: "application/octet-stream")
                dataOutputStream.flush()

                // Send file data
                val buffer = ByteArray(1024) // Smaller buffer for Bluetooth
                var bytesTransferred = 0L
                val startTime = System.currentTimeMillis()
                var lastProgressTime = startTime

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break

                    outputStream.write(buffer, 0, read)
                    bytesTransferred += read

                    // Update progress every 200ms (less frequent for Bluetooth)
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime > 200) {
                        val progress = ((bytesTransferred * 100) / file.size).toInt()
                        val elapsed = now - startTime
                        val speed = if (elapsed > 0) bytesTransferred * 1000 / elapsed else 0
                        onProgress(progress, bytesTransferred, speed)
                        lastProgressTime = now
                    }
                }

                // Final progress update
                onProgress(100, bytesTransferred, 0)

                inputStream.close()
                outputStream.flush()

                continuation.resume(Result.Success(Unit))
            } catch (e: Exception) {
                continuation.resume(Result.Error(TransferError.ConnectionLost("Transfer failed: ${e.message}", e)))
            }
        }.start()
    }

    override suspend fun receiveFile(
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<me.bipul.blueshare.core.model.TransferFile> = suspendCoroutine { continuation ->
        Thread {
            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                // Start listening for incoming connections
                if (!hasPermissions()) {
                    continuation.resume(Result.Error(TransferError.PermissionDenied()))
                    return@Thread
                }

                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    "BlueShare",
                    BT_UUID
                )

                if (serverSocket == null) {
                    continuation.resume(Result.Error(TransferError.NotSupported("Cannot create server socket")))
                    return@Thread
                }

                // Accept connection (blocks until a connection is made)
                socket = serverSocket!!.accept(30000) // 30 second timeout

                val inputStream = socket.getInputStream()
                val dataInputStream = java.io.DataInputStream(inputStream)

                // Read file metadata
                val fileName = dataInputStream.readUTF()
                val fileSize = dataInputStream.readLong()
                val mimeType = dataInputStream.readUTF()

                // Create file in Downloads
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val outputFile = java.io.File(downloadsDir, fileName)
                val outputStream = java.io.FileOutputStream(outputFile)

                // Receive file data
                val buffer = ByteArray(1024)
                var bytesTransferred = 0L
                val startTime = System.currentTimeMillis()
                var lastProgressTime = startTime

                while (bytesTransferred < fileSize) {
                    val toRead = minOf(buffer.size.toLong(), fileSize - bytesTransferred).toInt()
                    val read = inputStream.read(buffer, 0, toRead)
                    if (read == -1) break

                    outputStream.write(buffer, 0, read)
                    bytesTransferred += read

                    // Update progress every 200ms
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime > 200) {
                        val progress = ((bytesTransferred * 100) / fileSize).toInt()
                        val elapsed = now - startTime
                        val speed = if (elapsed > 0) bytesTransferred * 1000 / elapsed else 0
                        onProgress(progress, bytesTransferred, speed)
                        lastProgressTime = now
                    }
                }

                // Final progress update
                onProgress(100, bytesTransferred, 0)

                outputStream.close()
                inputStream.close()

                val transferFile = me.bipul.blueshare.core.model.TransferFile(
                    uri = android.net.Uri.fromFile(outputFile),
                    name = fileName,
                    size = fileSize,
                    mimeType = mimeType
                )

                continuation.resume(Result.Success(transferFile))
            } catch (e: Exception) {
                continuation.resume(Result.Error(TransferError.ConnectionLost("Receive failed: ${e.message}", e)))
            } finally {
                socket?.close()
            }
        }.start()
    }

    companion object {
        private val BT_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}