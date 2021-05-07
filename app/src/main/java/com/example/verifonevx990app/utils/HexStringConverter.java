package com.example.verifonevx990app.utils;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.verifonevx990app.customui.CustomToast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Observable;


public class HexStringConverter extends Observable {


    private static final char[] kHexChars = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    HexStringConverter() {
    }

    public static String bufferToHex(byte[] buffer) {
        return HexStringConverter.bufferToHex(buffer, 0, buffer.length);
    }

//    public static String stringToHex(String s) {
//        byte[] stringBytes = s.getBytes();
//
//        return HexStringConverter.bufferToHex(stringBytes);
//    }


    public static String bufferToHex(byte[] buffer, int startOffset, int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;

        for (int i = startOffset; i < endOffset; i++) {
            HexStringConverter.appendHexPair(buffer[i], hexString);
        }

        return hexString.toString();
    }

    public static String hexToString(String hexString)
            throws NumberFormatException, UnsupportedEncodingException {
        byte[] bytes = HexStringConverter.hexToBuffer(hexString);

        return new String(bytes, StandardCharsets.ISO_8859_1);//,
    }

    public static int bytes2Int(byte[] data) {
        if (data == null || data.length == 0) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < data.length; i++) {
            total += (data[i] & 0xff) << (data.length - i - 1) * 8;
        }
        return total;
    }

    private static byte[] hexToBuffer(String hexString)
            throws NumberFormatException {
        int length = hexString.length();
        byte[] buffer = new byte[(length + 1) / 2];
        boolean evenByte = true;
        byte nextByte = 0;
        int bufferOffset = 0;

        if ((length % 2) == 1) {
            evenByte = false;
        }

        for (int i = 0; i < length; i++) {
            char c = hexString.charAt(i);

            int nibble;
            if ((c >= '0') && (c <= '9')) {
                nibble = c - '0';
            } else if ((c >= 'A') && (c <= 'F')) {
                nibble = c - 'A' + 0x0A;
            } else if ((c >= 'a') && (c <= 'f')) {
                nibble = c - 'a' + 0x0A;
            } else {
                throw new NumberFormatException("Invalid hex digit '" + c
                        + "'.");
            }

            if (evenByte) {
                nextByte = (byte) (nibble << 4);
            } else {
                nextByte += (byte) nibble;
                buffer[bufferOffset++] = nextByte;
            }

            evenByte = !evenByte;
        }

        return buffer;
    }

    private static void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];
        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    //retrive binary  data and return String
    public static String binaryToString(byte[] binarry) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder resultString = new StringBuilder();
        for (byte aBinarry : binarry) {
            stringBuilder.append(aBinarry);
            if (stringBuilder.length() == 4) {
                int charCode = Integer.parseInt(stringBuilder.toString(), 2);
                //String str = Integer.toHexString(charCode);
                resultString.append((char) charCode);
                stringBuilder = new StringBuilder();
            }

        }
        return resultString.toString();
    }

//    public static CharSequence bufferToHex(StringBuffer mOutStringBuffer) {
//        return null;
//    }

    public static String hexDump(byte[] messageLength) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : messageLength) {
            stringBuilder.append(String.format("%02X", b));
        }
        return stringBuilder.toString();
    }

    public static String hexDump(String messageLength) {
        byte[] messLength = messageLength.getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : messLength) {
            stringBuilder.append(String.format("%02X", b));
        }
        return stringBuilder.toString();
    }

    //private static final char[] HEX_CHARS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    //to check required size in byte to crate packate
    public static boolean checkFieldByteSize(String inputValue, int size) {
        return inputValue.getBytes().length == size;
    }

    //adding 0 as prefix
    public static String addPreFixer(String s, int requiredLength) {
        //int iteration=requiredLength
        StringBuilder sBuilder = new StringBuilder(s);
        while (sBuilder.length() < requiredLength) {
            sBuilder.insert(0, "0");
        }
        s = sBuilder.toString();
        return s;
    }

    public static String getSubString(String s, int requiredLength) {
        int fromL = 0;
        if (s.length() > requiredLength)
            fromL = s.length() - requiredLength;
        return s.substring(fromL);
        //int iteration=requiredLength
//        StringBuilder sBuilder = new StringBuilder(s);
//        while (sBuilder.length() < requiredLength) {
//            sBuilder.insert(0, "0");
//        }
//        s = sBuilder.toString();

    }

    //    public String hexToString1(String args) {
//        //String hex = "75546f7272656e745c436f6d706c657465645c6e667375635f6f73745f62795f6d757374616e675c50656e64756c756d2d392c303030204d696c65732e6d7033006d7033006d7033004472756d202620426173730050656e64756c756d00496e2053696c69636f00496e2053696c69636f2a3b2a0050656e64756c756d0050656e64756c756d496e2053696c69636f303038004472756d2026204261737350656e64756c756d496e2053696c69636f30303800392c303030204d696c6573203c4d757374616e673e50656e64756c756d496e2053696c69636f3030380050656e64756c756d50656e64756c756d496e2053696c69636f303038004d50330000";
//        StringBuilder output = new StringBuilder();
//        for (int i = 0; i < args.length(); i += 2) {
//            String str = args.substring(i, i + 2);
//            output.append((char) Integer.parseInt(str, 16));
//        }
//        System.out.println(output);
//        return output.toString();
//    }
//adding 0's at prefix in integer values
    @SuppressLint("DefaultLocale")
    public static String intToHex(int nii) {
        return String.format("%04d", nii);
    }

//    public String hexToStringData(String hexString) {
//        String ch = null;
//        char Data1, Data2;
//        for (int i = 0; i < hexString.length() / 2; i++) {
//            Data1 = hexString.charAt(2 * i);
//            Data2 = hexString.charAt(2 * i + 1);
//            if ((Data1 > 0x46) || (Data1 < 0x30) ||
//                    (Data2 > 0x46) || (Data2 < 0x30)) {
//                //memset(pTarget, 0x00 ,Bytes);
//                //free(pSource);
//                //return ;
//                continue;
//            }
//            if (Data1 >= 0x41)
//                Data1 = (char) (Data1 - 0x37);
//            else
//                Data1 = (char) (Data1 - 0x30);
//
//            if (Data2 >= 0x41)
//                Data2 = (char) (Data2 - 0x37);
//            else
//                Data2 = (char) (Data2 - 0x30);
//            if (ch == null)
//                ch = String.valueOf((Data1 << 4) | Data2);
//            else
//                ch = ch + String.valueOf((Data1 << 4) | Data2);
//        }
//        return ch;
//
//    }

    ////Create Nibble(1 byte to 1/2 byte )
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String byteShiftOperation(String inputValues, int reqiuredSize) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        byte[] data = inputValues.getBytes(StandardCharsets.ISO_8859_1);//("ISO-8859-1");//US_ASCII
        int[] result = new int[reqiuredSize];
        int j = 0;
        for (int i = 0; i < data.length / 2; i++) {
            byte b = data[2 * i];
            int high = ((b & 0xf) << 4);
            // i = i + 1;
            b = data[2 * i + 1];
            int low = (b & 0xf);
            result[j] = (high | low);
            char ch = (char) result[j];
            stringBuilder.append(ch);
            j++;

        }

        return stringBuilder.toString();
    }

    public static byte[] hexString2Bytes(String data) {
        byte[] result = new byte[(data.length() + 1) / 2];
        if ((data.length() & 0x1) == 1) {
            data = data + "0";
        }
        for (int i = 0; i < result.length; i++) {
            result[i] = ((byte) (hex2byte(data.charAt(i * 2 + 1)) | hex2byte(data.charAt(i * 2)) << 4));
        }
        return result;
    }


    private static byte hex2byte(char hex) {
        if ((hex <= 'f') && (hex >= 'a')) {
            return (byte) (hex - 'a' + 10);
        }

        if ((hex <= 'F') && (hex >= 'A')) {
            return (byte) (hex - 'A' + 10);
        }

        if ((hex <= '9') && (hex >= '0')) {
            return (byte) (hex - '0');
        }

        return 0;
    }

    public static byte[] merage(byte[][] data) {
        int len = 0;
        for (byte[] aData : data) {
            if (aData == null) {
                throw new IllegalArgumentException("");
            }
            len += aData.length;
        }

        byte[] newData = new byte[len];
        len = 0;
        for (byte[] d : data) {
            System.arraycopy(d, 0, newData, len, d.length);
            len += d.length;
        }
        return newData;
    }

    public static String bytes2HexString(byte[] data) {
        if (data == null)
            return "";
        StringBuilder buffer = new StringBuilder();
        for (byte b : data) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                buffer.append('0');
            }
            buffer.append(hex);
        }
        return buffer.toString().toUpperCase();
    }

    public static byte[] subBytes(byte[] data, int offset, int len) {
        if ((offset < 0) || (data.length <= offset)) {
            return null;
        }

        if ((len < 0) || (data.length < offset + len)) {
            len = data.length - offset;
        }

        byte[] ret = new byte[len];

        System.arraycopy(data, offset, ret, 0, len);
        return ret;
    }

    public static String bcd2Ascii(final byte[] bcd) {
        if (bcd == null)
            return "";
        StringBuilder sb = new StringBuilder(bcd.length << 1);
        for (byte ch : bcd) {
            byte half = (byte) (ch >> 4);
            sb.append((char) (half + ((half > 9) ? ('A' - 10) : '0')));
            half = (byte) (ch & 0x0f);
            sb.append((char) (half + ((half > 9) ? ('A' - 10) : '0')));
        }
        return sb.toString();
    }

    public static String fromUtf8(byte[] data) {
        return fromBytes(data, "UTF-8");
    }

    private static String fromBytes(byte[] data, String charsetName) {
        try {
            return new String(data, charsetName);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String fromBytes(byte[] data) {
        return fromBytes(data, "ISO-8859-1");
    }

    public static byte[] int2Bytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static String str2HexStr(String str) {
        final char[] mChars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder();
        byte[] bs = str.getBytes();

        for (byte b : bs) {
            sb.append(mChars[(b & 0xFF) >> 4]);
            sb.append(mChars[b & 0x0F]);
        }
        return sb.toString().trim();
    }

    public static byte[] ASCII_To_BCD(byte[] ascii, int asc_len) {
        byte[] bcd = new byte[asc_len / 2];
        int j = 0;
        for (int i = 0; i < (asc_len + 1) / 2; i++) {
            bcd[i] = asc_to_bcd(ascii[j++]);
            bcd[i] = (byte) (((j >= asc_len) ? 0x00 : asc_to_bcd(ascii[j++])) + (bcd[i] << 4));
        }
        return bcd;
    }

    public static byte[] ascii2Bcd(String ascii) {
        if (ascii == null)
            return null;
        if ((ascii.length() & 0x01) == 1)
            ascii = "0" + ascii;
        byte[] asc = ascii.getBytes();
        byte[] bcd = new byte[ascii.length() >> 1];
        for (int i = 0; i < bcd.length; i++) {
            bcd[i] = (byte) (hex2byte((char) asc[2 * i]) << 4 | hex2byte((char) asc[2 * i + 1]));
        }
        return bcd;
    }

    private static byte asc_to_bcd(byte asc) {
        byte bcd;

        if ((asc >= '0') && (asc <= '9'))
            bcd = (byte) (asc - '0');
        else if ((asc >= 'A') && (asc <= 'F'))
            bcd = (byte) (asc - 'A' + 10);
        else if ((asc >= 'a') && (asc <= 'f'))
            bcd = (byte) (asc - 'a' + 10);
        else
            bcd = (byte) (asc - 48);
        return bcd;
    }


    String byteArrayToHex(byte[] s) {
        StringBuilder sb = new StringBuilder();
        try {

            for (byte b : s) {
                sb.append(String.format("%02X", b));
            }
            CustomToast.printAppLog(sb.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}