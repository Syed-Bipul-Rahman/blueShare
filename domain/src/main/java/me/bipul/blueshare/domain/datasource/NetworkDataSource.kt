package me.bipul.blueshare.domain.datasource

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.Result
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.TransferFile

/**
 * Data source interface for network operations.
 * Follows Dependency Inversion Principle - domain layer depends on abstraction, not implementation.
 */
interface NetworkDataSource {

    /**
     * Checks if the transfer method is available on this device.
     *
     * @return true if available, false otherwise
     */
    fun isAvailable(): Boolean

    /**
     * Checks if the transfer method is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    fun isEnabled(): Boolean

    /**
     * Starts discovering nearby devices.
     *
     * @return Flow emitting discovered devices
     */
    fun startDiscovery(): Flow<Result<Device>>

    /**
     * Stops device discovery.
     */
    suspend fun stopDiscovery()

    /**
     * Connects to a specific device.
     *
     * @param device The device to connect to
     * @return Result indicating connection success or failure
     */
    suspend fun connect(device: Device): Result<Unit>

    /**
     * Disconnects from the currently connected device.
     */
    suspend fun disconnect()

    /**
     * Sends a file to the connected device.
     *
     * @param file The file to send
     * @param onProgress Callback for progress updates (0-100)
     * @return Result indicating transfer success or failure
     */
    suspend fun sendFile(
        file: TransferFile,
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<Unit>

    /**
     * Receives a file from the connected device.
     *
     * @param onProgress Callback for progress updates (0-100)
     * @return Result containing the received file or error
     */
    suspend fun receiveFile(
        onProgress: (progress: Int, bytesTransferred: Long, speed: Long) -> Unit
    ): Result<TransferFile>

    /**
     * Gets the currently connected device, if any.
     *
     * @return The connected device, or null if not connected
     */
    fun getConnectedDevice(): Device?
}