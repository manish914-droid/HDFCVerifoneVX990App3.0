package com.example.verifonevx990app.vxUtils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.example.verifonevx990app.R
import com.example.verifonevx990app.digiPOS.*
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.PrefConstant
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*

interface IKeyExchangeInit {
    suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean): IWriter
}

interface IKeyExchange : IKeyExchangeInit {
    suspend fun createKeyExchangeIso(): IWriter
}

interface IVoidExchange {
    fun createVoidISOPacket(): IWriter
}

interface ITransactionPacketExchange {
    fun createTransactionPacket(): IWriter
}

interface IVoidRefundTransactionPacketExchange {
    fun createVoidRefundTransactionPacket(): IWriter
}

interface IOfflineSalePacketExchange {
    fun createOfflineSalePacket(): IWriter
}

interface IVoidOfflineSalePacketExchange {
    fun createVoidOfflineSalePacket(): IWriter
}

interface ISettlementPacketExchange {
    fun createSettlementISOPacket(): IWriter
}

interface IAppUpdatePacketExchange {
    fun createAppUpdatePacket(): IWriter
}

interface IAppUpdateConfirmationPacketExchange {
    fun createAppUpdateConfirmationPacket(): IWriter
}

interface ICrossSellPacketExchange {
    fun createCrossSellPacket(): IWriter
}

/**
 * KCV matching part is remaining, KCV extraction is done [18-07-2019]
 * */
typealias ApiCallback = (String, Boolean, Boolean) -> Unit

class KeyExchanger(
    private var context: Context,
    private val tid: String,
    private val callback: ApiCallback
) : IKeyExchange {

    var keWithInit = true
    var isHdfc = false

    companion object {
        private val TAG = KeyExchanger::class.java.simpleName

        fun getF61(): String {
            //region=========adding field 61=============
            val appName =
                addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false)

            //val deviceModel = VerifoneApp.getDeviceModel()
            //Getting Device Modal from VF Service AIDL:-
            val deviceModel = addPad(AppPreference.getString("deviceModel"), "*", 6, false)
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val connectionType = ConnectionType.GPRS.code
            val pccNo = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            val pcNo2 = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
            return "$connectionType$deviceModel$appName$version$pccNo$pcNo2"
        }

    }

    private var tmk = ""
    private var tmkKcv: ByteArray = byteArrayOf()
    private lateinit var rsa: Map<String, Any>

    private fun backToCalled(
        msg: String,
        success: Boolean,
        isProgressType: Boolean,
        isDismiss: Boolean = false
    ) {
        logger(TAG, "msg = $msg, success = $success, isProgressType = $isProgressType")
        GlobalScope.launch(Dispatchers.Main) {
            callback(msg, success, isProgressType)
        }
    }

    fun startExchange( isfreshInit:Boolean=true) {
        GlobalScope.launch {
            val isoW = createKeyExchangeIso()
            val bData = isoW.generateIsoByteRequest()
            HitServer.hitServer(bData, { result, success ->
                if (success) {
                    launch {
                        val iso = readIso(result)
                        logger(TAG, iso.isoMap)
                        val resp = iso.isoMap[39]
                        val f11 = iso.isoMap[11]

                        val f48 = iso.isoMap[48]

                        if (f48 != null) ConnectionTimeStamps.saveStamp(f48.parseRaw2String())

                        if (f11 != null) ROCProviderV2.incrementFromResponse(
                            f11.rawData,
                            AppPreference.HDFC_BANK_CODE
                        ) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)

                        if (resp != null && resp.rawData.hexStr2ByteArr().byteArr2Str() == "00") {
                            Log.d("Success:- ", "Logon Success")
                            if (tmk.isEmpty()) {
                                tmk = iso.isoMap[59]?.rawData
                                    ?: "" // tmk len should be 256 byte or 512 hex char
                                logger(TAG, "RAW TMK = $tmk")
                                if (tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                } else if (tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                    AppPreference.saveString("TMK", tmk)
                                }
                                startExchange()
                            } else {
                                val ppkDpk = iso.isoMap[59]?.rawData ?: ""
                                logger(TAG, "RAW PPKDPK = $ppkDpk")
                                if (ppkDpk.length == 64 || ppkDpk.length == 76) { // if ppkDpk.length is 76 it mean last 6 bytes belongs to KCV of dpk and ppk

                                    var ppkKcv = byteArrayOf()
                                    var dpkKcv = byteArrayOf()
                                    val dpk = ppkDpk.substring(0, 32)
                                    val ppk = ppkDpk.substring(32, 64)
                                    AppPreference.saveString("dpk", dpk)
                                    AppPreference.saveString("ppk", ppk)

                                    if (ppkDpk.length == 76) ppkDpk.substring(32) else {
                                        ppkKcv = ppkDpk.substring(64, 70).hexStr2ByteArr()
                                        dpkKcv = ppkDpk.substring(70).hexStr2ByteArr()
                                    }

                                    //  ROCProviderV2.resetRoc(AppPreference.HDFC_BANK_CODE)
                                    //   ROCProviderV2.resetRoc(AppPreference.AMEX_BANK_CODE)

                                    insertSecurityKeys(
                                        ppk.hexStr2ByteArr(),
                                        dpk.hexStr2ByteArr(), ppkKcv, dpkKcv
                                    ) {
                                        if (it) {
                                            launch {
                                                AppPreference.saveLogin(true)
                                            }
                                            if (keWithInit) {
                                                startInit()
                                            } else {
                                                backToCalled(
                                                    "Key Exchange Successful",
                                                    success,
                                                    false
                                                )
                                            }
                                        } else {
                                            AppPreference.saveBoolean(
                                                PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString(),
                                                true
                                            )
                                            launch { AppPreference.saveLogin(false) }
                                            backToCalled("Error in key insertion", false, false)
                                        }
                                    }
                                } else backToCalled("Key exchange error", false, false)
                            }
                        } else {
                            AppPreference.saveBoolean(
                                PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString(),
                                true
                            )
                            val msg = iso.isoMap[58]?.parseRaw2String() ?: ""
                            backToCalled(msg, false, false)
                        }
                    }
                } else {
                    AppPreference.saveBoolean(
                        PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString(),
                        true
                    )
                    backToCalled(result, false, false)
                }

            }, {
                backToCalled(it, false, true)
            })

        }
    }

    //region===============Below method is used to Download TMK and also insert PPK and DPK on Press of Download TMK from Bank Functions:-
    fun downloadTMKForHDFC() {
        GlobalScope.launch {
            val isoW = createKeyExchangeIso()
            val bData = isoW.generateIsoByteRequest()
            HitServer.hitServer(bData, { result, success ->
                if (success && !TextUtils.isEmpty(result)) {
                    launch {
                        val iso = readIso(result)
                        logger(TAG, iso.isoMap)
                        val resp = iso.isoMap[39]
                        val f11 = iso.isoMap[11]

                        val f48 = iso.isoMap[48]

                        if (f48 != null) ConnectionTimeStamps.saveStamp(f48.parseRaw2String())
                        if (f11 != null) ROCProviderV2.incrementFromResponse(
                            f11.rawData,
                            AppPreference.HDFC_BANK_CODE
                        ) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)
                        if (resp != null && resp.rawData.hexStr2ByteArr().byteArr2Str() == "00") {
                            Log.d("Success:- ", "Logon Success")
                            if (tmk.isEmpty()) {
                                tmk = iso.isoMap[59]?.rawData
                                    ?: "" // tmk len should be 256 byte or 512 hex char
                                logger(TAG, "RAW TMK = $tmk")
                                if (tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                } else if (tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                                    tmkKcv = tmk.substring(512, 518).hexStr2ByteArr()
                                    tmk = tmk.substring(0, 512)
                                    AppPreference.saveString("TMK", tmk)
                                }
                                downloadTMKForHDFC()
                            } else {
                                val ppkDpk = iso.isoMap[59]?.rawData ?: ""
                                logger(TAG, "RAW PPKDPK = $ppkDpk")
                                if (ppkDpk.length == 64 || ppkDpk.length == 76) { // if ppkDpk.length is 76 it mean last 6 bytes belongs to KCV of dpk and ppk

                                    var ppkKcv = byteArrayOf()
                                    var dpkKcv = byteArrayOf()
                                    val dpk = ppkDpk.substring(0, 32)
                                    val ppk = ppkDpk.substring(32, 64)
                                    AppPreference.saveString("dpk", dpk)
                                    AppPreference.saveString("ppk", ppk)

                                    if (ppkDpk.length == 76) ppkDpk.substring(32) else {
                                        ppkKcv = ppkDpk.substring(64, 70).hexStr2ByteArr()
                                        dpkKcv = ppkDpk.substring(70).hexStr2ByteArr()
                                    }

                                    insertAfterSettlementSecurityKeys(
                                        ppk.hexStr2ByteArr(),
                                        dpk.hexStr2ByteArr(),
                                        ppkKcv,
                                        onTMKCall = true,
                                        dpkKcv = dpkKcv
                                    ) {
                                        if (it) {
                                            launch {
                                                AppPreference.saveLogin(true)
                                            }
                                            AppPreference.saveBoolean(
                                                PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                                false
                                            )
                                            (context as MainActivity).hideProgress()
                                            GlobalScope.launch(Dispatchers.Main) {
                                                (context as MainActivity).alertBoxWithAction(
                                                    null, null,
                                                    context.getString(R.string.logon),
                                                    context.getString(R.string.logon_successfull),
                                                    false,
                                                    "",
                                                    {},
                                                    {})
                                            }
                                            //backToCalled("Logon successful", it, false)
                                        } else {
                                            launch { AppPreference.saveLogin(false) }
                                            AppPreference.saveBoolean(
                                                PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                                true
                                            )
                                            backToCalled("Error in key insertion", false, false)
                                        }
                                    }
                                } else backToCalled("Key exchange error", false, false)
                            }
                        } else {
                            AppPreference.saveBoolean(
                                PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                true
                            )
                            val msg = iso.isoMap[58]?.parseRaw2String() ?: ""
                            backToCalled(msg, false, false)
                        }
                    }
                } else {
                    AppPreference.saveBoolean(PrefConstant.TMK_DOWNLOAD.keyName.toString(), true)
                    if (!TextUtils.isEmpty(result))
                        backToCalled(result, false, false)
                    else
                        backToCalled("No Response Error", false, false)
                }

            }, {
                backToCalled(it, false, true)
            })

        }
    }
    //endregion

    fun insertPPKDPKAfterSettlement() {
        tmk = "TestData" //This is for Scenerio When we only want to do PPK & DPK Insertion:-
        GlobalScope.launch {
            val isoW = createKeyExchangeIso()
            val bData = isoW.generateIsoByteRequest()
            HitServer.hitServer(bData, { result, success ->
                if (success) {
                    launch {
                        val iso = readIso(result)
                        logger(TAG, iso.isoMap)
                        val resp = iso.isoMap[39]
                        val f11 = iso.isoMap[11]

                        val f48 = iso.isoMap[48]

                        if (f48 != null) ConnectionTimeStamps.saveStamp(f48.parseRaw2String())

                        if (f11 != null) ROCProviderV2.incrementFromResponse(
                            f11.rawData,
                            AppPreference.HDFC_BANK_CODE
                        ) else ROCProviderV2.increment(AppPreference.HDFC_BANK_CODE)

                        if (resp != null && resp.rawData.hexStr2ByteArr().byteArr2Str() == "00") {
                            Log.d("Success:- ", "Logon Success")
                            /* if (tmk.isEmpty()) {
                                 tmk = iso.isoMap[59]?.rawData ?: "" // tmk len should be 256 byte or 512 hex char
                                 logger(TAG, "RAW TMK = $tmk")
                                 if(tmk.length == 518) {  // if tmk len is 259 byte , it means last 3 bytes are tmk KCV
                                     tmkKcv = tmk.substring(512,518).hexStr2ByteArr()
                                     tmk = tmk.substring(0,512)
                                 }else if(tmk.length == 524) { // if tmk len is 262 byte, it means last 6 bytes are tmk KCV and tmk wallet KCV
                                     tmkKcv = tmk.substring(512,518).hexStr2ByteArr()
                                     tmk = tmk.substring(0,512)
                                     AppPreference.saveString("TMK" , tmk)
                                 }
                                 startExchange()
                             } else {*/
                            val ppkDpk = iso.isoMap[59]?.rawData ?: ""
                            logger(TAG, "RAW PPKDPK = $ppkDpk")
                            if (ppkDpk.length == 64 || ppkDpk.length == 76) { // if ppkDpk.length is 76 it mean last 6 bytes belongs to KCV of dpk and ppk

                                var ppkKcv = byteArrayOf()
                                var dpkKcv = byteArrayOf()
                                val dpk = ppkDpk.substring(0, 32)
                                val ppk = ppkDpk.substring(32, 64)
                                AppPreference.saveString("dpk", dpk)
                                AppPreference.saveString("ppk", ppk)

                                if (ppkDpk.length == 76) ppkDpk.substring(32) else {
                                    ppkKcv = ppkDpk.substring(64, 70).hexStr2ByteArr()
                                    dpkKcv = ppkDpk.substring(70).hexStr2ByteArr()
                                }

                                //   ROCProviderV2.resetRoc(AppPreference.HDFC_BANK_CODE)
                                //  ROCProviderV2.resetRoc(AppPreference.AMEX_BANK_CODE)

                                insertAfterSettlementSecurityKeys(
                                    ppk.hexStr2ByteArr(),
                                    dpk.hexStr2ByteArr(),
                                    ppkKcv,
                                    dpkKcv,
                                    onTMKCall = false
                                ) {
                                    if (it) {
                                        launch {
                                            AppPreference.saveLogin(true)
                                        }
                                        AppPreference.saveBoolean(
                                            PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                            false
                                        )
                                        (context as MainActivity).hideProgress()
                                        GlobalScope.launch(Dispatchers.Main) {
                                            (context as MainActivity).alertBoxWithAction(null,
                                                null, "",
                                                context.getString(R.string.logon_successfull),
                                                false,
                                                "",
                                                {},
                                                {})
                                        }
                                        //backToCalled("Logon successfull", it, false)
                                    } else {
                                        launch { AppPreference.saveLogin(false) }
                                        AppPreference.saveBoolean(
                                            PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                            true
                                        )
                                        backToCalled("Error in key insertion", false, false)
                                    }
                                }
                            } else backToCalled("Key exchange error", false, false)
                            //}
                        } else {
                            AppPreference.saveBoolean(
                                PrefConstant.INSERT_PPK_DPK.keyName.toString(),
                                true
                            )
                            val msg = iso.isoMap[58]?.parseRaw2String() ?: ""
                            backToCalled(msg, false, false)
                        }
                    }
                } else {
                    AppPreference.saveBoolean(PrefConstant.INSERT_PPK_DPK.keyName.toString(), true)
                    backToCalled(result, false, false)
                }

            }, {
                backToCalled(it, false, true)
            })

        }
    }

    override suspend fun createKeyExchangeIso(): IWriter = IsoDataWriter().apply {
        mti = Mti.MTI_LOGON.mti
        // adding processing code and field 59 for public and private key
        addField(
            3, if (tmk.isEmpty()) {
                //resume after
                //  ROCProviderV2.resetRoc(AppPreference.getBankCode())
                rsa = RSAProvider.generateKeyPair()
                val publicKey = RSAProvider.getPublicKeyBytes(rsa)
                val f59 = insertBitsInPublicKey(
                    publicKey.substring(44).hexStr2ByteArr().byteArr2Str()
                )
                addFieldByHex(59, f59)
                ProcessingCode.KEY_EXCHANGE.code
            } else ProcessingCode.KEY_EXCHANGE_RESPONSE.code
        )

        //adding stan (padding of stan is internally handled by iso)
        addField(11, ROCProviderV2.getRoc(AppPreference.AMEX_BANK_CODE).toString())

        //adding nii
        addField(24, getNII())

        //adding tid
        addFieldByHex(41, tid)

        //adding field 48
        addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

        //region=========adding field 61=============
        //adding Field 61
        val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
        val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
        val data = ConnectionType.GPRS.code + addPad(
            AppPreference.getString("deviceModel"), " ", 6, false
        ) +
                addPad(VerifoneApp.appContext.getString(R.string.app_name), " ", 10, false) +
                version + addPad("0", "0", 9) + pcNumber
        var f61 = data

        //append 1 in case of hdfc bank
        if (isHdfc) {
            f61 += "1"
        }

        addFieldByHex(61, f61)
        //endregion

        //region=====adding field 63============
        //  val bankCode: String = "01"//

        val bankCode = AppPreference.getBankCode()

        //Serial Number from VF Service AIDL:-
        val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
        val f63 = "$deviceSerial$bankCode"
        addFieldByHex(63, f63)
        //endregion
    }

    private fun insertBitsInPublicKey(privatePublicDatum: String): String {
        val stringBuilder = StringBuilder(privatePublicDatum.length + 7)
        var i = 0
        while (i < privatePublicDatum.length) {
            when (i) {
                0 -> {
                    stringBuilder.append('K')//1 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                9 -> {
                    stringBuilder.append('y')//11 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                18 -> {
                    stringBuilder.append('@')//21 char
                    stringBuilder.append(privatePublicDatum[i])
                }
                42 -> {
                    stringBuilder.append('s')//46 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                70 -> {
                    stringBuilder.append('h')//75 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                93 -> {
                    stringBuilder.append('D')//99 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                137 -> {
                    stringBuilder.append('B')//144 th char
                    stringBuilder.append(privatePublicDatum[i])
                }
                else -> stringBuilder.append(privatePublicDatum[i])
            }
            i++
        }
        return stringBuilder.toString()
    }

    private fun startInit() {
        GlobalScope.launch(Dispatchers.IO) {

            HitServer.hitInitServer({ result, success ->
                if (success) {
                    //setAutoSettlement()  // Setting auto settlement.
                    //  downloadPromo()  // Setting
                    // region ========= checking and getting the merchant promo on terminal====
                    runBlocking(Dispatchers.IO) {
                        val tpt = TerminalParameterTable.selectFromSchemeTable()
                        if (tpt != null) {
                            getPromotionData(
                                "000000000000",
                                ProcessingCode.INITIALIZE_PROMOTION.code,
                                tpt
                            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                if (isSuccess) {
                                    val spliter = responsef57.split("|")
                                    if (spliter[1] == "1") {
                                        val terminalParameterTable =
                                            TerminalParameterTable.selectFromSchemeTable()
                                        terminalParameterTable?.isPromoAvailable = true
                                        // CheckPromo....
                                        if (terminalParameterTable?.reservedValues?.get(4)
                                                .toString()
                                                .toInt() == 1 && terminalParameterTable?.isPromoAvailable == true
                                        ) {
                                            terminalParameterTable.hasPromo = "1"
                                            TerminalParameterTable.performOperation(
                                                terminalParameterTable
                                            ) {
                                                Log.i("TPT", "UPDATED with promo availability")
                                                TerminalParameterTable.updateMerchantPromoData(
                                                    Triple(
                                                        spliter[0],
                                                        spliter[1] == "1",
                                                        spliter[2] == "1"
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            getDigiPosStatus(
                                EnumDigiPosProcess.InitializeDigiPOS.code,
                                EnumDigiPosProcessingCode.DIGIPOSPROCODE.code, false
                            ) { isSuccess, responseMsg, responsef57, fullResponse ->
                                try {
                                    if (isSuccess) {
                                        //1^Success^Success^S101^Active^Active^Active^Active^0^1
                                        val responsF57List = responsef57.split("^")
                                        Log.e("F56->>", responsef57)
                                        if (responsF57List[4] == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                            val tpt1 =
                                                TerminalParameterTable.selectFromSchemeTable()
                                            tpt1?.isDigiposActive = "1"
                                            tpt1?.digiPosResponseType = responsF57List[0].toString()
                                            tpt1?.digiPosStatus = responsF57List[1].toString()
                                            tpt1?.digiPosStatusMessage =
                                                responsF57List[2].toString()
                                            tpt1?.digiPosStatusCode = responsF57List[3].toString()
                                            tpt1?.digiPosTerminalStatus =
                                                responsF57List[4].toString()
                                            tpt1?.digiPosBQRStatus = responsF57List[5].toString()
                                            tpt1?.digiPosUPIStatus = responsF57List[6].toString()
                                            tpt1?.digiPosSMSpayStatus = responsF57List[7].toString()
                                            tpt1?.digiPosStaticQrDownloadRequired =
                                                responsF57List[8].toString()
                                            tpt1?.digiPosCardCallBackRequired =
                                                responsF57List[9].toString()
                                            if (tpt1 != null) {
                                                TerminalParameterTable.performOperation(tpt1) {
                                                    logger(
                                                        LOG_TAG.DIGIPOS.tag,
                                                        "Terminal parameter Table updated successfully $tpt1 "
                                                    )
                                                    val ttp =
                                                        TerminalParameterTable.selectFromSchemeTable()
                                                    val tptObj = Gson().toJson(ttp)
                                                    logger(
                                                        LOG_TAG.DIGIPOS.tag,
                                                        "After success      $tptObj "
                                                    )
                                                }
                                                if (tpt1.digiPosBQRStatus == EDigiPosTerminalStatusResponseCodes.ActiveString.statusCode) {
                                                    runBlocking {
                                                        getStaticQrFromServerAndSaveToFile(context as BaseActivity){
                                                            // FAIL AND SUCCESS HANDELED IN FUNCTION getStaticQrFromServerAndSaveToFile itself
                                                        }
                                                    }
                                                }

                                            }
                                        } else {
                                            logger("DIGI_POS", "DIGI_POS_UNAVAILABLE")
                                        }
                                    }

                                } catch (ex: java.lang.Exception) {
                                    ex.printStackTrace()
                                    logger(
                                        LOG_TAG.DIGIPOS.tag,
                                        "Somethig wrong... in response data field 57"
                                    )
                                }
                            }

                        }
                    }
                    // endregion================
                    (context as MainActivity).hideProgress()
                    GlobalScope.launch(Dispatchers.Main) {
                        (context as MainActivity).alertBoxWithAction(null,
                            null,
                            "",
                            context.getString(R.string.successfull_init),
                            false,
                            "",
                            {},
                            {})
                    }
                    VFService.vfBeeper?.startBeep(200)
                    AppPreference.saveBoolean(
                        PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString(),
                        false
                    )
                    val tpt = TerminalParameterTable.selectFromSchemeTable()
                    VFService.setAidRid(
                        addPad(tpt?.minCtlsTransAmt ?: "", "0", 12, true),
                        addPad(tpt?.maxCtlsTransAmt ?: "", "0", 12, true)
                    )
                } else {
                    AppPreference.saveBoolean(
                        PrefConstant.INIT_AFTER_SETTLEMENT.keyName.toString(),
                        true
                    )
                    backToCalled(result, false, false)
                }
            }, {
                backToCalled(it, false, true)
            }, this@KeyExchanger)
        }
    }

    override suspend fun createInitIso(nextCounter: String, isFirstCall: Boolean): IWriter =
        IsoDataWriter().apply {
            mti = Mti.MTI_LOGON.mti
            // adding processing code and field 59 for public and private key
            addField(
                3, if (isFirstCall) {
                    addFieldByHex(60, "${addPad(0, "0", 8)}BP${addPad(0, "0", 4)}")
                    ProcessingCode.INIT.code
                } else {
                    addFieldByHex(60, nextCounter)
                    ProcessingCode.INIT_MORE.code
                }
            )
            //adding stan (padding of stan is internally handled by iso)
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())
            //adding nii
            addField(24, Nii.DEFAULT.nii)

            //adding tid
            addFieldByHex(41, tid)

            //adding field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //region=========adding field 61=============
            val f61 = "3VX675 BonusHub  01.01.32.200626000000000000000000"//
            getF61()
            addFieldByHex(61, f61)
            //endregion

            //region=====adding field 63============
            //  val bankCode: String = "01"

            val bankCode = AppPreference.getBankCode()

            //Serial Number from VF Service AIDL:-
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
            //endregion

        }

    private fun insertSecurityKeys(
        ppk: ByteArray, dpk: ByteArray,
        ppkKcv: ByteArray, dpkKcv: ByteArray, callback: (Boolean) -> Unit
    ) {
        var result: Boolean? = true
        try {
            val dTmkArr = RSAProvider.decriptTMK(tmk.hexStr2ByteArr(), rsa)
            val decriptedTmk = dTmkArr[0].hexStr2ByteArr()

            val x =
                "TMK=${decriptedTmk.byteArr2HexStr()}\nPPK=${ppk.byteArr2HexStr()} KCV=${ppkKcv.byteArr2HexStr()}\nDPK=${dpk.byteArr2HexStr()} KCV=${dpkKcv.byteArr2HexStr()}"
            logger(TAG, x)
            result = VFService.injectTMK(decriptedTmk, ppk, ppkKcv, dpk, dpkKcv)
            if (result == true) {
                VFService.vfBeeper?.startBeep(200)
            }
            Log.d("Key Insert Success:- ", result.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            logger("Key Exchange", e.message ?: "")
            result = false
        } finally {
            try {
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            if (result != null) {
                callback(result)
            }
        }
    }

    private fun insertAfterSettlementSecurityKeys(
        ppk: ByteArray, dpk: ByteArray,
        ppkKcv: ByteArray, dpkKcv: ByteArray, onTMKCall: Boolean, callback: (Boolean) -> Unit
    ) {
        var result: Boolean? = true
        try {
            //  ROCProviderV2.resetRoc(AppPreference.getBankCode())

            result = if (onTMKCall) {
                val dTmkArr = RSAProvider.decriptTMK(tmk.hexStr2ByteArr(), rsa)
                val decriptedTmk = dTmkArr[0].hexStr2ByteArr()

                val x =
                    "TMK=${decriptedTmk.byteArr2HexStr()}\nPPK=${ppk.byteArr2HexStr()} KCV=${ppkKcv.byteArr2HexStr()}\nDPK=${dpk.byteArr2HexStr()} KCV=${dpkKcv.byteArr2HexStr()}"
                logger(TAG, x)
                VFService.injectTMK(decriptedTmk, ppk, ppkKcv, dpk, dpkKcv)
            } else {
                VFService.injectTMK(null, ppk, ppkKcv, dpk, dpkKcv, isLoadMainKey = false)
            }

            if (result == true) {
                VFService.vfBeeper?.startBeep(200)
            }
            Log.d("Key Insert Success:- ", result.toString())
            if (result != null) {
                callback(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger("Key Exchange", e.message ?: "")
            result = false
            callback(result)
        } finally {
            try {
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            /*if (result != null) {
                callback(result)
            }*/
        }
    }
}


suspend fun downloadPromo() {
    val sc = ServerCommunicator()
    val tpt = TerminalParameterTable.selectFromSchemeTable()
    val fileArray = mutableListOf<Byte>()

    if (tpt != null && sc.open()) {
        //======First Get header and footer, then proceed for image downloading =========
        val isoW = IsoDataWriter().apply {
            mti = Mti.MTI_INIT.mti
            addField(3, ProcessingCode.CHARGE_SLIP_HEADER_FOOTER.code)
            addField(11, ROCProvider.getRoc().toString())
            addField(24, Nii.DEFAULT.nii)

            addFieldByHex(41, tpt.terminalId)

            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //region========Adding Field 61=========
            addFieldByHex(61, KeyExchanger.getF61())
            //endregion

            //region====Adding field 63==========
            val f63 = VerifoneApp.getDeviceSerialNo()
            val bankCode = AppPreference.getBankCode()
            addFieldByHex(63, "$f63$bankCode")
            //endregion


            addFieldByHex(60, "00000000000000")
        }

        //region======First Get header and footer, then proceed for image downloading =========

        val data = isoW.generateIsoByteRequest()

        val respH = sc.sendData(data)

        if (respH.isNotEmpty()) {
            val res = readIso(respH, false)

            //region==adding ROC==
            val roc = res.isoMap[11]?.rawData
            if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()
            //endregion
            if (res.isoMap[39]?.parseRaw2String() == "00") {
                val f59 = res.isoMap[59]?.parseRaw2String() ?: ""
                if (f59.isNotEmpty()) {
                    AppPreference.saveString(AppPreference.HEADER_FOOTER, f59)
                }
            }

        }

        //endregion=============

        //=====Changing processing code for image downloading========
        isoW.addField(3, ProcessingCode.CHARGE_SLIP_START.code)

        var pCode = ""
        var packetRecd: Int = 0
        do {
            val data = isoW.generateIsoByteRequest()

            val resp = sc.sendData(data)

            if (resp.isNotEmpty()) {
                val res = readIso(resp, false)
                pCode = res.isoMap[3]?.rawData ?: ""
                if (pCode.isNotEmpty()) isoW.addField(3, pCode)

                //region==adding ROC==
                val roc = res.isoMap[11]?.rawData
                if (roc != null) ROCProvider.incrementFromResponse(roc) else ROCProvider.increment()
                //endregion

                val f60 = res.isoMap[60]?.rawData ?: ""

                if (f60.isNotEmpty()) {

                    val entry = f60.substring(8..35).hexStr2ByteArr().byteArr2Str()

                    packetRecd = entry.substring(0, 8).toInt() - packetRecd

                    if (packetRecd > 0) {
                        isoW.addFieldByHex(60, entry)
                        val da1 = f60.substring(f60.length - (packetRecd * 2), f60.length)
                        fileArray.addAll(da1.hexStr2ByteArr().toList())
                    }
                }
            } else break
        } while (pCode == ProcessingCode.CHARGE_SLIP_CONTINUE.code)
        sc.close()
    }
    if (fileArray.isNotEmpty()) {
        unzipZipedBytes(fileArray.toByteArray())
    }

}

suspend fun getPromotionData(
    field57RequestData: String,
    processingCode: String, tpt: TerminalParameterTable,
    cb: (Boolean, String, String, String) -> Unit
) {

    // Promo Available or not at TID
    if ((tpt.reservedValues[4]).toString().toInt() == 1) {
        val idw = IsoDataWriter().apply {
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            if (terminalData != null) {
                mti = Mti.EIGHT_HUNDRED_MTI.mti

                //Processing Code Field 3
                addField(3, processingCode)

                //STAN(ROC) Field 11
                addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

                //NII Field 24
                addField(24, Nii.HDFC_DEFAULT.nii)

                //TID Field 41
                addFieldByHex(41, terminalData.terminalId)

                //Connection Time Stamps Field 48
                //   addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                //adding Field 57
                addFieldByHex(57, field57RequestData)

                //adding Field 61
                val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                val pcNumber2 =
                    addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
                val f61 = ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"),
                    " ",
                    6,
                    false
                ) + addPad(
                    VerifoneApp.appContext.getString(R.string.app_name),
                    " ",
                    10,
                    false
                ) + version + pcNumber + pcNumber2
                //adding Field 61
                addFieldByHex(61, f61)

                //adding Field 63
                val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
                val bankCode = AppPreference.getBankCode()
                val f63 = "$deviceSerial$bankCode"
                addFieldByHex(63, f63)
            }
        }

        logger("Transaction RESPONSE --->>", idw.isoMap, "e")

        val idwByteArray = idw.generateIsoByteRequest()

        var responseField57 = ""
        var responseMsg = ""
        var isBool = false
        HitServer.hitServer(idwByteArray, { result, success ->
            responseMsg = result
            if (success) {
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )
                val responseIsoData: IsoDataReader = readIso(result, false)
                logger("Transaction RESPONSE ", "---", "e")
                logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
                Log.e(
                    "Success 39-->  ",
                    responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                            responseIsoData.isoMap[58]?.parseRaw2String().toString()
                )
                val successResponseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
                if (responseIsoData.isoMap[57] != null) {
                    responseField57 = responseIsoData.isoMap[57]?.parseRaw2String().toString()
                }
                if (responseIsoData.isoMap[58] != null) {
                    responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
                }
                isBool = successResponseCode == "00"
                if (processingCode == ProcessingCode.INITIALIZE_PROMOTION.code)
                    cb(isBool, responseMsg, responseField57, result)
                else cb(true, responseMsg, responseField57, result)
            } else {
                ROCProviderV2.incrementFromResponse(
                    ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                    AppPreference.getBankCode()
                )
                cb(isBool, responseMsg, responseField57, result)
            }
        }, {})
    }
}

suspend fun getDigiPosStatus(
    field57RequestData: String,
    processingCode: String, isSaveTransAsPending: Boolean = false,
    cb: (Boolean, String, String, String) -> Unit
) {

    val idw = IsoDataWriter().apply {
        val terminalData = TerminalParameterTable.selectFromSchemeTable()
        if (terminalData != null) {
            mti = Mti.EIGHT_HUNDRED_MTI.mti

            //Processing Code Field 3
            addField(3, processingCode)

            //STAN(ROC) Field 11
            addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

            //NII Field 24
            addField(24, Nii.BRAND_EMI_MASTER.nii)

            //TID Field 41
            addFieldByHex(41, terminalData.terminalId)

            //Connection Time Stamps Field 48
            addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

            //adding Field 57
            addFieldByHex(57, field57RequestData)

            //adding Field 61
            val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
            val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
            val pcNumber2 =
                addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY_2), "0", 9)
            val f61 = ConnectionType.GPRS.code + addPad(
                AppPreference.getString("deviceModel"),
                " ",
                6,
                false
            ) + addPad(
                VerifoneApp.appContext.getString(R.string.app_name),
                " ",
                10,
                false
            ) + version + pcNumber + pcNumber2
            //adding Field 61
            addFieldByHex(61, f61)

            //adding Field 63
            val deviceSerial = addPad(AppPreference.getString("serialNumber"), " ", 15, false)
            val bankCode = AppPreference.getBankCode()
            val f63 = "$deviceSerial$bankCode"
            addFieldByHex(63, f63)
        }
    }

    logger("DIGIPOS REQ1>>", idw.isoMap, "e")

    // val idwByteArray = idw.generateIsoByteRequest()

    var responseField57 = ""
    var responseMsg = ""
    var isBool = false
    HitServer.hitDigiPosServer(idw, isSaveTransAsPending) { result, success ->
        responseMsg = result
        if (success) {
            ROCProviderV2.incrementFromResponse(
                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                AppPreference.getBankCode()
            )
            val responseIsoData: IsoDataReader = readIso(result, false)
            logger("Transaction RESPONSE ", "---", "e")
            logger("Transaction RESPONSE --->>", responseIsoData.isoMap, "e")
            Log.e(
                "Success 39-->  ",
                responseIsoData.isoMap[39]?.parseRaw2String().toString() + "---->" +
                        responseIsoData.isoMap[58]?.parseRaw2String().toString()
            )
            val successResponseCode = responseIsoData.isoMap[39]?.parseRaw2String().toString()
            if (responseIsoData.isoMap[57] != null) {
                responseField57 = responseIsoData.isoMap[57]?.parseRaw2String().toString()
            }
            if (responseIsoData.isoMap[58] != null) {
                responseMsg = responseIsoData.isoMap[58]?.parseRaw2String().toString()
            }
            isBool = successResponseCode == "00"
            cb(isBool, responseMsg, responseField57, result)
            /* if (processingCode == ProcessingCode.INITIALIZE_PROMOTION.code)
                 cb(isBool, responseMsg, responseField57, result)
             else cb(true, responseMsg, responseField57, result)*/
        } else {
            ROCProviderV2.incrementFromResponse(
                ROCProviderV2.getRoc(AppPreference.getBankCode()).toString(),
                AppPreference.getBankCode()
            )
            cb(isBool, responseMsg, responseField57, result)
        }
    }
}
