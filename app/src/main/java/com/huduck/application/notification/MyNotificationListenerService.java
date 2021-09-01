package com.huduck.application.notification;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;


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
        CharSequence contentCS = extras.getCharSequence(Notification.EXTRA_TEXT);
        String content = contentCS.toString();
        Log.d("onKakaoTalkPosted", "Sender: " + sender + ", Content: " + content);
        super.onNotificationPosted(sbn);
    }
}
