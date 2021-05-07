package com.example.verifonevx990app.utils;

import android.text.TextUtils;

public class NumberUtil {
    private NumberUtil() {
    }


    public static long parseLong(String sLong) {
        if (TextUtils.isEmpty(sLong)) {
            return 0;
        }

        long result = 0;
        try {
            result = Long.parseLong(sLong);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static int parseInt(String sInt) {
        if (TextUtils.isEmpty(sInt)) {
            return 0;
        }

        int result = 0;
        try {
            result = Integer.parseInt(sInt);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
    }
}
