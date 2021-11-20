package com.huduck.application.bleCentral;

import static com.huduck.application.bleCentral.Constants.CHARACTERISTIC_UUID;
import static com.huduck.application.bleCentral.Constants.CONFIG_UUID;
import static com.huduck.application.bleCentral.Constants.SCAN_PERIOD;
import static com.huduck.application.bleCentral.Constants.SERVICE_UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

public class CentralManager {
    private final String TAG = CentralManager.class.getSimpleName();

    protected static volatile CentralManager sInstance = null;

    private Context mContext;

    // ble adapter
    private BluetoothAdapter bleAdapter;
    // flag for scanning
    private boolean isScanning = false;
    // flag for connection
    @Getter
    private boolean isConnected = false;
    // scan results
    private Map<String, BluetoothDevice> scanResults;
    // scan callback
    private ScanCallback scanCallback;
    // ble scanner
    private BluetoothLeScanner bleScanner;
    // scan handler
    private Handler scanHandler;

    // BLE Gatt
    private BluetoothGatt bleGatt;
    @Getter
    private boolean isWritable = false;
    @Getter
    private int mtu = 0;

    // Callback listener
    private CentralCallback listener;

    public CentralManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public void setCallBack(CentralCallback listener) {
        this.listener = listener;
    }

    public static CentralManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CentralManager(context);
        }

        return sInstance;
    }

    public void initBle() {
        if(bleAdapter != null) return;
        // ble manager
        BluetoothManager ble_manager;
        ble_manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        // set ble adapter
        bleAdapter = ble_manager.getAdapter();
    }

    /**
     * Start BLE scan
     */
    public void startScan() {
        /**
         * 위치 권한이 있는지 체크한다.
         * 없다면 콜백으로 requestLocationPermission() 호출한다.
         */
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.requestLocationPermission();
//            listener.onStatusMsg("Scanning Failed: no fine location permission");
            return;
        }

        /**
         * 이미 Gatt Server 와 연결된 상태일 수 있으니 호출해준다.
         */
        if(isScanning)
            stopScan();

//        listener.onStatusMsg("Scanning...");
        /**
         * 블루투스를 사용할 수 있는 상태인지 체크한다.
         */
        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            listener.requestEnableBLE();
//            listener.onStatusMsg("Scanning Failed: ble not enabled");
            return;
        }

        bleScanner = bleAdapter.getBluetoothLeScanner();

        //// set scan filters
        // create scan filter list
        List<ScanFilter> filters = new ArrayList<>();

        //// scan settings
        // set low power scan mode
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanResults = new HashMap<String, BluetoothDevice>();
        scanCallback = new BLEScanCallback(scanResults);

        //// now ready to scan
        // start scan
        bleScanner.startScan(filters, settings, scanCallback);
        // set scanning flag
        isScanning = true;

        scanHandler = new Handler();
        scanHandler.postDelayed(this::stopScan, SCAN_PERIOD);

        listener.onStartScan();
    }

    /**
     * Stop scanning
     */
    private void stopScan() {
        // check pre-conditions
        if (isScanning && bleAdapter != null && bleAdapter.isEnabled() && bleScanner != null) {
            // stop scanning
            bleScanner.stopScan(scanCallback);
            listener.onFinishScan(scanResults);
        }
        // reset flags
        if (scanCallback != null)
            scanCallback = null;
        if (scanHandler != null)
            scanHandler = null;
        isScanning = false;
    }

    /**
     * Handle scan results after scan stopped
     */
    public void connectDevice(BluetoothDevice device) {
        stopScan();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                // check if nothing found
                if (scanResults.isEmpty()) {
//            listener.onStatusMsg("scan results is empty");
                    Log.d(TAG, "scan results is empty");
                    return;
                }

                if(!scanResults.containsValue(device)) {
//            listener.onStatusMsg("device is unknown");
                    Log.d(TAG, "device is unknown");
                    return;
                }

                // update the status
//        listener.onStatusMsg("Connecting to " + device.getAddress());
                GattClientCallback gatt_client_cb = new GattClientCallback();
                /*bleGatt = device.connectGatt(mContext, false, gatt_client_cb, BluetoothDevice.TRANSPORT_LE);*/

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bleGatt= device.connectGatt( mContext, false, gatt_client_cb, BluetoothDevice.TRANSPORT_LE);
                }
                else {
                    bleGatt= device.connectGatt( mContext, false, gatt_client_cb);
                }
            }
        }, 500);
    }

    /**
     * Disconnect Gatt Server
     */
    public void disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection");
        // reset the connection flag
        isConnected = false;
        isWritable = false;
        // disconnect and close the gatt
        if (bleGatt != null) {
            bleGatt.disconnect();
            bleGatt.close();
        }
        // listener.onStatusMsg("Closing Gatt connection");
        listener.disconnectedGattServer();
    }

    /**
     * Send Data
     * Gatt Server 에 데이타를 보낸다.
     * 20Byte 까지만 보낼 수 있다.
     */
    public void sendData(String message) {
        sendData(message.getBytes());
    }

    public void sendData(byte[] messageBytes) {
        // check connection
        if (!isConnected) {
            Log.e(TAG, "Failed to sendData due to no connection");
            return;
        }
        // find command characteristics from the GATT server
        BluetoothGattCharacteristic cmd_characteristic = BluetoothUtils.findCharacteristic(bleGatt, CHARACTERISTIC_UUID);
        // disconnect if the characteristic is not found
        if (cmd_characteristic == null) {
            Log.e(TAG, "Unable to find cmd characteristic");
            disconnectGattServer();
            return;
        }

        cmd_characteristic.setValue(messageBytes); // 20byte limit
        cmd_characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        // write the characteristic
        boolean success = bleGatt.writeCharacteristic(cmd_characteristic);
        BluetoothGattDescriptor descriptor = cmd_characteristic.getDescriptor(UUID.fromString(CONFIG_UUID));
//        descriptor.setValue(messageBytes);
//        boolean success = bleGatt.writeDescriptor(descriptor);
        isWritable = false;
        // check the result
        if (success) {
// //            listener.onStatusMsg("write : " + message);
            Log.e(TAG, "Success to write command");
//            listener.onWrite();
        } else {
            Log.e(TAG, "Failed to write command : " + cmd_characteristic.getUuid());
            Log.e(TAG, "Failed to write command");
            // listener.onStatusMsg("Failed to write command");
//            disconnectGattServer();
        }
    }

    /**
     * BLE Scan Callback class
     */
    private class BLEScanCallback extends ScanCallback {
        private Map<String, BluetoothDevice> cb_scan_results;

        /**
         * Constructor
         * @param _scan_results
         */
        BLEScanCallback(Map<String, BluetoothDevice> _scan_results) {
            cb_scan_results = _scan_results;
        }

        @Override
        public void onScanResult(int _callback_type, ScanResult _result) {
            Log.d(TAG, "onScanResult");
            addScanResult(_result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> _results) {
            for (ScanResult result : _results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int _error) {
            Log.e(TAG, "BLE scan failed with code " + _error);
        }

        /**
         * Add scan result
         * @param _result
         */
        private void addScanResult(ScanResult _result) {
            // get scanned device
            BluetoothDevice device = _result.getDevice();
            // get scanned device MAC address
            String device_address = device.getAddress();
            if(cb_scan_results.containsKey(device_address)) return;

            // add the device to the result list
            cb_scan_results.put(device_address, device);
            // log
            listener.onFindNewDevice(device);
            // listener.onStatusMsg("scan results device: " + device_address + ", " + device.getName());
            Log.e(TAG, "scan results device: " + device_address + ", " + device.getName());
        }
    }

    /**
     * Gatt Client Callback class
     */
    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int _mtu, int status) {
            super.onMtuChanged(gatt, _mtu, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                mtu = _mtu - 3;
                gatt.discoverServices();
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt _gatt, int _status, int _new_state) {
            super.onConnectionStateChange(_gatt, _status, _new_state);
            if (_status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (_status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (_new_state == BluetoothProfile.STATE_CONNECTED) {
                // update the connection status message
                // listener.onStatusMsg("Connected");
                isWritable = true;
                // set the connection flag
                isConnected = true;
                Log.d(TAG, "Connected to the GATT server");
                // _gatt.discoverServices();
                _gatt.requestMtu(512);
            } else if (_new_state == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt _gatt, int _status) {
            super.onServicesDiscovered(_gatt, _status);
            // check if the discovery failed
            if (_status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery failed, status: " + _status);
                return;
            }

            Log.e(TAG, "getDevice().getAddress() : " + _gatt.getDevice().getAddress() + ", " + _gatt.getDevice().getName());
            // find discovered characteristics
            List<BluetoothGattCharacteristic> matching_characteristics = BluetoothUtils.findBLECharacteristics(_gatt);
            for (BluetoothGattCharacteristic characteristic : matching_characteristics) {
                Log.e(TAG, "characteristic : " + characteristic.getUuid());
            }

            if (matching_characteristics.isEmpty()) {
                Log.e(TAG, "Unable to find characteristics");
                return;
            }

            // log for successful discovery
            Log.d(TAG, "Services discovery is successful");

            // Set CharacteristicNotification
            BluetoothGattCharacteristic cmd_characteristic = BluetoothUtils.findCharacteristic(bleGatt, CHARACTERISTIC_UUID);
            _gatt.setCharacteristicNotification(cmd_characteristic, true);
            /*BluetoothGattDescriptor descriptor = cmd_characteristic.getDescriptor(UUID.fromString(CONFIG_UUID));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean s = _gatt.writeDescriptor(descriptor);
            if(s) {
                s = false;
            }*/
            listener.connectedGattServer();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt _gatt, BluetoothGattCharacteristic _characteristic) {
            super.onCharacteristicChanged(_gatt, _characteristic);

            Log.d(TAG, "characteristic changed: " + _characteristic.getUuid().toString());
            readCharacteristic(_characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt _gatt, BluetoothGattCharacteristic _characteristic, int _status) {
            super.onCharacteristicWrite(_gatt, _characteristic, _status);
            if (_status == BluetoothGatt.GATT_SUCCESS || _status == 14) {
                Log.d(TAG, "Characteristic written successfully");
                isWritable = true;
                listener.onWrite();
            } else {
                Log.e(TAG, "Characteristic write unsuccessful, status: " + _status);
                disconnectGattServer();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic read successfully");
                readCharacteristic(characteristic);
            } else {
                Log.e(TAG, "Characteristic read unsuccessful, status: " + status);
                // Trying to read from the Time Characteristic? It doesnt have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer();
            }
        }

        /**
         * Log the value of the characteristic
         * @param _characteristic
         */
        private void readCharacteristic(BluetoothGattCharacteristic _characteristic) {
            byte[] msg = _characteristic.getValue();
            String message = new String(msg);
            Log.d(TAG, "read: " + message);
            // listener.onStatusMsg("read : " + message);
//            listener.onToast("read : " + message);
        }
    }
}
