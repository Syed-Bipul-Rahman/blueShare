package me.bipul.blueshare.domain.repository

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.Result
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferFile
import me.bipul.blueshare.core.model.TransferMethod

/**
 * Repository interface for file transfer operations.
 * Follows Interface Segregation Principle - defines only transfer-related operations.
 * Abstracts the underlying transfer mechanism (Bluetooth/Wi-Fi Direct).
 */
interface TransferRepository {

    /**
     * Starts device discovery for the specified transfer method.
     *
     * @param method The transfer method to use for discovery
     * @return Flow of FileTransferState updates
     */
    fun discoverDevices(method: TransferMethod): Flow<FileTransferState>

    /**
     * Stops device discovery.
     *
     * @return Result indicating success or failure
     */
    suspend fun stopDiscovery(): Result<Unit>

    /**
     * Connects to a discovered device.
     *
     * @param device The device to connect to
     * @return Flow of FileTransferState updates during connection
     */
    fun connectToDevice(device: Device): Flow<FileTransferState>

    /**
     * Disconnects from the currently connected device.
     *
     * @return Result indicating success or failure
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Sends files to the connected device.
     *
     * @param files List of files to transfer
     * @return Flow of FileTransferState updates during transfer
     */
    fun sendFiles(files: List<TransferFile>): Flow<FileTransferState>

    /**
     * Receives files from the connected device.
     *
     * @return Flow of FileTransferState updates during reception
     */
    fun receiveFiles(): Flow<FileTransferState>

    /**
     * Cancels an ongoing transfer.
     *
     * @return Result indicating success or failure
     */
    suspend fun cancelTransfer(): Result<Unit>

    /**
     * Pauses an ongoing transfer (if supported).
     *
     * @return Result indicating success or failure
     */
    suspend fun pauseTransfer(): Result<Unit>

    /**
     * Resumes a paused transfer (if supported).
     *
     * @return Result indicating success or failure
     */
    suspend fun resumeTransfer(): Result<Unit>
}