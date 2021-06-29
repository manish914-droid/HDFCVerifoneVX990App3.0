package com.example.verifonevx990app.emv.transactionprocess

import com.example.verifonevx990app.main.DetectCardType
import java.io.Serializable

//Below Modal class is for holding Card Returning Data Fields:-
class CardProcessedDataModal : Serializable {
    private var track1Data: String? = null
    private var track2Data: String? = null
    private var track3Data: String? = null
    private var panNumberData: String? = null
    private var serviceCodeData: String? = null
    private var readCardType: DetectCardType? = null
    private var applicationPanSequenceValue: String? = null
    private var posEntryMode: String? = null
    private var processingCode: String? = null
    private var transactionAmount: Long? = null
    private var emiTransactionAmount: Long? = null
    private var flexiPayTransactionAmount: Long? = null
    private var field55: String? = null
    private var isOnline: Int = 0
    private var genratedPinBlock: String? = null
    private var aid: String? = null
    private var aidPrint: String? = null
    private var cardholderName: String? = null
    private var date: String? = null
    private var time: String? = null
    private var timeStamp: String? = null
    private var fallBackType: Int = 0
    private var authCode: String? = null
    private var retrivalReferenceNumber: String? = null
    private var tc: String? = null
    private var retryTimes: Int = 0
    private var emitype: Int = 0
    private var otherAmount: Long? = 0
    private var transactionType: Int = 0

    private var authRoc: String? = null
    private var authBatch: String? = null
    private var authTid: String? = null
    private var encryptedPan: String? = null

    private var amountInResponse: String? = null
    private var tipAmount: Long? = 0L

    // used in case of sale with cash type
    private var saleAmount: Long? = 0
    private var enteredInvoice: String? = null
    private var acqReferalNumber: String? = null
    private var mobileBillExtraData: Pair<String, String>? = null

    //region==================================================Extra Field 57 TLE Data Variables:-
    private var applicationLabel: String? = null
    private var cardIssuerCountryCode: String? = null
    private var typeOfTxnFlag: String? = null
    private var pinEntryFlag: String? = null
    //endregion

    // For Insta EMI Available
    private var hasInstaEmi: Boolean = false

    private var cardlabel: String? = null

    private var pinByPass: Int? = null

    //region==================================================No cvm required value:-
    private var noCVMneeded: Boolean = false
    //region

    //region==================================================No cvm required value:-
    private var doubleTap: Boolean = false
    //region



    fun getTrack1Data(): String? {
        return track1Data
    }

    fun setTrack1Data(track1Data: String) {
        this.track1Data = track1Data
    }

    fun getTrack2Data(): String? {
        return track2Data
    }

    fun setTrack2Data(track2Data: String) {
        this.track2Data = track2Data
    }

    fun getTrack3Data(): String? {
        return track3Data
    }

    fun setTrack3Data(track3Data: String) {
        this.track3Data = track3Data
    }

    fun getPanNumberData(): String? {
        return panNumberData
    }

    fun setPanNumberData(panNumberData: String) {
        this.panNumberData = panNumberData
    }

    fun getServiceCodeData(): String? {
        return serviceCodeData
    }

    fun setServiceCodeData(serviceCodeData: String) {
        this.serviceCodeData = serviceCodeData
    }

    fun getReadCardType(): DetectCardType? {
        return readCardType
    }

    fun setReadCardType(readCardType: DetectCardType?) {
        this.readCardType = readCardType
    }

    fun getApplicationPanSequenceValue(): String? {
        return applicationPanSequenceValue
    }

    fun setApplicationPanSequenceValue(applicationPanSequenceValue: String) {
        this.applicationPanSequenceValue = applicationPanSequenceValue
    }

    fun getPosEntryMode(): String? {
        return posEntryMode
    }

    fun setPosEntryMode(posEntryMode: String) {
        this.posEntryMode = posEntryMode
    }

    fun getProcessingCode(): String? {
        return processingCode
    }

    fun setProcessingCode(processingCode: String) {
        this.processingCode = processingCode
    }

    fun getTransactionAmount(): Long? {
        return transactionAmount
    }

    fun setTransactionAmount(transactionAmount: Long) {
        this.transactionAmount = transactionAmount
    }

    fun getEmiTransactionAmount(): Long? {
        return emiTransactionAmount
    }

    fun setEmiTransactionAmount(emiTransAmount: Long) {
        this.emiTransactionAmount = emiTransAmount
    }

    fun getFlexiPayTransactionAmount(): Long? {
        return flexiPayTransactionAmount
    }


    fun setFlexiPayTransactionAmount(flexiPayTransactionAmount: Long) {
        this.flexiPayTransactionAmount = flexiPayTransactionAmount
    }

    fun setField55(field55: String) {
        this.field55 = field55
    }

    fun getFiled55(): String? {
        return field55
    }

    fun setIsOnline(isOnline: Int) {
        this.isOnline = isOnline
    }

    fun getIsOnline(): Int {
        return isOnline
    }

    fun setGeneratePinBlock(genratedPinBlock: String) {
        this.genratedPinBlock = genratedPinBlock
    }

    fun getGeneratePinBlock(): String? {
        return genratedPinBlock
    }

    fun setDate(date: String) {
        this.date = date
    }

    fun getDate(): String? {
        return date
    }

    fun setTime(time: String) {
        this.time = time
    }

    fun getTime(): String? {
        return time
    }

    fun setTimeStamp(timeStamp: String) {
        this.timeStamp = timeStamp
    }

    fun getTimeStamp(): String? {
        return timeStamp
    }

    fun setAuthCode(authCode: String) {
        this.authCode = authCode
    }

    fun getAuthCode(): String? {
        return authCode
    }

    fun setFallbackType(fallBackType: Int) {
        this.fallBackType = fallBackType
    }

    fun getFallbackType(): Int {
        return fallBackType
    }

    fun setRetrievalReferenceNumber(rrn: String) {
        this.retrivalReferenceNumber = rrn
    }

    fun getRetrievalReferenceNumber(): String? {
        return retrivalReferenceNumber
    }

    fun setTC(data: String) {
        this.tc = data
    }

    fun getTC(): String? {
        return tc
    }

    fun setRetryTimes(retryTimes: Int) {
        this.retryTimes = retryTimes
    }

    fun getRetryTimes(): Int {
        return retryTimes
    }

    fun getOtherAmount(): Long? {
        return otherAmount
    }

    fun setOtherAmount(otherAmount: Long) {
        this.otherAmount = otherAmount
    }

    fun setTransType(transType: Int) {
        this.transactionType = transType
    }

    fun getTransType(): Int {
        return transactionType
    }

    fun setAuthRoc(authRoc: String) {
        this.authRoc = authRoc
    }

    fun getAuthRoc(): String? {
        return authRoc
    }

    fun setAuthBatch(authBatch: String) {
        this.authBatch = authBatch
    }

    fun getAuthBatch(): String? {
        return authBatch
    }

    fun setAuthTid(authTid: String) {
        this.authTid = authTid
    }

    fun getAuthTid(): String? {
        return authTid
    }

    fun setEncryptedPan(encryptedPan: String) {
        this.encryptedPan = encryptedPan
    }

    fun getEncryptedPan(): String? {
        return encryptedPan
    }

    fun setAmountInResponse(amountInResponse: String) {
        this.amountInResponse = amountInResponse
    }

    fun getAmountInResponse(): String? {
        return amountInResponse
    }

    fun getTipAmount(): Long? {
        return tipAmount
    }

    fun setTipAmount(tipAmount: Long?) {
        this.tipAmount = tipAmount
    }

    // used in case of sale with cash type
    fun getSaleAmount(): Long? {
        return saleAmount
    }

    fun setSaleAmount(saleAmount: Long) {
        this.saleAmount = saleAmount
    }

    fun setEnteredInvoice(enteredInvoice: String) {
        this.enteredInvoice = enteredInvoice
    }

    fun getEnteredInvoice(): String? {
        return enteredInvoice
    }

    fun setAcqReferalNumber(acqReferalNumber: String) {
        this.acqReferalNumber = acqReferalNumber
    }

    fun getAcqReferalNumber(): String? {
        return acqReferalNumber
    }

    fun setAID(aid: String?) {

        this.aid = aid
    }

    fun getAID(): String? {

        return aid
    }

    fun setAIDPrint(aid: String?) {

        this.aidPrint = aid
    }
    fun getAIDPrint(): String? {

        return aidPrint
    }

    fun getCardHolderName(): String? {

        return cardholderName
    }

    fun setCardHolderName(cardholderName: String?) {
        this.cardholderName = cardholderName
    }

    fun setApplicationLabel(label: String) {
        this.applicationLabel = label
    }

    fun getApplicationLabel(): String? {
        return applicationLabel
    }

    fun setCardIssuerCountryCode(countryCode: String) {
        this.cardIssuerCountryCode = countryCode
    }

    fun getCardIssuerCountryCode(): String? {
        return cardIssuerCountryCode
    }

    fun setTypeOfTxnFlag(txnFlag: String) {
        this.typeOfTxnFlag = txnFlag
    }

    fun getTypeOfTxnFlag(): String? {
        return typeOfTxnFlag
    }

    fun setPinEntryFlag(entryFlag: String) {
        this.pinEntryFlag = entryFlag
    }

    fun getPinEntryFlag(): String? {
        return pinEntryFlag
    }

    fun setPinByPass(pinByPass: Int) {
        this.pinByPass = pinByPass
    }

    fun getPinByPass(): Int? {
        return pinByPass
    }

    fun setNoCVM(noCVMneeded: Boolean) {
        this.noCVMneeded = noCVMneeded
    }

    fun getNoCVM(): Boolean? {
        return noCVMneeded
    }

    fun setcardLabel(cardlabel: String?) {
        this.cardlabel = cardlabel
    }

    fun getcardLabel(): String? {
        return cardlabel
    }

    fun setDoubeTap(doubleTap: Boolean) {
        this.doubleTap = doubleTap
    }

    fun getDoubeTap(): Boolean? {
        return doubleTap
    }

    fun getEmiType(): Int {
        return emitype
    }

    fun setEmiType(emitype: Int) {
        this.emitype = emitype
    }

    fun getMobileBillExtraData(): Pair<String, String>? {
        return mobileBillExtraData
    }

    fun setMobileBillExtraData(mobileBillExtraData: Pair<String, String>) {
        this.mobileBillExtraData = mobileBillExtraData
    }
}