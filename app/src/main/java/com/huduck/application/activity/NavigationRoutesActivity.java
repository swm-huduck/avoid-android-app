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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.huduck.application.Navigation.NavigationFeatureParser;
import com.huduck.application.Navigation.NavigationLineString;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationRoutes;
import com.huduck.application.NetworkTask;
import com.huduck.application.R;
import com.huduck.application.service.NavigationService;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.SneakyThrows;

public class NavigationRoutesActivity extends AppCompatActivity {
    private Handler handler = new Handler(Looper.getMainLooper());
    private RelativeLayout loadingLayout;
    private Bundle routeBundle;
    private NaverMap naverMap;

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

    private NavigationRoutes navigationRoutes = new NavigationRoutes();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_routes);

        Intent routeIntent = getIntent();
        routeBundle = routeIntent.getExtras();

        // Loading
        ImageView logoLoading = (ImageView) findViewById(R.id.logo_loading);
        Glide.with(this)
                .asGif()    // GIF 로딩
                .load(R.raw.logo_loading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)    // Glide에서 캐싱한 리소스와 로드할 리소스가 같을때 캐싱된 리소스 사용
                .into(logoLoading);

        loadingLayout = findViewById(R.id.loading_layout);
        loadingLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

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
        searchOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String searchOption = searchOptionCodeHashMap.get(searchOptionList[position]);
                searchRoutes(searchOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchOptionSpinner.setSelection(0);
    }

    private void searchRoutes(String searchOption) {
        loadingLayout.setVisibility(View.VISIBLE);

        String url = "https://apis.openapi.sk.com/tmap/truck/routes?version=1&format=json&callback=result&appKey=l7xxf21cc9e0068d4fbbb7c939aa6bda5a25";

        // AsyncTask를 통해 HttpURLConnection 수행.

        ContentValues values = new ContentValues();

        values.put("startX", NavigationService.GetNavigationInfo().getCurrentPoint().getLongitude());
        values.put("startY", NavigationService.GetNavigationInfo().getCurrentPoint().getLatitude());
        values.put("endX", routeBundle.getString("target_poi_lng"));
        values.put("endY", routeBundle.getString("target_poi_lat"));
        values.put("reqCoordType", "WGS84GEO");
        values.put("resCoordType", "WGS84GEO");
        values.put("angle", "172");
        values.put("searchOption", searchOption);
        values.put("trafficInfo", "Y");
        values.put("truckType", "1");
        values.put("truckWidth", "100");
        values.put("truckHeight", "100");
        values.put("truckWeight", "35000");
        values.put("truckTotalWeight", "35000");
        values.put("truckLength", "200");

        NetworkTask networkTask = new NetworkTask(url, values) {
            @SneakyThrows
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                navigationRoutes = NavigationFeatureParser.parserTruckRoutes(s);
                drawRoutes();
                loadingLayout.setVisibility(View.GONE);
            }
        };
        networkTask.execute();
    }

    private PathOverlay currentPath = new PathOverlay();
    private Marker startMarker = new Marker();
    private Marker endMarker = new Marker();
    double minLat = 999999;
    double maxLat = -999999;
    double minLng = 999999;
    double maxLng = -999999;

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawRoutes() {
        ArrayList<Integer>
                navigationSequence = navigationRoutes.getNavigationSequence();
        HashMap<Integer, NavigationPoint>
                navigationPointHashMap = navigationRoutes.getNavigationPointHashMap();
        HashMap<Integer, NavigationLineString>
                navigationLineStringHashMap = navigationRoutes.getNavigationLineStringHashMap();

        ArrayList<LatLng> latLngs = new ArrayList<>();

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
                    startMarker.setIcon(MarkerIcons.BLACK);
                    startMarker.setIconTintColor(Color.RED);
                } else if (point.getProperties().getPointType().equals("E")) {
                    endMarker.setPosition(position);
                    endMarker.setIcon(MarkerIcons.BLACK);
                    endMarker.setIconTintColor(Color.GREEN);
                }
            } else if (navigationLineStringHashMap.containsKey(integer)) {
                navigationLineStringHashMap.get(integer).getGeometry().getCoordinates().forEach(doubles -> {
                    double lat = doubles.get(0);
                    double lng = doubles.get(1);
                    LatLng position = new LatLng(lat, lng);

//                   InfoWindow infoWindow = new InfoWindow();
//                   infoWindow.setVisible(true);
//                   infoWindow.setPosition(position);
//                   infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
//                       @NonNull
//                       @Override
//                       public CharSequence getText(@NonNull InfoWindow infoWindow) {
//                           return lat + ", " + lng;
//                       }
//                   });
//                    handler.post(() -> {
//                        infoWindow.setMap(naverMap);
//                    });

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

        currentPath.setCoords((List<LatLng>) latLngs);
        currentPath.setWidth(20);
        currentPath.setColor(Color.BLUE);
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
        });
    }
}