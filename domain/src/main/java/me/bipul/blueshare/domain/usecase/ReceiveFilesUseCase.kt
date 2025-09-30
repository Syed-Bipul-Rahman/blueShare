package me.bipul.blueshare.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.domain.repository.TransferRepository

/**
 * Use case for receiving files from a connected device.
 * Encapsulates the business logic for file reception.
 *
 * @property repository The transfer repository
 */
class ReceiveFilesUseCase(
    private val repository: TransferRepository
) {
    /**
     * Starts receiving files from the connected device.
     *
     * @return Flow of transfer states during reception
     */
    operator fun invoke(): Flow<FileTransferState> {
        return repository.receiveFiles()
    }

    /**
     * Cancels the ongoing reception.
     */
    suspend fun cancel() {
        repository.cancelTransfer()
    }
}