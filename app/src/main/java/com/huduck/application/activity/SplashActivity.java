package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;
import com.huduck.application.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SplashActivity extends AppCompatActivity {
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

        checkPermissions();
    }

    int permissionCnt = 3;
    int successPermissionLeftCnt = permissionCnt;

    private PermissionListener permissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            successPermissionLeftCnt--;

            if(successPermissionLeftCnt != 0) {
                checkPermissions();
                return;
            }

            if (isNotificationPermissionAllowed()) {
                goNextPage();
                return;
            }

            Toast.makeText(
                    getApplicationContext(),
                    "HUD에서 카카오톡에 대한 알림을 받기 위해서 권한이 필요합니다." +
                    "\n허용 후, 애플리케이션을 재시작해주세요.",
                    Toast.LENGTH_SHORT
            ).show();
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            finish();
            return;
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(getApplicationContext(), "권한이 허용되지 않아 애플리케이션이 종료됩니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    private List<TedPermission.Builder> permissionBuilderList = new ArrayList<TedPermission.Builder>(){{
        add(
                TedPermission.create()
                    .setPermissionListener(permissionListener)
                    .setRationaleMessage(
                            "HUD와 블루투스 통신, 내비게이션을 이용하기 위해서 권한 허용이 필요합니다." +
                            "\n권한을 허용하지 않으면 정상적인 애플리케이션 이용이 불가능합니다."
                    )
                    .setPermissions(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE,
                            Manifest.permission.CHANGE_NETWORK_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
        );

        add(
        TedPermission.create()
                .setPermissionListener(permissionListener)
                .setRationaleMessage(
                        "HUD에서 전화에 대한 알림을 받기 위해서 권한 허용이 필요합니다." +
                        "\n권한을 허용하지 않으면 정상적인 애플리케이션 이용이 불가능합니다."
                )
                .setPermissions(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_CONTACTS
                )
        );

        add(
        TedPermission.create()
                .setPermissionListener(permissionListener)
                .setRationaleMessage(
                        "HUD에서 문자에 대한 알림을 받기 위해서 권한 허용이 필요합니다." +
                        "\n권한을 허용하지 않으면 정상적인 애플리케이션 이용이 불가능합니다."
                )
                .setPermissions(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_CONTACTS
                )
        );
    }};

    private void checkPermissions() {
        int index = permissionCnt - successPermissionLeftCnt;
        permissionBuilderList.get(index).check();
    }

    private void goNextPage() {
        View logoHider = findViewById(R.id.logo_hider);
        logoHider.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.anim_move_right));


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        },1000);
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