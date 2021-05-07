package com.example.verifonevx990app.utils;
/*
 *
 **************************************************************
 *                                                            *
 *   .=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-.       *
 *    |                     ______                     |      *
 *    |                  .-"      "-.                  |      *
 *    |                 /            \                 |      *
 *    |     _          |              |          _     |      *
 *    |    ( \         |,  .-.  .-.  ,|         / )    |      *
 *    |     > "=._     | )(__/  \__)( |     _.=" <     |      *
 *    |    (_/"=._"=._ |/     /\     \| _.="_.="\_)    |      *
 *    |           "=._"(_     ^^     _)"_.="           |      *
 *    |               "=\__|IIIIII|__/="               |      *
 *    |              _.="| \IIIIII/ |"=._              |      *
 *    |    _     _.="_.="\          /"=._"=._     _    |      *
 *    |   ( \_.="_.="     `--------`     "=._"=._/ )   |      *
 *    |    > _.="                            "=._ <    |      *
 *    |   (_/                                    \_)   |      *
 *    |                                                |      *
 *    '-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-='      *
 *                                                            *
 *           File Modified by - Ajay Thakur                   *
 **************************************************************
 *
 **/

import com.vfi.smartpos.deviceservice.aidl.DRLData;

import java.util.ArrayList;
import java.util.List;

import static com.example.verifonevx990app.vxUtils.Converter.str2NibbleArr;

public class DRLUtil {
    private String cvmVal = "";  // Min amount value for ctls trans
    private String ctlsVal = ""; // Max amount value for ctls trans

    public DRLUtil(String cvmValueLimit, String ctlsMaxLimit) {
        this.cvmVal = cvmValueLimit;
        this.ctlsVal = ctlsMaxLimit;
    }

    /*private static class SingletonHolder {
        private static DRLUtil INSTANCE = new DRLUtil();
    }*/

    /*public static final DRLUtil getInstance() {
        return SingletonHolder.INSTANCE;
    }*/

    public List<DRLData> getAMEXDRL() {
        List<DRLData> drlDataList = new ArrayList<>();
        DRLData drlData = null;
        byte[] drlID = null;
        byte[] clssFloorLimit = null;
        byte[] clssTransLimit = null;
        byte[] clssRequiredLimit = null;
        drlDataList.clear();

        drlID = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

        clssFloorLimit = new byte[]{0, 0, 0, 0, 0, 0}; // Default value cls floor limit

        //CTLS Transaction Limit ByteArray:-
        clssTransLimit = str2NibbleArr(ctlsVal); //Here we are creating a Nibble Array of CTLS value coming from host end

        //CVM Transaction Limit ByteArray:-
        clssRequiredLimit = str2NibbleArr(cvmVal); //Here we are creating a Nibble Array of CVM value coming from host end

        drlData = new DRLData(drlID, clssFloorLimit, clssTransLimit, clssRequiredLimit);
        drlDataList.add(drlData);

        drlID = new byte[]{0x06};
        clssFloorLimit = new byte[]{0, 0, 0, 0, 4, 0};
        clssTransLimit = new byte[]{0, 0, 0, 0, 7, 0};
        clssRequiredLimit = new byte[]{0, 0, 0, 0, 2, 0};

        drlData = new DRLData(drlID, clssFloorLimit, clssTransLimit, clssRequiredLimit);
        drlDataList.add(drlData);

        drlID = new byte[]{0x0B};
        clssFloorLimit = new byte[]{0, 0, 0, 0, 1, 0};
        clssTransLimit = new byte[]{0, 0, 0, 0, 3, 0};
        clssRequiredLimit = new byte[]{0, 0, 0, 0, 2, 0};
        drlData = new DRLData(drlID, clssFloorLimit, clssTransLimit, clssRequiredLimit);
        drlDataList.add(drlData);
        return drlDataList;
    }
}
