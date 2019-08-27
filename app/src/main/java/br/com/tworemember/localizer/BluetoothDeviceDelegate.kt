package br.com.tworemember.localizer

import android.bluetooth.BluetoothDevice

interface BluetoothDeviceDelegate {

    fun connect(device: BluetoothDevice)
}