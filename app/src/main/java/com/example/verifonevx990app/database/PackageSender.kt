package com.example.verifonevx990app.database


import android.app.Dialog
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.example.verifonevx990app.customui.CustomToast
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.VFService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.ServerSocketChannel
import java.text.SimpleDateFormat
import java.util.*


//to send created package on server
class PackageSender(
    private val context: Context,
    private val data: String,
    private val serverResponce: ServerResponce,
    private var transactionType: Int
) {
    private val compositeDisposable = CompositeDisposable()
    private var dialog: Dialog? = null

    //  private var terminalCommunicationTable: TerminalCommunicationTable? = null
    private var totalRetry: Int? = 0
    private var isSecondary = false
    private var timeOut: Boolean = false
    fun sendServer() {
        CustomToast.printAppLog("plane text" + HexStringConverter.hexDump(data))
        if (checkInternetConnection())
            getTerminalComData()
        else
            noNetworkConnection()
    }

    private fun noNetworkConnection() {
        serverResponce.onNoInternet()

    }

    //send package data to server
    private fun sendToServer() {
        val disposable = doBackgroundTask().subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ stringBuilder ->
                //on sucess responce


                sendSucees(stringBuilder)
            }) { throwable ->
                //on error responce
                sendError(throwable)
            }

        compositeDisposable.add(disposable)


    }

    /////Observer back ground task
    private fun doBackgroundTask(): Observable<ByteArray> {
        return Observable.create { emitter ->
            val connection: Socket?

            try {

                ConnectionTimeStamps.reset()// resetting the time stamp before

                ConnectionTimeStamps.dialStart = getF48TypeTimeStamp()  //  Dialing started
                connection = openSocket()//8110//host 1//wifi ip 172.16.1.10
                ConnectionTimeStamps.dialConnected = getF48TypeTimeStamp()  //Dialing Connected

                /* if (transactionType == TransactionTypeValues.SETTLEMENT) {
                    AppPreference.saveString(AppPreference.ABATCH_KEY, "2", context)
                }*/

                if (connection != null && connection.isConnected && !timeOut) {

                    saveReversal()
                    ConnectionTimeStamps.startTransaction =
                        getF48TypeTimeStamp()  //  Transaction Started
                    val bytes = writeToAndReadFromSocket(connection, data)


                    ConnectionTimeStamps.recieveTransaction =
                        getF48TypeTimeStamp()  // Transaction Completed
                    try {
                        HexStringConverter.hexDump(bytes)
                        emitter.onNext(bytes)
                        emitter.onComplete()
                    } catch (e: NullPointerException) {
                        dismisDialog()
                        emitter.onError(e)
                    }
                } else if (connection != null && connection.isConnected && timeOut) {
                    onTimeOut()
                } else
                    connectionError()
            } catch (e: Exception) {
                e.printStackTrace()
                dismisDialog()

                if (!checkInternetConnection())
                    noNetworkConnection()
                else
                    connectionError()
            }
        }
    }


    private fun saveReversal() {

        serverResponce.saveReversal()
    }

    //to connect socket
    @Throws(Exception::class)
    private fun openSocket(): Socket? {
        timeOut = false
        var socket: Socket? = null
        // create a socket with a timeout
        try {
            showProgressShow("Please wait Connecting to Bonushub Server")
            //create a socket
            val serverSocketChannel = ServerSocketChannel.open()
            //serverSocketChannel.bind(socketAddress);
            serverSocketChannel.configureBlocking(false)
            socket = Socket()
            // this method will block no more than timeout ms.
            var conTimeOut = 30
            val timeoutInMs = conTimeOut * 1000   // 10 seconds
            //   val address = InetSocketAddress(InetAddress.getByName("122.176.84.29"),8112)
            val address =
                InetSocketAddress(InetAddress.getByName(VFService.NEW_IP_ADDRESS), VFService.PORT)
            Log.d("Connection Address:- ", address.toString())
            socket.connect(address, timeoutInMs)
            val revTimeOut = 30
            val revTimeOutMS = revTimeOut * 1000   //
            socket.soTimeout = revTimeOutMS
            dismisDialog()
            return socket
        } catch (ste: Exception) {
            ste.printStackTrace()
            dismisDialog()
            timeOut = true
            // throw ste;
            return socket
        }
    }

    private fun connectionError() {
        serverResponce.onConnectionFailed()
    }

    private fun dismisDialog() {
        GlobalScope.launch(Dispatchers.Main) {
            if (dialog != null) {
                dialog?.dismiss()
                //dialog = null
            }
        }
    }

    //writing and reading data on socket
    @Throws(Exception::class)
    private fun writeToAndReadFromSocket(socket: Socket?, writeTo: String): ByteArray {
        try {
            showProgressShow("Please wait Sending to Bonushub Server")
            val socketOutputStream = socket!!.getOutputStream()
            val b = writeTo.toByteArray(charset("ISO-8859-1"))
            CustomToast.printAppLog(HexStringConverter.hexDump(writeTo))
            socketOutputStream.write(b)
            socketOutputStream.flush()
            dismisDialog()
            showProgressShow("Please wait Receiving from Bonushub Server")
            val dis = DataInputStream(socket.getInputStream())
            val len = dis.readShort().toInt()
            //println("read data len:$len")
            val rs = ByteArray(len)
            dis.readFully(rs)
            //println("read data:" + HexStringConverter.hexDump(rs))
            dismisDialog()
            return rs
        } catch (e: IOException) {
            dismisDialog()
            e.printStackTrace()
            throw e
        } finally {
            socket?.close()
        }
    }

    //to terminate observer and send data to caller class
    private fun sendSucees(responce: ByteArray) {
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
            compositeDisposable.clear()
        }
        serverResponce.onSucees(responce)

    }

    //to terminate observer and send data to caller class
    private fun sendError(throwable: Throwable) {
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
            compositeDisposable.clear()
        }
        serverResponce.onError(throwable.toString())
    }


    private fun onTimeOut() {

        val rety = 2
        if (rety.times(2) != totalRetry) {
            isSecondary = !isSecondary
            sendToServer()
        } else {
            if (!compositeDisposable.isDisposed) {
                compositeDisposable.dispose()
                compositeDisposable.clear()
            }
            serverResponce.onResponseTimeOut()
        }

    }

    private fun showProgressShow(message: String) {
        GlobalScope.launch(Dispatchers.Main) {
            /*dialog = ProgressDialog.progressDialog(context as TransactionActivity, message)
        dialog?.show()*/
        }
    }

    private fun checkInternetConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    private fun getTerminalComData() {
        sendToServer()
    }
}


fun getF48TypeTimeStamp(): String {  // yyMMddhhmmss
    val currentTime = Calendar.getInstance().time
    val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
    return sdf.format(currentTime)
}


object ConnectionTimeStamps {
    var identifier: String = ""
    var dialStart = ""
    var dialConnected = ""
    var startTransaction = ""
    var recieveTransaction = ""


    fun getFormattedStamp(): String =
        "$identifier~$startTransaction~$recieveTransaction~$dialStart~$dialConnected"


    fun reset() {
        identifier = ""
        dialStart = ""
        dialConnected = ""
        startTransaction = ""
        recieveTransaction = ""
    }


    fun getEmptyStamp(): String = "~~~~"

}

