package com.huduck.application.device;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.huduck.application.bleCentral.CentralCallback;
import com.huduck.application.bleCentral.CentralManager;
import com.huduck.application.notification.CallListener;
import com.huduck.application.notification.MyNotificationListenerService;
import com.huduck.application.notification.SMSReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceService extends Service {
    private static final String TAG = "DeviceService";

    TelephonyManager telephony;
    CallListener callListener;
    SMSReceiver smsReceiver;

    SharedPreferences sharedPreferences;

    private CentralManager centralManager;
    private Map<String, BluetoothDevice> scanResult;
    private String deviceAddress;

    public DeviceService() {}

    DeviceServiceBinder binder = new DeviceServiceBinder();
    public class DeviceServiceBinder extends Binder {
        public DeviceService getService() {
            return DeviceService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        sharedPreferences = getSharedPreferences("device", MODE_PRIVATE);
        registerDevice(sharedPreferences.getString("deviceAddress", null));

        initBLE();
        initReceiver();
        return binder;
    }

    private void initBLE() {
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

    public void registerDevice(BluetoothDevice device) {
        String deviceAddress = device.getAddress();
        sharedPreferences
                .edit()
                .putString("deviceAddress", deviceAddress)
                .commit();

        registerDevice(deviceAddress);

        centralManager.startScan();
    }

    private void registerDevice(String deviceAddress) {
        if(deviceAddress == null) return;
        this.deviceAddress = deviceAddress;
    }

    public void updateSpeed(int speed) {
        Log.d(TAG, "(Speed) value: " + speed);
    }

    public void updateCall(String name, int callState) {
        String callType = "";
        switch (callState) {
            case TelephonyManager.CALL_STATE_RINGING:
                callType = "전화 옴";
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                callType = "전화 중";
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                callType = "전화 종료";
                break;
        }

        Log.d(TAG, "(Call) name: " + name + ", state: " + callType);
    }

    public void updateSms(String name, String content) {
        Log.d(TAG, "(Sms) name: " + name + ", content: " + content);
    }

    public void updateKakaoTalk(String name, String content) {
        Log.d(TAG, "(KakaoTalk) name: " + name + ", content: " + content);
    }

    private CentralCallback centralCallback = new CentralCallback() {
        @Override
        public void requestEnableBLE() {}

        @Override
        public void requestLocationPermission() {}

        @Override
        public void onStatusMsg(String message) {}

        @Override
        public void onToast(String message) {}

        @Override
        public void finishedScan(Map<String, BluetoothDevice> scanResult) {
            DeviceService.this.scanResult = scanResult;

            if(deviceAddress != null && !centralManager.isConnected())
                for (String address : scanResult.keySet())
                    if (address.equals(deviceAddress)) {
                        BluetoothDevice device = scanResult.get(address);
                        centralManager.connectDevice(device);
                    }

            for (OnFinishedScanCallback onFinishedScanCallback : onFinishedScanCallbackList)
                onFinishedScanCallback.onFinishedScan(scanResult);
        }

        @Override
        public void connectedGattServer() {}

        @Override
        public void disconnectedGattServer() {}

        @Override
        public void onWrite() {}
    };

    public interface OnFinishedScanCallback {
        void onFinishedScan(Map<String, BluetoothDevice> scanResult);
    }

    List<OnFinishedScanCallback> onFinishedScanCallbackList = new ArrayList<>();
    public void registerOnFinishedScanCallback(OnFinishedScanCallback onFinishedScanCallback) {
        onFinishedScanCallbackList.add(onFinishedScanCallback);
        if(scanResult != null)
            onFinishedScanCallback.onFinishedScan(scanResult);
    }

    public void unregisterOnFinishedScanCallback(OnFinishedScanCallback onFinishedScanCallback) {
        onFinishedScanCallbackList.remove(onFinishedScanCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        centralManager.disconnectGattServer();
        telephony.listen(null, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(smsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(kakaoTalkReceiver);
    }
}