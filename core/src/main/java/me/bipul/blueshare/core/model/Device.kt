package me.bipul.blueshare.core.model

/**
 * Represents a peer device discovered for file transfer.
 * Immutable data class following best practices.
 *
 * @property id Unique identifier for the device
 * @property name Human-readable device name
 * @property address Device address (MAC address for Bluetooth, IP for Wi-Fi Direct)
 * @property type Transfer method supported by this device
 * @property isConnected Whether device is currently connected
 */
data class Device(
    val id: String,
    val name: String,
    val address: String,
    val type: TransferMethod,
    val isConnected: Boolean = false
)