package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.backup.RestoreObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.huduck.application.Navigation.LatLngTool;
import com.huduck.application.Navigation.LocationProvider;
import com.huduck.application.Navigation.NavigationLoggerManager;
import com.huduck.application.Navigation.NavigationLogger;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationProvider;
import com.huduck.application.Navigation.NavigationRenderer;
import com.huduck.application.Navigation.NavigationRouter;
import com.huduck.application.Navigation.NavigationRoutes;
import com.huduck.application.Navigation.NavigationSpeaker;
import com.huduck.application.Navigation.NavigationTurnEventCalc;
import com.huduck.application.Navigation.NavigationUiManager;
import com.huduck.application.Navigation.Navigator;
import com.huduck.application.R;
import com.huduck.application.common.CommonMethod;
import com.huduck.application.databinding.ActivityNavigationTestBinding;
import com.huduck.application.device.DeviceService;
import com.huduck.application.fragment.LoadingFragment;
import com.huduck.application.myCar.TruckInformation;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.OverlayImage;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.skt.Tmap.TMapAddressInfo;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapView;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NavigationGuideDebugActivity
        extends AppCompatActivity
        implements  NaverMap.OnLocationChangeListener, Navigator.OnProgressChangedCallback,
                    Navigator.OnOffRouteCallback, NavigationLoggerManager.OnRouteChangedCallback {

    private NavigationGuideDebugActivity it = this;
    private ActivityNavigationTestBinding binding;

    private DeviceService deviceService;

    private MapFragment naverMapFragment;
    private NaverMap naverMap;

    private LocationSource locationSource;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Navigator navigator = new Navigator();
    private NavigationRenderer renderer = null;
    private NavigationSpeaker speaker = null;

    private LatLng destination = null;

    private NavigationUiManager uiManager;

    private LoadingFragment loadingFragment = null;

    private boolean openRoutesActivity = false;

    private NavigationLogger logger = new NavigationLogger();
    private NavigationLoggerManager loggerManager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        uiManager = new NavigationUiManager(this, binding, new TMapData());

        new TMapView(this).setSKTMapApiKey(getString(R.string.skt_map_api_key));

        bindService(new Intent(this, DeviceService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName componentName, IBinder service) {
                        DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
                        deviceService = binder.getService();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName componentName) {}
                }, Context.BIND_AUTO_CREATE
        );

        loggerManager = new NavigationLoggerManager(this, logger, this);

        naverMapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.naver_map_view);
        naverMapFragment.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            initActivity();
            initNaverMap();
            // startNavigation(NavigationProvider.getNavigationRoute());
            navigator.onLocationChanged(LocationProvider.getLastRowLocation());
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initActivity() {
        // renderer 초기화
        renderer = new NavigationRenderer(this, handler, naverMap);
        navigator.addOnRouteChangedCallback(renderer);
        navigator.addOnEnhancedLocationChangedCallback(renderer);
        navigator.addOnProgressChangedCallback(renderer);

        // speaker 초기화
        speaker = new NavigationSpeaker(this);
        navigator.addOnProgressChangedCallback(speaker);
//        navigator.addOnOffRouteCallback(speaker);

        navigator.addOnProgressChangedCallback(this);
        navigator.addOnOffRouteCallback(this);

        // UI Manager 초기화
        navigator.addOnRouteChangedCallback(uiManager);
        navigator.addOnProgressChangedCallback(uiManager);

        // 파일 불러오기 클릭
        NavigationGuideDebugActivity it = this;
        ChooserDialog chooserDialog = new ChooserDialog(it)
                .withFilter(false, false, "dat", "txt")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String dir, File dirFile) {
                        try {
                            Toast.makeText(it, "정상적으로 불러왔습니다.", Toast.LENGTH_SHORT).show();
                            logger = NavigationLogger.readFile(dirFile);
                            if(openRoutesActivity) {
                                NavigationProvider.setNavigationRoute(logger.getRouteLogList().get(0).getRoute());
                                startActivity(new Intent(it, NavigationRoutesDebugActivity.class));
                            }
                            else {
                                loggerManager.setLogger(logger);
                                loggerManager.start();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(it, "불러오기 실패", Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(it, "불러오기 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .build();


        binding.nextTurnEventLeftDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRoutesActivity = false;
                loggerManager.stop();
                chooserDialog.show();
            }
        });

        binding.nextNextTurnEventLeftDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRoutesActivity = true;
                loggerManager.stop();
                chooserDialog.show();
            }
        });

        // 기본 목적지 설정
        SharedPreferences sharedPreferences = getSharedPreferences("destination", MODE_PRIVATE);
        if(sharedPreferences.contains("destination")) {
            String destination = sharedPreferences.getString("destination", "아남타워");
            uiManager.setDestination(destination);
        }


        // 로딩 프레그먼트 가져오기
        loadingFragment = (LoadingFragment) getSupportFragmentManager().findFragmentByTag("loading");
        loadingFragment.isVisible(false);

        binding.refreshButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("목적지 설정");

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(input.getText() == null) return;
                    String destination = input.getText().toString();

                    uiManager.setDestination(destination);

                    SharedPreferences sharedPreferences = getSharedPreferences("destination", MODE_PRIVATE);
                    sharedPreferences.edit()
                            .putString("destination", destination)
                            .commit();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        });

        binding.exitButton.setOnClickListener(v -> {
            closeActivity();
        });
    }

    private void initNaverMap() {
        // Set event
        naverMap.setMapType(NaverMap.MapType.Navi);
        naverMap.setLocationSource(loggerManager);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        naverMap.setBuildingHeight(0.5f);
        naverMap.setSymbolScale(0);
        naverMap.setFpsLimit(30);

        // 위치 오버레이 지우기
        handler.post(() -> {
            naverMap.getLocationOverlay().setCircleRadius(0);
            naverMap.getLocationOverlay().setIcon(OverlayImage.fromResource(R.drawable.icon_null));
        });

        // 카메라 위치 초기화
        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition(
                        position,
                        17,
                        0, //60,
                        0//location.getBearing()
                );
                naverMap.setCameraPosition(cameraPosition);
                naverMap.removeOnLocationChangeListener(this);
                naverMap.addOnLocationChangeListener(it);
            }
        });
        naverMap.addOnLocationChangeListener(LocationProvider.locationChangeListener);
        naverMap.addOnLocationChangeListener(navigator);
        naverMap.addOnLocationChangeListener(renderer);
        naverMap.addOnLocationChangeListener(uiManager);
        naverMap.addOnLocationChangeListener(logger);

        // Set Naver Map UI
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setScaleBarEnabled(false);

        // 지도 터치 감지
        binding.cameraMoveToMyPosition.setOnClickListener(view -> {
            renderer.setDontMoveCamera(false);
            view.setVisibility(View.GONE);
        });

        binding.naverMapViewOverlay.setOnTouchListener((view, motionEvent) -> {
            renderer.setDontMoveCamera(true);
            binding.cameraMoveToMyPosition.setVisibility(View.VISIBLE);
            return false;
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startNavigation(NavigationRoutes navigationRoute) {
        navigator.setRoute(navigationRoute);
        navigator.startNavigator();
        afterStartNavigation();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startNavigation(LatLng destination) {
        this.destination = destination;

        LatLng lastLocation = LatLngTool.locationToLatlng(LocationProvider.getLastRowLocation());
        LatLng lastLastLocation = null;

        // 너무 극단적인 경로가 나오는 경우가 있어서 주석 처리함
        /*int lastLocSize = LocationProvider.getLastRowLocationSize();
        if(lastLocSize > 2) {
            lastLastLocation = LatLngTool.locationToLatlng(LocationProvider.getLastRowLocation(lastLocSize - 2));
        }*/

        NavigationRouter router = NavigationRouter.builder()
                .currentLocation(lastLocation)
                .targetLocation(destination)
                .truckInformation(TruckInformation.getInstance(this))
                .searchOption(NavigationProvider.getSearchOption())
                .build();

        router.findRoutes(getString(R.string.skt_map_api_key), lastLastLocation, navigationRoute -> {
            navigator.setRoute(navigationRoute);
            navigator.startNavigator();
            offRoute = false;
            afterStartNavigation();
        });
    }

    private void afterStartNavigation() {
        loadingFragment.isVisible(false);
    }

    @Override
    public void onProgressChanged(double totalProgress,
                                  NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter,
                                  NavigationTurnEventCalc.NavigationTurnEventData nextTurnEventData,
                                  NavigationPoint nextNextTurnEvent, double nextNextTurnEventLeftDistanceMeter) {
        deviceService.updateNavigationTurnEvent(
                nextTurnEvent.getProperties().getTurnType(),
                nextTurnEventLeftDistanceMeter,
                nextTurnEventData.getDistanceFromEventToFootOfPerpendicular(),
                nextTurnEventData.getDistanceFromCurrentPositionToFootOfPerpendicular(),
                nextNextTurnEvent == null ? -1 : nextNextTurnEvent.getProperties().getTurnType(),
                nextNextTurnEventLeftDistanceMeter
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChange(@NonNull Location location) {
        // 속도 업데이트
        deviceService.updateSpeed(location.getSpeed(), CommonMethod.MiliSecondToLocalTime(location.getTime()));
    }

    private boolean offRoute = false;
    private double lastOffRouteTime = 0;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onOffRoute() {
       /*
        double currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastOffRouteTime) * 0.001;
        if(deltaTime < 10) return;
        if(offRoute) return;
        offRoute = true;
        lastOffRouteTime = currentTime;
        refreshRoute();
        */
        /*Toast.makeText(this, "경로 이탈", Toast.LENGTH_SHORT).show();*/
        /*navigator.setRoute(route);
        navigator.startNavigator();
        afterStartNavigation();*/
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void refreshRoute() {
        lastOffRouteTime = System.currentTimeMillis();
        loadingFragment.isVisible(true);
        startNavigation(destination);
        speaker.onOffRoute();
    }

    @Override
    protected void onDestroy() {
        renderer.destroy();
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRouteChanged(NavigationRoutes route) {
        navigator.setRoute(route);
        navigator.startNavigator();
        afterStartNavigation();
    }

    @Override
    public void onBackPressed() {
        closeActivity();
    }

    private void closeActivity() {
        new AlertDialog.Builder( this )
                .setMessage( "내비게이션을 종료하시겠습니까?" )
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }
}
