package br.com.tworemember.localizer.bluetooth

import android.bluetooth.BluetoothDevice

interface BluetoothDeviceDelegate {

    fun connect(device: BluetoothDevice)
}