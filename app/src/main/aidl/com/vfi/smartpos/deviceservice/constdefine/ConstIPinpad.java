package com.vfi.smartpos.deviceservice.constdefine;

/**
 * Created by Simon on 2018/8/21.
 *
 * @brief \code{.java}
 * \endcode
 * @version
 * @see
 */

/**
 * @brief
 *
 *  \code{.java}
 * \endcode
 * @version
 * @see
 *
 */

/**
 * @brief const defines of class IPinpad
 *
 * \code{.java}
 * \endcode
 * @version
 * @see com.vfi.smartpos.deviceservice.aidl.IPinpad
 *
 * */
public class ConstIPinpad {

    /**
     * @brief const define of method IPinpad.startPinInput
     *
     * \code{.java}
     * \endcode
     * @version
     * @see com.vfi.smartpos.deviceservice.aidl.IPinpad#startPinInput
     *
     */
    public class startPinInput {
        /**
         * @brief param defines in IPinpad.startPinInput
         *
         *  \code{.java}
         * \endcode
         * @version
         * @see
         *
         */
        public class param {
            /**
             * @brief is online pin
             *
             *  \code{.java}
             * \endcode
             * @version
             * @see
             *
             */
            public static final String KEY_isOnline_boolean = "isOnline";

            /**
             * @brief the pan
             *
             *  \code{.java}
             * \endcode
             * @version
             * @see
             *
             */
            public static final String KEY_pan_String = "pan";

            /**
             * @brief the want pin length
             *
             * give the array of valid pin length
             * {0,4,5,6} means the valid pin length is 0, 4~6
             * \code{.java}
             * \endcode
             * @version
             * @see
             *
             */
            public static final String KEY_pinLimit_ByteArray = "pinLimit";

            public static final String KEY_timeout_int = "timeout";

            /**
             * @brief the desType
             *
             *  \code{.java}
             * \endcode
             * @version
             * @see
             *
             */
            public static final String KEY_desType_int = "desType";
            /**
             * @brief for 3DES (default)
             *
             *  \code{.java}
             * \endcode
             * @version
             * @see
             *
             */
            public static final int Value_desType_3DES = 1;   // 0x01 MK/SK + 3DES (default)
            public static final int Value_desType_AES = 2;    // 0x02 MK/SK + AES
            public static final int Value_desType_SM4 = 3;    // 0x03 MK/SK + SM4
            public static final int Value_desType_DUKPT_3DES = 4; // 0x04 DUKPT + 3DES

            public static final String KEY_promptString_String = "promptString";

        }

        public class globleParam {
            public static final String KEY_Display_One_String = "Display_One";

            public static final String KEY_Display_Two_String = "Display_Two";

            public static final String KEY_Display_Three_String = "Display_Three";

            public static final String KEY_Display_Four_String = "Display_Four";

            public static final String KEY_Display_Five_String = "Display_Five";

            public static final String KEY_Display_Six_String = "Display_Six";

            public static final String KEY_Display_Seven_String = "Display_Seven";

            public static final String KEY_Display_Eight_String = "Display_Eight";

            public static final String KEY_Display_Nine_String = "Display_Nine";

            public static final String KEY_Display_Zero_String = "Display_Zero";

            public static final String KEY_Display_Confirm_String = "Display_Confirm";

            public static final String KEY_Display_BackSpace_String = "Display_BackSpace";
        }
    }

    public class calculateByMSKey {
        public class keyType {
            public static final int Master_Key = 0x01;
            public static final int SM4_Master_Key = 0x02;
            public static final int AES_Master_Key = 0x03;
        }

        public class algorithmMode {
            public static final int Encrypt_ECB = 0x00;
            public static final int Decrypt_ECB = 0x01;
            public static final int Encrypt_CBC = 0x02;
            public static final int Decrypt_CBC = 0x03;
        }
    }
}
