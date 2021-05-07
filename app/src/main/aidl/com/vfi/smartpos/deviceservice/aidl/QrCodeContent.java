package com.vfi.smartpos.deviceservice.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class QrCodeContent implements Parcelable {

    public static final Creator<QrCodeContent> CREATOR = new Creator<QrCodeContent>() {
        @Override
        public QrCodeContent createFromParcel(Parcel in) {
            return new QrCodeContent(in);
        }

        @Override
        public QrCodeContent[] newArray(int size) {
            return new QrCodeContent[size];
        }
    };
    private final String barcode;
    private int height = 0;
    private int leftOffset = 0;

    public QrCodeContent(int height, int leftOffset, String barcode) {
        this.height = height;
        this.leftOffset = leftOffset;
        this.barcode = barcode;
    }

    protected QrCodeContent(Parcel in) {
        height = in.readInt();
        leftOffset = in.readInt();
        barcode = in.readString();
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getLeftOffset() {
        return leftOffset;
    }

    public void setLeftOffset(int leftOffset) {
        this.leftOffset = leftOffset;
    }

    public String getBarcode() {
        return barcode;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(height);
        dest.writeInt(leftOffset);
        dest.writeString(barcode);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
