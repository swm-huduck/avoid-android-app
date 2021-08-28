package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.huduck.application.Navigation.LatLngTool;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationRenderer;
import com.huduck.application.Navigation.NavigationRouter;
import com.huduck.application.Navigation.Navigator;
import com.huduck.application.R;
import com.huduck.application.common.CommonMethod;
import com.huduck.application.databinding.ActivityNavigationTestBinding;
import com.huduck.application.myCar.TruckInformation;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.skt.Tmap.TMapAddressInfo;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapView;


import java.util.TimerTask;

public class NavigationTestActivity extends AppCompatActivity implements NaverMap.OnLocationChangeListener, NaverMap.OnMapLongClickListener, Navigator.OnProgressChangedCallback
{
    private NavigationTestActivity it = this;
    private ActivityNavigationTestBinding binding;

    private MapFragment naverMapFragment;
    private NaverMap naverMap;

    private LocationSource locationSource;

    private Handler handler = new Handler(Looper.getMainLooper());

    private Navigator navigator = new Navigator();
    private NavigationRenderer renderer = null;

    private LatLng currentPosition = null;
    private LatLng destination = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new TMapView(this).setSKTMapApiKey(getString(R.string.skt_map_api_key));

        naverMapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.naver_map_view);
        naverMapFragment.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            initActivity();
            initNaverMap();
        });
    }

    private void initActivity() {
        // renderer 초기화
        renderer = new NavigationRenderer(this, handler, naverMap);
        navigator.addOnRouteChangedCallback(renderer);
        navigator.addOnEnhancedLocationChangedCallback(renderer);
        navigator.addOnProgressChangedCallback(renderer);
        navigator.addOnProgressChangedCallback(this);

//        // KalmanLocationManager 등록
//        new KalmanLocationManager(this).requestLocationUpdates(
//                KalmanLocationManager.UseProvider.GPS_AND_NET,
//                30, 1000, 1000, navigator, true
//        );
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
//       handler.post(() -> {
//            naverMap.getLocationOverlay().setCircleRadius(0);
//            naverMap.getLocationOverlay().setIcon(OverlayImage.fromResource(R.drawable.icon_null));
//        });

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

        // 길게 누른 이벤트 등록
        naverMap.setOnMapLongClickListener(this);

        // Set Naver Map UI
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setZoomControlEnabled(false);
//        uiSettings.setScaleBarEnabled(false);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startNavigation(LatLng destination) {
        this.destination = destination;

        NavigationRouter router = NavigationRouter.builder()
                .currentLocation(currentPosition)
                .targetLocation(destination)
                .truckInformation(TruckInformation.builder().build())
                .searchOption("0")
                .build();

        router.findRoutes(getString(R.string.skt_map_api_key), null, navigationRoute -> {
            navigator.setRoute(navigationRoute);
            navigator.startNavigator();
        });
    }

    @Override
    public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter) {
        String turnEvent = NavigationPoint.TurnType.get(nextTurnEvent.getProperties().getTurnType());
        binding.nextTurnEvent.setText(turnEvent);
        binding.nextTurnEventLeftDistance.setText((int)(Math.floor(nextTurnEventLeftDistanceMeter / 10) * 10)+"m");
    }

    private class NavigationSimulator extends TimerTask {
        // deltaTime
        private long lastTime = 0;
        private double deltaTime = 0;

        private boolean started = false;
        private boolean finished = false;

        private int routeIndex = -1;
        private LatLng startPoint = null;
        private LatLng targetPoint = null;
        private LatLng currentPoint = null;
        private LatLng moveDir = null;
        private double moveSpeed = 0;

        private double bearing = 0;
        private double currentBearing = 0;
        private double bearingSpeed = 100;
        private int bearingDirOrigin = 1;
        private int bearingDir = 1;
        private boolean convertedBearing = false;

        private boolean cameraUpdatable = true;

        private double totalLength = 0;
        private double progressLength = 0;

        NavigationSimulator(double totalLength) {
            this.totalLength = totalLength;
        }

        public void setMoveSpeed(double moveSpeed) {
            this.moveSpeed = Math.round(moveSpeed * 1000000) / 1000000.0;
            Log.d("setMoveSpeed", this.moveSpeed + "");
        }

        public void start() {
            lastTime = System.currentTimeMillis();

            started = true;
            updateState();
            Log.d("StartSimulator", "START");

            currentBearing = bearing;
        }

        @Override
        public void run() {
            if (finished) return;

            if (!started) start();

            updateDeltaTime();

            double leftMoveMag = moveSpeed * deltaTime;
            progressLength += leftMoveMag;

            while (leftMoveMag != 0) {
                if (finished) break;

                double mag = LatLngTool.mag(LatLngTool.sub(targetPoint, currentPoint));
                if (mag <= leftMoveMag) {
                    currentPoint = targetPoint;
                    leftMoveMag -= mag;
                    updateState();
                } else {
                    currentPoint = LatLngTool.add(currentPoint, LatLngTool.mul(moveDir, leftMoveMag));
                    leftMoveMag = 0;
                }
            }

            if (currentBearing != bearing) {
                currentBearing += bearingDir * bearingSpeed * deltaTime;

                if (bearingDir == 1 && currentBearing >= 360) {
                    currentBearing = 0;
                    convertedBearing = true;
                } else if (bearingDir == -1 && currentBearing <= 0) {
                    currentBearing = 360 - currentBearing;
                    convertedBearing = true;
                }

                if (bearingDir == 1 && currentBearing > bearing) {
                    if (bearingDirOrigin == 1)
                        currentBearing = bearing;
                    else if (bearingDirOrigin == -1 && convertedBearing)
                        currentBearing = bearing;
                } else if (bearingDir == -1 && currentBearing < bearing) {
                    if (bearingDirOrigin == 1 && convertedBearing)
                        currentBearing = bearing;
                    else if (bearingDirOrigin == -1)
                        currentBearing = bearing;
                }
            }

            handler.post(() -> {
                CameraPosition originCamPos = naverMap.getCameraPosition();
                CameraPosition cameraPosition = new CameraPosition(
                        currentPoint,
                        originCamPos.zoom,
                        60,
                        currentBearing
                );

                CameraUpdate cameraUpdate = CameraUpdate
                        .toCameraPosition(cameraPosition)
                        .pivot(new PointF(0.5f, 0.8f));
//                        .animate(CameraAnimation.Linear, 10000)

//                naverMap.moveCamera(cameraUpdate);
            });
        }

        private void updateDeltaTime() {
            long currentTime = System.currentTimeMillis();
            deltaTime = (currentTime - lastTime) * 0.001;
            lastTime = currentTime;
        }

        private void updateState() {
            routeIndex++;
//            if (routeIndex >= routeLatLngList.size() - 1) {
//                Log.d("Finished", "Fin");
//                finished = true;
//                return;
//            }
//            startPoint = routeLatLngList.get(routeIndex);
//            targetPoint = routeLatLngList.get(routeIndex + 1);
            currentPoint = new LatLng(startPoint.latitude, startPoint.longitude);
            moveDir = LatLngTool.normalize(LatLngTool.sub(targetPoint, startPoint));
            bearing = LatLngTool.deg(moveDir);

            bearingDirOrigin = 1;
            if (bearing - currentBearing < 0) bearingDirOrigin = -1;

            if (Math.abs(bearing - currentBearing) >= 180) {
                double angle = 360 - Math.abs(bearing - currentBearing);
                bearingSpeed = CommonMethod.lerp(20, 150, angle / 180);
                bearingDir = -bearingDirOrigin;
            } else {
                double angle = Math.abs(bearing - currentBearing);
                bearingSpeed = CommonMethod.lerp(20, 150, angle / 180);
                bearingDir = bearingDirOrigin;
            }

            convertedBearing = false;

            TMapData tMapData = new TMapData();
            tMapData.reverseGeocoding(currentPoint.latitude, currentPoint.longitude, "A02", new TMapData.reverseGeocodingListenerCallback() {
                @Override
                public void onReverseGeocoding(TMapAddressInfo tMapAddressInfo) {
                    if (tMapAddressInfo == null) return;
                    String address = tMapAddressInfo.strFullAddress;
                    handler.post(() -> binding.currentAddress.setText(address));
                }
            });
        }
    }


    private int updateAddressOrigin = 30;
    private int updateAddressCnt = 0;
    TMapData tMapData = new TMapData();
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChange(@NonNull Location location) {
        this.currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        // 속도 업데이트
        double speedKh = location.getSpeed() * 3.6;
        handler.post(() -> binding.currentSpeed.setText((int)speedKh + ""));

        renderer.setSpeed(speedKh);

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

    private LatLng pos1 = new LatLng(0, 0);
    private LatLng pos2 = new LatLng(0, 0);

    private Marker mar1 = new Marker(new LatLng(0, 0));
    private Marker mar2 = new Marker(new LatLng(0, 0));


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
       /* if(mar1.getMap() == null) {
            handler.post(()-> {
                mar1.setMap(naverMap);
                mar2.setMap(naverMap);
                mar2.setIconTintColor(Color.RED);
            });
        }


        pos2 = pos1;
        pos1 = latLng;

        double deg = LatLngTool.deg(LatLngTool.sub(pos2, pos1));

        handler.post(() -> {
            mar1.setPosition(pos1);
            mar2.setPosition(pos2);
            naverMap.moveCamera(CameraUpdate.toCameraPosition(new CameraPosition(pos1, 17, 0 , deg)));
        });

        Log.d("Deg", deg+"");



        if(true) return;*/
        startNavigation(latLng);

        // 목적지 상단 표
        new TMapData().reverseGeocoding(destination.latitude, destination.longitude, "A02", tMapAddressInfo -> {
            if (tMapAddressInfo == null) return;
            handler.post(() -> {
                binding.targetAddress.setText(tMapAddressInfo.strFullAddress);
            });
        });
    }
}