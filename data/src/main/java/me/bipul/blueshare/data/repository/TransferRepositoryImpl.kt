package me.bipul.blueshare.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.bipul.blueshare.core.Result
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferError
import me.bipul.blueshare.core.model.TransferFile
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.domain.datasource.NetworkDataSource
import me.bipul.blueshare.domain.repository.TransferRepository

/**
 * Implementation of TransferRepository.
 * Follows Dependency Inversion - depends on NetworkDataSource abstraction.
 * Coordinates between different network data sources based on transfer method.
 *
 * @property wifiDirectDataSource Wi-Fi Direct data source
 * @property bluetoothDataSource Bluetooth data source
 */
class TransferRepositoryImpl(
    private val wifiDirectDataSource: NetworkDataSource,
    private val bluetoothDataSource: NetworkDataSource
) : TransferRepository {

    private var currentDataSource: NetworkDataSource? = null
    private var discoveredDevices = mutableListOf<Device>()

    override fun discoverDevices(method: TransferMethod): Flow<FileTransferState> = flow {
        emit(FileTransferState.Discovering)

        val dataSource = selectDataSource(method)
        if (dataSource == null) {
            emit(FileTransferState.Failed(TransferError.NotSupported("Selected method not available")))
            return@flow
        }

        currentDataSource = dataSource
        discoveredDevices.clear()

        dataSource.startDiscovery()
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        val device = result.data
                        if (!discoveredDevices.any { it.id == device.id }) {
                            discoveredDevices.add(device)
                        }
                        FileTransferState.DevicesFound(discoveredDevices.toList())
                    }
                    is Result.Error -> FileTransferState.Failed(result.error)
                }
            }
            .catch { e ->
                emit(FileTransferState.Failed(TransferError.Unknown(throwable = e)))
            }
            .collect { state ->
                emit(state)
            }
    }

    override suspend fun stopDiscovery(): Result<Unit> {
        currentDataSource?.stopDiscovery()
        return Result.Success(Unit)
    }

    override fun connectToDevice(device: Device): Flow<FileTransferState> = flow {
        emit(FileTransferState.Connecting(device))

        val dataSource = currentDataSource
        if (dataSource == null) {
            emit(FileTransferState.Failed(TransferError.NotSupported("No data source selected")))
            return@flow
        }

        when (val result = dataSource.connect(device)) {
            is Result.Success -> {
                emit(FileTransferState.Connected(device))
            }
            is Result.Error -> {
                emit(FileTransferState.Failed(result.error))
            }
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        currentDataSource?.disconnect()
        return Result.Success(Unit)
    }

    override fun sendFiles(files: List<TransferFile>): Flow<FileTransferState> = flow {
        val dataSource = currentDataSource
        if (dataSource == null) {
            emit(FileTransferState.Failed(TransferError.NotSupported("No active connection")))
            return@flow
        }

        val totalBytes = files.sumOf { it.size }
        var bytesTransferred = 0L
        val startTime = System.currentTimeMillis()

        files.forEachIndexed { index, file ->
            var fileBytes = 0L

            when (val result = dataSource.sendFile(file) { progress, bytes, speed ->
                fileBytes = bytes
                val totalTransferred = bytesTransferred + bytes
                val overallProgress = ((totalTransferred * 100) / totalBytes).toInt()
                val elapsed = System.currentTimeMillis() - startTime
                val avgSpeed = if (elapsed > 0) totalTransferred * 1000 / elapsed else 0
                val remaining = if (avgSpeed > 0) (totalBytes - totalTransferred) * 1000 / avgSpeed else 0

                // Note: Progress updates would be handled here
                // In a real implementation, we'd emit these via SharedFlow or StateFlow
            }) {
                is Result.Success -> {
                    bytesTransferred += fileBytes
                }
                is Result.Error -> {
                    emit(FileTransferState.Failed(result.error))
                    return@flow
                }
            }
        }

        val duration = System.currentTimeMillis() - startTime
        emit(FileTransferState.Completed(
            filesTransferred = files.size,
            totalBytes = totalBytes,
            duration = duration
        ))
    }

    override fun receiveFiles(): Flow<FileTransferState> = flow {
        val dataSource = currentDataSource
        if (dataSource == null) {
            emit(FileTransferState.Failed(TransferError.NotSupported("No active connection")))
            return@flow
        }

        // Simplified implementation - in reality would handle multiple files
        emit(FileTransferState.Transferring(
            progress = 0,
            bytesTransferred = 0,
            totalBytes = 0,
            transferSpeed = 0,
            remainingTime = 0,
            currentFile = "Receiving..."
        ))

        when (val result = dataSource.receiveFile { progress, bytes, speed ->
            // Update progress
        }) {
            is Result.Success -> {
                emit(FileTransferState.Completed(
                    filesTransferred = 1,
                    totalBytes = result.data.size,
                    duration = 0
                ))
            }
            is Result.Error -> {
                emit(FileTransferState.Failed(result.error))
            }
        }
    }

    override suspend fun cancelTransfer(): Result<Unit> {
        // Implementation would cancel ongoing transfer
        return Result.Success(Unit)
    }

    override suspend fun pauseTransfer(): Result<Unit> {
        // Implementation would pause transfer
        return Result.Success(Unit)
    }

    override suspend fun resumeTransfer(): Result<Unit> {
        // Implementation would resume transfer
        return Result.Success(Unit)
    }

    /**
     * Selects appropriate data source based on transfer method.
     * Follows Strategy Pattern for method selection.
     */
    private fun selectDataSource(method: TransferMethod): NetworkDataSource? {
        return when (method) {
            TransferMethod.WifiDirect -> {
                if (wifiDirectDataSource.isAvailable() && wifiDirectDataSource.isEnabled()) {
                    wifiDirectDataSource
                } else null
            }
            TransferMethod.Bluetooth -> {
                if (bluetoothDataSource.isAvailable() && bluetoothDataSource.isEnabled()) {
                    bluetoothDataSource
                } else null
            }
            TransferMethod.Auto -> {
                // Prefer Wi-Fi Direct, fallback to Bluetooth
                when {
                    wifiDirectDataSource.isAvailable() && wifiDirectDataSource.isEnabled() -> wifiDirectDataSource
                    bluetoothDataSource.isAvailable() && bluetoothDataSource.isEnabled() -> bluetoothDataSource
                    else -> null
                }
            }
        }
    }
}