package com.example.verifonevx990app.database;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;


public class ISOPackageReader implements Serializable {


    public String autthCode;
    public String procceissingCode;
    public String transacnalAmmount;
    public String dateTime;
    public String stan;
    public String localTransactionTime;
    public String nii;
    public String aquiringIdentificationCode;
    public String retrievalReferenceNumber;
    public String resCode;
    public String tid;
    public String mid;
    public String currencyCode;
    public String reservedIso;
    public String reasionCode;
    public byte[] field59;
    @NotNull
    public byte[] fiield60;
    public byte[] byteCounter;
    public byte[] partialFileName;
    public byte[] fullFileName;
    public byte[] appFullSize;
    public byte[] noOfSplits;
    public byte[] crc;
    public int field60DataLength;
    public byte[] fiield60ActualData;
    public byte[] getField63;
    public String field31;
    public String field57 = "";
    public byte[] field57Length;
    private byte[] field61;
    private String referenceNumber;
    private ResponseMessage responseMessage;
    private String message;
    private byte[] field55;
    private byte[] field39;
    private byte[] field38;
    private byte[] field55Length;
    private byte[] field48;
    private String field48Str;

    public ISOPackageReader(ResponseMessage responseMessage) {
        this.responseMessage = responseMessage;

    }

    public ISOPackageReader() {

    }

    public byte[] getField55Length() {
        return field55Length;
    }

    public void setField55Length(byte[] field55Length) {
        this.field55Length = field55Length;
    }

    public byte[] getGetField63() {
        return getField63;
    }

    public void setGetField63(byte[] getField63) {
        this.getField63 = getField63;
    }

    @Nullable
    public String getReferenceNumber() {
        return referenceNumber != null ? referenceNumber : "";
    }

    public void setReferenceNumber(@Nullable String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    @NotNull
    public String getAutthCode() {
        return autthCode != null ? autthCode : "";
    }

    public ResponseMessage getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(ResponseMessage responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    //reading response
    public void readResponseData(byte[] bytes, int transactionType) throws UnsupportedEncodingException {
        byte[] lengttData = Arrays.copyOfRange(bytes, 0, 2);
        byte[] headerData = Arrays.copyOfRange(bytes, 2, 5);
        byte[] mtiData = Arrays.copyOfRange(bytes, 5, 7);
        //convert to binary
        byte[] bitmap = Arrays.copyOfRange(bytes, 7, 15);
        //    CustomToast.printAppLog("hex Bit map" + HexStringConverter.hexDump(bitmap));
        String bmp = toBinary(bitmap);
        //     CustomToast.printAppLog("BIT MAP " + bmp);
        byte[] packageData = Arrays.copyOfRange(bytes, 15, bytes.length);
        parseResponsePackage(bmp, packageData, transactionType);
    }

    private void parseResponsePackage(String bmp, byte[] packageData, int transactionType) throws UnsupportedEncodingException {
        int i = 0;
        TransactionHandling transactionHandling = new TransactionHandling(packageData, this);
        while (i < bmp.length()) {
            if (bmp.charAt(i) == '1') {
                transactionHandling.fieldNumber(i + 1, null, transactionType, false);
            }
            i++;
        }

        if (transactionHandling.isResponseError) {
            responseMessage.onResponseError(this);

        } else {
            responseMessage.onSuccessMessage(this);

        }
    }

    private String toBinary(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for (int i = 0; i < Byte.SIZE * bytes.length; i++)
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }

    public byte[] getField55() {
        return field55;
    }

    public void setField55(byte[] field55) {
        this.field55 = field55;
    }

    public byte[] getField39() {
        return field39;
    }

    public void setField39(byte[] field39) {
        this.field39 = field39;
    }

    public byte[] getField38() {
        return field38;
    }

    public void setField38(byte[] field38) {
        this.field38 = field38;
    }

    public void setField61(byte[] field61) {
        this.field61 = field61;
    }


    public byte[] getField48() {
        return field48;
    }

    public void setField48(byte[] field48) {
        this.field48 = field48;
    }

    public String getField48Str() {
        return field48Str;
    }

    public void setField48Str(String field48Str) {
        this.field48Str = field48Str;
    }


}
