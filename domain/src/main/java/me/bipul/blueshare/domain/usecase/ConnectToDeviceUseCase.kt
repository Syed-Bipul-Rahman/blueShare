package me.bipul.blueshare.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.domain.repository.TransferRepository

/**
 * Use case for connecting to a device.
 * Encapsulates the business logic for device connection.
 *
 * @property repository The transfer repository
 */
class ConnectToDeviceUseCase(
    private val repository: TransferRepository
) {
    /**
     * Connects to a specific device.
     *
     * @param device The device to connect to
     * @return Flow of transfer states during connection
     */
    operator fun invoke(device: Device): Flow<FileTransferState> {
        return repository.connectToDevice(device)
    }
}