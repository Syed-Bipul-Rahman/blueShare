package me.bipul.blueshare.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.domain.repository.TransferRepository

/**
 * Use case for discovering nearby devices.
 * Follows Single Responsibility Principle - handles only device discovery logic.
 *
 * @property repository The transfer repository
 */
class DiscoverDevicesUseCase(
    private val repository: TransferRepository
) {
    /**
     * Executes device discovery.
     *
     * @param method The transfer method to use (Auto, Bluetooth, or Wi-Fi Direct)
     * @return Flow of transfer states during discovery
     */
    operator fun invoke(method: TransferMethod = TransferMethod.Auto): Flow<FileTransferState> {
        return repository.discoverDevices(method)
    }

    /**
     * Stops device discovery.
     */
    suspend fun stop() {
        repository.stopDiscovery()
    }
}