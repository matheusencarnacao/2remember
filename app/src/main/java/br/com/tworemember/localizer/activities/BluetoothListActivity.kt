package br.com.tworemember.localizer.activities

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.adapter.DeviceAdapter
import br.com.tworemember.localizer.bluetooth.BluetoothDeviceDelegate
import br.com.tworemember.localizer.bluetooth.ConnectThread
import br.com.tworemember.localizer.bluetooth.ConnectionDelegate
import br.com.tworemember.localizer.providers.ProgressDialogProvider
import kotlinx.android.synthetic.main.activity_bluetooth_list.*


class BluetoothListActivity : AppCompatActivity(),
    BluetoothDeviceDelegate {

    private var btnAdapter: BluetoothAdapter? = null
    private val request_enable_bt = 817
    private var devices = ArrayList<BluetoothDevice>()
    private var dialog: ProgressDialog? = null
    private var connectThread: ConnectThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_list)
        btnAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btnAdapter == null) {
            Toast.makeText(
                this,
                "Infelizmente o recurso de bluetooth está indisponível neste aparelho",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        verifyBluetoothEnabled()

        rv_devices.layoutManager = LinearLayoutManager(this)
        rv_devices.adapter = DeviceAdapter(this, devices, this)

    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDiscovery()
        unregisterReceiver(receiver)
        connectThread?.cancel()
    }

    private fun verifyBluetoothEnabled() {
        if (btnAdapter!!.isEnabled()) {
            startDiscovery()
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, request_enable_bt)
    }

    private fun startDiscovery() {
        btnAdapter!!.startDiscovery()
        // Register the BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter) // Don't forget to unregister during onDestroy
    }

    private fun cancelDiscovery() {
        btnAdapter?.let { if (it.isDiscovering) it.cancelDiscovery() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == request_enable_bt) {
            if (resultCode == Activity.RESULT_OK)
                startDiscovery()
            else {
                Toast.makeText(
                    this,
                    "O bluetooth precisa estar habilitado para conectar a outros dispositivos",
                    Toast.LENGTH_SHORT
                ).show()
                verifyBluetoothEnabled()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val action = intent.action
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND == action) {
                    // Get the BluetoothDevice object from the Intent
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    // Add the name and address to an array adapter to show in a ListView
                    if (!verifyIfExists(device)) {
                        devices.add(device)
                        rv_devices.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun verifyIfExists(device: BluetoothDevice): Boolean {
        return devices.any { it.address == device.address }
    }

    override fun connect(device: BluetoothDevice) {
        cancelDiscovery()
        connectThread = ConnectThread(this, device, delegate)
        connectThread?.start()
    }

    private val delegate = object : ConnectionDelegate {

        override fun onError(message: String) {
            dialog?.dismiss()
            Toast.makeText(this@BluetoothListActivity, message, Toast.LENGTH_SHORT).show()
        }

        override fun onConnected() {
            dialog?.dismiss()
            Toast.makeText(this@BluetoothListActivity, "Conectado com sucesso!", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onConnecting() {
            dialog = ProgressDialogProvider.showProgressDialog(
                this@BluetoothListActivity,
                "Conectando ao dispositivo..."
            )
        }
    }
}