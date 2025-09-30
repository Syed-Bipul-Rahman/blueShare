package me.bipul.blueshare

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.FileTransferState
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.core.util.FileUtils
import me.bipul.blueshare.data.repository.TransferRepositoryImpl
import me.bipul.blueshare.databinding.ActivityTransferBinding
import me.bipul.blueshare.domain.usecase.DiscoverDevicesUseCase
import me.bipul.blueshare.domain.usecase.ReceiveFilesUseCase
import me.bipul.blueshare.domain.usecase.SendFilesUseCase
import me.bipul.blueshare.network.bluetooth.BluetoothManager
import me.bipul.blueshare.network.wifidirect.WifiDirectManager
import me.bipul.blueshare.ui.adapter.DeviceAdapter
import me.bipul.blueshare.ui.transfer.TransferViewModel

class TransferActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_SEND = "mode_send"
        const val MODE_RECEIVE = "mode_receive"
    }

    private lateinit var binding: ActivityTransferBinding
    private lateinit var viewModel: TransferViewModel
    private lateinit var deviceAdapter: DeviceAdapter
    private var isSendMode = true

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()

            result.data?.clipData?.let { clipData ->
                // Multiple files selected
                for (i in 0 until clipData.itemCount) {
                    clipData.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            } ?: result.data?.data?.let { uri ->
                // Single file selected
                uris.add(uri)
            }

            if (uris.isNotEmpty()) {
                val files = uris.mapNotNull { uri ->
                    FileUtils.getFileFromUri(this, uri)
                }
                viewModel.addFiles(files)
                updateFileCount(files.size)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isSendMode = intent.getStringExtra(EXTRA_MODE) == MODE_SEND

        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeTransferState()

        if (isSendMode) {
            setupSendMode()
        } else {
            setupReceiveMode()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = if (isSendMode) "Send Files" else "Receive Files"
    }

    private fun setupViewModel() {
        // Initialize dependencies
        val wifiManager = WifiDirectManager(applicationContext)
        val bluetoothManager = BluetoothManager(applicationContext)
        val repository = TransferRepositoryImpl(wifiManager, bluetoothManager)

        val discoverUseCase = DiscoverDevicesUseCase(repository)
        val connectUseCase = me.bipul.blueshare.domain.usecase.ConnectToDeviceUseCase(repository)
        val sendUseCase = SendFilesUseCase(repository)
        val receiveUseCase = ReceiveFilesUseCase(repository)

        viewModel = TransferViewModel(discoverUseCase, connectUseCase, sendUseCase, receiveUseCase)
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            onDeviceSelected(device)
        }
        binding.recyclerDevices.apply {
            layoutManager = LinearLayoutManager(this@TransferActivity)
            adapter = deviceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectFiles.setOnClickListener {
            openFilePicker()
        }

        binding.btnStartTransfer.setOnClickListener {
            if (isSendMode) {
                viewModel.sendFiles()
            }
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancelTransfer()
        }
    }

    private fun setupSendMode() {
        binding.btnSelectFiles.visibility = View.VISIBLE
        binding.tvInstruction.text = "Select files and choose a device to send"

        // Start device discovery
        startDiscovery()
    }

    private fun setupReceiveMode() {
        binding.btnSelectFiles.visibility = View.GONE
        binding.tvInstruction.text = "Waiting for sender to connect...\nDiscovering nearby devices..."

        // Start device discovery to make this device visible
        startDiscovery()

        // Also start listening for incoming connections
        lifecycleScope.launch {
            viewModel.receiveFiles()
        }
    }

    private fun startDiscovery() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Discovering devices..."

        // Determine which method to use
        val method = TransferMethod.Auto
        viewModel.startDiscovery(method)
    }

    private fun observeTransferState() {
        lifecycleScope.launch {
            viewModel.transferState.collect { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: FileTransferState) {
        when (state) {
            is FileTransferState.Idle -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Ready"
            }

            is FileTransferState.Discovering -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStatus.text = "Discovering devices..."
                binding.recyclerDevices.visibility = View.VISIBLE
            }

            is FileTransferState.DevicesFound -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Found ${state.devices.size} device(s)"
                deviceAdapter.submitList(state.devices)

                if (state.devices.isEmpty()) {
                    binding.tvStatus.text = "No devices found. Make sure the other device is also in ${if (isSendMode) "receive" else "send"} mode."
                }
            }

            is FileTransferState.Connecting -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.tvStatus.text = "Connecting to ${state.device.name}..."
            }

            is FileTransferState.Connected -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Connected to ${state.device.name}"

                if (isSendMode) {
                    binding.btnStartTransfer.visibility = View.VISIBLE
                }
            }

            is FileTransferState.Transferring -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.isIndeterminate = false
                binding.progressBar.progress = state.progress

                val speed = FileUtils.formatSpeed(state.transferSpeed)
                val transferred = FileUtils.formatFileSize(state.bytesTransferred)
                val total = FileUtils.formatFileSize(state.totalBytes)
                val remaining = FileUtils.formatDuration(state.remainingTime)

                binding.tvStatus.text = "Transferring ${state.currentFile}\n" +
                        "$transferred / $total ($speed)\n" +
                        "Remaining: $remaining"

                binding.btnCancel.visibility = View.VISIBLE
                binding.btnStartTransfer.visibility = View.GONE
            }

            is FileTransferState.Completed -> {
                binding.progressBar.visibility = View.GONE
                binding.btnCancel.visibility = View.GONE

                val size = FileUtils.formatFileSize(state.totalBytes)
                val duration = FileUtils.formatDuration(state.duration)

                MaterialAlertDialogBuilder(this)
                    .setTitle("Transfer Complete!")
                    .setMessage("Successfully transferred ${state.filesTransferred} file(s)\n" +
                            "Size: $size\n" +
                            "Time: $duration")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .show()
            }

            is FileTransferState.Failed -> {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Transfer failed: ${state.error.message}"

                MaterialAlertDialogBuilder(this)
                    .setTitle("Transfer Failed")
                    .setMessage(state.error.message)
                    .setPositiveButton("Retry") { _, _ ->
                        if (isSendMode) {
                            startDiscovery()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        finish()
                    }
                    .show()
            }

            is FileTransferState.Cancelled -> {
                binding.tvStatus.text = "Transfer cancelled"
                Snackbar.make(binding.root, "Transfer cancelled", Snackbar.LENGTH_SHORT).show()
                finish()
            }

            is FileTransferState.Paused -> {
                binding.tvStatus.text = "Transfer paused"
            }
        }
    }

    private fun onDeviceSelected(device: Device) {
        viewModel.selectDevice(device)

        MaterialAlertDialogBuilder(this)
            .setTitle("Connect to Device")
            .setMessage("Connect to ${device.name}?")
            .setPositiveButton("Connect") { _, _ ->
                viewModel.connectToDevice(device)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun updateFileCount(count: Int) {
        binding.tvFileCount.visibility = View.VISIBLE
        binding.tvFileCount.text = "$count file(s) selected"
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopDiscovery()
    }
}