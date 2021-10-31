package com.huduck.application.device;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.huduck.application.bleCentral.CentralCallback;
import com.huduck.application.bleCentral.CentralManager;
import com.huduck.application.common.CommonMethod;
import com.huduck.application.notification.CallListener;
import com.huduck.application.notification.SMSReceiver;
import com.huduck.application.setting.SettingConstants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class DeviceService extends Service {
    private static final String TAG = "DeviceService";

    private boolean isConnected = false;
    public boolean isConnected() {
        return isConnected;
    }

    private boolean isScanning = false;
    public boolean isScanning() {
        return isScanning;
    }

    TelephonyManager telephony;
    CallListener callListener;
    SMSReceiver smsReceiver;

    SharedPreferences sharedPreferences;
    private String registeredDeviceAddress;

    private CentralManager centralManager;

    private Queue<byte[]> dataQueue = new LinkedList<>();

    private List<BluetoothDevice> scanDeviceList = new ArrayList<>();
    public List<BluetoothDevice> getScanDeviceList() {
        return scanDeviceList;
    }

    public DeviceService() {}

    DeviceServiceBinder binder = new DeviceServiceBinder();
    public class DeviceServiceBinder extends Binder {
        public DeviceService getService() {
            return DeviceService.this;
        }
    }

    private boolean isFirstScan = true;
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("device", MODE_PRIVATE);
        registerDevice(sharedPreferences.getString("deviceAddress", null));

        initBLE();
        initReceiver();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void initBLE() {
        isFirstScan = true;
        centralManager = CentralManager.getInstance(this);
        centralManager.setCallBack(centralCallback);
        centralManager.initBle();
        centralManager.startScan();
    }

    private void initReceiver() {
        Context context = getApplicationContext();

        // Call
        callListener = new CallListener(context, this);
        telephony = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Sms
        smsReceiver = new SMSReceiver(this);
        IntentFilter smsFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, smsFilter);

        // KakaoTalk
        LocalBroadcastManager.getInstance(this).registerReceiver(kakaoTalkReceiver, new IntentFilter("kakaotalk"));
    }

    private BroadcastReceiver kakaoTalkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent.getStringExtra("name");
            String content = intent.getStringExtra("content");
            updateKakaoTalk(name, content);
        }
    };

    private boolean refresh = false;
    public void refreshDeviceList() {
        if(!centralManager.isConnected()) {
            refreshDevice();
            return;
        }

        refresh = true;
        isFirstScan = false;
        centralManager.disconnectGattServer();
    }

    private void refreshDevice() {
        refresh = false;
        scanDeviceList = new ArrayList<>();
        centralManager.startScan();
    }

    public void registerDevice(BluetoothDevice device) {
        String deviceAddress = device.getAddress();
        sharedPreferences
                .edit()
                .putString("deviceAddress", deviceAddress)
                .commit();

        registerDevice(deviceAddress);

        centralManager.connectDevice(device);
    }

    private void registerDevice(String deviceAddress) {
        if(deviceAddress == null) return;
        registeredDeviceAddress = deviceAddress;
    }

    public String getRegisteredDeviceAddress() {
        sharedPreferences = getSharedPreferences("device", MODE_PRIVATE);
        return sharedPreferences.getString("deviceAddress", null);
    }

    public void updateSpeed(float speedMs) {
        if(!centralManager.isConnected()) return;
        // 전송 데이터
        String data = new StringBuilder("s").append(speedMs).toString();
        updateQueue(data);
    }

    public void updateCall(String name, int callState) {
        if(!centralManager.isConnected()) return;
        String sendData = new StringBuilder("c")
                .append(CommonMethod.subStringBytes(name, 30 * 3, 3))
                .append(callState)
                .toString();

        updateQueue(sendData);
//        Log.d(TAG, "(Call) name: " + name + ", state: " + callType);
    }

    public void updateSms(String name, String content) {
        if(!centralManager.isConnected()) return;
        name = name.replaceAll("\\{]", "{}");
        content = content.replaceAll("\\{]", "{}");

        Log.d(TAG, "(SMS) name: " + name + ", content: " + content);
        StringBuilder sb = new StringBuilder("m");
        sb
                .append(CommonMethod.subStringBytes(name, 5 * 3, 3)).append("{]")
                .append(CommonMethod.subStringBytes(content, 25 * 3, 3));

        updateQueue(sb.toString());
    }

    public void updateKakaoTalk(String name, String content) {
        if(!centralManager.isConnected()) return;
        name = name.replaceAll("\\{]", "{}");
        content = content.replaceAll("\\{]", "{}");

        Log.d(TAG, "(KakaoTalk) name: " + name + ", content: " + content);
        StringBuilder sb = new StringBuilder("k");
        sb
                .append(CommonMethod.subStringBytes(name, 5 * 3, 3)).append("{]")
                .append(CommonMethod.subStringBytes(content, 25 * 3, 3));

        updateQueue(sb.toString());
    }

    public void updateNavigationTurnEvent(int nextEventTurnType, double nextEventLeftDistanceM,
                                          double nextEventRelationalPositionX, double nextEventRelationalPositionY,
                                          int nextNextEventTurnType, double nextNextEventLeftDistanceM) {

        if(!centralManager.isConnected()) return;
        String data = new StringBuilder("n")
                .append(nextEventTurnType).append("{]")
                .append(nextEventLeftDistanceM).append("{]")
                .append(nextEventRelationalPositionX).append("{]")
                .append(nextEventRelationalPositionY).append("{]")
                .append(nextNextEventTurnType).append("{]")
                .append(nextNextEventLeftDistanceM).append("{]").toString();

        updateQueue(data);
    }

    public void updateSetting(String settingItem, String settingValue) {
        if(!centralManager.isConnected()) return;
        Log.d(TAG, "(Setting) item: " + settingItem + ", value: " + settingValue);
        StringBuilder sb = new StringBuilder("p");
        sb
                .append(settingItem).append("{]")
                .append(settingValue);

        updateQueue(sb.toString());
    }

    private void updateQueue(byte[] bytes) {
        if(bytes.length > 20) return;
        dataQueue.add(bytes);
        sendQueue();
    }

    private void updateQueue(String data) {
        if(!centralManager.isConnected()) return;

        int QUEUE_SIZE = centralManager.getMtu();
        int DATA_SIZE = QUEUE_SIZE - 1;

        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        if(bytes.length == 0) return;

        int leftBytes = bytes.length;
        int i = 0;
        while (leftBytes > 0) {
            int startIdx = i * DATA_SIZE;
            int endIdx = (i + 1) * DATA_SIZE;

            byte[] queueBytes = new byte[QUEUE_SIZE];
            if (leftBytes <= DATA_SIZE)
                queueBytes[0] = 1;
            else {
                queueBytes[0] = 0;
                endIdx = bytes.length;
            }

            System.arraycopy(
                    Arrays.copyOfRange(bytes, startIdx, endIdx), 0,
                    queueBytes, 1, DATA_SIZE);

            dataQueue.add(queueBytes);

            i++;
            leftBytes -=  Math.min(leftBytes, DATA_SIZE);
        }

        sendQueue();
    }

    private boolean wroteData = false;
    private void sendQueue() {
        if(!centralManager.isConnected()) return;
        if(dataQueue.size() <= 0) return;

        if(centralManager.isWritable() && !wroteData) {
            wroteData = true;
            centralManager.sendData(dataQueue.peek());
        }
    }

    private CentralCallback centralCallback = new CentralCallback() {
        @Override
        public void requestEnableBLE() {
            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.requestEnableBLE();
        }

        @Override
        public void requestLocationPermission() {
            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.requestLocationPermission();
        }

        @Override
        public void onStartScan() {
            isScanning = true;

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onStartScan();
        }

        @Override
        public void onFinishScan(Map<String, BluetoothDevice> scanResult) {
            isScanning = false;

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onFinishScan(scanResult);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void connectedGattServer() {
            isConnected = true;
            isScanning = false;

            // send MTU
            updateSetting("mtu", Integer.valueOf(centralManager.getMtu()).toString());

            // send setting value
            SharedPreferences preferenceSharedPreferences = getSharedPreferences("preference", MODE_PRIVATE);
            if(preferenceSharedPreferences.contains("saved")) {
                SettingConstants.SETTING_ITEM_MAP.forEach((String key, Class class_) -> {
                    String value = null;
                    if(class_ == String.class) {
                        value = preferenceSharedPreferences.getString(key, null);
                    }
                    else if(class_ == Boolean.class) {
                        value = preferenceSharedPreferences.getBoolean(key, false) ? "true" : "false";
                    }

                    if(value != null)
                        updateSetting(key, value);
                });
            }
            else {
                SettingConstants.SETTING_ITEM_MAP.forEach((String key, Class class_) -> {
                    String value = SettingConstants.SETTING_DEFAULT_VALUE.get(key);

                    if(value != null)
                        updateSetting(key, value);
                });
            }

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.connectedGattServer();
        }

        @Override
        public void disconnectedGattServer() {
            isConnected = false;
            wroteData = false;
            dataQueue.clear();

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.disconnectedGattServer();

            if(refresh)
                refreshDevice();
        }

        @Override
        public void onWrite() {
            if(wroteData) {
                wroteData = false;
                if(dataQueue.size() > 0)
                    dataQueue.poll();
                sendQueue();
            }

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onWrite();
        }

        @Override
        public void onFindNewDevice(BluetoothDevice bluetoothDevice) {
            scanDeviceList.add(bluetoothDevice);

            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onFindNewDevice(bluetoothDevice);

            if(isFirstScan && registeredDeviceAddress != null) {
                if(!bluetoothDevice.getAddress().equals(registeredDeviceAddress)) return;

                // 첫번째 스캔이고, 등록된 디바이스 검색되었으면
                centralManager.connectDevice(bluetoothDevice);
            }
        }
    };

    List<CentralCallback> centralCallbackList = new ArrayList<>();
    public void registerCentralCallback(CentralCallback centralCallback) {
        centralCallbackList.add(centralCallback);
    }

    public void unregisterCentralCallback(CentralCallback centralCallback) {
        centralCallbackList.remove(centralCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        centralManager.disconnectGattServer();
        telephony.listen(callListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(smsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(kakaoTalkReceiver);
    }
}