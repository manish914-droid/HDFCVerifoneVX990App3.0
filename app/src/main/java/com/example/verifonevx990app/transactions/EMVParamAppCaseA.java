package com.example.verifonevx990app.transactions;

import com.example.verifonevx990app.emv.EMVParamApplication;


/**
 * Created by Simon on 2018/9/6.
 * This a Fixed EMVParamApplication DEMO
 * You can change the TAG or VALUE here refer YOUR Specification
 */

public class EMVParamAppCaseA extends EMVParamApplication {
    private static final String TAG = "EMVParamAppCaseA";

    public EMVParamAppCaseA(String bankcode) {
    }

    @Override
    public String append(int tag, String value) {
        int fixedTag = tag;
        String fixedValue = value;

        //Below code is used in case of Demo Purpose only:-
       /* // THIS IS THE DEMO CODE !!!!, PLEASE FIX IT Refer YOUR Specification.
        if( tag == TAG_CTLSFloorLimit_DF19
                || tag == TAG_CTLSTransLimit_DF20
                || tag == TAG_CTLSCVMLimit_DF21 ) {
            // the CTLS offline minimum limit, TransLimit, CVM limit
            // the download value in Dollar or Yuan, but the kernel is in Cent, so need *100 here
            long l = Long.valueOf(value);
            fixedValue = Long.toString(l*100);
            int len = value.length() - fixedValue.length();
            if( len > 0  ) {
                fixedValue = value.substring(0,len) + fixedValue;
            } else if( len < 0 ) {
                fixedValue = fixedValue.substring((0-len), value.length()+1 );
            }
            Log.d(TAG, "reset value " + value + " -> " + fixedValue + " of tag " + Integer.toHexString(tag));
        } else if( tag == 0x9F08 ) {
            // fix the tag 9F08 to 9F09
           // fixedTag = TAG_VerNum_9F09;
            Log.d(TAG, "reset tag " + Integer.toHexString(tag) + " -> " + Integer.toHexString(fixedTag));
        }*/
        return super.append(fixedTag, fixedValue);
    }
}
