package me.bipul.blueshare.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.bipul.blueshare.core.model.Device
import me.bipul.blueshare.core.model.TransferMethod
import me.bipul.blueshare.databinding.ItemDeviceBinding

/**
 * Adapter for displaying discovered devices in a RecyclerView.
 * Uses DiffUtil for efficient updates.
 */
class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onDeviceClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val onDeviceClick: (Device) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceAddress.text = device.address

            // Set device type text and icon
            when (device.type) {
                is TransferMethod.WifiDirect -> {
                    binding.tvDeviceType.text = "Wi-Fi Direct"
                    binding.ivDeviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth)
                }
                is TransferMethod.Bluetooth -> {
                    binding.tvDeviceType.text = "Bluetooth"
                    binding.ivDeviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth)
                }
                is TransferMethod.Auto -> {
                    binding.tvDeviceType.text = "Auto"
                    binding.ivDeviceIcon.setImageResource(android.R.drawable.stat_sys_data_bluetooth)
                }
            }

            // Show connected indicator
            binding.ivConnectedIcon.visibility = if (device.isConnected) View.VISIBLE else View.GONE

            // Set click listener
            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }

    private class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}