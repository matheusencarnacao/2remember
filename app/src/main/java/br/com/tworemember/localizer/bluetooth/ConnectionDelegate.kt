package br.com.tworemember.localizer.bluetooth

interface ConnectionDelegate {

    fun onConnected()

    fun onConnecting()

    fun onSendedInfo(message: String, connectedThread: ConnectedThread)

    fun onError(message: String)
}