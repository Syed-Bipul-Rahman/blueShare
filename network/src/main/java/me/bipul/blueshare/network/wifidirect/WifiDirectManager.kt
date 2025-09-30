package me.bipul.blueshare.network.wifidirect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
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
 * Manager for Wi-Fi Direct operations.
 * Implements NetworkDataSource for Wi-Fi Direct communication.
 * Follows Single Responsibility Principle - handles only Wi-Fi Direct functionality.
 *
 * @property context Android application context
 */
class WifiDirectManager(private val context: Context) : NetworkDataSource {

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private val wifiChannel: WifiP2pManager.Channel? by lazy {
        manager?.initialize(context, context.mainLooper, null)
    }

    private var connectedDevice: Device? = null
    private var connectionInfo: WifiP2pInfo? = null

    override fun isAvailable(): Boolean {
        return manager != null && context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
    }

    override fun isEnabled(): Boolean {
        // Wi-Fi Direct availability is checked via system broadcasts
        // This is a simplified check
        return isAvailable()
    }

    override fun startDiscovery(): Flow<Result<Device>> = callbackFlow {
        if (!hasPermissions()) {
            trySend(Result.Error(TransferError.PermissionDenied("Wi-Fi Direct permissions not granted")))
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        manager?.requestPeers(wifiChannel) { peers: WifiP2pDeviceList ->
                            peers.deviceList.forEach { wifiP2pDevice ->
                                val device = wifiP2pDevice.toDevice()
                                trySend(Result.Success(device))
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        wifiChannel?.let { ch ->
            manager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Discovery started successfully
                }

                override fun onFailure(reason: Int) {
                    trySend(Result.Error(TransferError.DeviceNotFound("Discovery failed: $reason")))
                }
            })
        }

        awaitClose {
            context.unregisterReceiver(receiver)
            manager?.stopPeerDiscovery(wifiChannel, null)
        }
    }

    override suspend fun stopDiscovery() {
        manager?.stopPeerDiscovery(wifiChannel, null)
    }

    override suspend fun connect(device: Device): Result<Unit> = suspendCoroutine { continuation ->
        if (!hasPermissions()) {
            continuation.resume(Result.Error(TransferError.PermissionDenied()))
            return@suspendCoroutine
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.address
        }

        manager?.connect(wifiChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectedDevice = device
                continuation.resume(Result.Success(Unit))
            }

            override fun onFailure(reason: Int) {
                continuation.resume(Result.Error(TransferError.ConnectionFailed("Connection failed: $reason")))
            }
        })
    }

    override suspend fun disconnect() {
        manager?.removeGroup(wifiChannel, null)
        connectedDevice = null
        connectionInfo = null
    }

    override fun getConnectedDevice(): Device? = connectedDevice

    /**
     * Checks if required permissions are granted.
     */
    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Converts WifiP2pDevice to our Device model.
     */
    private fun WifiP2pDevice.toDevice(): Device {
        return Device(
            id = deviceAddress,
            name = deviceName,
            address = deviceAddress,
            type = TransferMethod.WifiDirect,
            isConnected = status == WifiP2pDevice.CONNECTED
        )
    }

    // Note: File transfer implementation would go here
    // For brevity, these are placeholder implementations
    override suspend fun sendFile(
        file: me.bipul.blueshare.core.model.TransferFile,
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<Unit> {
        // Implementation would use sockets to transfer data
        return Result.Error(TransferError.NotSupported("Send not yet implemented"))
    }

    override suspend fun receiveFile(
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<me.bipul.blueshare.core.model.TransferFile> {
        // Implementation would use sockets to receive data
        return Result.Error(TransferError.NotSupported("Receive not yet implemented"))
    }
}