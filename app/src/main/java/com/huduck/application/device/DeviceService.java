package com.huduck.application.device;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.huduck.application.bleCentral.CentralCallback;
import com.huduck.application.bleCentral.CentralManager;
import com.huduck.application.common.CommonMethod;
import com.huduck.application.notification.CallListener;
import com.huduck.application.notification.SMSReceiver;

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

    public void refreshDeviceList() {
        isFirstScan = false;
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

    public void updateSpeed(int speed) {
        if(!centralManager.isConnected()) return;
        // 속도 필터 (0~255)
        speed = Math.min(speed, 255);
        speed = Math.max(0, speed);

        // 전송 데이터
        updateQueue(new StringBuilder("s").append(speed).toString());
    }

    public void updateCall(String name, int callState) {
        if(!centralManager.isConnected()) return;
        String sendData = new StringBuilder("c")
                .append(CommonMethod.subStringBytes(name, 15*3, 3))
                .append(callState)
                .toString();

        updateQueue(sendData);
//        Log.d(TAG, "(Call) name: " + name + ", state: " + callType);
    }

    public void updateSms(String name, String content) {
        if(!centralManager.isConnected()) return;
        Log.d(TAG, "(SMS) name: " + name + ", content: " + content);
        StringBuilder sb = new StringBuilder("m");
        sb
                .append(CommonMethod.subStringBytes(name, 5 * 3, 3)).append("{]")
                .append(CommonMethod.subStringBytes(content, 25 * 3, 3));

        updateQueue(sb.toString());
    }

    public void updateKakaoTalk(String name, String content) {
        if(!centralManager.isConnected()) return;
        Log.d(TAG, "(KakaoTalk) name: " + name + ", content: " + content);
        StringBuilder sb = new StringBuilder("k");
        sb
                .append(CommonMethod.subStringBytes(name, 5 * 3, 3)).append("{]")
                .append(CommonMethod.subStringBytes(content, 25 * 3, 3));

        updateQueue(sb.toString());
    }

    private void updateQueue(byte[] bytes) {
        if(bytes.length > 20) return;
        dataQueue.add(bytes);
        sendQueue();
    }

    private void updateQueue(String data) {
        int DATA_SIZE = 19;
        int QUEUE_SIZE = DATA_SIZE + 1;

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
            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onStartScan();
        }

        @Override
        public void onFinishScan(Map<String, BluetoothDevice> scanResult) {
            for (CentralCallback centralCallback : centralCallbackList)
                centralCallback.onFinishScan(scanResult);
        }

        @Override
        public void connectedGattServer() {
            isConnected = true;

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