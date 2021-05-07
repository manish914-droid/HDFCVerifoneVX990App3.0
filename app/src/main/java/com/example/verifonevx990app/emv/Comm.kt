package com.example.verifonevx990app.emv

import android.util.Log
import com.example.verifonevx990app.vxUtils.ROCProviderV2.byte2HexStr
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class Comm(hostIP: String?, hostPort: Int) {

    private val TAG = "EMVDemo-Comm"
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var status = 0
    var ip: String? = null
    var port = 0

    fun Comm() {
        status = 0
        ip = ""
        port = 0
        outputStream = null
        inputStream = null
    }

    fun Comm(ip: String?, port: Int) {
        status = 0
        this.ip = ip
        this.port = port
        outputStream = null
        inputStream = null
    }


    fun connect(ip: String?, port: Int): Boolean {
        this.ip = ip
        this.port = port
        return connect()
    }

    fun connect(): Boolean {
        if (status > 0) {
            if (this.ip === ip && this.port == port) {
                return true
            } else {
                disconnect()
            }
        }
        try {
            socket = Socket(ip, port)
            if (null == socket) {
                return false
            }
            this.ip = ip
            this.port = port
            status = 1
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }


    fun send(data: ByteArray): Int {
        if (status <= 0) {
            return 0
        }
        Log.d(TAG, "SEND:")
        Log.d(TAG, byte2HexStr(data) ?: "")
        try {
            outputStream = socket!!.getOutputStream()
            if (null == outputStream) {
                return 0
            }
            outputStream?.write(data)
            outputStream?.flush()
            return data.size
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }


    fun receive(wantLength: Int, timeoutSecond: Int): ByteArray? {
        if (status <= 0) {
            return null
        }
        try {
            socket!!.soTimeout = timeoutSecond * 1000
            inputStream = socket!!.getInputStream()
            if (null == inputStream) {
                return null
            }
            val tmp = ByteArray(wantLength)
            val recvLen = inputStream!!.read(tmp)
            if (recvLen > 0) {
                val ret = ByteArray(recvLen)
                System.arraycopy(tmp, 0, ret, 0, recvLen)
                return ret
            } else if (recvLen == 0) {
                return null
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun disconnect() {
        status = 0
        try {
            if (null != inputStream) {
                inputStream!!.close()
                inputStream = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            if (null != outputStream) {
                outputStream!!.close()
                outputStream = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            if (null != socket) {
                socket!!.close()
                socket = null
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        status = 0
    }
}