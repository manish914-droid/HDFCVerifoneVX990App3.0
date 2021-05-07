package com.vfi.smartpos.deviceservice.constdefine;

/**
 * Created by Simon on 2018/8/21.
 */

public class ConstIPBOC {
    public class importCertConfirmResult {
        public class option {
            public static final int CANCEL = 0; // cancel ( BYPASS )
            public static final int CONFIRM = 1; //  confirm
            public static final int NOTMATCH = 2;   //  not match
        }
    }

    public class inputOnlineResult {
        public class onlineResult {
            public static final String KEY_isOnline_boolean = "isOnline"; // (boolean)is online</li>
            public static final String KEY_respCode_String = "respCode"; // (String) the response code</li>
            public static final String KEY_authCode_String = "authCode"; // (String) the authorize code</li>
            public static final String KEY_field55_String = "field55";   // (String) the response of field 55 data</li>
        }
    }

    public class importCardConfirmResult {
        public class pass {
            public static final boolean allowed = true;
            public static final boolean refused = false;
        }
    }

    public class startEMV {
        public class processType {
            public static final int full_process = 1;
            public static final int simple_process = 2;
        }

        public class intent {
            // public static final String KEY_ = "";
            // public static final int VALUE_ = ;

            public static final String KEY_isPanConfirmOnSimpeProcess_boolean = "isPanConfirmOnSimpeProcess";

            public static final String KEY_cardType_int = "cardType";
            public static final int VALUE_cardType_smart_card = 0;
            public static final int VALUE_cardType_contactless = 1;

            public static final String KEY_transProcessCode_byte = "transProcessCode";

            public static final String KEY_authAmount_long = "authAmount";

            public static final String KEY_isSupportQ_boolean = "isSupportQ";
            public static final boolean VALUE_supported = true;
            public static final boolean VALUE_unsupported = false;

            public static final String KEY_isSupportSM_boolean = "isSupportSM";

            public static final String KEY_isQPBOCForceOnline_boolean = "isQPBOCForceOnline";
            public static final boolean VALUE_forced = true;
            public static final boolean VALUE_unforced = false;

            public static final String KEY_merchantName_String = "merchantName";

            public static final String KEY_merchantId_String = "merchantId";

            public static final String KEY_terminalId_String = "terminalId";

            /**
             * added by lucky singh(11/02/2021)
             */
            public static final String KEY_isSupportPBOCFirst_String = "isSupportPBOCFirst";
            public static final String KEY_transCurrCode_String = "transCurrCode";
            public static final String KEY_otherAmount_String = "otherAmount";
        }

    }

    public class updateAID {
        public class operation {
            public static final int append = 1;
            public static final int remove = 2;
            public static final int clear = 3;
        }

        public class aidType {
            public static final int smart_card = 1;
            public static final int contactless = 2;
        }
    }

    public class updateRID {
        public class operation {
            public static final int append = 1;
            public static final int remove = 2;
            public static final int clear = 3;
        }
    }

    public class checkCard {
        public class cardOption {
            public static final String KEY_MagneticCard_boolean = "supportMagCard";
            public static final String KEY_SmartCard_boolean = "supportICCard";
            public static final String KEY_Contactless_boolean = "supportRFCard";
            public static final boolean VALUE_supported = true;
            public static final boolean VALUE_unsupported = false;

        }
    }
}
