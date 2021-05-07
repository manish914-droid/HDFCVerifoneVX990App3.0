// RequestACTypeHandler.aidl
package com.vfi.smartpos.deviceservice.aidl;


interface RequestACTypeHandler {
    /**
    * @param aid, current aid
    * @param defaultACType 0-AAC, 1-ARQC, 2-TC
    */
    void requestConfirmACType(String aid, int defaultACType);
}
