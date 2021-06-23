package com.example.verifonevx990app.preAuth

import com.example.verifonevx990app.R
import com.example.verifonevx990app.emv.transactionprocess.CardProcessedDataModal
import com.example.verifonevx990app.realmtables.IssuerParameterTable
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.utils.HexStringConverter
import com.example.verifonevx990app.vxUtils.*
import java.text.SimpleDateFormat
import java.util.*

class CreateAuthPacket {
    //---
    fun createPreAuthCompleteAndVoidPreauthISOPacket(authCompletionData: AuthCompletionData, cardProcessedData: CardProcessedDataModal): IsoDataWriter =
        IsoDataWriter().apply {

            //     val batchFileDataTable = BatchFileDataTable.selectBatchData()
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            if (terminalData != null) {
                mti = Mti.PRE_AUTH_COMPLETE_MTI.mti

                //Processing Code Field 3
                addField(3, cardProcessedData.getProcessingCode().toString())

                //Transaction Amount Field
                //val formattedTransAmount = "%.2f".format(cardProcessedData.getTransactionAmount()?.toDouble()).replace(".", "")
                addField(
                    4,
                    addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true)
                )

                //STAN(ROC) Field 11
                addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

                //Date and Time Field 12 & 13
                addIsoDateTime(this)

                //NII Field 24
                addField(24, Nii.DEFAULT.nii)

                //TID Field 41
                addFieldByHex(41, terminalData.terminalId)

                //MID Field 42
                addFieldByHex(42, terminalData.merchantId)

                //Connection Time Stamps Field 48
                addFieldByHex(48, Field48ResponseTimestamp.getF48Data())

                val dateTime: Long = Calendar.getInstance().timeInMillis
                val time: String = SimpleDateFormat("HHmmss", Locale.getDefault()).format(dateTime)
                val date: String = SimpleDateFormat("MMdd", Locale.getDefault()).format(dateTime)
                val year: String = SimpleDateFormat("yy", Locale.getDefault()).format(dateTime)
                logger("AUTH YEAR->", year, "e")
                //adding field 56

                val rocF56 = authCompletionData.authRoc?.let { addPad(it, "0", 6, true) }
                val batchF56 = authCompletionData.authBatchNo?.let { addPad(it, "0", 6, true) }
                val tidF56AuthCompletion = authCompletionData.authTid

                val formatedDate = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(dateTime)


                if(tidF56AuthCompletion?.isNotBlank() == true && batchF56?.isNotBlank() == true && rocF56?.isNotBlank() == true) {
                    addFieldByHex(56, "${tidF56AuthCompletion}${batchF56}${rocF56}${formatedDate}${""}${""}")
                    println("Field 56 data is" + "${tidF56AuthCompletion}${batchF56}${rocF56}${formatedDate}${""}${""}")
                }



                when (cardProcessedData.getTransType()) {
                    TransactionType.PRE_AUTH_COMPLETE.type -> {
                        //new data of field56       //tid              //batchno   //roc //datetime       //authcode //invoice
                        val f56AuthCompletion = "${tidF56AuthCompletion}${batchF56}${rocF56}${formatedDate}${""}${""}"

                        println("Field 56 data iin preAuth Complete" + f56AuthCompletion)

                        //old data
                     //   val f56AuthCompletion = tidF56AuthCompletion + batchF56 + rocF56 + year + date + time
                        //   logger("F56AuthComp-->>", f56AuthCompletion, "e")
                        addFieldByHex(56, f56AuthCompletion)
                        additionalData["F56reversal"] = f56AuthCompletion
                    }

                    TransactionType.VOID_PREAUTH.type -> {

                        println("Field 56 void data is" + "${terminalData.terminalId}${batchF56}${rocF56}${formatedDate}${"000000"}${""}")

                        //new data of field 56  //tid              //batchno   //roc //datetime       //authcode //invoice
                        val f56AuthVoid = "${terminalData.terminalId}${batchF56}${rocF56}${formatedDate}${"000000"}${""}"

                        //old data
                     //   val f56AuthVoid = terminalData.terminalId + batchF56 + rocF56 + year + date + time
                        //    logger("F56Void-->>", f56AuthVoid, "e")
                        addFieldByHex(56, f56AuthVoid)
                        additionalData["F56reversal"] = f56AuthVoid

                    }
                }


                //Batch Number
                addFieldByHex(60, addPad(terminalData.batchNumber, "0", 6, true))

                //adding field 61
                val issuerParameterTable = IssuerParameterTable.selectFromIssuerParameterTable(
                    AppPreference.WALLET_ISSUER_ID
                )
                val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                val data = ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"),
                    "*",
                    6,
                    false
                ) +
                        addPad(
                            VerifoneApp.appContext.getString(R.string.app_name),
                            " ",
                            10,
                            false
                        ) +
                        version + pcNumber + addPad("0", "0", 9)
                val customerID = HexStringConverter.addPreFixer(
                    issuerParameterTable?.customerIdentifierFiledType,
                    2
                )

                val walletIssuerID =
                    HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)
                addFieldByHex(
                    61, addPad(
                        AppPreference.getString("serialNumber"), " ", 15, false
                    ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
                )

                //adding field 62
                addFieldByHex(62, terminalData.invoiceNumber)

            }
        }

    fun createPendingPreAuthISOPacket(
        cardProcessedData: CardProcessedDataModal,
        counter: Int
    ): IsoDataWriter =
        IsoDataWriter().apply {
            //     val batchFileDataTable = BatchFileDataTable.selectBatchData()
            val terminalData = TerminalParameterTable.selectFromSchemeTable()
            if (terminalData != null) {
                mti = Mti.PRE_AUTH_MTI.mti

                //Processing Code Field 3
                addField(3, cardProcessedData.getProcessingCode().toString())

                //Transaction Amount Field
                //val formattedTransAmount = "%.2f".format(cardProcessedData.getTransactionAmount()?.toDouble()).replace(".", "")
                addField(
                    4,
                    addPad(cardProcessedData.getTransactionAmount().toString(), "0", 12, true)
                )

                //STAN(ROC) Field 11
                addField(11, ROCProviderV2.getRoc(AppPreference.getBankCode()).toString())

                //Date and Time Field 12 & 13
                addIsoDateTime(this)

                //NII Field 24
                addField(24, Nii.DEFAULT.nii)

                //TID Field 41
                addFieldByHex(41, terminalData.terminalId)

                //MID Field 42
                addFieldByHex(42, terminalData.merchantId)

                //Connection Time Stamps Field 48
                addFieldByHex(48, Field48ResponseTimestamp.getF48Data())


                //Batch Number
                addFieldByHex(60, addPad(terminalData.batchNumber, "0", 6, true))

                //adding field 61
                val issuerParameterTable =
                    IssuerParameterTable.selectFromIssuerParameterTable(AppPreference.WALLET_ISSUER_ID)
                val version = addPad(getAppVersionNameAndRevisionID(), "0", 15, false)
                val pcNumber = addPad(AppPreference.getString(AppPreference.PC_NUMBER_KEY), "0", 9)
                val data = ConnectionType.GPRS.code + addPad(
                    AppPreference.getString("deviceModel"),
                    "*",
                    6,
                    false
                ) + addPad(
                    VerifoneApp.appContext.getString(R.string.app_name),
                    " ",
                    10,
                    false
                ) + version + addPad("0", "0", 9) + pcNumber
                val customerID = HexStringConverter.addPreFixer(
                    issuerParameterTable?.customerIdentifierFiledType,
                    2
                )
                val walletIssuerID =
                    HexStringConverter.addPreFixer(issuerParameterTable?.issuerId, 2)

                //Adding field 61
                addFieldByHex(
                    61,
                    addPad(
                        AppPreference.getString("serialNumber"),
                        " ",
                        15,
                        false
                    ) + AppPreference.getBankCode() + customerID + walletIssuerID + data
                )

                //adding field 62
                val counterString = counter.toString()
                addFieldByHex(62, counterString)
            }
        }


}