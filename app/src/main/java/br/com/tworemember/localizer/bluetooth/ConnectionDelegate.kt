package br.com.tworemember.localizer.bluetooth

interface ConnectionDelegate {

    fun onConnected()

    fun onConnecting()

    fun onError(message: String)
}