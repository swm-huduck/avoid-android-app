package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.huduck.application.Navigation.LatLngTool;
import com.huduck.application.Navigation.LocationProvider;
import com.huduck.application.Navigation.NavigationLogger;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationProvider;
import com.huduck.application.Navigation.NavigationRenderer;
import com.huduck.application.Navigation.NavigationRouter;
import com.huduck.application.Navigation.NavigationRoutes;
import com.huduck.application.Navigation.NavigationSpeaker;
import com.huduck.application.Navigation.NavigationTurnEventCalc;
import com.huduck.application.R;
import com.huduck.application.databinding.ActivityNavigationTestBinding;
import com.huduck.application.device.DeviceService;
import com.huduck.application.fragment.LoadingFragment;
import com.huduck.application.Navigation.Navigator;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;
import com.skt.Tmap.TMapAddressInfo;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapView;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NavigationGuideActivity extends AppCompatActivity implements NaverMap.OnLocationChangeListener, Navigator.OnProgressChangedCallback, Navigator.OnOffRouteCallback {
    private NavigationGuideActivity it = this;
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

    private LoadingFragment loadingFragment = null;

    private NavigationLogger logger = new NavigationLogger();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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


        naverMapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.naver_map_view);
        naverMapFragment.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            initActivity();
            initNaverMap();
            startNavigation(NavigationProvider.getNavigationRoute());
            navigator.onLocationChanged(LocationProvider.getLastRowLocation());
        });
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

        navigator.addOnRouteChangedCallback(logger);

        // 상단 목적지 출력
        this.destination = LatLngTool.tMapPointToLatlng(NavigationProvider.getDestination().getPOIPoint());
        binding.targetAddress.setText(NavigationProvider.getDestination().name);

        // 로딩 프레그먼트 가져오기
        loadingFragment = (LoadingFragment) getSupportFragmentManager().findFragmentByTag("loading");
        loadingFragment.isVisible(false);

        binding.refreshButton.setOnClickListener(v -> {
            this.refreshRoute();
        });

        binding.exitButton.setOnClickListener(v -> {
            finish();
        });

        // 로거 연결
        binding.targetAddress.setOnClickListener(view -> {
            try {
                logger.writeFile(this);
                Toast.makeText(this, "저장 성공", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // KalmanLocationManager 등록
        /*new KalmanLocationManager(this).requestLocationUpdates(
                KalmanLocationManager.UseProvider.GPS_AND_NET,
                30, 1000, 1000, navigator, true
        );*/
    }

    private void initNaverMap() {
        // Set event
        naverMap.setMapType(NaverMap.MapType.Navi);
        locationSource = new FusedLocationSource(this, 1000);
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.NoFollow);
        naverMap.setBuildingHeight(0.5f);
        naverMap.setSymbolScale(0);
        naverMap.setFpsLimit(30);

        // 위치 오버레이 지우기
       /*handler.post(() -> {
            naverMap.getLocationOverlay().setCircleRadius(0);
            naverMap.getLocationOverlay().setIcon(OverlayImage.fromResource(R.drawable.icon_null));
        });*/

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
        naverMap.addOnLocationChangeListener(navigator);
        naverMap.addOnLocationChangeListener(LocationProvider.locationChangeListener);
        naverMap.addOnLocationChangeListener(renderer);
        naverMap.addOnLocationChangeListener(logger);

        // Set Naver Map UI
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setScaleBarEnabled(false);
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
                .truckInformation(NavigationProvider.getTruckInformation())
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
        Date arrivedDate = new Date(System.currentTimeMillis() + (navigator.getNavigationRoute().getTotalTime() * 1000));
        SimpleDateFormat format = new SimpleDateFormat("hh:mm");
        binding.arrivedTime.setText(format.format(arrivedDate));
        format = new SimpleDateFormat("a");
        binding.amOrPm.setText(format.format(arrivedDate));
    }

    @Override
    public void onProgressChanged(double totalProgress,
                                  NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter,
                                  NavigationTurnEventCalc.NavigationTurnEventData nextTurnEventData,
                                  NavigationPoint nextNextTurnEvent, double nextNextTurnEventLeftDistanceMeter) {

        String turnEvent = NavigationPoint.TurnType.get(nextTurnEvent.getProperties().getTurnType());
        binding.nextTurnEvent.setText(turnEvent);
        binding.nextTurnEventLeftDistance.setText((int) (Math.floor(nextTurnEventLeftDistanceMeter / 10) * 10) + "m");

        int leftDistanceKm = (int) (navigator.getRouteTotalLeftDistance() / 1000);
        binding.leftDistance.setText(leftDistanceKm + "");

        deviceService.updateNavigationTurnEvent(
                nextTurnEvent.getProperties().getTurnType(),
                nextTurnEventLeftDistanceMeter,
                nextTurnEventData.getDistanceFromEventToFootOfPerpendicular(),
                nextTurnEventData.getDistanceFromCurrentPositionToFootOfPerpendicular(),
                nextNextTurnEvent == null ? -1 : nextNextTurnEvent.getProperties().getTurnType(),
                nextNextTurnEventLeftDistanceMeter
        );
    }

    private int updateAddressOrigin = 30;
    private int updateAddressCnt = 0;
    TMapData tMapData = new TMapData();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChange(@NonNull Location location) {
        // 속도 업데이트
        int speedKh = (int) (location.getSpeed() * 3.6);
        handler.post(() -> binding.currentSpeed.setText(speedKh + ""));

        deviceService.updateSpeed(speedKh);

//        renderer.setSpeed(location.getSpeed());

        // 현재 위치(주소) 업데이트
        updateAddressCnt--;
        if (updateAddressCnt <= 0) {
            updateAddressCnt = updateAddressOrigin;
            tMapData.reverseGeocoding(location.getLatitude(), location.getLongitude(), "A02", new TMapData.reverseGeocodingListenerCallback() {
                @Override
                public void onReverseGeocoding(TMapAddressInfo tMapAddressInfo) {
                    if (tMapAddressInfo == null) return;
                    String address = tMapAddressInfo.strFullAddress;
                    handler.post(() -> binding.currentAddress.setText(address));
                }
            });
        }
    }

    private boolean offRoute = false;
    private double lastOffRouteTime = 0;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onOffRoute() {
        double currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastOffRouteTime) * 0.001;
        if(deltaTime < 10) return;
        if(offRoute) return;
        offRoute = true;
        lastOffRouteTime = currentTime;
        refreshRoute();
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

}