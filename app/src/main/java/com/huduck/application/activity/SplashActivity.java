package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import android.widget.Toast;

import com.huduck.application.R;

import java.util.ArrayList;
import java.util.function.Consumer;

public class SplashActivity extends AppCompatActivity {
    private boolean paused = false;
    private String[] essentialPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.READ_PHONE_STATE
    };
    static final int PERMISSION_REQUEST_CODE = 1000;

    ArrayList<String> needEssentialPermissions = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() {
        super.onStart();

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
        },500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode != PERMISSION_REQUEST_CODE) return;

        // 허락된 권한만 리스트에서 삭제
        for(int i = 0; i < grantResults.length; i++) {
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.d("Aa", "aaaaaaaaa");
                Toast.makeText(getApplicationContext(),"앱권한설정하세요",Toast.LENGTH_LONG).show();
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
}