package com.example.verifonevx990app.vxUtils;


import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;


public class RSAProvider {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String PUBLIC_KEY = "publicKey";
    private static final String PRIVATE_KEY = "privateKey";

    private static final String MODULES = "RSAModules";


    public static Map<String, Object> generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        int KEYSIZE = 1024;
        keyPairGen.initialize(KEYSIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        //keyPairGen.initialize(128);
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        BigInteger modules = privateKey.getModulus();
        Map<String, Object> keys = new HashMap<>(5);
        keys.put(PUBLIC_KEY, publicKey);
        keys.put(PRIVATE_KEY, privateKey);
        keys.put(MODULES, modules);

        return keys;
    }


    public static String getPublicKeyBytes(Map<String, Object> keys) throws Exception {
        RSAPublicKey key = (RSAPublicKey) keys.get(PUBLIC_KEY);
        assert key != null;
        String data = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
        byte[] data1 = Base64.decode(data, Base64.DEFAULT);
        Log.e("public key", Converter.byteArr2HexStr(data1));
        Log.e("public key", Base64.encodeToString(key.getEncoded(), Base64.DEFAULT));
        return (Converter.byteArr2HexStr(data1));///"308189028181" + HexStringConverter.hexDump(key.getModulus().toByteArray()) + "0203" + HexStringConverter.hexDump(key.getPublicExponent().toByteArray());//HexStringConverter.hexDump(CustomBase64.decode(key.getEncoded()));
    }

    public static RSAPrivateKey getPrivetKeys(Map<String, Object> keys) throws Exception {
        return (RSAPrivateKey) keys.get(PRIVATE_KEY);
    }

    public static String getHexPrivateKey(RSAPrivateKey privateKey) {
        return Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
    }


    public static String[] decriptTMK(final byte[] encryptedBytes, Map<String, Object> keys) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String[] encryptedData = new String[2];
        byte[] dataFirst = new byte[encryptedBytes.length / 2];
        byte[] dataSecond = new byte[encryptedBytes.length / 2];
        System.arraycopy(encryptedBytes, 0, dataFirst, 0, encryptedBytes.length / 2);
        System.arraycopy(encryptedBytes, encryptedBytes.length / 2, dataSecond, 0, encryptedBytes.length / 2);
        Key key = (Key) keys.get(PRIVATE_KEY);
        Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher1.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedFirst = cipher1.doFinal(dataFirst);
        byte[] decryptedBytesSecond = cipher1.doFinal(dataSecond);
//        Log.e(" Data : ", " decripted mess" + ByteUtil.bufferToHex(decryptedFirst));
        encryptedData[0] = ByteUtil.bufferToHex(decryptedFirst);
        encryptedData[1] = ByteUtil.bufferToHex(decryptedBytesSecond);
        return encryptedData;
    }

    public static String[] decryptPPKDPK(final byte[] encryptedBytes, String tmk) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException {
        String[] encryptedData = new String[2];
        int MAX_KEY_LENGTH = DESedeKeySpec.DES_EDE_KEY_LEN;
        byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] dataFirst = new byte[encryptedBytes.length / 2];
        byte[] dataSecond = new byte[encryptedBytes.length / 2];
        System.arraycopy(encryptedBytes, 0, dataFirst, 0, encryptedBytes.length / 2);
        System.arraycopy(encryptedBytes, encryptedBytes.length / 2, dataSecond, 0, encryptedBytes.length / 2);
        byte[] key = padKeyToLength(tmk.getBytes(StandardCharsets.UTF_8), MAX_KEY_LENGTH);
        DESedeKeySpec skeySpec = new DESedeKeySpec(key);
        SecretKey keyFactory = SecretKeyFactory.getInstance("DESede").generateSecret(skeySpec);
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        // reinitialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, keyFactory, new IvParameterSpec(iv));
        //cipher.init(Cipher.DECRYPT_MODE, keyFactory,iv);
        byte[] decrypted = cipher.doFinal(dataFirst);
        byte[] decryptedSecond = cipher.doFinal(dataSecond);
        encryptedData[0] = Converter.byteArr2HexStr(decrypted);
        encryptedData[1] = Converter.byteArr2HexStr(decryptedSecond);
        Log.e("Decrypted Data", encryptedData[0]);
        return encryptedData;

    }

    private static byte[] padKeyToLength(byte[] key, int len) {
        byte[] newKey = new byte[len];
        System.arraycopy(key, 0, newKey, 0, Math.min(key.length, len));
        return newKey;
    }

    public static String tripleDesDecrypt(byte[] key, byte[] data) {
        try {
            byte[] masterKey = decode(key);
            byte[] desKey = new byte[24];
            System.arraycopy(masterKey, 0, desKey, 0, 16);
            System.arraycopy(masterKey, 0, desKey, 16, 8);

            DESedeKeySpec keySpec = new DESedeKeySpec(desKey);

            SecretKey secretKey = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);

            Cipher ecipher = Cipher.getInstance("DESede/ECB/NoPadding");
            ecipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decryptedData = ecipher.doFinal(decode(data));
            return encodeHexString(decryptedData).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] calcKCVLocal(String key) {

        String strData = "0000000000000000";

        return tripleDesEncrypt(key, strData);
    }

    private static byte[] decode(byte[] hexString) {
        byte[] b = new byte[hexString.length / 2];
        char[] chars = new char[2];

        for (int i = 0; i < b.length; ++i) {
            chars[0] = (char) hexString[2 * i];
            chars[1] = (char) hexString[2 * i + 1];
            String hex = new String(chars);
            b[i] = (byte) Integer.parseInt(hex, 16);
        }

        return b;
    }

    private static String encodeHexString(byte[] data) {
        String hex = String.format("%016x", new BigInteger(1, data));
        if (hex.length() % 2 == 1) {
            hex = "0" + hex;
        }
        return hex;
    }


    private static byte[] tripleDesEncrypt(String key, String data) {
        try {
            byte[] masterKey = decode(key.getBytes());
            byte[] desKey = new byte[24];
            System.arraycopy(masterKey, 0, desKey, 0, 16);
            System.arraycopy(masterKey, 0, desKey, 16, 8);

            DESedeKeySpec keySpec = new DESedeKeySpec(desKey);
            SecretKey secretKey = SecretKeyFactory.getInstance("DESede").generateSecret(keySpec);

            Cipher ecipher = Cipher.getInstance("DESede/ECB/NoPadding");
            ecipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return ecipher.doFinal(decode(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}


class ByteUtil {
    private static final char[] kHexChars = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    //private static final String TAG = "[ByteUtil]";

    private ByteUtil() {
    }

    public static int bytecmp(byte[] hex1, byte[] hex2, int len) {
        for (int i = 0; i < len; i++) {
            if (hex1[i] != hex2[i]) {
                return 1;
            }
        }

        return 0;
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

    public static boolean checkFieldByteSize(String inputValue, int size) {
        return inputValue.getBytes().length == size;
    }

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

    /**
     * Merge byte array.
     */
    public static byte[] merge(byte[]... data) {
        if (data == null) {
            return null;
        }

        byte[] bytes = null;
        for (byte[] aData : data) {
            bytes = mergeBytes(bytes, aData);
        }

        return bytes;
    }

    /**
     * Sub bytes.
     */
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

    /**
     * Merge bytes.
     */
    public static byte[] mergeBytes(byte[] bytesA, byte[] bytesB) {
        if ((bytesA == null) || (bytesA.length == 0))
            return bytesB;
        if ((bytesB == null) || (bytesB.length == 0)) {
            return bytesA;
        }

        byte[] bytes = new byte[bytesA.length + bytesB.length];

        System.arraycopy(bytesA, 0, bytes, 0, bytesA.length);
        System.arraycopy(bytesB, 0, bytes, bytesA.length, bytesB.length);

        return bytes;
    }

    public static String intToHex(int nii) {
        return String.format(Locale.getDefault(), "%04d", nii);
    }

    private static byte hex2byte(char hex) {
        if (hex <= 'f' && hex >= 'a') {
            return (byte) (hex - 'a' + 10);
        }

        if (hex <= 'F' && hex >= 'A') {
            return (byte) (hex - 'A' + 10);
        }

        if (hex <= '9' && hex >= '0') {
            return (byte) (hex - '0');
        }

        return 0;
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

    public static byte[] hexString2Bytes(String data) {
        if (data == null)
            return null;
        byte[] result = new byte[(data.length() + 1) / 2];
        if ((data.length() & 1) == 1) {
            data += "0";
        }
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (hex2byte(data.charAt(i * 2 + 1)) | (hex2byte(data.charAt(i * 2)) << 4));
        }
        return result;
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

    public static String bufferToHex(byte[] buffer) {
        return ByteUtil.bufferToHex(buffer, 0, buffer.length);
    }

    public static String bufferToHex(byte[] buffer, int startOffset, int length) {
        StringBuffer hexString = new StringBuffer(2 * length);
        int endOffset = startOffset + length;

        for (int i = startOffset; i < endOffset; i++) {
            appendHexPair(buffer[i], hexString);
        }

        return hexString.toString();
    }

    private static void appendHexPair(byte b, StringBuffer hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];
        hexString.append(highNibble);
        hexString.append(lowNibble);
    }

    public static String hexToString(String hexString)
            throws NumberFormatException, UnsupportedEncodingException {
        byte[] bytes = hexToBuffer(hexString);

        return new String(bytes, StandardCharsets.ISO_8859_1);//,
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


    public static String getSubString(String s, int requiredLength) {
        int fromL = 0;
        if (s.length() > requiredLength)
            fromL = s.length() - requiredLength;
        return s.substring(fromL);


    }

    /**
     * Change long type data to BCD bytes.
     */
    public static byte[] toBCDAmountBytes(long data) {
        byte[] bcd = {0, 0, 0, 0, 0, 0};
        byte[] bcdDou = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        if (data <= 0) {
            return bcd;
        }

        int i = bcdDou.length - 1;

        while (data != 0) {
            bcdDou[i] = (byte) (data % 10);
            data /= 10;
            i--;
        }

        for (i = bcd.length - 1; i >= 0; i--) {
            bcd[i] = (byte) (((bcdDou[i * 2 + 1] & 0x0f)) | ((bcdDou[i * 2] << 4) & 0xf0));
        }

        return bcd;
    }

    /**
     * Change int type data to four places byte array.
     */
    public static byte[] toFourByteArray(int i) {
        byte[] array = new byte[4];
        array[0] = (byte) (i >> 24 & 0x7F);
        array[1] = (byte) (i >> 16);
        array[2] = (byte) (i >> 8);
        array[3] = (byte) i;
        return array;
    }

    /**
     * Change byte to hex string data.
     */
    public static String byte2HexString(byte data) {
        StringBuilder buffer = new StringBuilder();
        String hex = Integer.toHexString(data & 0xFF);
        if (hex.length() == 1) {
            buffer.append('0');
        }
        buffer.append(hex);
        return buffer.toString().toUpperCase();
    }

    /**
     * Change byte array to GBK string data.
     */
    public static String fromGBK(byte[] data) throws UnsupportedEncodingException {
        return fromBytes(data, "GBK");
    }

    /**
     * Change bytes to sring data.
     */
    public static String fromBytes(byte[] data, String charsetName) throws UnsupportedEncodingException {
        return new String(data, charsetName);
    }

    public static String fromUtf8(byte[] data) throws UnsupportedEncodingException {
        return fromBytes(data, "UTF-8");
    }

//    private static String fromBytes(byte[] data, String charsetName) {
//        try {
//            return new String(data, charsetName);
//        } catch (UnsupportedEncodingException e) {
//            return null;
//        }
//    }

    public static String fromBytesISO(byte[] data) throws UnsupportedEncodingException {
        return fromBytes(data, "ISO-8859-1");
    }
}
