package br.com.tworemember.localizer.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.Toast
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset


class ConnectedThread(private val context: Activity,
                      private val socket: BluetoothSocket,
                      private val delegate: ConnectionDelegate) : Thread() {

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

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                // Send the obtained bytes to the UI activity
                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                val message = IOUtils.toString(inputStream, Charset.defaultCharset())
                context.runOnUiThread { delegate.onSendedInfo(message) }
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