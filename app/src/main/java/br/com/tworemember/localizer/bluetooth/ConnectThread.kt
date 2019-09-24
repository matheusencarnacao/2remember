package br.com.tworemember.localizer.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.Toast
import br.com.tworemember.localizer.model.ConfiguracaoRaio
import br.com.tworemember.localizer.providers.Preferences
import com.google.gson.Gson
import java.io.IOException
import java.util.*


class ConnectThread(
    private val context: Activity,
    device: BluetoothDevice,
    private val delegate: ConnectionDelegate
) : Thread() {

    private var bluetoothSocker: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    init {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        var tmp: BluetoothSocket? = null

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        bluetoothSocker = tmp
    }

    override fun run() {

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("Conectando", "conectando ao dispositivo")
            context.runOnUiThread { delegate.onConnecting() }
            bluetoothSocker?.connect()
            Log.d("Conectado", "Conectado com sucesso")
        } catch (connectException: IOException) {
            context.runOnUiThread { delegate.onError("Erro ao conectcar com o dispositivo") }

            connectException.printStackTrace()
            // Unable to connect; close the socket and get out
            try {
                bluetoothSocker?.close()
            } catch (closeException: IOException) {
                context.runOnUiThread { delegate.onError("Erro ao fechar conexão com socket") }
                closeException.printStackTrace()
            }

            return
        }

        // Do work to manage the connection (in a separate thread)
        manageConnectedSocket(bluetoothSocker!!)
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        context.runOnUiThread { delegate.onConnected() }
        val connectedThread =
            ConnectedThread(context, socket)
        //TODO: pegar das preferencias

        val prefs = Preferences(context)

        val safePosition = prefs.getSafePosition()
        safePosition?.let {
            val conf = ConfiguracaoRaio(it.lat, it.lng, prefs.getRaio())
            val gson = Gson()
            val bytes = gson.toJson(conf).toByteArray()
            connectedThread.write(bytes)
        }
    }


    /** Will cancel an in-progress connection, and close the socket */
    fun cancel() {
        try {
            bluetoothSocker?.close()
        } catch (e: IOException) {
            e.printStackTrace()
            context.runOnUiThread { Toast.makeText(context, "Erro ao fechar o socket", Toast.LENGTH_SHORT).show() }
        }
    }

}