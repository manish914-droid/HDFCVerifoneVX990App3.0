// ISettingsManager.aidl
package com.vfi.smartpos.system_service.aidl.settings;

// Declare any non-default types here with import statements
import android.os.Bundle;

interface ISettingsManager {

    /**
    * settings set actions
    *
    * @version 1.6.0.0
    * @param settingsType: please refer to SettingType class
    * @param bundle: passing corresponding parameters according to the settingsType parameter.
    *   For example:
    *   SettingsType.DATE_TIME:
    *       //set system time whether to sync auto network time
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *       bundle.putInt("AUTO_SYSTEM_TIME", 0);
    *       0: disable sync
    *       1: sync
    *
    *   @return 0: set success;   -1: set fail
    *
    *       //set system time zone whether to sync auto network time zone
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_ZONE_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *       bundle.putInt("AUTO_SYSTEM_TIME_ZONE", 0);
    *       0: disable sync
    *       1: sync
    *
    *   @return 0: set success;   -1: set fail
    *
    *   @version 1.6.0.1
    *       //set system time
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.SET_AUTO_SYSTEM_TIME_ZONE_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *       bundle.putString("SYSTEM_DATE", "20200707"); date format is yyyyMMdd;
    *       bundle.putString("SYSTEM_TIME", "150629"); time format is HHmmss;
    *
    *   @return 0: set success;   -1: set fail
    *
    *   @version 1.6.0.1
    *   SettingsType.LAUNCHER:
    *       //set launcher
    *        Bundle bundle = new Bundle();
    *       bundle.putString("LAUNCHER_ACTIONS", SettingsActions.LauncherActions.SET_LAUNCHER);
    *       More informations of "LAUNCHER_ACTIONS" pleae refer to SettingsActions.LauncherActions class;
    *       bundle.putString("LAUNCHER_PACKAGE_NAME", "com.vfi.android.payment"); // passing launcher application package name.
    *       bundle.putBoolean("RUN_PACKAGE", true); //true: run application, false: not run application.
    *
    *   @return 0: set success;   -1: set fail
    *
    **/
    int settingsSetActions(int settingsType, in Bundle bundle);

    /**
    * settings read actions
    *
    * @version 1.6.0.0
    * @param settingsType: please refer to SettingType
    * For example: SettingsType.DATE_TIME
    * @param bundle: passing corresponding parameters according to the settingsType parameter.
    *   For example:
    *   SettingsType.DATE_TIME:
    *       //get system time whether to sync auto network time
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *
    *   @return bundle
    *       int state = bundle.getInt("AUTO_SYSTEM_TIME");
    *       state: 1: sync auto system time;    0: disable sync auto system time
    *
    *       //get system time zone whether to sync auto network time zone
    *       Bundle bundle = new Bundle();
    *       bundle.putString("SYSTEM_TIME_ACTIONS", SettingsActions.SystemTimeActions.GET_AUTO_SYSTEM_TIME_ZONE_STATE);
    *       More informations of "SYSTEM_TIME_ACTIONS" pleae refer to SettingsActions.SystemTimeActions class;
    *
    *   @return bundle
    *       int state = bundle.getInt("AUTO_SYSTEM_TIME");
    *       state: 1: sync auto system time zone;    0: disable sync auto system time zone
    **/
    Bundle settingsReadActions(int settingsType, in Bundle bundle);


    /**
    * set PCI reboot time.
    * @param hour(24-hour format range:0~23)
    * @param min(range:0~59)
    * @param sec(range:0~59)
    * @return result
    **/
    boolean settingPCIRebootTime(int hour, int min, int sec);

    /**
    * get PCI reboot time.
    * @return seconds for unit
    **/
    long getPCIRebootTime();
}
