package me.bipul.blueshare.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferFile
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.domain.usecase.DiscoverDevicesUseCase
import me.bipul.blueshare.domain.usecase.SendFilesUseCase

/**
 * ViewModel for file transfer screen.
 * Follows MVVM architecture with StateFlow for reactive UI updates.
 * Manages transfer state and coordinates use cases.
 *
 * @property discoverDevicesUseCase Use case for device discovery
 * @property sendFilesUseCase Use case for sending files
 */
class TransferViewModel(
    private val discoverDevicesUseCase: DiscoverDevicesUseCase,
    private val sendFilesUseCase: SendFilesUseCase
) : ViewModel() {

    private val _transferState = MutableStateFlow<FileTransferState>(FileTransferState.Idle)
    val transferState: StateFlow<FileTransferState> = _transferState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<List<TransferFile>>(emptyList())
    val selectedFiles: StateFlow<List<TransferFile>> = _selectedFiles.asStateFlow()

    private var selectedDevice: Device? = null

    /**
     * Starts device discovery with the specified method.
     */
    fun startDiscovery(method: TransferMethod = TransferMethod.Auto) {
        viewModelScope.launch {
            discoverDevicesUseCase(method).collect { state ->
                _transferState.value = state
            }
        }
    }

    /**
     * Stops device discovery.
     */
    fun stopDiscovery() {
        viewModelScope.launch {
            discoverDevicesUseCase.stop()
        }
    }

    /**
     * Selects a device for connection and file transfer.
     */
    fun selectDevice(device: Device) {
        selectedDevice = device
    }

    /**
     * Adds files to the transfer list.
     */
    fun addFiles(files: List<TransferFile>) {
        _selectedFiles.value = _selectedFiles.value + files
    }

    /**
     * Removes a file from the transfer list.
     */
    fun removeFile(file: TransferFile) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != file.uri }
    }

    /**
     * Clears all selected files.
     */
    fun clearFiles() {
        _selectedFiles.value = emptyList()
    }

    /**
     * Sends selected files to the selected device.
     */
    fun sendFiles() {
        val device = selectedDevice
        val files = _selectedFiles.value

        if (device == null) {
            // Handle error: no device selected
            return
        }

        if (files.isEmpty()) {
            // Handle error: no files selected
            return
        }

        viewModelScope.launch {
            sendFilesUseCase(device, files).collect { state ->
                _transferState.value = state

                // Clear files on successful completion
                if (state is FileTransferState.Completed) {
                    clearFiles()
                }
            }
        }
    }

    /**
     * Cancels the ongoing transfer.
     */
    fun cancelTransfer() {
        viewModelScope.launch {
            sendFilesUseCase.cancel()
            _transferState.value = FileTransferState.Cancelled
        }
    }

    /**
     * Pauses the ongoing transfer.
     */
    fun pauseTransfer() {
        viewModelScope.launch {
            sendFilesUseCase.pause()
        }
    }

    /**
     * Resumes a paused transfer.
     */
    fun resumeTransfer() {
        viewModelScope.launch {
            sendFilesUseCase.resume()
        }
    }
}