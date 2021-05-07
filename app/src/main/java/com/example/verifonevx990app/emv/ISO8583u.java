package com.example.verifonevx990app.emv;


import java.util.Arrays;

/**
 * Created by Simon on 2018/8/27.
 */

public class ISO8583u extends ISO8583 {

    public static int F_MessageType_00 = 0;
    public static int F_AccountNumber_02 = 2;
    public static int F_AmountOfTransactions_04 = 4;
    public static int F_DateOfExpired_14 = 14;
    public static int F_Track_2_Data_35 = 35;
    public static int F_Track_3_Data_36 = 36;
    public static int F_AuthorizationIdentificationResponseCode_38 = 38;
    public static int F_ResponseCode_39 = 39;
    public static int F_PINData_52 = 52;
    public static int F_BalancAmount_54 = 54;
    public static int F_55 = 55;

    static int[][] FIELD_ATTRIBUTE_ARRAY =
            {// type, length, not defined, not defined
                    {TYPE_BCD, 4, 0, 0},    // field	0, (Message Type Identifier)
                    {TYPE_BIN, 8, 0, 0},    // field	1, bitmap
                    {TYPE_L_BCD, 19, 0, 0},    // field	2, (Primary Account Number), N..19(LLVAR)，2个字节的长度值＋最大19个字节的主账号，压缩时用BCD码表示的1个字节的长度值＋用左靠BCD码表示的最大10个字节的主账号。
                    {TYPE_BCD, 6, 0, 0},    // field	3, (Transaction Processing Code), N6，6个字节的定长数字字符域，压缩时用BCD码表示的3个字节的定长域。
                    {TYPE_BCD, 12, 0, 0},    // field	4, (Amount Of Transactions), N12，12个字节的定长数字字符域，压缩时用BCD码表示的6个字节的定长域。
                    {0, 0, 0, 0},    // field	5
                    {0, 0, 0, 0},    // field	6
                    {0, 0, 0, 0},    // field	7
                    {0, 0, 0, 0},    // field	8
                    {0, 0, 0, 0},    // field	9
                    {0, 0, 0, 0},    // field	10
                    {TYPE_BCD, 6, 0, 0},    // field	11, 受卡方系统跟踪号(System Trace Audit Number), N6，6个字节的定长数字字符域，压缩时用BCD码表示的3个字节的定长域。
                    {TYPE_BCD, 6, 0, 0},    // field	12, 受卡方所在地时间(Local Time Of Transaction), hhmmss, N6，6个字节的定长数字字符域，压缩时用BCD码表示的3个字节的定长域。
                    {TYPE_BCD, 4, 0, 0},    // field	13, 受卡方所在地日期(Local Date Of Transaction), MMDD, N4，4个字节的定长数字字符域，压缩时用BCD码表示的2个字节的定长域。
                    {TYPE_BCD, 4, 0, 0},    // field	14, 卡有效期(Date Of Expired), YYMM, N4，4个字节的定长数字字符域，压缩时用BCD码表示的2个字节的定长域。格式：YYMM。
                    {TYPE_BCD, 4, 0, 0},    // field	15, 清算日期(Date Of Settlement), N4，4个字节的定长数字字符域，压缩时用BCD码表示的2个字节的定长域。格式：MMDD。,
                    {0, 0, 0, 0},    // field	16
                    {0, 0, 0, 0},    // field	17
                    {0, 0, 0, 0},    // field	18
                    {0, 0, 0, 0},    // field	19
                    {0, 0, 0, 0},    // field	20
                    {0, 0, 0, 0},    // field	21
                    {TYPE_BCD, 3, 0, 0},    // field	22, 服务点输入方式码(Point Of Service Entry Mode), N3，3个字节的定长数字字符域，压缩时用左靠BCD码表示的2个字节的定长域。
                    {TYPE_BCD, 3, 0, 0},    // field	23, 卡序列号(Card Sequence Number), N3，3个字节的定长数字字符域，压缩时用右靠BCD码表示的2个字节的定长域。
                    {TYPE_ASC, 2, 0, 0},    // field	24
                    {TYPE_BCD, 2, 0, 0},    // field	25, 服务点条件码(Point Of Service Condition Mode), N2，2个字节的定长数字字符域，压缩时用左靠BCD码表示的1个字节的定长域。
                    {TYPE_BCD, 2, 0, 0},    // field	26, 服务点PIN获取码(Point Of Service PIN Capture Code), N2，2个字节的定长数字字符域，压缩时用BCD码表示的1个字节的定长域。
                    {TYPE_BCD, 2, 0, 0},    // field	27
                    {TYPE_BCD, 2, 0, 0},    // field	28
                    {TYPE_ASC, 8, 0, 0},    // field	29
                    {TYPE_BCD, 8, 0, 0},    // field	30
                    {TYPE_BCD, 8, 0, 0},    // field	31
                    {TYPE_L_BCD, 11, 0, 0},    // field	32, 受理机构标识码(Acquiring Institution Identification Code), N..11(LLVAR)，2个字节的长度值＋最大11个字节的受理方标识码，压缩时用BCD码表示的1个字节的长度值＋用左靠BCD码表示的最大6个字节的受理方标识码。
                    {TYPE_L_BCD, 11, 0, 0},    // field	33
                    {TYPE_L_BCD, 28, 0, 0},    // field	34
                    {TYPE_L_BCD, 37, 0, 0},    // field	35, 2磁道数据(Track 2 Data), Z..37(LLVAR)，2个字节的长度值＋最大37个字节的第二磁道数据(数字和分隔符)，压缩时用BCD码表示的1个字节的长度值＋用左靠BCD码表示的最大19个字节的第二磁道数据
                    {TYPE_LL_BCD, 104, 0, 0},    // field	36, 3磁道数据(Track 3 Data), Z...104(LLLVAR)，3个字节的长度值＋最大104个字节的第三磁道数据(数字和分隔符)，压缩时用右靠BCD码表示的2个字节的长度值＋用左靠BCD码表示的最大52个字节的第三磁道数据。
                    {TYPE_ASC_FS, 12, 0, 0},    // field	37, 检索参考号(Retrieval Reference Number), AN12，12个字节的定长字符域
                    {TYPE_ASC_FS, 6, 0, 0},    // field	38, 授权标识应答码(Authorization Identification Response Code), AN6，6个字节定长的字母、数字和特殊字符。
                    {TYPE_ASC_FS, 2, 0, 0},    // field	39, 应答码(Response Code), AN2，2个字节的定长字符域。
                    {TYPE_ASC, 3, 0, 0},    // field	40
                    {TYPE_ASC_FS, 8, 0, 0},    // field	41, 受卡机终端标识码(Card Acceptor Terminal Identification), ANS8，8个字节的定长的字母、数字和特殊字符。
                    {TYPE_ASC, 15, 0, 0},    // field	42, 受卡方标识码(Card Acceptor Identification Code), ANS15，15个字节的定长的字母、数字和特殊字符。
                    {TYPE_ASC, 40, 0, 0},    // field	43
                    {TYPE_L_ASC, 25, 0, 0},    // field	44, 附加响应数据(Additional Response Data), AN..25，2个字节长度+ 最大25个字节的数据。压缩时用右靠BCD码表示的1个字节的长度值＋用ASCII码表示的最大25个字节的数据。
                    {TYPE_L_ASC, 76, 0, 0},    // field	45
                    {TYPE_LL_ASC, 999, 0, 0},    // field	46
                    {TYPE_LL_ASC, 999, 0, 0},    // field	47, 营销信息域, 该域是一个变长域（LLLVAR），最长可达999个字节，最开始是一个占3个字节的长度值信息。压缩时采用右靠BCD码表示长度信息，长度信息占两个字节。
                    {TYPE_LL_BCD, 322, 0, 0},    // field	48, 附加数据 - 私有(Additional Data - Private), N...322(LLLVAR)，3个字节长度+ 最大322个字节的数据。压缩时用右靠BCD码表示的2个字节的长度值＋用左靠BCD码表示的最大161个字节的数据。
                    {TYPE_ASC, 3, 0, 0},    // field	49, 交易货币代码(Currency Code Of Transaction), AN3，3个字节的定长字符域。
                    {TYPE_ASC, 3, 0, 0},    // field	50
                    {TYPE_ASC, 3, 0, 0},    // field	51
                    {TYPE_BIN, 8, 0, 0},    // field	52, 个人标识码数据(PIN Data), B64，8个字节的定长二进制数域。
                    {TYPE_BCD, 16, 0, 0},    // field	53, 安全控制信息(Security Related Control Information ), n16，16个字节的定长数字字符域。压缩时用BCD码表示的8个字节的定长域。
                    {TYPE_LL_ASC, 20, 0, 0},    // field	54, 余额(Balanc Amount), AN...020(LLLVAR)，3个字节的长度值＋最大20个字节的数据。压缩时用右靠BCD码表示的2个字节的长度值＋用ASCII码表示的最大20个字节的数据。
                    {TYPE_LL_BIN, 255, 0, 0},    // field	55, IC卡数据域(Intergrated Circuit Card System Related Data), 该域是一个变长域（LLLVAR），最长可达255个字节，最开始是一个占3个字节的长度值信息。压缩时采用右靠BCD码表示长度信息，长度信息占两个字节。
                    {TYPE_LL_ASC, 999, 0, 0},    // field	56
                    {TYPE_LL_ASC, 999, 0, 0},    // field	57
                    {TYPE_LL_ASC, 100, 0, 0},    // field	58, PBOC电子钱包标准的交易信息（PBOC_ELECTRONIC_DATA）, ans...100(LLLVAR)，3个字节的长度值＋最大100个字节的字母、数字字符、特殊符号，压缩时采用右靠2个字节表示长度值。
                    {TYPE_LL_ASC, 999, 0, 0},    // field	59, 自定义域(Reserved Private), 该域是一个变长域（LLLVAR），最长可达999字节，最开始是一个占3个字节的长度值信息。压缩时采用右靠BCD码表示长度信息，长度信息占两个字节。
                    {TYPE_LL_BCD, 17, 0, 0},    // field	60, 自定义域(Reserved Private), N...17(LLLVAR)，3个字节的长度值＋最大17个字节的数字字符域。压缩时用右靠BCD码表示的2个字节的长度值＋用左靠BCD码表示的最大9个字节的数据。
                    {TYPE_LL_BCD, 29, 0, 0},    // field	61, 原始信息域(Original Message), N...029(LLLVAR)，3个字节的长度值＋最大29个字节的数字字符域，压缩时用右靠BCD码表示的2个字节的长度值＋用左靠BCD码表示的最大15个字节的数据。
                    {TYPE_LL_ASC, 512, 0, 0},    // field	62, 自定义域(Reserved Private), ANS...512(LLLVAR)，3个字节的长度值＋最大512个字节的数据域。压缩时用右靠BCD码表示的2个字节的长度值＋用ASCII码表示的最大512个字节的数据。
                    {TYPE_LL_ASC, 163, 0, 0},    // field	63, 自定义域(Reserved Private), ANS...163(LLLVAR)，3个字节的长度值＋最大163个字节的数据。压缩时用右靠BCD码表示的2个字节的长度值＋用ASCII码表示的最大163个字节的数据。
                    {TYPE_BIN, 8, 0, 0},    // field	64, 报文鉴别码(Message Authentication Code), B64，8个字节的定长域
            };

    public ISO8583u() {
        super.attribute_array = FIELD_ATTRIBUTE_ARRAY;
        super.header = "6000800000" + "603100010202";
        super.tail = "";
    }

    @Override
    protected byte[] calculateMac(byte[] packet, int offset, int length) {
        int start = offset + 0;

        int len = length - start;

        int i, j;
        int cnt = (len % 8 != 0) ? (len / 8 + 1) : len / 8;
        byte[] mac = new byte[8];
        Arrays.fill(mac, (byte) 0);

        cnt += start;
        for (i = start; i < cnt; i++) {
            for (j = 0; j < 8; j++) {
                mac[j] ^= packet[i * 8 + j];
            }
        }

        return mac;
    }

    @Override
    protected int getHeaderLen() {
        return 11;
    }

}
