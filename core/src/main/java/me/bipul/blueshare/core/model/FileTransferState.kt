package me.bipul.blueshare.core.model

/**
 * Represents the state of a file transfer operation.
 * Sealed class for type-safe state management following SOLID principles.
 */
sealed class FileTransferState {
    /**
     * Initial state - no transfer in progress
     */
    data object Idle : FileTransferState()

    /**
     * Discovering available devices
     */
    data object Discovering : FileTransferState()

    /**
     * Devices discovered and available for connection
     * @property devices List of discovered devices
     */
    data class DevicesFound(val devices: List<Device>) : FileTransferState()

    /**
     * Connecting to a device
     * @property device The device being connected to
     */
    data class Connecting(val device: Device) : FileTransferState()

    /**
     * Successfully connected to device
     * @property device The connected device
     */
    data class Connected(val device: Device) : FileTransferState()

    /**
     * Transfer in progress
     * @property progress Progress percentage (0-100)
     * @property bytesTransferred Number of bytes transferred
     * @property totalBytes Total bytes to transfer
     * @property transferSpeed Transfer speed in bytes per second
     * @property remainingTime Estimated remaining time in milliseconds
     * @property currentFile Name of the file currently being transferred
     */
    data class Transferring(
        val progress: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val transferSpeed: Long,
        val remainingTime: Long,
        val currentFile: String
    ) : FileTransferState()

    /**
     * Transfer completed successfully
     * @property filesTransferred Number of files transferred
     * @property totalBytes Total bytes transferred
     * @property duration Duration of transfer in milliseconds
     */
    data class Completed(
        val filesTransferred: Int,
        val totalBytes: Long,
        val duration: Long
    ) : FileTransferState()

    /**
     * Transfer failed
     * @property error The error that occurred
     * @property canRetry Whether the transfer can be retried
     */
    data class Failed(
        val error: TransferError,
        val canRetry: Boolean = true
    ) : FileTransferState()

    /**
     * Transfer cancelled by user
     */
    data object Cancelled : FileTransferState()

    /**
     * Transfer paused
     * @property progress Current progress percentage
     */
    data class Paused(val progress: Int) : FileTransferState()
}