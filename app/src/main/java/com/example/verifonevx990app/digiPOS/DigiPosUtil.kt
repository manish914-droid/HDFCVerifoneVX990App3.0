package com.example.verifonevx990app.digiPOS

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
    Failed(2,"Failed"),
    UNKnown(3,"Something went wrong"),

}






