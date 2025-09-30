package me.bipul.blueshare.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferFile
import me.bipul.blueshare.domain.repository.TransferRepository

/**
 * Use case for sending files to a connected device.
 * Encapsulates the business logic for file transmission.
 *
 * @property repository The transfer repository
 */
class SendFilesUseCase(
    private val repository: TransferRepository
) {
    /**
     * Sends files to a device.
     * First connects to the device, then sends the files.
     *
     * @param device The target device
     * @param files List of files to send
     * @return Flow of transfer states during the operation
     */
    operator fun invoke(device: Device, files: List<TransferFile>): Flow<FileTransferState> {
        // Note: This is a simplified version. In a real implementation,
        // we would chain the connection flow with the send flow.
        return repository.sendFiles(files)
    }

    /**
     * Cancels the ongoing transfer.
     */
    suspend fun cancel() {
        repository.cancelTransfer()
    }

    /**
     * Pauses the ongoing transfer.
     */
    suspend fun pause() {
        repository.pauseTransfer()
    }

    /**
     * Resumes a paused transfer.
     */
    suspend fun resume() {
        repository.resumeTransfer()
    }
}