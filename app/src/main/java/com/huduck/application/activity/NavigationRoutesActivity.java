package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.huduck.application.Navigation.LatLngTool;
import com.huduck.application.Navigation.LocationProvider;
import com.huduck.application.Navigation.NavigationProvider;
import com.huduck.application.Navigation.NavigationRouter;
import com.huduck.application.fragment.LoadingFragment;
import com.huduck.application.Navigation.NavigationLineString;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationRoutes;
import com.huduck.application.common.NetworkTask;
import com.huduck.application.R;
import com.huduck.application.myCar.TruckInformation;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.SneakyThrows;

public class NavigationRoutesActivity extends AppCompatActivity{
    private Handler handler = new Handler(Looper.getMainLooper());
    private LoadingFragment loadingFragment;
    private Bundle routeBundle;
    private NaverMap naverMap;
    private List<NetworkTask> networkTasks = new ArrayList<>();

    private String[] searchOptionList = {
            "교통최적+추천",
            "교통최적+무료우선",
            "교통최적+최소시간",
            "교통최적+초보",
            "교통최적+고속도로우선",
            "최단거리+유/무료",
            "이륜차도로우선"
    };

    private HashMap<String, String> searchOptionCodeHashMap = new HashMap<String, String>() {{
        put("교통최적+추천", "0");
        put("교통최적+무료우선", "1");
        put("교통최적+최소시간", "2");
        put("교통최적+초보", "3");
        put("교통최적+고속도로우선", "4");
        put("최단거리+유/무료", "10");
        put("이륜차도로우선", "12");
    }};

    private NavigationRoutes routes = new NavigationRoutes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_routes);

        Intent routeIntent = getIntent();
        routeBundle = routeIntent.getExtras();
        loadingFragment = (LoadingFragment) getSupportFragmentManager().findFragmentByTag("loading");

        // 지도 활성화 이벤트 등록
        MapView mapView = findViewById(R.id.map_view);
        mapView.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            initActivity();
        });
    }

    private void initActivity() {
        // 스피너
        Spinner searchOptionSpinner = findViewById(R.id.search_option_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                searchOptionList);
        searchOptionSpinner.setAdapter(adapter);
        searchOptionSpinner.setSelection(0);
        searchOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String searchOption = searchOptionCodeHashMap.get(searchOptionList[position]);
                try {
                    searchRoutes(searchOption);
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                    return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 길 안내 시작 버튼
        LinearLayout startGuideBtn = findViewById(R.id.start_guide_btn);
        startGuideBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, NavigationGuideActivity.class);
            startActivity(intent);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void searchRoutes(String searchOption) {
        loadingFragment.isVisible(true);

        NavigationProvider.setSearchOption(searchOption);

        NavigationRouter router = NavigationRouter.builder()
                .currentLocation(
                        LatLngTool.locationToLatlng(LocationProvider.getLastRowLocation())
                )
                .targetLocation(new LatLng(routeBundle.getDouble("target_poi_lat"), routeBundle.getDouble("target_poi_lng")))
                .searchOption(searchOption)
                .truckInformation(TruckInformation.getInstance(this))
                .build();


        router.findRoutes(getString(R.string.skt_map_api_key), null, navigationRoutes -> {
            routes = navigationRoutes;
            NavigationProvider.setNavigationRoute(routes);
            drawRoutes();
            loadingFragment.isVisible(false);
        });
    }

    private PathOverlay currentPath = new PathOverlay();
    private Marker startMarker = new Marker();
    private Marker endMarker = new Marker();
    double minLat = 999999;
    double maxLat = -999999;
    double minLng = 999999;
    double maxLng = -999999;

    JSONArray jsonArray =  new JSONArray();

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawRoutes() {
        jsonArray = new JSONArray();

        ArrayList<Integer>
                navigationSequence = routes.getNavigationSequence();
        HashMap<Integer, NavigationPoint>
                navigationPointHashMap = routes.getNavigationPointHashMap();
        HashMap<Integer, NavigationLineString>
                navigationLineStringHashMap = routes.getNavigationLineStringHashMap();

        ArrayList<LatLng> latLngs = new ArrayList<>();

        /*List<InfoWindow> infoWindows = new ArrayList<>();*/

        navigationSequence.forEach(integer -> {
            if (navigationPointHashMap.containsKey(integer)) {
                NavigationPoint point = navigationPointHashMap.get(integer);
                double lat = point.getGeometry().getCoordinates().get(0);
                double lng = point.getGeometry().getCoordinates().get(1);
                LatLng position = new LatLng(lat, lng);
                latLngs.add(position);
                if(lat < minLat)
                    minLat = lat;
                if(lat > maxLat)
                    maxLat = lat;
                if(lng < minLng)
                    minLng = lng;
                if(lng > maxLng)
                    maxLng = lng;

                // 시작, 끝 지점 마커 생성
                if (point.getProperties().getPointType().equals("S")) {
                    startMarker.setPosition(position);
                    startMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_navi_start));
                    startMarker.setIconPerspectiveEnabled(true);
                    startMarker.setWidth(70);
                    startMarker.setHeight(100);
                } else if (point.getProperties().getPointType().equals("E")) {
                    endMarker.setPosition(position);
                    endMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_navi_end));
                    startMarker.setIconPerspectiveEnabled(true);
                    endMarker.setWidth(70);
                    endMarker.setHeight(100);
                }

                /*InfoWindow infoWindow = new InfoWindow();
                infoWindow.setPosition(position);
                infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
                    String type = null;

                    @NonNull
                    @Override
                    public CharSequence getText(@NonNull InfoWindow infoWindow) {
                        if (type == null)
                            type = NavigationPoint.TurnType.get(point.getProperties().getTurnType());
                        return type;
                    }
                });
                infoWindows.add(infoWindow);*/

            } else if (navigationLineStringHashMap.containsKey(integer)) {
                navigationLineStringHashMap.get(integer).getGeometry().getCoordinates().forEach(doubles -> {
                    double lat = doubles.get(0);
                    double lng = doubles.get(1);
                    LatLng position = new LatLng(lat, lng);


                    JSONArray a =new JSONArray() {{
                        try {
                            put(lat);
                            put(lng);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }};
                    jsonArray.put(a);

                    latLngs.add(position);
                    if(lat < minLat)
                        minLat = lat;
                    if(lat > maxLat)
                        maxLat = lat;
                    if(lng < minLng)
                        minLng = lng;
                    if(lng > maxLng)
                        maxLng = lng;
                });
            }
        });

        if(latLngs.size() < 2) return;
        currentPath.setCoords((List<LatLng>) latLngs);
        currentPath.setWidth(20);
        currentPath.setColor(getColor(R.color.indigo500));
        currentPath.setPatternImage(OverlayImage.fromResource(R.drawable.ic_baseline_arrow_drop_up_24));
        currentPath.setPatternInterval(50);

        LatLng southWest = new LatLng(minLat, minLng);
        LatLng northEast = new LatLng(maxLat, maxLng);
        CameraUpdate cameraUpdate = CameraUpdate.fitBounds(new LatLngBounds(southWest, northEast), 200);

        handler.post(() -> {
            currentPath.setMap(naverMap);
            startMarker.setMap(naverMap);
            endMarker.setMap(naverMap);
            naverMap.moveCamera(cameraUpdate);
            /*for (InfoWindow infoWindow : infoWindows)
                infoWindow.setMap(naverMap);*/
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPause() {
        Log.d("RoutesActivity", "onPause");
        networkTasks.forEach(networkTask -> networkTask.cancel(true));
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d("RoutesActivity", "onResume");
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onDestroy() {
        Log.d("RoutesActivity", "onDestroy");
        networkTasks.forEach(networkTask -> networkTask.cancel(true));
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBackPressed() {
//        finish();
        networkTasks.forEach(networkTask -> networkTask.cancel(true));
        super.onBackPressed();
    }
}