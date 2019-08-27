package br.com.tworemember.localizer

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val context: Context,
    private val devices: ArrayList<BluetoothDevice>,
    private val delegate: BluetoothDeviceDelegate
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(android.R.layout.simple_expandable_list_item_2, parent, false)
        return DeviceViewHolder(view, delegate)
    }

    override fun getItemCount(): Int { return devices.size }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }


    inner class DeviceViewHolder(itemView: View, private val delegate: BluetoothDeviceDelegate) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(device: BluetoothDevice) {
            val txtNome = itemView.findViewById<TextView>(android.R.id.text1)
            val txtAddress = itemView.findViewById<TextView>(android.R.id.text2)

            txtNome.text = device.name
            txtAddress.text = device.address

            itemView.setOnClickListener { delegate.connect(device) }
        }
    }
}