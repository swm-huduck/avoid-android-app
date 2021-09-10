package com.huduck.application.notification;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.huduck.application.device.DeviceService;

public class CallListener extends PhoneStateListener {
    private static final String TAG = "CallListener";
    private int phoneState = 0;

    private Context context;
    private DeviceService deviceService;

    public CallListener(Context context, DeviceService deviceService) {
        this.context = context;
        this.deviceService = deviceService;
    }

    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        if (state == phoneState)
            return;
        else
            phoneState = state;

        String phoneNo = phoneNumber;
        if (phoneNo == null || phoneNo.isEmpty()) return;

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

        deviceService.updateCall(flagDisplayName ? displayName : phoneNo, phoneState);
    }
}
