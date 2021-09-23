package com.huduck.application.bleCentral;

import java.util.UUID;

public class Constants {
    // used to identify adding bluetooth names
    public final static int REQUEST_ENABLE_BT = 3054;
    // used to request fine location permission
    public final static int REQUEST_FINE_LOCATION = 3055;
    // scan period in milliseconds
    public final static int SCAN_PERIOD = 100000;

    public static String SERVICE_STRING = "00000001-1E3C-FAD4-74E2-97A033F1BFAA";
    public static final UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);
    public static String CHARACTERISTIC_UUID = "00000002-1E3C-FAD4-74E2-97A033F1BFAA";
    public static String CONFIG_UUID = "00002901-0000-1000-8000-00805f9b34fb";
}
