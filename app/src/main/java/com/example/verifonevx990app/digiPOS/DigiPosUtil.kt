package com.example.verifonevx990app.digiPOS

import java.text.SimpleDateFormat
import java.util.*

enum class LOG_TAG(val tag:String){
    DIGIPOS("DIGI_POS_TAG")

}

enum class EnumDigiPosProcess(val code:String){
    InitializeDigiPOS("1"),
    UPIDigiPOS("2"),
    SMS_PAYDigiPOS("5"),
    GET_STATUS("6"),
    TXN_LIST("7"),
    DYNAMIC_QR("3"),
    STATIC_QR("4"),


}
enum class EnumDigiPosProcessingCode(val code: String) {
    DIGIPOSPROCODE("982003")
}

enum class EnumDigiPosTerminalStatusCode(val code: String, val description:String){
    TerminalStatusCodeE106("E106","Decryption Failed"),
    TerminalStatusCodeP101("P101","Invalid Request"),
   // StatusCodeP101("P101","Terminal ID is null or Invalid"),
    TerminalStatusCodeS102("S102","Failed"),
    TerminalStatusCodeS101("S101","Success")
}
enum class EDigiPosTerminalStatusResponseCodes(val statusCode:String){
    SuccessString("Success"),
    FailString("Failed"),
    ActiveString("Active"),
    DeactiveString("Deactive"),
}

enum class EDigiPosPaymentStatus(val code: Int,val desciption:String){
    Pending(0,"InProgress"),
    Approved(1,"Success"),
    Failed(2,"SaleFailed"),
    UNKnown(3,"Something went wrong"),

}

fun saveDateInServerFormatDigipos(): String {
    val dateNow = Date()
    val ft4 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return ft4.format(dateNow)
}
fun getDateInDisplayFormatDigipos(dateStr:String): String {
    //val dateStr = "2021-06-11 11:00:45"//Date()
    val ft = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
    val ft2 = SimpleDateFormat("dd MMMM, h:mm aa", Locale.getDefault())
    return ft2.format(ft)
}

fun getCurrentDateInDisplayFormatDigipos(): String
{
    val dNow =Date()
    val fttt = SimpleDateFormat("dd MMMM, h:mm aa", Locale.getDefault())
    return fttt.format(dNow)
}








