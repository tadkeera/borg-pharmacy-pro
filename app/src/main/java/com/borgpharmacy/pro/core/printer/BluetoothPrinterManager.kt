package com.borgpharmacy.pro.core.printer
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
class BluetoothPrinterManager { suspend fun print(device:BluetoothDevice,bytes:ByteArray)=withContext(Dispatchers.IO){device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")).use{it.connect();it.outputStream.use{out->out.write(bytes);out.flush()}}}; fun bonded():Set<BluetoothDevice> = BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet() }
