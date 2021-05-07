package com.vfi.smartpos.deviceservice.constdefine;

/**
 * Created by Simon on 2019/3/26.
 */

public class ConstISmartCardReaderEx {

    public class CardType {
        public static final byte AT24 = 1;
        public static final byte SLE44X2 = 2;
        public static final byte SLE44X8 = 3;
        public static final byte AT88SC102 = 4;
        public static final byte AT88SC1604 = 5;
        public static final byte AT88SC1608 = 6;
    }

    public class AreaType {
        public static final byte PRIMAERY = 1;
        public static final byte PROTECTED = 2;
        public static final byte SAFE = 3;
        public static final byte VENDOR_CODE = 4;
        public static final byte MERCHANT_CODE = 5;
        public static final byte PASSWORD_CHECK_COUNTING = 6;
        public static final byte APPLICATION_1 = 7;
        public static final byte APPLICATION_2 = 8;
        public static final byte APPLICATION_ANY = 9;
        public static final byte USER_0 = 16;
        public static final byte USER_1 = 17;
        public static final byte USER_2 = 18;
        public static final byte USER_3 = 19;
        public static final byte USER_4 = 20;
        public static final byte USER_5 = 21;
        public static final byte USER_6 = 22;
        public static final byte USER_7 = 23;
    }

    public class CommType {
        public static final byte READ = 1;
        public static final byte WRITE = 2;
        public static final byte PASSWORD_AUTHENTICATE = 4;
        public static final byte PASSWORD_MODIFY = 8;
        public static final byte ERASE_CARD_102_FLASH = 16;
        public static final byte READ_AUTHENTICATE_1608 = 20;
        public static final byte WRITE_AUTHENTICATE_1608 = 36;
    }

    public class communicate {
        public class returnCode {
            public static final int SUCCESS = 0;
            public static final int AUTH_FAILS = 1;
            public static final int READ_FAILS = 2;
            public static final int WRITE_FAILS = 3;
            public static final int PASSWORD_MODIFY_FAILS = 4;
            public static final int ERASE_102_FAILS = 5;
            public static final int CAN_NOT_ERASE = 6;
            public static final int NEED_ERASE_BEFORE_WRITE = 7;
            public static final int WRONG_AREA_TYPE = 8;
            public static final int NEED_AUTH = 9;
            public static final int NOT_SUPPORT = 15;
            public static final int OTHER = 255;

        }
    }

    public class Listener {
        public class errorCode {
            public static final int ERROR = -1;
        }

    }


}
