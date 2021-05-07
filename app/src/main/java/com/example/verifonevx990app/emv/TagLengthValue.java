package com.example.verifonevx990app.emv;

/**
 * Created by Simon on 2018/8/31.
 *
 * @brief for get the TLV string
 * <p>
 * append each tag & value, and getTlvString to get the TLV string
 */
public class TagLengthValue {

    public String comment = "";
    protected String tlvString = "";
    protected DefaultTagValue[] defaultTagValue = null;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLength(int length, int maxBytes) {
        String sLength = Integer.toHexString(length);
        int len = sLength.length();

        if (len > (maxBytes << 1)) {
            return null;
        } else if (1 == (len & 0x01)) {
            //
            return "0" + sLength;
        } else {
            return sLength;
        }
    }

    public String append(int tag, String value) {
        if (null == value) {
            return null;
        }
        String sLen = getLength(value.length() / 2, 4);
        if (null == sLen) {
            return null;
        }
        String tlv = "";
        String sTag = Integer.toHexString(tag).toUpperCase();
        if (0x01 == (sTag.length() & 0x01)) {
            tlv = "0";
        }
        tlv += sTag;
        tlv += sLen;
        tlv += value;

        tlvString += tlv;

        if (null != defaultTagValue) {
            for (DefaultTagValue tagValue : defaultTagValue
            ) {
                if (tag == tagValue.tag) {
                    tagValue.available = false;
                }
            }
        }


        return tlv;

    }

    public int append(String tlv) {
        // read tag
        // read length
        // read value
        return 0;
    }

    public String getTlvString() {
        if (null != defaultTagValue) {
            for (DefaultTagValue tagValue : defaultTagValue
            ) {
                if (true == tagValue.available) {
                    append(tagValue.tag, tagValue.value);
                }
            }
        }
        return tlvString;
    }

    public void clean() {
        tlvString = "";
    }


    protected class DefaultTagValue {
        public int tag;
        public boolean available;
        public String value;

        public DefaultTagValue(int tag, String value) {
            this.tag = tag;
            this.value = value;
            available = true;
        }

    }
}
