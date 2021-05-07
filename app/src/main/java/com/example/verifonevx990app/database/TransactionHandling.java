package com.example.verifonevx990app.database;


import com.example.verifonevx990app.customui.CustomToast;
import com.example.verifonevx990app.utils.HexStringConverter;
import com.example.verifonevx990app.utils.PaxUtils;
import com.example.verifonevx990app.utils.TransactionTypeValues;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class TransactionHandling {
    public boolean isResponseError;
    private ISOPackageReader isoPackageReader = null;
    //private StringBuilder dataForField56 = new StringBuilder();
    private int lengthFroField56 = 0;
    private byte[] responseData;
    //private String errorMessage;
    private StringBuilder stringBuilder;
    //length of package
    private long length;

    public TransactionHandling(byte[] responseData, ISOPackageReader isoPackageReader) {
        if (responseData != null)
            this.responseData = responseData;
        this.isoPackageReader = isoPackageReader;
    }

    public TransactionHandling() {

        stringBuilder = new StringBuilder();
    }

//    String getErrorMessage() {
//        return errorMessage;
//    }

//    private void setErrorMessage(String errorMessage) {
//        this.errorMessage = errorMessage;
//    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public void setStringBuilder(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

//    //to store no of active data
//    private ArrayList<IsoFiledLength> isoPackageWriters = new ArrayList<>();

    //check field which is to be created
    public void fieldNumber(int fieldNumber, IsoFiledLength isoFiledLength, int transactionType, boolean isRequestType) throws UnsupportedEncodingException {
        // int i = 0;
        //StringBuilder stringBuilder = new StringBuilder();

        String prefixedValue;
        String s;
        switch (fieldNumber) {
            case 1:
                //reserved for bitmap
                break;
            case 2:
                /*PAN - PRIMARY ACCOUNT NUMBER*/
                /*This Field enabled except(init,settlement,App update,Key Exchange )*/
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 2);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 1));
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 8)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 8));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                    length = length + 9;
                }
                break;
            case 3:
                /* PROCESSING CODE */
                /*This Enabled for All Transaction*/
                if (isRequestType) {
                    length = length + 3;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 3)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 3));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 3);
                    responseData = Arrays.copyOfRange(responseData, 3, responseData.length);
                    CustomToast.printAppLog(" Processing code" + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.procceissingCode = HexStringConverter.bytes2HexString(headerData);
                }
                break;
            case 4:
                /*Amount, transaction*/
                /*this field enable except(init,settlement,Balance Enquiry,App Update,Key Exchanged)*/
                if (isRequestType) {
                    length = length + 6;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues()), 12);
                    if (!HexStringConverter.checkFieldByteSize(prefixedValue, 6)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 6));
                    } else {
                        stringBuilder.append(prefixedValue);
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 6);
                    responseData = Arrays.copyOfRange(responseData, 6, responseData.length);
                    CustomToast.printAppLog(" Transaction Amt " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.transacnalAmmount = HexStringConverter.bytes2HexString(headerData);
                }
                break;
            case 5:
                /*Amount, settlement*/

                break;
            case 6:

                /* Amount, cardholder billing*/

                break;
            case 7:
                /* Transmission date & time */

                if (isRequestType) {


                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 5);
                    responseData = Arrays.copyOfRange(responseData, 5, responseData.length);
                    CustomToast.printAppLog(" Response date and time f7 " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.dateTime = HexStringConverter.bytes2HexString(headerData);
                }
                break;
            case 8:
                /* Amount, cardholder billing fee */


                break;
            case 9:
                /*Conversion rate, settlement */


                break;
            case 10:
                /* Conversion rate, cardholder billing */


                break;
            case 11:
                /* System trace audit number (STAN) */
                /*this field enabled except SETTLEMENT*/
                /*if (Reversal, void, adjust,tip,sale compition)->save old data*/

                if (isRequestType) {
                    length = length + 3;
                    String dataF11;
                    s = HexStringConverter.addPreFixer(isoFiledLength.getFieldValues(), 6);
                    if (!HexStringConverter.checkFieldByteSize(s, 3)) {
                        dataF11 = HexStringConverter.byteShiftOperation(s, 3);
                    } else {
                        dataF11 = s;

                    }

                    if (transactionType == TransactionTypeValues.REVERSAL || transactionType == TransactionTypeValues.VOID || transactionType == TransactionTypeValues.ADJUSTMENT || transactionType == TransactionTypeValues.SALE_WITH_TIP || transactionType == TransactionTypeValues.SALE_COMPLETION) {
                        lengthFroField56 = lengthFroField56 + 3;
                        //dataForField56.append(dataF11);
                    }
                    stringBuilder.append(dataF11);

                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 3);
                    responseData = Arrays.copyOfRange(responseData, 3, responseData.length);
                    CustomToast.printAppLog("Response STAN " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.stan = HexStringConverter.bytes2HexString(headerData);
                }

                //checkFieldType(xmlFieldModel.getType(), isoFiledLength, stringBuilder);
                break;
            case 12:
                /* Time, local transaction (HHmmss) */
                /* This Field enabled except(INIT,SETTLEMENT,APP UPDATE,KEY EXCHANGE)*/
                if (isRequestType) {
                    length = length + 3;
                    String dataF12;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 3)) {
                        dataF12 = HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 3);


                    } else {
                        dataF12 = isoFiledLength.getFieldValues();

                    }

                    if (transactionType == TransactionTypeValues.REVERSAL || transactionType == TransactionTypeValues.VOID || transactionType == TransactionTypeValues.ADJUSTMENT || transactionType == TransactionTypeValues.SALE_WITH_TIP || transactionType == TransactionTypeValues.SALE_COMPLETION) {
                        lengthFroField56 = lengthFroField56 + 3;
                        //dataForField56.append(dataF12);
                    }
                    stringBuilder.append(dataF12);
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 6);
                    responseData = Arrays.copyOfRange(responseData, 6, responseData.length);
                    CustomToast.printAppLog(" STAN " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.localTransactionTime = HexStringConverter.bytes2HexString(headerData);

                }
                break;
            case 13:
                /* Date, local transaction (MMDD) */
                /* This Field enabled except(INIT,SETTLEMENT,APP UPDATE,KEY EXCHANGE)*/
                if (isRequestType) {
                    length = length + 2;
                    String dataF13;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 2)) {
                        dataF13 = HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 2);
                    } else {
                        dataF13 = isoFiledLength.getFieldValues();
                    }
                    if (transactionType == TransactionTypeValues.REVERSAL || transactionType == TransactionTypeValues.VOID || transactionType == TransactionTypeValues.ADJUSTMENT || transactionType == TransactionTypeValues.SALE_WITH_TIP || transactionType == TransactionTypeValues.SALE_COMPLETION) {
                        lengthFroField56 = lengthFroField56 + 2;
                        //dataForField56.append(dataF13);
                    }
                    stringBuilder.append(dataF13);
                }
                break;
            case 14:
                /* NA */
                /* Date, expiration */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 15:
                /* Date, settlement */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 16:
                /* Date, conversion */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 17:
                /* Date, capture */

//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 18:
                /* Merchant type/Merchant Category Code */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 19:
                /* Acquiring institution country code */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 20:
                /* PAN extended, country code */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 21:
                /* Forwarding institution. country code */
//                if (isRequest) {
//
//                } else {
//
//                }
                break;
            case 22:
                /* Point of service entry mode */
                /*This Field enabled in(SALE,VOID,REFUND,VOID_REFUND,SALE_WITH_CASH,CASH,BATCH_UPLOAD,PRE AUTH,SALE_TIP,ADJUST,REVERSAL,EMI_SALE)*/
                if (isRequestType) {
                    length = length + 2;
                    s = HexStringConverter.addPreFixer(isoFiledLength.getFieldValues(), 4);

                    if (!HexStringConverter.checkFieldByteSize(s, 2)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(s, 2));
                    } else {
                        stringBuilder.append(s);
                    }
                }
                break;
            case 23:
                /* Application PAN sequence number */
                /*This field only applicable for EMV*/
                /*This field enabled in(SALE,SALE_WITH_CASH,CASH,REFUND,PRE AUTH,REVERSAL,EMI_SALE)*/
                if (isRequestType) {
                    length = length + 3;
                    s = HexStringConverter.addPreFixer(isoFiledLength.getFieldValues(), 3);
                    if (!HexStringConverter.checkFieldByteSize(s, 3)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(s, 3));
                    } else {
                        stringBuilder.append(s);
                    }
                }
                break;
            case 24:
                /*Enabled in All Transaction*/

                if (isRequestType) {
                    length = length + 2;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 2)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 2));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    CustomToast.printAppLog("Response NII " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.nii = HexStringConverter.bytes2HexString(headerData);

                }

                break;
            case 25:
                /* Point of service condition code */

                break;
            case 26:
                /* Point of service capture code */

                break;
            case 27:
                /* Authorizing identification response length */

                break;
            case 28:
                /* Amount, transaction fee */

                break;
            case 29:
                /* Amount, settlement fee */

                break;
            case 30:
                /* Amount, transaction processing fee */

                break;
            case 31:
                /* Amount, settlement processing fee */
                /*This Field Enabled in(VOID,TIP,ADJUSTMENT)*/
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    length = length + s.length() + 1;

                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 1);

                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 1));

                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 1);
                    responseData = Arrays.copyOfRange(responseData, 1, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    isoPackageReader.field31 = HexStringConverter.bytes2HexString(bbMagT2);
                }
                break;
            case 32:
                /* Acquiring institution identification code */
                if (isRequestType) {

                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 1);
                    responseData = Arrays.copyOfRange(responseData, 1, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog(HexStringConverter.bufferToHex(responseData, 0, mesgLength));
                    CustomToast.printAppLog(HexStringConverter.bufferToHex(bbMagT2));

                    isoPackageReader.aquiringIdentificationCode = HexStringConverter.bytes2HexString(bbMagT2);
                }
                break;
            case 33:
                /* Forwarding institution identification code */

                break;
            case 34:
                /* Primary account number, extended */

                break;
            case 35:
                /* Track 2 data */
                /* NA */
                break;
            case 36:
                /* Track 3 data */

                break;
            case 37:
                /* Retrieval reference number */
                /*This field enabled if(TRANSACTION is  EMV and TRANSACTION TYPE IS REVERSAL)*/
                if (isRequestType) {
                    length = length + 12;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 12)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 12));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 12);
                    responseData = Arrays.copyOfRange(responseData, 12, responseData.length);
                    CustomToast.printAppLog("Retrieval reference number " + HexStringConverter.bytes2HexString(headerData));
                    String resData = new String(headerData, StandardCharsets.ISO_8859_1);
                    CustomToast.printAppLog("Response auth Code " + resData);
                    isoPackageReader.retrievalReferenceNumber = HexStringConverter.hexToString(HexStringConverter.bcd2Ascii(headerData));
                }
                break;
            case 38:
                /*Authorization identification response */
                /*This field enabled if(TRANSACTION is  EMV and TRANSACTION TYPE IS REVERSAL)*/
                if (isRequestType) {
                    length = length + 12;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 12)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 12));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] field38 = Arrays.copyOfRange(responseData, 0, 12);
                    responseData = Arrays.copyOfRange(responseData, 12, responseData.length);
                    CustomToast.printAppLog("Response auth Code " + HexStringConverter.bytes2HexString(field38));
                    String resData = new String(field38, StandardCharsets.ISO_8859_1);
                    CustomToast.printAppLog("Response auth Code " + resData);
                    isoPackageReader.autthCode = resData.trim();
                    isoPackageReader.setField38(resData.trim().getBytes());
                }
                break;
            case 39:
                /* Response code */
                /*This field enabled if(TRANSACTION is  EMV and TRANSACTION TYPE IS REVERSAL)*/
                if (isRequestType) {
                    stringBuilder.append(isoFiledLength.getFieldValues());
                } else {
                    byte[] responseCode = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    CustomToast.printAppLog(new String(responseCode));
                    String resData = new String(responseCode, StandardCharsets.ISO_8859_1);
                    isResponseError = !resData.equalsIgnoreCase("00");
                    isoPackageReader.resCode = resData;
                    isoPackageReader.setField39(responseCode);
                }
                break;
            case 40:
                /* Service restriction code */

                break;
            case 41:
                /*               Card acceptor terminal identification */
                /*                         Enabled For All Transactions*/
                if (isRequestType) {
                    length = length + 8;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 8)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 8));

                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 8);
                    responseData = Arrays.copyOfRange(responseData, 8, responseData.length);
                    CustomToast.printAppLog("Response TID " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.tid = HexStringConverter.bytes2HexString(headerData);
                }


                // checkFieldType(xmlFieldModel.getType(), isoFiledLength, stringBuilder);
                break;
            case 42:
                /* Card acceptor identification code */
                /*This Field Enabled except(INIT,UPDATE,KEY_EXCHANGE)*/
                if (isRequestType) {
                    length = length + 15;
                    if (!HexStringConverter.checkFieldByteSize(isoFiledLength.getFieldValues(), 15)) {
                        stringBuilder.append(HexStringConverter.byteShiftOperation(isoFiledLength.getFieldValues(), 15));
                    } else {
                        stringBuilder.append(isoFiledLength.getFieldValues());
                    }
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 15);
                    responseData = Arrays.copyOfRange(responseData, 15, responseData.length);
                    CustomToast.printAppLog("Response MID " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.mid = HexStringConverter.bytes2HexString(headerData);
                }
                break;
            case 43:

                /*Card acceptor name/location (1-23 address 24-36 city 37-38 state 39-40 country)*/
                break;
            case 44:

                /* Additional response data */
                break;
            case 45:

                /* Track 1 data */
                break;
            case 46:

                /* Additional data - ISO */
                break;
            case 47:

                /* Additional data - national */
                break;
            case 48:
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    //  s = HexStringConverter.hexToString(s);
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);

                    isoPackageReader.setField48(bbMagT2);

                    String hs = HexStringConverter.bytes2HexString(bbMagT2);
                    CustomToast.printAppLog("Field 48 " + hs);
                    isoPackageReader.setField48Str(HexStringConverter.hexToString(hs));
                }
                break;
            case 49:

                /* Currency code, transaction */
                if (isRequestType) {

                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    CustomToast.printAppLog("Currency code,transaction " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.currencyCode = HexStringConverter.bytes2HexString(headerData);
                }
                break;
            case 50:

                /* Currency code, settlement */
                break;
            case 51:

                /* 3	Currency code, cardholder billing */
                break;
            case 52:

                /* 	Personal identification number data */
                /*This Field Enabled only if PIN ENTERED By USER*/
                /*This Field Enabled in(SALE,SALE_WITH_CASH,CASH,PRE AUTH,REFUND,EMI SALE)*/
                if (isRequestType) {
                    s = HexStringConverter.hexToString(isoFiledLength.getFieldValues());
                    length = length + 8;// it was 9

                    CustomToast.printAppLog(HexStringConverter.hexDump(isoFiledLength.getFieldValues().getBytes()));
                    stringBuilder.append(s);
                    //  }
                }
                break;
            case 53:
                /* Security related control information */

                break;
            case 54:

                /* Additional amounts */
                /*This Field Enable in(SALE_WITH_CASH,TIP,ADJUSTMENT,CASH AT POS)*/
                if (isRequestType) {
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues()), 12);
                    length = length + prefixedValue.length() + 2;
                    stringBuilder.append(HexStringConverter.byteShiftOperation(HexStringConverter.addPreFixer(String.valueOf(prefixedValue.length()), 4), 2));
//                    if (!HexStringConverter.checkFieldByteSize(prefixedValue, 6)) {
//                        stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 6));
//                    } else {
                    stringBuilder.append(prefixedValue);
                    //}


                }
                break;
            case 55:
                /* ICC Data - EMV having multiple tags */
                /*This Field Enable in(SALE_WITH_CASH,TIP,ADJUSTMENT)*/
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    s = HexStringConverter.hexToString(s);
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog("Field 55 " + HexStringConverter.bufferToHex(bbMagT2));
                    isoPackageReader.setField55(bbMagT2);
                    isoPackageReader.setField55Length(messageLength);
                    //String totalData = new String(bbMagT2, "ISO-8859-1");
                    isoPackageReader.reservedIso = HexStringConverter.bytes2HexString(bbMagT2);
                }
                break;
            case 56:
                /* Reserved ISO */
                /*This Field Enable in(REVERSAL,TIP,VOID,SALE_COMPLITION,TIP_ADJUSTMENT)*/
                if (isRequestType) {
                    length = length + isoFiledLength.getFieldValues().length() + 1;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues().length()), 1);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 1));
                    stringBuilder.append(isoFiledLength.getFieldValues());
                }
                break;
            case 57:
                /* Reserved national */
                /*This Field Enable in(REFUND,SALE WITH CASH,SALE,TIP_ADJUSTMENT,SALE,CASH,VOID,TIP,ADJUST,EMI_SALE)*/
                if (isRequestType) {
                    s = HexStringConverter.hexToString(isoFiledLength.getFieldValues());
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {

                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog("Response MID " + HexStringConverter.bytes2HexString(bbMagT2));
                    isoPackageReader.field57 = PaxUtils.hexByteToString(bbMagT2);
                    isoPackageReader.field57Length = messageLength;
//                    CustomToast.printAppLog("Field 55 " + HexStringConverter.bufferToHex(bbMagT2));
//                    isoPackageReader.setField55(bbMagT2);
//                    isoPackageReader.setField55Length(messageLength);
//                    //String totalData = new String(bbMagT2, "ISO-8859-1");
//                    isoPackageReader.reservedIso = HexStringConverter.bytes2HexString(bbMagT2);
//                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 2);
//                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
//                    CustomToast.printAppLog("Response MID " + HexStringConverter.bytes2HexString(headerData));
//                    // String totalData = new String(bbMagT2, "ISO-8859-1");

                }

                break;
            case 58:
                /*Reserved national */

                /*this field enabled in(sale complition,refund,sale with cash,cash,sale,pre auth,reversal,void,tip,adjustment,emi sale)*/
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);

                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog(HexStringConverter.bufferToHex(bbMagT2));
                    String totalData = new String(bbMagT2, StandardCharsets.ISO_8859_1);
                    isoPackageReader.setMessage(totalData);
                }
                break;
            case 59:

                /* Terminal RSA Key */
                /*this field enable in(Key Exchange)*/
                if (isRequestType) {
                    //if(transactionType==TransactionTypeValues.)
                    s = isoFiledLength.getFieldValues();
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    //responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog("Advice/reason code (private reserved) " + HexStringConverter.bufferToHex(bbMagT2));
                    isoPackageReader.field59 = bbMagT2;
                }
                break;
            case 60:
                /*this field enable except(Key Exchange)*/
                if (isRequestType) {
                    s = isoFiledLength.getFieldValues();
                    length = length + s.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(s.length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(s);
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    isoPackageReader.fiield60 = bbMagT2;
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog("Advice/reason code (private reserved) " + HexStringConverter.bufferToHex(bbMagT2));
                    //isoPackageReader.reasionCode = new String(bbMagT2, "ISO-8859-1");
                    if (transactionType == TransactionTypeValues.INIT) {
                        splitsSubFields(isoPackageReader.fiield60);
                    }
                }


                /* Reserved national (Ex: This field may be used as Advise Reason Code: Batch Number - Settlement Request, Original Txn Amount - Advice Txns, Original MTI + Original RRN + Original STAN for Batch Upload, etc) */
                break;
            case 61:
                /*this field enable for All */

                if (isRequestType) {
                    String data61 = isoFiledLength.getFieldValues();
                    length = length + data61.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues().length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(isoFiledLength.getFieldValues());
                } else {
                    byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    isoPackageReader.setField61(bbMagT2);
                }

                break;
            case 62:
                /*this field enable except(Balance enquiry,init,key exchange,app update) */
                if (isRequestType) {
                    String data61 = isoFiledLength.getFieldValues();
                    length = length + data61.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues().length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(isoFiledLength.getFieldValues());
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 6);
                    responseData = Arrays.copyOfRange(responseData, 6, responseData.length);
                    CustomToast.printAppLog(" Reserved private " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.reasionCode = HexStringConverter.bytes2HexString(headerData);
                }
                /* Reserved private (Invoice Number - Transactions, TPK Key in Key Exchange Txns, etc) */
                break;
            case 63:
                /*this field enable in(settlement,init,key exchange,app update)*/
                if (isRequestType) {
                    String data2 = isoFiledLength.getFieldValues();
                    //String data13 = data2 + data2.length();
                    length = length + data2.length() + 2;
                    prefixedValue = HexStringConverter.addPreFixer(String.valueOf(isoFiledLength.getFieldValues().length()), 4);
                    stringBuilder.append(HexStringConverter.byteShiftOperation(prefixedValue, 2));
                    stringBuilder.append(isoFiledLength.getFieldValues());
                } else {
                    byte[] headerData = Arrays.copyOfRange(responseData, 0, 2);
                    int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(headerData));
                    responseData = Arrays.copyOfRange(responseData, 2, responseData.length);
                    byte[] bbMagT2 = new byte[mesgLength];
                    System.arraycopy(responseData, 0, bbMagT2, 0, mesgLength);
                    responseData = Arrays.copyOfRange(responseData, mesgLength, responseData.length);
                    CustomToast.printAppLog(" Reserved private " + HexStringConverter.bytes2HexString(headerData));
                    isoPackageReader.setGetField63(bbMagT2);
                }
                break;
            case 64:

                break;
        }


    }

    private void splitsSubFields(byte[] fiield60) {
        //byte[] messageLength = Arrays.copyOfRange(responseData, 0, 2);
        fiield60 = Arrays.copyOfRange(fiield60, 2, fiield60.length);
        byte[] messageLength = Arrays.copyOfRange(fiield60, 0, 2);
        int mesgLength = Integer.parseInt(HexStringConverter.bufferToHex(messageLength));
        fiield60 = Arrays.copyOfRange(fiield60, 2, fiield60.length);
        isoPackageReader.byteCounter = Arrays.copyOfRange(fiield60, 0, 8);
        fiield60 = Arrays.copyOfRange(fiield60, 8, fiield60.length);
        isoPackageReader.partialFileName = Arrays.copyOfRange(fiield60, 0, 6);
        fiield60 = Arrays.copyOfRange(fiield60, 6, fiield60.length);
        isoPackageReader.fullFileName = Arrays.copyOfRange(fiield60, 0, 10);
        fiield60 = Arrays.copyOfRange(fiield60, 10, fiield60.length);
        isoPackageReader.appFullSize = Arrays.copyOfRange(fiield60, 0, 4);
        fiield60 = Arrays.copyOfRange(fiield60, 4, fiield60.length);
        isoPackageReader.noOfSplits = Arrays.copyOfRange(fiield60, 0, 2);
        fiield60 = Arrays.copyOfRange(fiield60, 2, fiield60.length);
        isoPackageReader.crc = Arrays.copyOfRange(fiield60, 0, 12);
        fiield60 = Arrays.copyOfRange(fiield60, 12, fiield60.length);

        byte[] dataLength = Arrays.copyOfRange(fiield60, 0, 2);

        isoPackageReader.field60DataLength = Integer.parseInt(HexStringConverter.bufferToHex(dataLength));
        //  isoPackageReader.crc=Arrays.copyOfRange(fiield60, 0, 12);
        fiield60 = Arrays.copyOfRange(fiield60, 2, fiield60.length);
        isoPackageReader.fiield60ActualData = fiield60;

    }
}




