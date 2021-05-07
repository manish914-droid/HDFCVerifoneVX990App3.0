package com.vfi.smartpos.deviceservice.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class BLKData implements Parcelable {

    public static final Creator<BLKData> CREATOR = new Creator<BLKData>() {
        @Override
        public BLKData createFromParcel(Parcel in) {
            return new BLKData(in.createByteArray(), in.readByte());
        }

        @Override
        public BLKData[] newArray(int size) {
            return new BLKData[size];
        }
    };
    private final byte[] cardblk; // tag5A
    private final byte sn; // tag5F34

    public BLKData(byte[] cardblk, byte sn) {
        this.cardblk = cardblk;
        this.sn = sn;
    }

    public byte[] getCardblk() {
        return cardblk;
    }

    public byte getSn() {
        return sn;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(cardblk);
        dest.writeByte(sn);
    }
}
