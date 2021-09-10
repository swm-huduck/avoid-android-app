package com.huduck.application.bleCentral;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public interface CentralCallback {

    void requestEnableBLE();

    void requestLocationPermission();

    void onStatusMsg(final String message);

    void onToast(final String message);

    void finishedScan(Map<String, BluetoothDevice> scanResult);

    void connectedGattServer();

    void disconnectedGattServer();

    void onWrite();
}
