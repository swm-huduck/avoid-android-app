package com.huduck.application.bleCentral;

import android.bluetooth.BluetoothDevice;

import java.util.Map;

public interface CentralCallback {
    void requestEnableBLE();

    void requestLocationPermission();

    /* 스캔 관련 함수 */
    void onStartScan();

    void onFindNewDevice(BluetoothDevice bluetoothDevice);

    void onFinishScan(Map<String, BluetoothDevice> scanResult);

    /* 연결 관련 함수 */
    void connectedGattServer();

    void disconnectedGattServer();

    void onWrite();
}
