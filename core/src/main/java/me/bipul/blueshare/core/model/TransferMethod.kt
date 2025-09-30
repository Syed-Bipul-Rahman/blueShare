package me.bipul.blueshare.core.model

/**
 * Represents the available methods for peer-to-peer file transfer.
 * Follows Open/Closed Principle - can be extended with new methods without modifying existing code.
 */
sealed class TransferMethod {
    /**
     * Wi-Fi Direct transfer - high-speed, preferred method
     */
    data object WifiDirect : TransferMethod()

    /**
     * Bluetooth transfer - fallback method for compatibility
     */
    data object Bluetooth : TransferMethod()

    /**
     * Auto-select best available method based on device capabilities
     */
    data object Auto : TransferMethod()
}