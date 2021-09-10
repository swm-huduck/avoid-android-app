package com.huduck.application.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;

import com.huduck.application.device.DeviceService;

import java.util.Date;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";

    private DeviceService deviceService;

    public SMSReceiver(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        SmsMessage[] messages = parseSmsMessage(bundle);

        if(messages.length <= 0) return;

        String phoneNo = messages[0].getOriginatingAddress();
        String content = messages[0].getMessageBody();
        if(phoneNo == null || phoneNo.isEmpty()) return;
        if(content == null || content.isEmpty()) return;

        // 전화번호부 확인
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNo));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        String displayName = "";
        boolean flagDisplayName = false;
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                //이름이 있으면 찾고
                displayName = cursor.getString(0);
                flagDisplayName = true;
            }
        }
        cursor.close();

        deviceService.updateSms(flagDisplayName ? displayName : phoneNo, content);
    }

    private SmsMessage[] parseSmsMessage(Bundle bundle){
        Object[] objs = (Object[])bundle.get("pdus");
        SmsMessage[] messages = new SmsMessage[objs.length];

        for(int i=0;i<objs.length;i++){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                String format = bundle.getString("format");
                messages[i] = SmsMessage.createFromPdu((byte[])objs[i], format);
            }
            else{
                messages[i] = SmsMessage.createFromPdu((byte[])objs[i]);
            }

        }
        return messages;
    }
}
