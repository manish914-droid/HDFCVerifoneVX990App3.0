package com.example.verifonevx990app.utils;

/**
 * Created by IntelliJ IDEA
 * User: Sirius
 * Date: 2014/11/13
 * Time: 15:25
 */
public class StringUtil {

    public static boolean isNull(CharSequence str) {
        return str == null || str.length() == 0;
    }


    //为 EditText 获取相应的 selection index.即设置光标位置为最右方
//    public static int getSelectionIndex(CharSequence str) {
//        return isNull(str) ? 0 : str.length();
//    }


    public static String leftPadding(char fill, int totalLength, String str) {
        StringBuffer buffer = new StringBuffer();
        for (int i = str.length(); i < totalLength; i++) {
            buffer.append(fill);
        }
        buffer.append(str);
        return buffer.toString();
    }

    public static String leftPadding(String fill, int totalLength, String str) {
        StringBuffer buffer = new StringBuffer();
        for (int i = str.length(); i < totalLength; i++) {
            buffer.append(fill);
        }
        buffer.append(str);
        return buffer.toString();
    }

    //左边增加一定长度的字符串
    public static String leftAppend(String fill, int appendLength, String str) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < appendLength; i++) {
            buffer.append(fill);
        }
        buffer.append(str);
        return buffer.toString();
    }

    //右边增加一定长度的字符串
    private static String rightAppend(String fill, int appendLength, String str) {
        StringBuilder buffer = new StringBuilder(str);
        for (int i = 0; i < appendLength; i++) {
            buffer.append(fill);
        }
        return buffer.toString();
    }

    public static String rightPadding(String fill, int totalLength, String str) {

        if (totalLength < str.length()) {
            return str.substring(0, totalLength);
        }
        StringBuilder buffer = new StringBuilder(str);
        if (str.length() == totalLength)
            return buffer.toString();

        for (int i = str.length(); i < totalLength; i++) {
            buffer.append(fill);
        }
        return buffer.toString();
    }


    //得到字符串的字节长度
    public static int getContentByteLength(String content) {
        if (content == null || content.length() == 0)
            return 0;
        int length = 0;
        for (int i = 0; i < content.length(); i++) {
            length += getByteLength(content.charAt(i));
        }
        return length;
    }

    //得到几位字节长度
    private static int getByteLength(char a) {
        String tmp = Integer.toHexString(a);
        return tmp.length() >> 1;
    }

    //文本右边补空格
    public static String fillRightSpacePrintData(String context, int fillDataLength) {

        if (context != null) {
            int printDataLength = fillDataLength - context.length();
            if (printDataLength > 0) {
                StringBuilder contextBuilder = new StringBuilder(context);
                for (int i = 0; i < printDataLength; i++) {
                    contextBuilder.append(" ");
                }
                context = contextBuilder.toString();
            }

        } else {
            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < fillDataLength; i++) {
                contextBuilder.append(" ");
            }
            context = contextBuilder.toString();
        }

        return context;
    }

//    //文本左边补空格
//    public static String fillLeftSpacePrintData(String context, int fillDataLength) {
//
//        if (context != null) {
//            int printDataLength = fillDataLength - context.length();
//            if (printDataLength > 0) {
//                StringBuilder tempSpace = new StringBuilder();
//                for (int i = 0; i < printDataLength; i++) {
//                    tempSpace.append(" ");
//                }
//
//                context = tempSpace + context;
//            }
//
//        } else {
//            StringBuilder contextBuilder = new StringBuilder();
//            for (int i = 0; i < fillDataLength; i++) {
//                contextBuilder.append(" ");
//            }
//            context = contextBuilder.toString();
//        }
//
//        return context;
//    }

    public static void main(String[] args) {
        String a = "阿斯顿法";
        a = leftAppend("-", 10, a);
        //System.out.println(a);

        a = rightAppend("*", 6, a);
        //System.out.println(a);
    }
}
