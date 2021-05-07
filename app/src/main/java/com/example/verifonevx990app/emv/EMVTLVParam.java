package com.example.verifonevx990app.emv;


import com.example.verifonevx990app.vxUtils.ROCProviderV2;

public class EMVTLVParam extends TagLengthValue {
    private static final String TAG = "EMVTLVParam";
    public int flagAppendRemoveClear = 0;

    /**
     * @brief get the updateRID, updateAID operation for append, remove, clear
     */
    public int getFlagAppendRemoveClear() {
        return flagAppendRemoveClear;
    }

    /**
     * @brief get the updateRID, updateAID operation for append, remove, clear
     */
    public void setFlagAppendRemoveClear(int flagAppendRemoveClear) {
        this.flagAppendRemoveClear = flagAppendRemoveClear;
    }

    /**
     * @brief get the Length in the AID,RID Length format
     */
    @Override
    public String getLength(int length, int maxBytes) {

        String sLength = null;
        if (length <= 0) {

        } else if (length < 16) {
            sLength = "0" + Integer.toHexString(length).toUpperCase();
        } else if (length < 127) {
            // one byte length
            sLength = Integer.toHexString(length).toUpperCase();
        } else if (length < 256) {
            // 81+len
            sLength = "81" + Integer.toHexString(length).toUpperCase();

        } else if (length < 65535) {
            // 82 + len
        }

        return sLength;
    }

    @Override
    public int append(String tlv) {
        int count = 0;
        byte[] bTLV = ROCProviderV2.INSTANCE.hexStr2Byte(tlv);
        for (int offset = 0; offset < bTLV.length; ) {
            // read tag
            int tagLen = getTagLen(bTLV[offset]);
            String sTag = tlv.substring(offset * 2, offset * 2 + tagLen * 2);
            int tag = Integer.parseInt(sTag, 16);
            offset += (tagLen);
            // read length
            int lengthLen = getLengthLen(bTLV[offset]);
            if (lengthLen > 1) {
                offset += 1;
            }
            int length = Integer.parseInt(tlv.substring(offset * 2, offset * 2 + 2), 16);
            offset += 1;
            // read value
            String value = tlv.substring(offset * 2, offset * 2 + length * 2);
            offset += length;

            append(tag, value);
            ++count;
        }
        return count;
    }

    protected int getTagLen(byte tag1) {
        if ((tag1 & 31) == 31) {
            return 2;
        } else {
            return 1;
        }
    }

    protected int getLengthLen(byte len1) {
        if ((len1 & 0x81) == 0x81) {
            return 2;
        } else {
            return 1;
        }
    }

    public byte[] getTLV(int tag, byte[] value) {
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
            return null;
        }
        lenTag = sTag.length();
        lenLen = offset;
        lenTag /= 2;

        TLV = new byte[lenTag + lenLen + value.length];

        System.arraycopy(ROCProviderV2.INSTANCE.hexStr2Byte(sTag), 0, TLV, 0, lenTag);
        offset = lenTag;
        System.arraycopy(bLength, 0, TLV, offset, lenLen);
        offset += lenLen;
        System.arraycopy(value, 0, TLV, offset, value.length);

        return TLV;
    }
}
