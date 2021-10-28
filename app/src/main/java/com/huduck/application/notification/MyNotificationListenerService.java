package com.huduck.application.notification;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MyNotificationListenerService extends NotificationListenerService {

    private static String KAKAO_TALK_PACKAGE = "com.kakao.talk";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();

            if(packageName.equals(KAKAO_TALK_PACKAGE))
                onKakaoTalkPosted(sbn);
        }
        catch (NullPointerException e) {
//            e.printStackTrace();
        }
    }

    private void onKakaoTalkPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String sender = extras.getString(Notification.EXTRA_TITLE);
        if(sender == null || sender.isEmpty()) sender = "";
        CharSequence contentCS = extras.getCharSequence(Notification.EXTRA_TEXT);
        if(contentCS == null || contentCS.length() == 0) return;
        String content = contentCS.toString();

        Intent intent = new Intent("kakaotalk");
        intent.putExtra("name", sender)
                .putExtra("content", content);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        super.onNotificationPosted(sbn);
    }
}
