package com.vfi.smartpos.deviceservice.constdefine;

/**
 * Created by Simon on 2018/8/21.
 */

public class ConstOnlineResultHandler {
    public class onProccessResult {
        public class result {
            public static final int TC = 0;             // online result TC (success)
            public static final int Online_AAC = 1;     // online result AAC (refuse)
            public static final int Offline_TC = 101;   // online false, offline success
            public static final int SCRIPT_NOT_EXECUTE = 102;   // the script not execute
            public static final int SCRIPT_EXECUTE_FAIL = 103; // failure while execute script
            public static final int NO_SCRIPT = 104;            // online failure, not send the script
            public static final int TOO_MANY_SCRIPTNO = 105;    // online failure, more than one script
            public static final int TERMINATE = 106;             // online failure, transaction terminate. return transaction terminate while GAC is not 9000, 0x8F
            public static final int ERROR = 107;                // online failure, error in EMV

        }

        public class data {
            public static final String KEY_TC_DATA_String = "TC_DATA";

            public static final String KEY_SCRIPT_DATA_String = "SCRIPT_DATA";
            public static final String KEY_REVERSAL_DATA_String = "REVERSAL_DATA";
        }

    }
}
