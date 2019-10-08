package br.com.tworemember.localizer.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


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
        val buffer = ByteArray(1024)
        var bytes: Int?

        val stringBuilder = StringBuilder()

        // Keep looping to listen for received messages
        while (true) {
            try {
                bytes = inputStream?.read(buffer) //read bytes from input buffer
                bytes?.let {
                    val readMessage = String(buffer, 0, it)
                    stringBuilder.append(readMessage)
                    // Send the obtained bytes to the UI Activity via handler
                    delegate.onSendedInfo(stringBuilder.toString(), this)
                    stringBuilder.setLength(0)
                }
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