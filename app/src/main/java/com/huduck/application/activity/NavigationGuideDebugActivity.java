package com.huduck.application.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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
import com.huduck.application.Navigation.Navigator;
import com.huduck.application.R;
import com.huduck.application.databinding.ActivityNavigationTestBinding;
import com.huduck.application.fragment.LoadingFragment;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
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
    private static final String TAG = "NavigationGuideDebugActivity";

    private static final int EX_FILE_PICKER_RESULT = 0;

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
    private NavigationLoggerManager loggerManager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new TMapView(this).setSKTMapApiKey(getString(R.string.skt_map_api_key));
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
/*
        if (requestCode == EX_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if (result != null && result.getCount() > 0) {
                Log.d(TAG, result.getPath());
                try {
                    logger = NavigationLogger.readFile(result.getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }*/

        /*if(requestCode == FilePickerConst.REQUEST_CODE_DOC) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                List<Uri> docPaths = new ArrayList<>();
                docPaths.addAll(data.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                try {
                    logger = NavigationLogger.readFile(docPaths.get(0).getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }*/
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

        // 상단 목적지 출력
        NavigationGuideDebugActivity it = this;
        ChooserDialog chooserDialog = new ChooserDialog(it)
                .withFilter(false, false, "dat", "txt")
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String dir, File dirFile) {
                        try {
                            Toast.makeText(it, "정상적으로 불러왔습니다.", Toast.LENGTH_SHORT).show();
                            logger = NavigationLogger.readFile(dirFile);
                            loggerManager.setLogger(logger);
                            loggerManager.start();
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

        binding.targetAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loggerManager.stop();
                chooserDialog.show();
            }
        });

        // 로딩 프레그먼트 가져오기
        loadingFragment = (LoadingFragment) getSupportFragmentManager().findFragmentByTag("loading");
        loadingFragment.isVisible(false);

        binding.refreshButton.setOnClickListener(v -> {
            this.refreshRoute();
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
        SimpleDateFormat format = new SimpleDateFormat("a hh : mm");
        binding.arrivedTime.setText(format.format(arrivedDate));
    }

    @Override
    public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter) {
        String turnEvent = NavigationPoint.TurnType.get(nextTurnEvent.getProperties().getTurnType());
        binding.nextTurnEvent.setText(turnEvent);
        binding.nextTurnEventLeftDistance.setText((int) (Math.floor(nextTurnEventLeftDistanceMeter / 10) * 10) + "m");
    }

    private int updateAddressOrigin = 30;
    private int updateAddressCnt = 0;
    TMapData tMapData = new TMapData();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChange(@NonNull Location location) {
        // 속도 업데이트
        double speedKh = location.getSpeed() * 3.6;
        handler.post(() -> binding.currentSpeed.setText((int) speedKh + ""));

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
       /* double currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastOffRouteTime) * 0.001;
        if(deltaTime < 10) return;
        if(offRoute) return;
        offRoute = true;
        lastOffRouteTime = currentTime;
        refreshRoute();*/
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
}