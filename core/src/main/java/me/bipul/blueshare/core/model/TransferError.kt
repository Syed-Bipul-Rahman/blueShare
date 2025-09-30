package me.bipul.blueshare.core.model

/**
 * Represents errors that can occur during file transfer.
 * Sealed class for exhaustive error handling.
 */
sealed class TransferError {
    abstract val message: String
    abstract val throwable: Throwable?

    /**
     * Permission denied error
     */
    data class PermissionDenied(
        override val message: String = "Required permission not granted",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Device not found or discovery failed
     */
    data class DeviceNotFound(
        override val message: String = "No devices found",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Connection failed
     */
    data class ConnectionFailed(
        override val message: String = "Failed to connect to device",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Connection lost during transfer
     */
    data class ConnectionLost(
        override val message: String = "Connection lost during transfer",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * File I/O error
     */
    data class FileError(
        override val message: String = "File operation failed",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Transfer timeout
     */
    data class Timeout(
        override val message: String = "Transfer timed out",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Feature not supported by device
     */
    data class NotSupported(
        override val message: String = "Feature not supported",
        override val throwable: Throwable? = null
    ) : TransferError()

    /**
     * Unknown error
     */
    data class Unknown(
        override val message: String = "Unknown error occurred",
        override val throwable: Throwable? = null
    ) : TransferError()
}