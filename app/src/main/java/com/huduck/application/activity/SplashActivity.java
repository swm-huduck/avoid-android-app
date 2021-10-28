package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.huduck.application.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

public class SplashActivity extends AppCompatActivity {
    private boolean paused = false;
    private String[] essentialPermissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    static final int PERMISSION_REQUEST_CODE = 1000;

    ArrayList<String> needEssentialPermissions = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoHider = findViewById(R.id.logo_hider);
        logoHider.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.anim_move_right));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() {
        super.onStart();

        initPermissions();
    }

    private void initPermissions() {
        if (!isNotificationPermissionAllowed())
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));

        // 권한 확인
        needEssentialPermissions = checkNeedEssentialPermission();
        if (needEssentialPermissions.size() == 0) {
            goNextPage();
            return;
        }

        // 권한 얻기
        String[] needEssentialPermissionsArray = new String[needEssentialPermissions.size()];
        int idx = 0;
        for(String temp : needEssentialPermissions)
            needEssentialPermissionsArray[idx++] = temp;

        requestPermissions(needEssentialPermissionsArray, PERMISSION_REQUEST_CODE);
    }

    private void goNextPage() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(paused) {
                    finish();
                    return;
                }

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        },1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode != PERMISSION_REQUEST_CODE) return;

        // 허락된 권한만 리스트에서 삭제
        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),"앱 권한 설정하세요",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        paused = true;
        super.onPause();
        finish();
    }

    private ArrayList<String> checkNeedEssentialPermission() {
        ArrayList<String> needPermission = new ArrayList<>();

        for (int i = 0; i < essentialPermissions.length; i++) {
//            int check = ContextCompat.checkSelfPermission(this, essentialPermissions[i]);
            int check = checkCallingOrSelfPermission(essentialPermissions[i]);
            if(check != PackageManager.PERMISSION_GRANTED)
                needPermission.add(essentialPermissions[i]);
        }

        return needPermission;
    }

    /**
     * Notification 접근 권한 체크 메서드
     * @return 접근권한이 있을 경우 true, 아니면 false
     */
    private boolean isNotificationPermissionAllowed() {
        Set<String> packageNames =  NotificationManagerCompat.getEnabledListenerPackages(this);
        return packageNames != null && packageNames.contains("com.huduck.application");
    }
}