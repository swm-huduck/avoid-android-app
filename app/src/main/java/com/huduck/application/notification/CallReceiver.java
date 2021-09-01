package com.huduck.application.notification;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Locale;

public class CallReceiver extends BroadcastReceiver {
    private static String TAG = "CallReceiver";
    private String phoneState;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("aasdf",this.toString());
        if (!intent.getAction().equals("android.intent.action.PHONE_STATE")) return;
//        TelecomManager telephonyManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        String state = extras.getString(TelephonyManager.EXTRA_STATE); // 현재 폰 상태 가져옴

        if (state.equals(phoneState)) {
            return;
        } else {
            phoneState = state;
        }

        String phoneNo = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (phoneNo == null) return;

        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            Log.d(TAG, "통화벨 울리는중");

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

            if(flagDisplayName)
                Log.d(TAG, "Sender: " + displayName);
            else
                Log.d(TAG, "Sender: " + PhoneNumberUtils.formatNumber(phoneNo, context.getResources().getConfiguration().locale.getCountry()));

        } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            Log.d(TAG, "통화중");

        } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            Log.d(TAG, "통화종료 혹은 통화벨 종료");
        }
    }
}
