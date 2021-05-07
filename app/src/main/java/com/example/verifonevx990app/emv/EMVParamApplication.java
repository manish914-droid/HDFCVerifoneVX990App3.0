package com.example.verifonevx990app.emv;

/**
 * Created by Simon on 2018/8/31.
 */

public class EMVParamApplication extends EMVTLVParam {

    /**
     * the AID TAG list
     */
    public static int TAG_CurrencyCodeTerm_5F2A = 0x5F2A; //  "Transaction Currency Code Indicates the currency code of the transaction according to ISO 4217"
    public static int TAG__5F36_Optional = 0x5F36; //  "Transaction Currency Exponent Indicates the implied position of the decimal point from the right of the transaction amount represented according to ISO 4217"
    public static int TAG_DefaultTDOL_97_Optional = 0x97; //  "Transaction Certificate Data Object List (TDOL) List of data objects (tag and length) to be used by the terminal in generating the TC Hash Value"
    public static int TAG__9F01_Optional = 0x9F01; //  "Acquirer Identifier Uniquely identifies the acquirer within each payment system"
    public static int TAG_AID_9F06 = 0x9F06; //  "Application Identifier (AID) - terminal Identifies the application as described in ISO/IEC 7816-5"
    // 9F08; // CARD_APPL_VERSION
    public static int TAG_VerNum_9F09 = 0x9F09; //  "Application Version Number Version number assigned by the payment system for the application"
    public static int TAG__9F15_Optional = 0x9F15; //  "Merchant Category Code Classifies the type of business being done by the merchant, represented according to ISO 8583:1993 for Card Acceptor Business Code"
    public static int TAG_CountryCodeTerm_9F1A = 0x9F1A; //  "Terminal Country Code Indicates the country of the terminal, represented according to ISO 3166"
    public static int TAG_FloorLimit_9F1B = 0x9F1B; //  "Terminal Floor Limit Indicates the floor limit in the terminal in conjunction with the AID"
    public static int TAG_AppTermCap_9F33 = 0x9F33; //  "Terminal Capabilities Indicates the card data input, CVM, and security capabilities of the terminal"
    public static int TAG_AppTerminalType_9F35 = 0x9F35; //  "Terminal Type Indicates the environment of the terminal, its communications capability, and its operational control"
    public static int TAG_AppTermAddCap_9F40 = 0x9F40; //  "Additional Terminal Capabilities Indicates the data input and output capabilities of the terminal"
    public static int TAG_DynamicDDOL_9F49 = 0x9F49; //  "Dynamic Data Authentication Data Object List (DDOL) List of data objects (tag and length) to be passed to the ICC in the INTERNAL AUTHENTICATE command"
    public static int TAG__9F5A = 0x9F5A; // Terminal Transaction Type
    public static int TAG__9F66 = 0x9F66; //  Terminal Transaction Type
    public static int TAG_ECTransLimit_9F7B = 0x9F7B; //  Electronic cash transaction limit
    public static int TAG_ASI_DF01 = 0xDF01; //  "Application Select Identifier 0-Terminal AID & CARD AID Partial match 1-Terminal AID & CARD AID All match"
    public static int TAG__DF04 = 0xDF04; //  CVMRequirements
    public static int TAG__DF08_Optional = 0xDF08; //  Termianl Priority
    public static int TAG_TAC_Default_DF11 = 0xDF11; //  TAC - default
    public static int TAG_TAC_Online_DF12 = 0xDF12; //  TAC-online
    public static int TAG_TAC_Denial_DF13 = 0xDF13; //  TAC-refuse
    public static int TAG_DefaultDDOL_DF14 = 0xDF14; //  "Default Dynamic Data Authentication Data Object List (DDOL) DDOL to be used for constructing the INTERNAL AUTHENTICATE command if the DDOL in the card is not present"
    public static int TAG_Threshold_DF15 = 0xDF15; //  "Threshold Value for Biased Random Selection Value used in terminal risk management for random transaction selection"
    public static int TAG_MaxTargetPercentage_DF16 = 0xDF16; //  "Maximum Target Percentage to be used for Biased Random Selection Value used in terminal risk management for random transaction selection"
    public static int TAG_TargetPercentage_DF17 = 0xDF17; //  "Target Percentage to be Used for Random Selection Value used in terminal risk management for random transaction selection"
    public static int TAG__DF18 = 0xDF18; //
    public static int TAG_CTLSFloorLimit_DF19 = 0xDF19; //  CTLS offline minimum limit
    public static int TAG_CTLSTransLimit_DF20 = 0xDF20; //  CTLS transaction limit
    public static int TAG_CTLSCVMLimit_DF21 = 0xDF21; //  Terminal implement CVM limit, if ctls amount over the limit, then need a CVM public static int TAG__9F66 =
    public static int TAG__DF25_Optional = 0xDF25; //
    public static int TAG__DF26_Optional = 0xDF26; //  international
    public static int TAG__DF27_Optional = 0xDF27; //
    public int aidType;
    // DF18

    // DF1B,9F5A, DF04, DF1A, 9F08

    public EMVParamApplication() {
        super();
        /**
         * set the default value of some Tags
         * value null means the tag is optional
         * */
        defaultTagValue = new DefaultTagValue[]{
                new DefaultTagValue(TAG_ASI_DF01, "01"),
                new DefaultTagValue(TAG__DF08_Optional, null),
                new DefaultTagValue(TAG__DF26_Optional, null),
                new DefaultTagValue(TAG__DF27_Optional, null),
                new DefaultTagValue(TAG__DF25_Optional, null),
                new DefaultTagValue(TAG_VerNum_9F09, null),
                new DefaultTagValue(TAG__9F15_Optional, null),
                new DefaultTagValue(TAG_DefaultTDOL_97_Optional, null),
                new DefaultTagValue(TAG__5F36_Optional, null),
                new DefaultTagValue(TAG__9F01_Optional, null),
        };
    }

    public int getAidType() {
        return aidType;
    }

    public void setAidType(int aidType) {
        this.aidType = aidType;
    }

    @Override
    public void clean() {
        super.clean();
        if (defaultTagValue != null) {
            for (DefaultTagValue tagValue : defaultTagValue) {
                tagValue.available = true;
            }
        }
    }
}
