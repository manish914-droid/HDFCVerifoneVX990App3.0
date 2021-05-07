package com.vfi.smartpos.system_service.aidl.networks;


/*
 *Exposed APIs for Application to control terminal networks
 *
*/
interface INetworkManager {
    /*
      Set network type of terminal :
      status code:
        7 = Global
        6 = EvDo only
        5 = CDMA w/o EvDo
        4 = CDMA / EvDo auto
        3 = GSM / WCDMA auto
        2 = WCDMA only
        1 = GSM only
        0 = GSM / WCDMA preferred
        -1 = acquiring failed
    */
    void setNetworkType(int mode);
    /*
    Get current network type of terminal
    */
    int getNetworkType();
    /*
     Control WiFi
    */
    void enableWifi(boolean state);
    /*
      Control Airplane mode
    */
    void enableAirplayMode(boolean state);

    /*
    * Add a new APN configuration and set it to default <br/>
    * @param infos - add APN infos in bundle, example:
            Bundle infos = new Bundle();
            infos.putString("name", "test01");
            infos.putString("apn", "test01");
            infos.putString("authtype", "-1");
            //numeric is mcc and mnc
            infos.putString("numeric", "46002");
    //        infos.putString("mcc", "460");
    //        infos.putString("mnc", "02");
            infos.putString("proxy","");
            infos.putString("port","");
            infos.putString("mmsproxy", "");
            infos.putString("mmsport", "");
            infos.putString("user", "");
            infos.putString("server", "");
            infos.putString("password","");
            infos.putString("mmsc", "");
            infos.putString("current", "1");
            infos.putString("carrier_enabled", "1");
            infos.putString("protocol", "IP");
            infos.putString("roaming_protocol", "IP");
            infos.putString("bearer", "0");
            infos.putString("max_conns", "0");
            infos.putString("max_conns_time", "0");
            infos.putString("modem_cognitive", "0");
            infos.putString("localized_name", "");
            infos.putString("mvno_match_data", "");
            infos.putString("mvno_type", "");
            infos.putString("profile_id", "0");
            infos.putString("read_only", "0");
            infos.putString("sub_id", "1");
            infos.putString("type", "");

            // "SLOT"   // Add by Simon on version 1.6.0.3
            // SLOT: 1 or 2 for SIM card in slot 1 or 2.
            // using the active slot as default if there is no SLOT setting & no "fixed_numeric" setting
            infos.putString("SLOT", "1");

            // "fixed_numeric"  // add by Simon on version 1.6.0.3
            // fixed the numeric to fixed_numeric for specific SIM card
            // using the "SLOT" if there is no "fixed_numeric" setting
            infos.putString("fixed_numeric", "46002");
    */
    int setAPN(in Bundle infos);
    /*
      Control Mobile data
    */
    void enableMobileData( boolean state);
    /*
      Get current selected APN configurations
    */
    Bundle getSelectedApnInfo();

    /**
    * slotIdx 1 or 2 to select it use for mobile data
    * @version 1.7.0.1
    * */
    int selectMobileDataOnSlot( int slotIdx);

    /**
    * isMultiNetwork
    */
    boolean isMultiNetwork();

    /**
    * setMultiNetwork
    */
    void setMultiNetwork(boolean enable);

    /**
    * getMultiNetworkPrefer
    */
    String getMultiNetworkPrefer();

    /**
    * setMultiNetworkPrefer
    * public static final String TRANSPORT_WIFI_ETHERNET_CELLULAR = "wifi,ethernet,cellular";
    * public static final String TRANSPORT_WIFI_CELLULAR_ETHERNET = "wifi,cellular,ethernet";
    * public static final String TRANSPORT_CELLULAR_WIFI_ETHERNET = "cellular,wifi,ethernet";
    * public static final String TRANSPORT_CELLULAR_ETHERNET_WIFI = "cellular,ethernet,wifi";
    * public static final String TRANSPORT_ETHERNET_CELLULAR_WIFI = "ethernet,cellular,wifi";
    * public static final String TRANSPORT_ETHERNET_WIFI_CELLULAR = "ethernet,wifi,cellular";
    */
    boolean setMultiNetworkPrefer(String prefer);
}
