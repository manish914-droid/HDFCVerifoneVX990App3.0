package com.example.verifonevx990app.emv;

import android.util.Log;
import android.util.SparseArray;

import com.example.verifonevx990app.utils.Utility;

import java.util.Arrays;
import java.util.HashMap;


/**
 * Created by Simon on 2018/8/23.
 */

public class ISO8583 {
    protected static final int TYPE_BCD = 1;
    protected static final int TYPE_ASC = 2;
    protected static final int TYPE_BIN = 4;
    // 8
    // 16
    protected static final int TYPE_LEN = 32;
    protected static final int TYPE_LEN_LEN = 64;
    // 128
    protected static final int TYPE_FILL_SPACE_RIGHT = 128;
    // 256
    protected static final int TYPE_FILL_SPACE_LEFT = 256;
    // 512
    protected static final int TYPE_FILL_ZERO_RIGHT = 512;
    // 1024
    protected static final int TYPE_FILL_ZERO_LEFT = 1024;
    protected static final int TYPE_L_BCD = TYPE_LEN + TYPE_BCD;
    protected static final int TYPE_LL_BCD = TYPE_LEN_LEN + TYPE_BCD;
    protected static final int TYPE_L_ASC = TYPE_ASC + TYPE_LEN;
    protected static final int TYPE_LL_ASC = TYPE_ASC + TYPE_LEN_LEN;
    protected static final int TYPE_L_BIN = TYPE_BIN + TYPE_LEN;
    protected static final int TYPE_LL_BIN = TYPE_BIN + TYPE_LEN_LEN;
    protected static final int TYPE_ASC_FS = TYPE_ASC + TYPE_FILL_SPACE_RIGHT;
    protected static final int ATTR_INDEX_TYPE = 0;
    protected static final int ATTR_INDEX_LEN_DEFAULT = 1;
    protected static final int BITMAP_FIELD_INDEX_ = 1;
    protected static final int ISO_BIT_MAX = 64;
    private static final String TAG = "EMVDemo-8583";
    public static HashMap<Integer, String> fieldhasmap = new HashMap<>();
    public boolean[] unpackValidField;
    protected String header;
    protected String tail;
    protected int[][] attribute_array = null;
    boolean[] validField;
    byte[][] allField;
    EMVTLVParam emvtlvF55 = null;
    SparseArray<String> isoData;

    public ISO8583() {
        validField = new boolean[ISO_BIT_MAX + 1];
        Arrays.fill(validField, false);

        allField = new byte[ISO_BIT_MAX + 1][];
        Arrays.fill(allField, null);
    }

    protected byte[] calculateMac(byte[] packet, int offset, int length) {
        return null;
    }

    protected int getHeaderLen() {
        return 0;
    }

    /**
     * \brief set the field of ISO data refer the format #attribute_array
     *
     * @param field the field index, 0 for message type, 1 for bitmap(bitmap should not be set manually)
     * @param value the readable String value in ASC/BCD format for the field
     * @return the field value, include the length header (refer the format ), null for invalid value given.
     */
    public byte[] setField(int field, String value) {
        if (field > ISO_BIT_MAX) {
            return null;
        }

        if (null == attribute_array) {
            Log.e(TAG, "no attribute_array set!");
            return null;
        }
        if (value.length() == 0) {
            Log.e(TAG, "invalid value(len:0) of field:" + field);
            return null;
        }
        byte[] bytes = null;
        int len = 0;
        String trimedValue;
        if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_BCD)) {
            // is bcd
            trimedValue = value.replace(" ", "");
            len = trimedValue.length();
            bytes = Utility.hexStr2Byte(trimedValue);
        } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_ASC)) {
            // is ASC
            len = value.length();
            bytes = value.getBytes();
        } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_BIN)) {
            // is bin
            trimedValue = value.replace(" ", "");
            len = trimedValue.length();
            len /= 2;
            bytes = Utility.hexStr2Byte(trimedValue);
        } else {
            Log.e(TAG, "type " + Integer.toHexString(attribute_array[field][ATTR_INDEX_TYPE]) + " invalided");
            validField[field] = false;

            return null;
        }

        return setField(field, bytes, len);
    }

    /**
     * \brief set the ISO field given byte , will insert the length refer the  format #attribute_array
     *
     * @param field the field index, 0 for message, 1 for bitmap (should be set automatic)
     * @param value the byte value of the field, will insert the length refer the  format #attribute_array
     * @return the field value, null for invalid value
     */
    public byte[] setField(int field, byte[] value, int len) {
        if (field > ISO_BIT_MAX) {
            return null;
        }

        if (null == attribute_array) {
            Log.e(TAG, "no attribute_array set!");
            return null;
        }
//        String valueTrimed = new String(value);
        byte[] retBytes = null;

        int wantLen = attribute_array[field][ATTR_INDEX_LEN_DEFAULT];
        int type = attribute_array[field][ATTR_INDEX_TYPE];

        byte[] lenHeader = new byte[2];
        if (0 != (type & TYPE_LEN_LEN)) {
            lenHeader[0] = Utility.HEX2DEC(len / 100);
            lenHeader[1] = Utility.HEX2DEC(len % 100);
            retBytes = new byte[value.length + 2];
            System.arraycopy(lenHeader, 0, retBytes, 0, 2);
            System.arraycopy(value, 0, retBytes, 2, value.length);


        } else if (0 != (type & TYPE_LEN)) {
            lenHeader[0] = Utility.HEX2DEC(len);
            retBytes = new byte[value.length + 1];
            System.arraycopy(lenHeader, 0, retBytes, 0, 1);
            System.arraycopy(value, 0, retBytes, 1, value.length);

        } else if (len == wantLen) {
            retBytes = value;
        } else {
            retBytes = null;
        }

        if (retBytes == null) {
            Log.e(TAG, "type " + Integer.toHexString(attribute_array[field][ATTR_INDEX_TYPE]) + ", len " + value.length + " of field: " + field + " invalid :" + wantLen);
            validField[field] = false;
        } else {
            allField[field] = retBytes;
            validField[field] = true;
            Log.d(TAG, "save field:" + field + ", type " + Integer.toHexString(attribute_array[field][ATTR_INDEX_TYPE]) + ", len:" + value.length + ", value:" + Utility.byte2HexStr(retBytes));
        }
        return retBytes;
    }

    public byte[] getField(int field) {
        if (null == allField[field]) {
            return null;
        }
        int len = allField[field].length;
        byte[] tmp;

        if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_LEN_LEN)) {
            tmp = new byte[len - 2];
            System.arraycopy(allField[field], 2, tmp, 0, len - 2);
        } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_LEN)) {
            tmp = new byte[len - 1];
            System.arraycopy(allField[field], 1, tmp, 0, len - 1);
        } else {
            tmp = allField[field];
        }
        return tmp;
    }

    public byte[] getPacket(String header, String tail, PACKET_TYPE type) {
        byte[] h;
        byte[] t;
        if (null != header) {
            h = Utility.hexStr2Byte(header);
        } else {
            h = null;
        }

        if (null != tail) {
            t = Utility.hexStr2Byte(tail);
        } else {
            t = null;
        }

        return getPacket(h, t, type);
    }

    public byte[] getPacket(byte[] header, byte[] tail, PACKET_TYPE type) {
        byte[] tmp = new byte[4096];
        byte[] bitmap;
        int offset = 0;
        int len = 0;
        if (null != header) {
            len = header.length;
            System.arraycopy(header, 0, tmp, 0, len);
            offset += len;
        }
        if (null != emvtlvF55) {
            //
            setField(55, emvtlvF55.getTlvString());
        }
        // validField
        if (false == validField[1]) {
            Log.d(TAG, "calculate the bitmap:");
            bitmap = new byte[ISO_BIT_MAX >> 3];
            for (int i = 1; i <= ISO_BIT_MAX; i++) {
                if (validField[i]) {
                    bitmap[(i - 1) >> 3] |= 1;
                }
                if (0 != (i & 0x07)) {
                    bitmap[(i - 1) >> 3] = (byte) (bitmap[(i - 1) >> 3] << 1);
                }
            }
            Log.d(TAG, Utility.byte2HexStr(bitmap));

            setField(1, bitmap, bitmap.length);
        }


        for (int i = 0; i <= ISO_BIT_MAX; i++) {
            if (validField[i]) {
                len = allField[i].length;
                System.arraycopy(allField[i], 0, tmp, offset, len);
                offset += len;

                Log.d(TAG, "set field " + i + ", len:" + len + ", value:" + Utility.byte2HexStr(allField[i]));
                fieldhasmap.put(i, Utility.byte2HexStr(allField[i]));
            } else {
            }
        }

        if (false == validField[ISO_BIT_MAX]) {
            byte[] mac = calculateMac(tmp, header.length, offset);
            if (null != mac) {
                Log.d(TAG, "get mac:" + Utility.byte2HexStr(mac));
                len = 8;
                System.arraycopy(mac, 0, tmp, offset, len);
                offset += len;
            } else {
                Log.e(TAG, "calculate mac fails");
            }
        }

        if (null != tail) {
            len = tail.length;
            System.arraycopy(tail, 0, tmp, 0, len);
            offset += len;
        }
        len = offset;
        if (type == PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF) {
            len += 2;
        }
        byte[] packet = new byte[len];
        if (type == PACKET_TYPE.PACKET_TYPE_NONE) {
            System.arraycopy(tmp, 0, packet, 0, offset);
        } else if (type == PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF) {
            System.arraycopy(tmp, 0, packet, 2, offset);
            packet[0] = (byte) (offset / 256);
            packet[1] = (byte) (offset % 256);
            Log.d(TAG, "Len:" + offset + "buf size:" + packet.length);
        } else {
            return null;
        }

        Log.d(TAG, Utility.byte2HexStr(packet));

        return packet;

    }

    public boolean unpack(byte[] packet) {
        return unpack(packet, 0);
    }

    public String getUnpack(int field) {
        int a = field;
        if (a >= 200) {
            a -= 200;
        }
        if (unpackValidField[a]) {
            return isoData.get(field);
        } else {
            return null;
        }
    }

    public boolean unpack(byte[] packet, int offset) {
        int headerLen = getHeaderLen();
        int index = offset + headerLen;
        int fieldOffset = 0;

        Log.d(TAG, "unpack:" + Utility.byte2HexStr(packet));

        isoData = new SparseArray<>();
        unpackValidField = new boolean[ISO_BIT_MAX + 1];

        Log.d(TAG, "message type:" + Utility.byte2HexStr(packet, index, 2));

        isoData.put(0, Utility.byte2HexStr(packet, index, 2));
        unpackValidField[0] = true;

        index += 2;
        Log.d(TAG, "Index:" + index);
        Log.d(TAG, "packet:" + Utility.byte2HexStr(packet, index, 32));
        int fieldMark = 0;
        fieldOffset = index + 8;
        --index;
        for (int field = 1; field <= ISO_BIT_MAX; field++) {
            if (fieldMark == 0) {
                ++index;
                fieldMark = 0x0080;
                Log.d(TAG, "bitmap:" + Integer.toHexString((int) packet[index]));
            }
            if (0 != (fieldMark & packet[index])) {
                unpackValidField[field] = true;
                Log.d(TAG, "Mark Field:" + field);

                int length = attribute_array[field][ATTR_INDEX_LEN_DEFAULT];
                if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_LEN)) {
                    // one byte length
                    length = Utility.DEC2INT(packet[fieldOffset]);
                    ++fieldOffset;
                } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_LEN_LEN)) {
                    length = Utility.DEC2INT(packet[fieldOffset]);
                    length *= 100;
                    ++fieldOffset;
                    length += Utility.DEC2INT(packet[fieldOffset]);
                    ++fieldOffset;
                }
                Log.d(TAG, "try read field:" + field + ", type:" + Integer.toHexString(attribute_array[field][ATTR_INDEX_TYPE]) + ", Length:" + length);

                if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_BCD)) {
                    // is bcd
                    if (1 == (length & 1)) {
                        ++length;
                    }
                    length = (length >> 1);
                    isoData.put(field, Utility.byte2HexStr(packet, fieldOffset, length));
                    fieldOffset += length;
                } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_ASC)) {
                    // is ASC
                    isoData.put(field, new String(packet, fieldOffset, length));
                    isoData.put(field + 200, Utility.byte2HexStr(packet, fieldOffset, length));
                    fieldOffset += length;
                } else if (0 != (attribute_array[field][ATTR_INDEX_TYPE] & TYPE_BIN)) {
                    if (1 == (length & 1)) {
                        ++length;
                    }

                    isoData.put(field, Utility.byte2HexStr(packet, fieldOffset, length));
                    fieldOffset += length;
                }

                Log.d(TAG, "set field:" + field + ", type:" + Integer.toHexString(attribute_array[field][ATTR_INDEX_TYPE]) + ", Length:" + length + ", value:" + isoData.get(field));

            } else {
                unpackValidField[field] = false;
            }
            fieldMark = (fieldMark >> 1);
        }
        Log.d(TAG, "Index:" + index);
        return true;
    }

    public byte[] appendF55(int tag, String value) {
        if (null == emvtlvF55) {
            emvtlvF55 = new EMVTLVParam();
        }
        String tlv = emvtlvF55.append(tag, value);
        return Utility.hexStr2Byte(tlv);
    }

    public byte[] appendF55(String value) {
        return appendF55(value.getBytes());
    }

    public byte[] appendF55(int tag, byte[] value) {
        return appendF(55, tag, value);
    }

    public byte[] appendF55(byte[] TLV) {
        return appendF(55, TLV);
    }

    public byte[] appendF(int field, int tag, byte[] value) {
        byte[] TLV;
        byte[] bLength = new byte[2 + 3];
        String sTag = Integer.toHexString(tag).toUpperCase();
        int offset = 0;
        int lenTag;
        int lenLen;

        int len = value.length;
        if (len <= 0x7F) {
            bLength[offset] = (byte) len;
            ++offset;
        } else if (len <= 0xFF) {
            bLength[offset] = (byte) 0b10000001;
            ++offset;
            bLength[offset] = (byte) (0b10000000 | (len & 0x7F));
            ++offset;
        } else {
            Log.e(TAG, "invalid length:" + len + " of TAG:" + Integer.toHexString(tag) + " to Field:" + field);
            return null;
        }
        lenTag = sTag.length();
        lenLen = offset;
        lenTag /= 2;

        TLV = new byte[lenTag + lenLen + value.length];

        System.arraycopy(Utility.hexStr2Byte(sTag), 0, TLV, 0, lenTag);
        offset = lenTag;
        System.arraycopy(bLength, 0, TLV, offset, lenLen);
        offset += lenLen;
        System.arraycopy(value, 0, TLV, offset, value.length);
        Log.d(TAG, "TLV:" + Utility.byte2HexStr(TLV));

        return appendF(field, TLV);
    }

    public byte[] appendF(int field, byte[] TLV) {
        if (TLV.length <= 0) {
            return null;
        }
        validField[field] = true;
        if (null != allField[field]) {
            byte[] tmp = getField(field);
            byte[] all = new byte[tmp.length + TLV.length];
            System.arraycopy(tmp, 0, all, 0, tmp.length);
            System.arraycopy(TLV, 0, all, tmp.length, TLV.length);
            setField(field, all, all.length);
        } else {
            setField(field, TLV, TLV.length);
        }

        return TLV;

    }

    public byte[] makePacket(SparseArray<String> data, PACKET_TYPE type) {
        int fieldIndex;
        String fieldValue;
        byte[] tmp;

        for (int i = 0; i < data.size(); i++) {
            fieldIndex = data.keyAt(i);
            fieldValue = data.valueAt(i);
            tmp = setField(fieldIndex, fieldValue);
            if (tmp == null) {
                Log.e(TAG, "error of index:" + fieldIndex + ", value:" + fieldValue);
            }
        }

        return getPacket(header, tail, type);
    }


    public enum PACKET_TYPE {
        PACKET_TYPE_NONE,
        PACKET_TYPE_HEXLEN_BUF,
    }

}
