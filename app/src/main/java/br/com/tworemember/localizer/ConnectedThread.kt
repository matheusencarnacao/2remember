package br.com.tworemember.localizer

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class ConnectedThread(private val context: Activity,
                      private val socket: BluetoothSocket) : Thread() {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.inputStream
            tmpOut = socket.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            context.runOnUiThread {Toast.makeText(context, "Erro ao abrir o stream de Entrada e Saida", Toast.LENGTH_SHORT).show()}
        }

        inputStream = tmpIn
        outputStream = tmpOut
    }

    override fun run() {
        val buffer = ByteArray(1024)  // buffer store for the stream
        var bytes: Int? = null // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = inputStream?.read(buffer)
                // Send the obtained bytes to the UI activity
                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                print(bytes)
            } catch (e: IOException) {
                break
            }

        }
    }

    /* Call this from the main activity to send data to the remote device */
    fun write(bytes: ByteArray) {
        try {
            Log.d("Enviado", "enviando dados...")
            outputStream?.write(bytes)
            Log.d("Enviado", "dados enviados")

        } catch (e: IOException) {
            e.printStackTrace()
            context.runOnUiThread {Toast.makeText(context, "Erro ao enviar os dados para o dispositivo", Toast.LENGTH_SHORT).show()}
        }

    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
            context.runOnUiThread {Toast.makeText(context, "Erro ao fechar a conexão do socket", Toast.LENGTH_SHORT).show()}
        }

    }
}