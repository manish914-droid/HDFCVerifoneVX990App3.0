package com.vfi.smartpos.deviceservice.constdefine;

/**
 * Created by Simon on 2018/8/21.
 */

public class ConstPBOCHandler {
    public class onTransactionResult {
        public class result {
            public static final int EMV_COMPLETE = 9;   // emv complete
            public static final int AARESULT_TC = 0;    // TC on action analysis
            public static final int AARESULT_AAC = 1;   // refuse on action analysis
            public static final int EMV_CARD_BIN_CHECK_FAIL = 24; // read card fail
            public static final int EMV_MULTI_CARD_ERROR = 26;  // multi cards
            /**
             *
             * <li>EMV_COMPLETE(9) - emv complete </li>
             * <li>EMV_OTHER_ERROR(11) - emv other error,transaction abort</li>
             * <li>EMV_FALLBACK(12) - FALLBACK </li>
             * <li>EMV_DATA_AUTH_FAIL(13) - 脱机数据认证失败 </li>
             * <li>EMV_APP_BLOCKED(14) - 应用被锁定 </li>
             * <li>EMV_NOT_ECCARD(15) - 非电子现金卡 </li>
             * <li>EMV_UNSUPPORT_ECCARD(16) - 该交易不支持电子现金卡 </li>
             * <li>EMV_AMOUNT_EXCEED_ON_PURELYEC(17) - 纯电子现金卡消费金额超限 </li>
             * <li>EMV_SET_PARAM_ERROR(18) - set parameter fail on 9F7A </li>
             * <li>EMV_PAN_NOT_MATCH_TRACK2(19) - 主账号与二磁道不符 </li>
             * <li>EMV_CARD_HOLDER_VALIDATE_ERROR(20) - 持卡人认证失败 </li>
             * <li>EMV_PURELYEC_REJECT(21) - 纯电子现金卡被拒绝交易 </li>
             * <li>EMV_BALANCE_INSUFFICIENT(22) - 余额不足 </li>
             * <li>EMV_AMOUNT_EXCEED_ON_RFLIMIT_CHECK(23) - 交易金额超过非接限额检查 </li>
             * <li>EMV_CARD_BIN_CHECK_FAIL(24) - 读卡失败 </li>
             * <li>EMV_CARD_BLOCKED(25) - 卡被锁 </li>
             * <li>EMV_MULTI_CARD_ERROR(26) - 多卡冲突 </li>
             * <li>EMV_BALANCE_EXCEED(27) - 余额超出 </li>
             * <li>EMV_RFCARD_PASS_FAIL(60) - tap card failure</li>
             * <li>EMV_IN_QPBOC_PROCESS(99) - qPBOC is processing </li>
             *
             * <li>AARESULT_TC(0) - TC on action analysis</li>
             * <li>AARESULT_AAC(1) - refuse on action analysis</li>
             * <li>QPBOC_AAC(202) - refuse on qPBOC </li>
             * <li>QPBOC_ERROR(203) - error on qPBOC </li>
             * <li>QPBOC_TC(204) - TC on qPBOC</li>
             * <li>QPBOC_CONT(205) - need contact</li>
             * <li>QPBOC_NO_APP(206) - result of qPBOC, no application (UP Card maybe available)</li>
             * <li>QPBOC_NOT_CPU_CARD(207) - not a cpu card</li>
             * <li>QPBOC_ABORT(208) - Transation abort</li>
             * <li>PAYPASS_COMPLETE(301)-paypass complete</li>
             * <li>PAYPASS_EMV_TC(304)-paypass交易结果，交易批准 </li>
             * <li>PAYPASS_EMV_AAC(305)-paypass result, refuse</li>
             * <li>PAYPASS_EMV_ERROR(306)-paypass交易结果，交易失败</li>
             * <li>PAYPASS_END_APP(307)-paypass交易结果，交易终止</li>
             * <li>PAYPASS_TRYOTHER(308)-paypass result, try other (contact, magnetic card)</li>
             * <li>EMV_SEE_PHONE(150)-paypass result, please check the result on phone</li>
             * * */

        }

        public class data {
            public static final String KEY_TC_DATA_String = "TC_DATA";

            public static final String KEY_REVERSAL_DATA_String = "REVERSAL_DATA"; // (String) - the string of reversal data</li>

            public static final String KEY_ERROR_String = "ERROR"; // * <li>(String) - the error description ( from the result of PBOC) </li>
        }
    }

    public class onConfirmCardInfo {
        public class info {
            public static final String KEY_PAN_String = "PAN"; // (String) the PAN </li>

            public static final String KEY_TRACK2_String = "TRACK2"; // (String) track 2</li>

            public static final String KEY_CARD_SN_String = "CARD_SN"; // (String) card serial number</li>

            public static final String KEY_SERVICE_CODE_String = "SERVICE_CODE"; // (String) service code</li>

            public static final String KEY_EXPIRED_DATE_String = "EXPIRED_DATE"; //(String) expired date</li>


            public static final String KEY_CARD_TYPE_String = "CARD_TYPE"; //(String) expired date</li>
        }
    }

    public class onRequestOnlineProcess {
        public class aaResult {
            public static final String KEY_RESULT_int = "RESULT";
            public static final String KEY_SIGNATURE_boolean = "SIGNATURE";
            public static final int VALUE_RESULT_QPBOC_ARQC = 201;   //) - QPBOC_ARQC, online request, part of PBOC standard<br>
            public static final int VALUE_RESULT_AARESULT_ARQC = 2;  //, the action analysis result<br>
            public static final int VALUE_RESULT_PAYPASS_MAG_ARQC = 302; // -the mode of magnetic card on paypass request<br>
            public static final int VALUE_RESULT_PAYPASS_EMV_ARQC = 303; //- the mode of EMV on paypass request<br>

            public static final String KEY_ARQC_DATA_String = "ARQC_DATA";

            public static final String KEY_REVERSAL_DATA_String = "REVERSAL_DATA";

        }
    }
}
