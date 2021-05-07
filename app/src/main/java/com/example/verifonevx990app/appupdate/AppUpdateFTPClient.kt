package com.example.verifonevx990app.appupdate

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.vxUtils.AppPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class AppUpdateFTPClient(
    private var ftpIPAddress: String,
    private var port: Int,
    private var userName: String,
    private var password: String,
    private var downloadAppFileName: String,
    var context: Context,
    var downloadFileSize: String,
    var appUpdateDownloadCB: (Boolean, Uri?) -> Unit
) {
    private var fileUri: Uri? = null
    private val appName = "BonusHub.apk"
    private val client = FTPClient()
    private var outputStream: OutputStream? = null
    private val downloadedFilePath = File(context.externalCacheDir, appName)
    private var fileToDownloadPath: String? = null
    private var ftpURL: String? = null
    private var ftpPort: Int? = 0
    private var ftpUserName: String? = null
    private var ftpPassword: String? = null

    init {
        startFTPAndDownloadFile()
    }

    //Below method is used to connect and download file from FTP Server:-
    private fun startFTPAndDownloadFile() {

        fileToDownloadPath = downloadAppFileName
        ftpURL = ftpIPAddress
        ftpPort = port
        ftpUserName = userName
        ftpPassword = password

        //Here we are saving FTP IP Address and PORT for future App update use:-
        AppPreference.saveString(PrefConstant.FTP_IP_ADDRESS.keyName.toString(), ftpIPAddress)
        AppPreference.setIntData(PrefConstant.FTP_IP_PORT.keyName.toString(), ftpPort ?: 0)
        AppPreference.saveString(PrefConstant.FTP_USER_NAME.keyName.toString(), ftpUserName)
        AppPreference.saveString(PrefConstant.FTP_PASSWORD.keyName.toString(), ftpPassword)
        AppPreference.saveString(PrefConstant.FTP_FILE_NAME.keyName.toString(), downloadAppFileName)
        AppPreference.saveString(PrefConstant.FTP_FILE_SIZE.keyName.toString(), downloadFileSize)

        //region=======================Creating Handler To be execute after 15 Minutes:-
        GlobalScope.launch(Dispatchers.Main) {
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("HandlerCalled", "SUCCESS")
                Log.d("FileSize", downloadedFilePath.length().toString())
                outputStream?.close()
                appUpdateDownloadCB(true, Uri.fromFile(downloadedFilePath))
            }, 900000)
        }
        //endregion

        downloadAppFromFTP()
    }

    //region============================Download App Update File From FTP Path:-
    private fun downloadAppFromFTP() {
        try {
            downloadedFilePath.createNewFile()
            client.connect(ftpURL, ftpPort ?: 0)
            client.login(ftpUserName, ftpPassword)
            client.enterLocalPassiveMode()
            client.autodetectUTF8 = true
            client.setFileType(FTP.BINARY_FILE_TYPE)
            outputStream = FileOutputStream(downloadedFilePath)
            client.bufferSize = 2024 * 2048
            if (client.isConnected) {
                client.retrieveFile("/$fileToDownloadPath", outputStream)
                fileUri = Uri.fromFile(downloadedFilePath)
                outputStream?.close()
                client.logout()
                client.disconnect()
                appUpdateDownloadCB(true, fileUri)
            }

        } catch (e: IOException) {
            e.printStackTrace()
            appUpdateDownloadCB(false, fileUri)
        } finally {
            try {
                if (client.isConnected) {
                    outputStream?.close()
                    client.logout()
                    client.disconnect()
                    appUpdateDownloadCB(false, fileUri)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                outputStream?.close()
                client.logout()
                client.disconnect()
                appUpdateDownloadCB(false, fileUri)
            }
        }
    }
    //endregion
}