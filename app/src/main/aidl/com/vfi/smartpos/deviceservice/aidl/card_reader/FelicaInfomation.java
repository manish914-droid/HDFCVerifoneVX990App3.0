package com.vfi.smartpos.deviceservice.aidl.card_reader;

import android.os.Parcel;
import android.os.Parcelable;

public class FelicaInfomation implements Parcelable {
    public static final Creator<FelicaInfomation> CREATOR = new Creator<FelicaInfomation>() {
        @Override
        public FelicaInfomation createFromParcel(Parcel in) {
            byte[] idm = in.createByteArray();
            byte[] pMm = in.createByteArray();
            byte[] responseData = in.createByteArray();
            return new FelicaInfomation(idm, pMm, responseData);
        }

        @Override
        public FelicaInfomation[] newArray(int size) {
            return new FelicaInfomation[size];
        }
    };
    private final byte[] idm;
    private final byte[] pMm;
    private final byte[] responseData;

    public FelicaInfomation(byte[] idm, byte[] pMm, byte[] responseData) {
        this.idm = idm;
        this.pMm = pMm;
        this.responseData = responseData;
    }

    public byte[] getIdm() {
        return idm;
    }

    public byte[] getpMm() {
        return pMm;
    }

    public byte[] getResponseData() {
        return responseData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(idm);
        dest.writeByteArray(pMm);
        dest.writeByteArray(responseData);
    }
}
