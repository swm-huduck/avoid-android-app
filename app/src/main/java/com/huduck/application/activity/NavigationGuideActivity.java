package com.huduck.application.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.huduck.application.Navigation.NavigationLineString;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationRoutes;
import com.huduck.application.manager.NavigationManager;
import com.huduck.application.R;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.common.HttpServiceFactory;
import com.mapbox.common.HttpServiceInterface;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.navigation.base.internal.VoiceUnit;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteLegProgress;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteStepProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.MapboxNavigationProvider;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.history.ReplayEventBase;
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver;
import com.mapbox.navigation.core.reroute.RerouteController;
import com.mapbox.navigation.core.reroute.RerouteState;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewOptions;
import com.mapbox.navigation.ui.OnNavigationReadyCallback;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.map.NavigationMapboxMap;
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationGuideActivity extends AppCompatActivity implements Runnable, OnMapReadyCallback {

    Handler handler = new Handler();

    private MapboxNavigation mapboxNavigation = NavigationManager.getInstance().getNavigation();
    private LocationObserver locationObserver;
    private RouteProgressObserver routeProgressObserver;
    private OffRouteObserver offRouteObserver;
    private VoiceInstructionsObserver voiceInstructionsObserver;
    private List<List<Point>> points;
    private List<DirectionsRoute> routes;
    private Point currentPoint;

    // Naver Map
    private MapFragment mapFragment;
//    private MapView mapView;
    private NaverMap naverMap;
    private LocationOverlay locationOverlay;
    private boolean locationOverlayPositionInit = false;
    private PathOverlay pathOverlay = new PathOverlay();
    private Marker rowPositionMarker = new Marker();
    int idx = 0;

    // XML
    private TextView speedTextView;
    private TextView stateTextView;
    private TextView offRouteTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_navigation_guide);

        speedTextView = findViewById(R.id.speed);
        stateTextView = findViewById(R.id.state);
        offRouteTextView = findViewById(R.id.off_route);

        FragmentManager fm = getSupportFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentById(R.id.map_view_guide);
        if(mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_view_guide, mapFragment).commit();
        }

        Thread th = new Thread(this::run);
        th.start();
    }

    @Override
    public void run() {
        initNaverMapApi();
    }

    private void initMapboxApi() throws JSONException {
//        NavigationOptions navigationOptions = MapboxNavigation
//                .defaultNavigationOptionsBuilder(getApplicationContext(), getString(R.string.mapbox_access_token))
////                .locationEngine(locationEngine)
//                .build();
//
//        // MapboxNavigation
//        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions);

        // LocationObserver
        locationObserver = new LocationObserver() {
            @Override
            public void onRawLocationChanged(@NonNull Location location) {
                if(rowPositionMarker.getMap() == null)
                    handler.post(() -> rowPositionMarker.setMap(naverMap));

                rowPositionMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onEnhancedLocationChanged(@NonNull Location location, @NonNull List<? extends Location> list) {
                // 가장 정확한 위치를 업데이트 (도로 스냅 기능)
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                LatLng position = new LatLng(lat, lng);
                currentPoint = Point.fromLngLat(lat, lng);

                CameraPosition cameraPosition = new CameraPosition(
                        position,
                        17,
                        60,
                        location.getBearing()
                );

                double speed = location.getSpeed();     // m/s
                speed *= 3.6;                           // km/h
                speed = Math.round(speed);   // 소수점 한자리
                speedTextView.setText(String.format("%d", (int)speed));

                long animationDuration = 1000;
                if(!locationOverlayPositionInit) {
                    animationDuration = 0;
                    locationOverlayPositionInit = true;
                }

                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(position)
                        .toCameraPosition(cameraPosition)
                        .animate(CameraAnimation.Linear, animationDuration)
                        .pivot(new PointF(0.5f, 0.8f));

                locationOverlay.setPosition(position);
                locationOverlay.setBearing(location.getBearing());

                handler.post(() -> {
                    if(naverMap != null)
                        naverMap.moveCamera(cameraUpdate);
                });
            }
        };
        mapboxNavigation.registerLocationObserver(locationObserver);

        // RouteProgressObserver
        routeProgressObserver = new RouteProgressObserver() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onRouteProgressChanged(@NonNull RouteProgress routeProgress) {

                /*stateTextView.setText(routeProgress.getCurrentState().toString());

                RouteLegProgress legProgress = routeProgress.getCurrentLegProgress();
                if(legProgress == null) return;

                RouteStepProgress stepProgress = legProgress.getCurrentStepProgress();
                if(stepProgress == null) return;

                LegStep currentStep = stepProgress.getStep();
                if(currentStep == null) return;

                LegStep upcomingStep = legProgress.getUpcomingStep();
                if(upcomingStep == null) return;

                int currentIntersectionIdx = stepProgress.getIntersectionIndex();
                int nextIntersectionIdx = currentIntersectionIdx + 1;
                StepIntersection currentIntersection = currentStep.intersections().get(currentIntersectionIdx);
                StepIntersection nextIntersection;
                if(currentStep.intersections().size() <= nextIntersectionIdx)
                    nextIntersection = upcomingStep.intersections().get(0);
                else
                    nextIntersection = currentStep.intersections().get(nextIntersectionIdx);

                Point currentIntersectionPoint = currentIntersection.location();
                Point nextIntersectionPoint = nextIntersection.location();

                Point targetDir = Point.fromLngLat(
                        (nextIntersectionPoint.longitude() - currentIntersectionPoint.longitude()),
                        (nextIntersectionPoint.latitude() - currentIntersectionPoint.latitude())
                );*/
            }
        };
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);

        // OffRouteObserver
        offRouteObserver = new OffRouteObserver() {
            @Override
            public void onOffRouteStateChanged(boolean b) {
                if(b)
                    offRouteTextView.setText("벗어남");
                else
                    offRouteTextView.setText("잘가는중");
            }
        };
        mapboxNavigation.registerOffRouteObserver(offRouteObserver);

        // VoiceInstructionsObserver
        voiceInstructionsObserver = new VoiceInstructionsObserver() {
            @Override
            public void onNewVoiceInstructions(@NonNull VoiceInstructions voiceInstructions) {
                Log.d("", voiceInstructions.announcement().toString());
            }
        };
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);

        // Custom Route
        Activity it = this;
        points = points();
        getMatchingMapsByPoints(points, new GetMatchingMapsCallback() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onSuccess(List<DirectionsRoute> routes) {
                mapboxNavigation.setRoutes(routes);
                if (ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
//                mapboxNavigation.startTripSession();

                // Draw Origin Route
                List<LatLng> latLngs = new ArrayList<>();
                points.forEach(points1 -> points1.forEach(point -> {
                    latLngs.add(new LatLng(point.latitude(), point.longitude()));
                }));

                handler.post(() -> {
                    PathOverlay pathOverlay = new PathOverlay();
                    pathOverlay.setCoords(latLngs);
                    pathOverlay.setWidth(50);
                    pathOverlay.setColor(Color.RED);
                    pathOverlay.setMap(naverMap);

                });

                // Draw New Route
                drawRoute(routes);
            }

            @Override
            public void onFailure() {

            }
        });
    }

    private void getMatchingMapsByPoints(List<List<Point>> points, GetMatchingMapsCallback callback) {
        List<DirectionsRoute> routes = new ArrayList<>();
        getMatchingMapsByPointsRoutine(0, points, routes, callback);
    }

    private void getMatchingMapsByPointsRoutine(int pointListIdx, List<List<Point>> points, List<DirectionsRoute> routes, GetMatchingMapsCallback callback) {
        List<Point> pointList = points.get(pointListIdx);
        MapboxMapMatching mapboxMapMatching = MapboxMapMatching.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .language(Locale.KOREA)
                .coordinates(pointList)
                .steps(true)
                .voiceInstructions(true)
                .voiceUnits(DirectionsCriteria.METRIC)
                .bannerInstructions(true)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .baseUrl(Constants.BASE_API_URL)
                .user(Constants.MAPBOX_USER)
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .annotations(
                        DirectionsCriteria.ANNOTATION_CONGESTION,
                        DirectionsCriteria.ANNOTATION_DURATION,
                        DirectionsCriteria.ANNOTATION_MAXSPEED,
                        DirectionsCriteria.ANNOTATION_SPEED,
                        DirectionsCriteria.ANNOTATION_DISTANCE
                )
                .overview("full")
                .build();

        mapboxMapMatching.enqueueCall(new Callback<MapMatchingResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<MapMatchingResponse> call, Response<MapMatchingResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onFailure();
                    return;
                }

                response.body().matchings().forEach(mapMatchingMatching -> {
                    routes.add(mapMatchingMatching.toDirectionRoute());
                });

                int nextIdx = pointListIdx + 1;
                if (nextIdx < points.size())
                    getMatchingMapsByPointsRoutine(nextIdx, points, routes, callback);
                else
                    callback.onSuccess(routes);
            }

            @Override
            public void onFailure(Call<MapMatchingResponse> call, Throwable t) {
                callback.onFailure();
            }
        });
    }

    private interface GetMatchingMapsCallback {
        public void onSuccess(List<DirectionsRoute> routes);
        public void onFailure();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawRoute(List<DirectionsRoute> routes) {
        List<LatLng> latLngs = new ArrayList<>();

        routes.forEach(
            directionsRoute -> directionsRoute.legs().forEach(
                routeLeg -> routeLeg.steps().forEach(
                    legStep -> legStep.intersections().forEach(
                        stepIntersection -> {
                            Point point = stepIntersection.location();
                            double lat = point.latitude();
                            double lng = point.longitude();
                            latLngs.add(new LatLng(lat, lng));
                        }
                    )
                )
            )
        );

        pathOverlay.setCoords(latLngs);
        pathOverlay.setWidth(30);
        pathOverlay.setPatternImage(OverlayImage.fromResource(R.drawable.ic_baseline_arrow_drop_up_24));
        pathOverlay.setPatternInterval(30);
        pathOverlay.setColor(Color.BLACK);

        handler.post(() -> {
            pathOverlay.setMap(naverMap);
        });
    }

    private void initNaverMapApi() {
//        mapView = findViewById(R.id.map_view_guide);
        mapFragment.getMapAsync(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        handler.post(() -> {
            naverMap.setBuildingHeight(0.4f);
            UiSettings uiSettings = naverMap.getUiSettings();
            uiSettings.setLocationButtonEnabled(false);
            uiSettings.setZoomControlEnabled(false);
            uiSettings.setScaleBarEnabled(false);
            uiSettings.setCompassEnabled(false);

            locationOverlay = naverMap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.mapbox_ic_user_puck));
            locationOverlay.setIconWidth(100);
            locationOverlay.setIconHeight(100);

            try {
                initMapboxApi();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapboxNavigation.unregisterLocationObserver(locationObserver);
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
        mapboxNavigation.unregisterOffRouteObserver(offRouteObserver);
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
//        mapboxNavigation.stopTripSession();
    }

    private List<List<Point>> points() throws JSONException {

        List<List<Point>> result = new ArrayList<>();
        List<Point> pointList = new ArrayList<>();
        result.add(pointList);
        int cnt = 0;

        NavigationRoutes routes = NavigationManager.getInstance().getRoutes();
        ArrayList<Integer>
                navigationSequence = routes.getNavigationSequence();
        HashMap<Integer, NavigationPoint>
                navigationPointHashMap = routes.getNavigationPointHashMap();
        HashMap<Integer, NavigationLineString>
                navigationLineStringHashMap = routes.getNavigationLineStringHashMap();

        for(int i = 0; i < navigationSequence.size(); i++) {
            int key = navigationSequence.get(i);

            if(navigationPointHashMap.containsKey(key)) {
                NavigationPoint nPoint = navigationPointHashMap.get(key);
                double lat = nPoint.getGeometry().getCoordinates().get(0);
                double lng = nPoint.getGeometry().getCoordinates().get(1);

                pointList.add(Point.fromLngLat(lng, lat));
                cnt ++;
                if(cnt == 100) {
                    pointList = new ArrayList<>();
                    result.add(pointList);
                    cnt = 0;
                }
            }

            else if(navigationLineStringHashMap.containsKey(key)) {
                NavigationLineString nLineString = navigationLineStringHashMap.get(key);
                NavigationLineString.Geometry geometry = nLineString.getGeometry();
                for(int g = 0; g < geometry.getCoordinates().size(); g++) {
                    double lat = geometry.getCoordinates().get(g).get(0);
                    double lng = geometry.getCoordinates().get(g).get(1);

                    pointList.add(Point.fromLngLat(lng, lat));
                    cnt ++;
                    if(cnt == 100) {
                        pointList = new ArrayList<>();
                        result.add(pointList);
                        cnt = 0;
                    }
                }
            }
        }

        /*
        navigationRoutes.getNavigationSequence().forEach(integer -> {
            if(navigationRoutes.getNavigationPointHashMap().containsKey(integer)) {
                double lat = navigationRoutes.getNavigationPointHashMap().get(integer).getGeometry().getCoordinates().get(0);
                double lng = navigationRoutes.getNavigationPointHashMap().get(integer).getGeometry().getCoordinates().get(1);

                pointList.add(Point.fromLngLat(lng, lat));
                if(pointList.size() == 100) {
                    pointList = new ArrayList<>();
                }
            }
            else if(navigationRoutes.getNavigationLineStringHashMap().containsKey(integer)) {

            }
        });

//        String json = "[[37.39205258084344,126.81421866387888],[37.43834233181613,127.00821638068321],[37.39225534779356,126.81493525966306],[37.390705536173215,126.81541025962278],[37.4373481186357,127.01468248722094],[37.38993875779166,126.804094644377],[37.43648717436349,127.01841550528599],[37.39320499602977,126.80132258352006],[37.43530394200947,127.01638516773482],[37.39509085903956,126.799681013764],[37.39452146598646,126.79885054967755],[37.393063286217796,126.79796178232168],[37.39331119696699,126.83837754090584],[37.395830478796505,126.84568791635822],[37.385678770754666,126.99542458070066],[37.38554272236702,126.99804934592031],[37.401804783179124,127.00134581150863],[37.43127360254921,127.0023282264448],[37.436209129587716,127.0017086993789],[37.43575370703851,127.00618330519463]]";
        String json = "[[37.39233030266769,126.81288266664205],[37.391991451399115,126.81278546271241],[37.391833135462704,126.81272991662556],[37.39159705028569,126.81264659745645],[37.39122209027498,126.81244662610965],[37.39084435537123,126.8123910861369],[37.39084435537123,126.8123910861369],[37.39089989417835,126.81181058187609],[37.39091933375377,126.81166337251244],[37.39093599666231,126.81156060362527],[37.39099986985578,126.81109397765523],[37.39120538740524,126.81029682227079],[37.391266483537954,126.80985241657824],[37.39126369785454,126.80938856999127],[37.391230361986196,126.8090274926783],[37.39118591802125,126.80876362904763],[37.39093592852194,126.80771928663678],[37.39076093790269,126.80710268097644],[37.39067483141935,126.80679993315744],[37.39055817105608,126.80639164024207],[37.39021929880305,126.80511121068774],[37.39004153023737,126.80446682985522],[37.390013754059105,126.80437517230617],[37.38993875779166,126.804094644377],[37.38993875779166,126.804094644377],[37.38992764526144,126.80394188081527],[37.389933197512136,126.80379189431429],[37.389963747451404,126.80367523741621],[37.390024849398706,126.80355857966715],[37.39011094888678,126.80346691894619],[37.39028314978387,126.80339192097652],[37.39078308803877,126.80318359268124],[37.39126358326905,126.80292804700373],[37.391849619878144,126.80256417491432],[37.39232733578064,126.80220308336732],[37.392738392559465,126.80183088357768],[37.39320499602977,126.80132258352006],[37.39320499602977,126.80132258352006],[37.39332442502981,126.80123369939594],[37.39394934413898,126.80076428027752],[37.394532600778454,126.80025875449617],[37.39495754414267,126.79984766897216],[37.39509085903956,126.799681013764],[37.39509085903956,126.799681013764],[37.39452146598646,126.79885054967755],[37.39452146598646,126.79885054967755],[37.394304819354666,126.79856169312173],[37.39393819028695,126.798281173318],[37.39377431904795,126.79820340718489],[37.39362155792545,126.79814230589172],[37.3934910167897,126.79810064665399],[37.39337991" +
                "788999,126.79806176439962],[37.393063286217796,126.79796178232168],[37.393063286217796,126.79796178232168],[37.39295218761352,126.79793956521667],[37.392782762399555,126.7979145722112],[37.39010806874597,126.79767577955676],[37.389833100329675,126.79765634453939],[37.38965256573716,126.79765634956671],[37.389522025437955,126.79766190825164],[37.38932204905241,126.79768413401965],[37.38912207276529,126.79771191483744],[37.38892209667508,126.79775080575489],[37.388777669591796,126.79778414007565],[37.388586026261244,126.79784525096042],[37.388430489701484,126.79789802826498],[37.388247178933625,126.7979702490174],[37.38814996883609,126.79801746964797],[37.38798054568923,126.79810913268818],[37.38786944890617,126.79818968400458],[37.38776668448894,126.79827023508898],[37.38765281079223,126.79838133925695],[37.38755560207339,126.79850633058557],[37.387458393797736,126.79865631963857],[37.38739451478845,126.79879519766384],[37.38733619147761,126.79897851593334],[37.38731952925733,126.79912017016883],[37.38731953171951,126.7992590464154],[37.38734453162394,126.79941736464042],[37.38739452877365,126.79958401474417],[37.38745563530129,126.79972566681418],[37.38752785135461,126.79985065342521],[37.38762506440697,126.79997008429028],[37.38774449720006,126.80009506958643],[37.387905592659536,126.80026727164619],[37.38805280059639,126.80042558646794],[37.38811668359446,126.80051168796183],[37.38818889945085,126.80062556447297],[37.38825833824597,126.80076166126088],[37.388308334903385,126.80090053611514],[37.388347221739714,126.80103941127881],[37.38837499850869,126.8011643991272],[37.38840555460444,126.80139493284564],[37.38842222362084,126.80163657705056],[37.38846389180334,126.8019948766065],[37.388533335277515,126.80239483826291],[37.388527803072655,126.80367527741211],[37.3885333691149,126.8043029978928],[37.38854729249747,126.8063389232854],[37.38855007586402,126.80667222620083],[37.38856399181134,126.80828874532997],[37.38856401442434,126.80956362927932],[37.388580709998" +
                "48,126.81130235943172],[37.38860019244182,126.81357159677438],[37.38862520870818,126.81465205328462],[37.388694665344325,126.81579361410617],[37.38883078775902,126.8173212490398],[37.38897523618351,126.81849058302228],[37.389169680874666,126.81977657166013],[37.38927801267228,126.82039873423231],[37.38942800881361,126.82116254941636],[37.38961133609504,126.82202079952016],[37.38993354664241,126.82347065857007],[37.39040297272527,126.82550657128392],[37.39121405303268,126.8290756682549],[37.39164459337057,126.83103381135412],[37.391739034544216,126.83146710261619],[37.391772366966876,126.83163375318502],[37.39304731942424,126.83729153600034],[37.39320842352347,126.83794980492611],[37.39331119696699,126.83837754090584],[37.39331119696699,126.83837754090584],[37.39342786454482,126.83919135247048],[37.3935195302891,126.83973852233447],[37.39363619811458,126.84056622152433],[37.39372786775742,126.84133281586223],[37.393913986128254,126.84295766278568],[37.39395009748824,126.84320763902684],[37.39398620786138,126.84340206476827],[37.39403065069915,126.84360204532713],[37.39409175807141,126.84379091532095],[37.39418063970065,126.8439631193897],[37.394288963369014,126.84412699034084],[37.39441395162117,126.84428252825185],[37.39455838201112,126.84443528809518],[37.394844464891605,126.84471581013433],[37.39551384361155,126.84539073002284],[37.395830478796505,126.84568791635822],[37.395830478796505,126.84568791635822],[37.39599435315033,126.84594066655765],[37.39608879048011,126.84615731087048],[37.396166566335516,126.84655727229747],[37.39618601409974,126.84687113207812],[37.39616379875128,126.84711277737219],[37.39611658763778,126.84742941653879],[37.39605548821133,126.8476877280684],[37.39596105912386,126.84793493042855],[37.395747201926255,126.84832101237235],[37.395063964012046,126.84922650459373],[37.39465290857331,126.84967369759337],[37.39376414029783,126.85065418872956],[37.39347529067102,126.85097638969354],[37.392872594566015,126.85163467994491],[37.39271983743586," +
                "126.85179855818475],[37.39257541242482,126.85194854856702],[37.39242543255247,126.85210131662939],[37.392264342760086,126.85224852995194],[37.39190050213517,126.85258739816032],[37.391681086669394,126.85278460856145],[37.39134501931398,126.85305126034443],[37.391306135482424,126.85308181420507],[37.391095051938166,126.85325402664861],[37.390839529404786,126.85344290548285],[37.390250715588486,126.8538206653224],[37.38972578161529,126.85410676505317],[37.38944526114418,126.85424842666002],[37.38918973737606,126.8543678673689],[37.38887310978789,126.85449841988356],[37.3886564696538,126.85457619663161],[37.388256518503944,126.85471230652213],[37.38781768268289,126.85483175234779],[37.387359404377975,126.85493453356584],[37.38620676327296,126.85509566219034],[37.38611788494866,126.85510955229621],[37.385181884421115,126.85521790189705],[37.38383482114141,126.8553623707947],[37.383593182969626,126.85538737526379],[37.38134067116316,126.85564019290331],[37.38032690217502,126.85576520981961],[37.38009637407029,126.85580410160303],[37.37988528810372,126.85584021531884],[37.379477003644695,126.85592355246305],[37.37924647578646,126.8559763318716],[37.37906871943219,126.85602077723244],[37.378635438466546,126.8561374453746],[37.37844101767598,126.85619855635072],[37.37836602682499,126.85622355616867],[37.37822160003374,126.8562735556496],[37.37801329221294,126.85634855463847],[37.37780776179811,126.85642077602475],[37.37761889616485,126.85649577447113],[37.37721894564992,126.85666799218433],[37.37683010515372,126.85685131968779],[37.37645792953766,126.85704297930151],[37.376299616110664,126.85712908699571],[37.37613019301015,126.85722352757509],[37.3759607700083,126.85732352320461],[37.37579134705576,126.85742629635922],[37.375621924201894,126.857534624564],[37.375310852850504,126.85774017009837],[37.37501922368671,126.85794571509037],[37.37472203990813,126.85816792538786],[37.37458594656806,126.85827903018793],[37.37427765351062,126.85853179357139],[37.374113786208774,126.8" +
                "5867622944737],[37.37398047037333,126.85879011169511],[37.37383049024969,126.85892899213357],[37.373680510126015,126.85906787257204],[37.373516643021446,126.8592234185485],[37.37318335444433,126.85956506343243],[37.37303337466601,126.85972338654666],[37.3729250560277,126.8598428231474],[37.37271675101852,126.86007614106632],[37.37255010705063,126.8602650174216],[37.372247370976105,126.86063443670467],[37.372089059818556,126.86084831055348],[37.37194741299856,126.8610399637367],[37.37179465709728,126.86127328010635],[37.37176132842213,126.86131772143769],[37.371508586347275,126.86167602922627],[37.371341943465055,126.86192601113416],[37.37108920237732,126.86233986942504],[37.370833684327735,126.8627815030447],[37.370705925426336,126.86300926366738],[37.37069203864288,126.86303703930584],[37.37025321515036,126.86385086640406],[37.37025321515036,126.86385086640406],[37.36972829152709,126.86472024640767],[37.36947555167325,126.8652035428281],[37.36924780807899,126.86563962062458],[37.36924780807899,126.86563962062458],[37.36899784370592,126.86601181596374],[37.36876732013852,126.86630624005753],[37.368559015177766,126.8665423355041],[37.36839792542884,126.86669232635487],[37.368192396886705,126.8668700936967],[37.367820222647886,126.86713952401698],[37.36750637388682,126.8673478471574],[37.367331395725046,126.86743395531808],[37.36717308204898,126.86750617538817],[37.36699254868004,126.86757561855309],[37.366817569925715,126.86762839641226],[37.3665981517363,126.8676728429359],[37.366317629379125,126.86770895858886],[37.36590656602391,126.86771174758215],[37.36568436914462,126.86768675605494],[37.36484279693775,126.86750624039935],[37.36454282977551,126.8673951477622],[37.36397900251964,126.86718129405547],[37.36324852690013,126.86690911696874],[37.36278746564324,126.86670359296939],[37.36233195909962,126.86648695871408],[37.36161259221494,126.86615367576178],[37.361057096188745,126.8658759387408],[37.359604474153144,126.8651538227151],[37.357724118151346,126.8641039706" +
                "4363],[37.35748803196945,126.86396510096895],[37.3563353754661,126.86326241924363],[37.35594097308866,126.86305411585306],[37.35564378234337,126.86288469510413],[37.35564378234337,126.86288469510413],[37.35356345309492,126.86203483039918],[37.352716323499976,126.86171543861867],[37.35265244157989,126.86169044267282],[37.35265244157989,126.86169044267282],[37.35212749622812,126.86133771160891],[37.351621994692685,126.86107663833269],[37.35084152279464,126.86068780656157],[37.35017770530928,126.86037118719241],[37.3493639035005,126.85995735862086],[37.34913615027568,126.85985181901104],[37.348808408209145,126.85972128446082],[37.348808408209145,126.85972128446082],[37.34849455450521,126.8596518550771],[37.3478946210107,126.85947688770798],[37.3476474262877,126.85941023399155],[37.34739467690027,126.85935746805563],[37.34711970821407,126.85932414541489],[37.34685307243688,126.85932137531877],[37.346564217114235,126.85932416089199],[37.3462975818301,126.85934916604744],[37.34600872704982,126.85938250439737],[37.34545323737957,126.85946306810384],[37.34489497015472,126.859538076837],[37.344367254841366,126.85960752966834],[37.34412006179428,126.85963531180667],[37.343892311083664,126.85967142597887],[37.343684002561545,126.85970753960939],[37.343484026800496,126.85976587320924],[37.34329516130535,126.85984920422624],[37.3431229608885,126.85995197745531],[37.34295353837104,126.8600797483337],[37.342800780981214,126.8602297389494],[37.342661911214215,126.86039917185458],[37.34253970647623,126.86058526944673],[37.34243694407484,126.86077969907288],[37.34234806919766,126.86098801593818],[37.342273081746065,126.86120466499226],[37.34222031398796,126.86142409095258],[37.34218421101232,126.8616462939739],[37.342167550176015,126.86186571892846],[37.342164776617345,126.86208514349615],[37.3421842227522,126.86230734497005],[37.34221477856119,126.86252121355889],[37.34226477665753,126.86274063665645],[37.342331439339034,126.86295172671417],[37.34241198934751,126.86316559391017],[37" +
                ".34249809421774,126.86337668342617],[37.34259530905808,126.8635961052082],[37.34268141392838,126.86380719472415],[37.34276751884806,126.86402106176526],[37.342839736638176,126.86424326176889],[37.34289528984322,126.8644737948124],[37.34292862370015,126.8647209936262],[37.342939737912985,126.86496819305913],[37.3429341874914,126.86522094800684],[37.342906417031024,126.86545148337213],[37.342892532664216,126.86561535774575],[37.34290087405934,126.86612364462489],[37.3425203970911,126.86806236781857],[37.34237597966672,126.86864009708582],[37.34215101919014,126.86939559021134],[37.34196493904498,126.86992332518678],[37.34171497860698,126.8705177225479],[37.341537228361304,126.87090658103133],[37.34135669977188,126.87124544413854],[37.34104563407856,126.87177040507333],[37.340684574777704,126.87232869770448],[37.34035128810754,126.8727786660785],[37.34025963419674,126.87289810221733],[37.34020408641836,126.87297309694638],[37.33951251405308,126.87376471090869],[37.33927643600708,126.874084132889],[37.33893203818176,126.87445910839286],[37.33844599214673,126.87494796637903],[37.33733225260615,126.8760812277134],[37.33696285777864,126.87646175896563],[37.3368850904543,126.87654230936485],[37.33616574274078,126.87728948370211],[37.336146300946695,126.87731170444587],[37.33611019471536,126.8773505908055],[37.335604707575285,126.87790055489127],[37.334890915956166,126.87871438968138],[37.33438542940755,126.8792976840714],[37.333671638478314,126.880150404217],[37.33249124582095,126.88161141539982],[37.33195798649195,126.88229470147625],[37.330549848644196,126.88410290967055],[37.32989438413105,126.88494729562171],[37.329577761597946,126.88536393323757],[37.329561097209925,126.88538337637895],[37.32901117354051,126.88608888312642],[37.328802869060354,126.88635275383365],[37.32878342726609,126.88637497457776],[37.32846680414021,126.8867582818912],[37.327839112355015,126.88749989863642],[37.32738361921309,126.88803875123715],[37.326797589014326,126.8887664791964],[37.32636709326" +
                "8516,126.88932199625339],[37.3261393472896,126.88962475285808],[37.3255505410661,126.89043302913039],[37.32527835722887,126.89081633520793],[37.32492007518029,126.89136351767829],[37.324706217318436,126.89171349182715],[37.324339605124585,126.89238566316938],[37.32407575587572,126.89289395765482],[37.323947997607526,126.89315782612017],[37.323803575444956,126.89346891298038],[37.32366748550114,126.89377166703255],[37.32347029454544,126.89424385183207],[37.323225890004196,126.89489379956815],[37.323064806021165,126.89536876088732],[37.322917609118846,126.89583261171845],[37.322728753986816,126.89649922305948],[37.32263988157903,126.89684641620197],[37.322553786923216,126.8972102744191],[37.3224649157007,126.89762412816964],[37.32233994190066,126.89828240515541],[37.32225107378954,126.89887124300196],[37.32218164707793,126.89941563990163],[37.32217887036307,126.89945730285896],[37.322117776265515,126.90001558715329],[37.32209833733553,126.900198904367],[37.3220677947564,126.90072941255669],[37.32204836348169,126.9013432461968],[37.32204281686779,126.90180987060741],[37.322045600991096,126.90218483644985],[37.322053940420915,126.9025820223402],[37.322078946362836,126.90307919867804],[37.32213451039246,126.90391800978045],[37.32228173959726,126.90527066051493],[37.32251230279775,126.90720381172257],[37.32258175045261,126.90783708556391],[37.32279286578594,126.90945082190093],[37.32300120277645,126.91101456286],[37.323140095916415,126.91215889942917],[37.323215096261585,126.91266718447598],[37.323340098270435,126.91359487445591],[37.32348176486565,126.91451423139516],[37.32361231738885,126.91519472146435],[37.323748425762766,126.91592798436031],[37.32375398106944,126.91595020440815],[37.32381231183895,126.91618629243582],[37.32392619592247,126.91665846856867],[37.32411229830132,126.91738061996847],[37.32451505433113,126.91878325903515],[37.32468726523153,126.91926932116716],[37.32494835802483,126.91994147501772],[37.32522889379899,126.92065806873119],[37.32699821768754,1" +
                "26.92541314276161],[37.327550957667476,126.92689077082284],[37.32774538890885,126.92741571768788],[37.327956485774486,126.92799065954402],[37.32811480890607,126.9284489468083],[37.328214802650365,126.92874891675547],[37.32833979522648,126.92914609939228],[37.32847867681195,126.92964049502892],[37.32858422779234,126.93007100851284],[37.328650891970376,126.93036542433981],[37.32876200162126,126.93100702959609],[37.32880922395623,126.93132088864354],[37.32884255812275,126.93158475262226],[37.32884533607301,126.93161252779834],[37.328873116367,126.93193471996483],[37.3289175694574,126.9327096483003],[37.32892868477047,126.93301795330552],[37.32892869100298,126.93336792150153],[37.32892036753977,126.93386787630007],[37.32889538142047,126.93448448762965],[37.328867615273445,126.93495666771788],[37.32873989109814,126.93713425117679],[37.32870102052204,126.93790918184209],[37.32865937308443,126.93871744288994],[37.328570519885254,126.94014231589115],[37.32850665705901,126.94118944474381],[37.3283622745926,126.94372810697897],[37.328228998093174,126.94605012192824],[37.328126254604,126.94730556627668],[37.328040166396285,126.94803050281034],[37.327931860028436,126.94883876572726],[37.32777078432966,126.94977757381318],[37.327676359895094,126.95028586360017],[37.32759304300635,126.95066638690851],[37.32740141356358,126.95150798246091],[37.32726255102674,126.95208293410047],[37.32712090959877,126.95257733758126],[37.327068141505165,126.9527773208853],[37.32705980968267,126.95280787389764],[37.32690705872918,126.95331894284179],[37.32670709310846,126.95394666917309],[37.326368260545635,126.95490214738481],[37.326115523811865,126.95556042797189],[37.32587111825701,126.95615204771666],[37.32559616123429,126.95677422109546],[37.32556005569665,126.95685199281637],[37.32537397319609,126.95724640662714],[37.325223996035135,126.95755193861547],[37.32492404205983,126.95818244527047],[37.324849053380326,126.95832965621398],[37.3242741406495,126.9594851228611],[37.32421581611,126.9595990" +
                "0303394],[37.323851982188714,126.96031561476482],[37.3216495418196,126.964915258458],[37.321441241109774,126.9653902211326],[37.32126904583852,126.96578185703467],[37.3210885188935,126.96621237852621],[37.3209079922952,126.96666234269617],[37.32081078605015,126.96692621033168],[37.320574714002625,126.96758171293861],[37.32041640858565,126.96811777977737],[37.32031087240052,126.96851774639373],[37.320138683469516,126.9692649055562],[37.32008036190203,126.96954543725826],[37.32000260111648,126.96999262103158],[37.319963721640114,126.97026759713937],[37.31994150531306,126.9704536919665],[37.319880410500474,126.97097031341089],[37.31983875758093,126.97147026915911],[37.31981932083982,126.97177579750377],[37.31979988647655,126.97221464707081],[37.31979711511396,126.97255628278074],[37.31980267473156,126.97282014754471],[37.31981657072909,126.97330899163853],[37.319830467618544,126.97384783119094],[37.319861027954516,126.97431445461532],[37.31992492335782,126.97509493748585],[37.32005826863146,126.97663090534311],[37.32011938623368,126.9773919456138],[37.32013605418875,126.9775724843035],[37.32014161004165,126.97762525713215],[37.320152722391605,126.9777669106206],[37.32020550693362,126.97848906576804],[37.32024718145492,126.97920011112458],[37.32027774674834,126.97994448709784],[37.32030275703162,126.98068330817581],[37.32030276595179,126.98118326276204],[37.320294445230644,126.98183598148299],[37.32027779035846,126.98238870951992],[37.32026946017188,126.98251092087422],[37.320261130877384,126.98268312768721],[37.32021670214202,126.98327474185824],[37.320175050168764,126.98382747059507],[37.320175050168764,126.98382747059507],[37.320091739829095,126.9845746272817],[37.31998898734917,126.98532456203826],[37.319927890262065,126.98571341731615],[37.31984457511527,126.9861911540315],[37.31976403623471,126.98660223005761],[37.319702937115586,126.98687720679082],[37.31961684322456,126.98728272792167],[37.319458540094246,126.98794656094411],[37.31913359740207,126.98904647013282" +
                "],[37.318889194680835,126.98979640885626],[37.31863923650297,126.99051579495536],[37.31842260551381,126.99110741394934],[37.318383722818844,126.9912018509045],[37.31781714579047,126.9925434115785],[37.315903550662696,126.9965708771262],[37.31524809460434,126.99788744256811],[37.315200879641324,126.99798743480845],[37.31479260876666,126.99883459152059],[37.31471762028725,126.99899291257448],[37.31418992317806,127.00008449487726],[37.31396218077999,127.000587233373],[37.3139371845706,127.00063722953223],[37.31324284581766,127.00204823193876],[37.312481850903204,127.00361477764447],[37.31179306750841,127.00505077763174],[37.3116791960367,127.00528687049086],[37.31152088735541,127.0056396206671],[37.31135146974375,127.00604236661444],[37.31124593148265,127.0063256771751],[37.31109317930026,127.00676730801416],[37.31099597340663,127.00705061834189],[37.31089876855466,127.00739225670642],[37.310790454822,127.0077866683677],[37.31071269176393,127.00810608598385],[37.310607157719396,127.00862548621797],[37.31057383142446,127.00880324878682],[37.310554391366864,127.0089226829302],[37.310490517969086,127.00937542138504],[37.310446085075434,127.00973372342665],[37.31040165545598,127.01027534215565],[37.310382219962456,127.0106503086512],[37.310376672690644,127.01107804774384],[37.310376680231364,127.01150023162995],[37.31039057445592,127.0118890848206],[37.31043224626504,127.01244736629342],[37.31046558347437,127.01288065934905],[37.310521140032634,127.01329728662925],[37.31057669470567,127.01360836793786],[37.31065447115874,127.0140388822239],[37.31077113343221,127.0145499436628],[37.31085446315986,127.0148887994495],[37.31094056979761,127.01519710237712],[37.31107111890407,127.01568316569703],[37.31140165708568,127.01685527223361],[37.31154053856028,127.01734133532007],[37.311982181874235,127.01889951479988],[37.31222105806182,127.01973832083391],[37.31244326847291,127.02051879929746],[37.312757140683836,127.02162146816057],[37.31295435171067,127.02227418114755],[37.31314600" +
                "752843,127.02291022913656],[37.313279336377434,127.02352405855478],[37.31334877962298,127.02390735513953],[37.313359890240456,127.02395179523756],[37.31341822664139,127.02450174366837],[37.31343767553373,127.02487670907703],[37.31345157140102,127.0253572206135],[37.31342103150027,127.0260349377115],[37.313376601342014,127.02654600366387],[37.31330995004803,127.02697374447148],[37.31320719336765,127.02748758958406],[37.31306833005971,127.0280181008621],[37.31296834735996,127.02833751910624],[37.312885027905665,127.02857361111592],[37.312779489648904,127.02885692168326],[37.31259896251794,127.02927633310587],[37.3124073247693,127.02965130443056],[37.31222679599986,127.02997905750904],[37.3117324224186,127.03073733584917],[37.31154078253482,127.03099287357398],[37.31055480925218,127.03230944833635],[37.31006598884388,127.0329705131275],[37.30911889909724,127.03423986886925],[37.308582862882425,127.03495370797222],[37.308418996861185,127.03517035956195],[37.30798294682166,127.03575920721146],[37.30740525070525,127.03658137098289],[37.306677577286436,127.03770073420006],[37.30600267552844,127.03882009594051],[37.30574993514087,127.03927283970086],[37.30566105938435,127.03943116115244],[37.30425849369258,127.04220039351787],[37.30383077929734,127.04297255763323],[37.30377800922531,127.04306143993274],[37.3030697799829,127.04429190364262],[37.30288925091452,127.04460299157482],[37.30153111368706,127.04676672212268],[37.3008145526226,127.0480277388556],[37.30017020243027,127.04912210202701],[37.2996841634478,127.05000536881228],[37.299511967044864,127.05033312166778],[37.29937032171279,127.05060810067938],[37.298992601522826,127.0513802634037],[37.29789000196131,127.05419948287457],[37.29764282175201,127.05494664421276],[37.29730121827833,127.05626597849083],[37.29711792079782,127.05708257618261],[37.2968957454449,127.05826303083121],[37.29676244122745,127.05902685413503],[37.29672078366267,127.05926572251296],[37.296468061166685,127.0607183755395],[37.29631809350707,127.06" +
                "155441498072],[37.2960264895627,127.06317094311758],[37.295895962356,127.06390976861599],[37.29565990285968,127.06526520778223],[37.29529609401286,127.06738169257923],[37.29503781725819,127.06887045360797],[37.2949183981873,127.06951484292456],[37.29358813308656,127.077128078268],[37.29342427984608,127.07805855398814],[37.29307157511914,127.07985562304013],[37.29274663948216,127.08134716348373],[37.29228839138241,127.08314145797742],[37.29152741928924,127.0859801106556],[37.29143576946099,127.08632730394778],[37.290688683706435,127.08911318325737],[37.29066091108714,127.08922150754184],[37.290377630330816,127.09032419322368],[37.2903165306282,127.09056583968082],[37.29000826038189,127.09209348751007],[37.289813862796436,127.09345170307317],[37.28979720054867,127.09359057983],[37.289680567203334,127.09469603836592],[37.28966668375531,127.09490990824116],[37.2896194801915,127.09564595389973],[37.28960005422374,127.09655142785226],[37.2895889533084,127.09704860528036],[37.28959174071834,127.09760411036044],[37.289638976084596,127.09864290368107],[37.28971954893738,127.10012887771953],[37.28976122317154,127.10082048047298],[37.28979456299721,127.1013982049029],[37.289850127996644,127.10228423407277],[37.28988068857486,127.10276196765234],[37.28995292438486,127.10398685450257],[37.29003904443902,127.10504231188963],[37.290089046612785,127.10548671461427],[37.29025849438099,127.10676714925238],[37.290505714503496,127.10824756356507],[37.290702934295,127.1093863436079],[37.29078904489224,127.10991407109329],[37.291050153892385,127.1114833658428],[37.291164043084564,127.11223607213901],[37.29122793269043,127.11268880705197],[37.291239043709716,127.11275546735938],[37.29128626803196,127.11317764995658],[37.29131682502847,127.11345540167966],[37.291377942757244,127.114219219562],[37.29143350681975,127.11505247574604],[37.29144463900885,127.1162995845257],[37.2914196524154,127.1168856431757],[37.291375228063735,127.11771890217025],[37.29127803653212,127.11880213997209],[37.291" +
                "10308071702,127.12013535728443],[37.291039206502745,127.12054087785009],[37.29085869179125,127.12165189325432],[37.290689286904524,127.12276290834761],[37.29067262421132,127.12287678737503],[37.29065040725384,127.12302677439439],[37.29028104845669,127.12541545699406],[37.29001721983532,127.12707086981044],[37.29001166557113,127.1271069778025],[37.28985892343076,127.12810689139957],[37.28967007879832,127.12935400581287],[37.289539553416766,127.13019282228831],[37.289145199818655,127.13270927179686],[37.288942468607445,127.13399527196823],[37.28870641350808,127.13559235597472],[37.28859810947292,127.13652560771],[37.28858144787743,127.13670059230796],[37.28854812752815,127.13720888047827],[37.28854535236538,127.13733664674616],[37.28852591745188,127.13774216606922],[37.28852314463234,127.13800047605288],[37.28852314463234,127.13800047605288],[37.28852037570164,127.13847543305428],[37.28852593874033,127.13892816961456],[37.28855094461959,127.13941701346461],[37.28858150526152,127.13989752458097],[37.28859539643073,127.14011417120834],[37.28863429028868,127.14064190003037],[37.28870929885387,127.14160292187327],[37.28871207670881,127.14162514200213],[37.28879819559758,127.14261393879175],[37.288923209044775,127.14416934976693],[37.2890593361583,127.14593862992336],[37.28914823270613,127.14693853674024],[37.28926491374087,127.1484911704269],[37.28930658810115,127.14918832825174],[37.289309370794605,127.14947996839149],[37.289289942269775,127.15024101103104],[37.289267728956496,127.15059375744373],[37.28924273798773,127.1509353938311],[37.28921496931357,127.15126314266726],[37.28907333387109,127.15208807184152],[37.28900112590043,127.15241582192829],[37.28886781883561,127.15301854879768],[37.28857898351156,127.15413789986005],[37.288429011311834,127.15471840699232],[37.28841234782279,127.15478784560864],[37.28814850710952,127.15576831967496],[37.28814850710952,127.15576831967496],[37.28761247944521,127.1569571158441],[37.2873597420206,127.15757373370683],[37.2870486798150" +
                "7,127.158290344144],[37.28687092996199,127.15869864545449],[37.28669873507097,127.1591097241348],[37.28656820082337,127.15945414102111],[37.28645988592492,127.15978189212686],[37.28629047103496,127.16033462455127],[37.2862321489486,127.16058460352491],[37.28615994077956,127.1609012435114],[37.286118282780954,127.16111511417932],[37.286046076757806,127.16155118778045],[37.286007197812246,127.16185393919973],[37.28597109607329,127.16214280291135],[37.28594055084348,127.16252054729614],[37.28592944750682,127.16288162597837],[37.285926676338846,127.16323159432251],[37.2859266800319,127.16343713124066],[37.285946128861994,127.16380654164104],[37.285960019735676,127.16400652311658],[37.286001690011226,127.16447592382492],[37.286071136844065,127.16505642478857],[37.28609891649564,127.16533973165107],[37.2861211428822,127.16571469702545],[37.28612392517971,127.165984116962],[37.28611004259109,127.16624520478985],[37.286087827634304,127.1665062928523],[37.286048947991034,127.16677015890981],[37.286007290741985,127.16702569246779],[37.285935082873756,127.16735899761179],[37.28586565086438,127.167603421848],[37.28578233227346,127.1678867318382],[37.285587919882026,127.16841724476363],[37.285401838111795,127.16885054404723],[37.285249083967614,127.16918107393334],[37.284568633734445,127.17065595936067],[37.2843075636453,127.17125591231313],[37.28418813902416,127.1715892187881],[37.28408815659566,127.1719225247159],[37.2840464979993,127.172103065075],[37.28394096360418,127.1726002451901],[37.283890974337034,127.17287522166603],[37.28385487125204,127.1730890921803],[37.28380211386497,127.17388346608656],[37.28380211386497,127.17388346608656],[37.28361880805367,127.17423343951675],[37.283552151354165,127.17435843006109],[37.28347160707463,127.17446675584073],[37.283277188643,127.17466118813078],[37.28312720851029,127.17480006865078],[37.28300222543247,127.17493617094112],[37.28292723601514,127.17504171903852],[37.282874466246575,127.17514726651038],[37.28284391627656,127.175261145" +
                "93457],[37.28282725358704,127.1753750249678],[37.2828355882021,127.1755000134011],[37.2828605874539,127.17561944631335],[37.28290780635448,127.17573887860001],[37.28296613482981,127.17584442294411],[37.28312167611592,127.17605273301156],[37.28323277680633,127.17618882865531],[37.28343275823883,127.17644435541263],[37.28358829932529,127.17664155537601],[37.283671624655874,127.17673321138639],[37.28372439727038,127.17678598289363],[37.28379105696563,127.17682764390607],[37.283868826385394,127.17686374955373],[37.28395770523006,127.17687763468099],[37.2840521386873,127.17687485449609],[37.28414379463851,127.17686929686343],[37.28422711822143,127.17686373946539],[37.28429655467367,127.17686651503621],[37.28441876279166,127.17686928912113],[37.28449097689972,127.17688317471752],[37.28457430103187,127.1769081701049],[37.28465762526387,127.17693872054414],[37.284760392437505,127.17701093332515],[37.28495759421678,127.17714424901794],[37.2852186780853,127.17731089322317],[37.285318668002574,127.17739421618604],[37.285424213281445,127.17750253672601],[37.28560475157253,127.1777052910364],[37.28583528462253,127.17793859672425],[37.28583528462253,127.17793859672425],[37.285996379722285,127.17808580106298],[37.28611581198198,127.17817745605593],[37.28622135616223,127.1782246710248],[37.28636300742141,127.17828021755463],[37.28660742500518,127.17836075892383],[37.286829623139965,127.17845241102245],[37.28697682961087,127.17852462255132],[37.28710181648303,127.17859961223185],[37.2872295813606,127.17870515461952],[37.28741289645853,127.17887180101341],[37.28752399724883,127.17901345170758],[37.287626765720844,127.17915788016232],[37.28772120177468,127.17929953132578],[37.28780452805415,127.17944396032804],[37.28795173822079,127.17972170877631],[37.28825171351635,127.18027998304211],[37.28837114842313,127.18051884690928],[37.28844614253428,127.18067438624982],[37.288521137094975,127.18085492332385],[37.28858502203115,127.18104657081456],[37.288635019786845,127.18124377374834],[37." +
                "288676685124386,127.18143819939088],[37.28872390567382,127.18164929003262],[37.28879057146274,127.18202980920933],[37.28882112782524,127.18227145310591],[37.28884335156978,127.18249920960753],[37.288862800006186,127.18284639980308],[37.288865581857976,127.18309082200814],[37.288865585554404,127.1832963589282],[37.28886003618693,127.18360466446487],[37.28883505351872,127.18440737016819],[37.288821172632495,127.18476289388097],[37.28880173383686,127.18495176619332],[37.28878229414204,127.18509064303859],[37.288721193754945,127.18529340415452],[37.28864898319383,127.18547672290184],[37.28856566220889,127.18562671165077],[37.28846289898139,127.18577392342155],[37.28832958403665,127.18593780120885],[37.28820737811695,127.1860572382679],[37.287976853058325,127.18626833673531],[37.2877963218096,127.1864572135866],[37.28761579091052,127.18666553311968],[37.287488031027706,127.1868377433286],[37.28735471638256,127.18701828627194],[37.28723528886798,127.1871904962462],[37.28707420101074,127.1874460331731],[37.28700199004991,127.18760713171336],[37.286949221281745,127.1877682297059],[37.28692978073773,127.1878598886104],[37.28690200867476,127.18799876569102],[37.28689090139833,127.1881404198281],[37.286885349833476,127.1883265142241],[37.28689090549507,127.18836817695704],[37.28690479507374,127.18849594276006],[37.28694923756878,127.18867370316963],[37.28699923452637,127.18882646568902],[37.28707422903852,127.18900422523777],[37.28716311058135,127.18916809676537],[37.287329763443,127.18947361992585],[37.28741031251768,127.18963193663625],[37.28751308204057,127.18983469313594],[37.287585298796984,127.18999578760703],[37.287651960891054,127.19017076986444],[37.28770473565471,127.19034297498717],[37.28776028882384,127.19056795302497],[37.28779917660689,127.19075682369481],[37.28784084534376,127.19114012110391],[37.28787140415645,127.19151786377421],[37.28789362675335,127.19168173718002],[37.287929736830634,127.19185672029836],[37.287982511144826,127.19200392768742],[37.2880463953" +
                "8336,127.19215668981532],[37.288126944408454,127.19231222899953],[37.28819916096534,127.19246221336662],[37.288268600116126,127.19261497533792],[37.288332484404705,127.19277051499174],[37.28839081398101,127.19293716490593],[37.28842136889644,127.19309826055094],[37.2884380361816,127.19323991390557],[37.288435261673904,127.19340378801587],[37.28841859998569,127.19357321756952],[37.28839360532952,127.19370931704641],[37.28839360532952,127.19370931704641],[37.28804643546108,127.1943842656415],[37.287596502265664,127.1952008709582],[37.287565953496994,127.19538141100753],[37.28757706532045,127.19549251173335],[37.28777427284988,127.19594524290906],[37.28784371375042,127.19619521828962],[37.28792148831876,127.19651740911063],[37.28794927142627,127.19699236526942],[37.2879576143405,127.19757842301536],[37.287977070928065,127.19837834994857],[37.287999295325314,127.1986422142901],[37.288035404253996,127.19875331431142],[37.28807984490122,127.19882830626013],[37.288152059909535,127.19889218732197],[37.28821594209968,127.19893107088491],[37.288277046533814,127.19895328937025],[37.28836870263518,127.19895606431251],[37.28844647105614,127.19893661943843],[37.288502019628346,127.19890606508679],[37.28858256455568,127.19883384714085],[37.28862144759152,127.19875885284335],[37.288657553021295,127.19867552604627],[37.288671438652315,127.1985838672976],[37.28866865909688,127.19846721128495],[37.28858254926065,127.19798392419241],[37.28855754765732,127.19773394755934],[37.288560323364045,127.19763673407195],[37.28859365033814,127.19749785683378],[37.2886353106302,127.19741175235424],[37.288690858252735,127.19732842500925],[37.28878251275457,127.19724231912049],[37.2888491704514,127.19717287909221],[37.28930744475968,127.19684234058366],[37.28939909936139,127.19676178974692],[37.28949075371319,127.19666735128035],[37.28959074018335,127.19655902494908],[37.28967128476072,127.19646736432166],[37.289754606094455,127.19633681825242],[37.2898295950599,127.19620627241811],[37.2898795871704" +
                "04,127.19608961491825],[37.29003234115794,127.19575075244438],[37.29009899795503,127.19563131694889],[37.29018509734458,127.19553410111313],[37.29027119718395,127.19546188301105],[37.290335078074285,127.1954285508988],[37.29052672159492,127.19537577250341],[37.290721142921434,127.19534243671146],[37.29160992727007,127.19525353082686],[37.29178212934745,127.19524241586872],[37.29199321600989,127.19524240991824],[37.292173750955236,127.1952590699847],[37.292173750955236,127.1952590699847],[37.2923542865503,127.19531183788854],[37.29241261402771,127.19536183171151],[37.2924626093368,127.19542293587318],[37.29250427287743,127.19551737058124],[37.29251260654521,127.19558958602123],[37.29250705248275,127.1956368041191],[37.29242928646145,127.19578957023914],[37.29228208608736,127.19605621688105],[37.29225986708846,127.19609232534492],[37.29219043218584,127.19617565308118],[37.292084889754314,127.19622565152386],[37.29126276130319,127.19614512644726]]";
        JSONArray array = new JSONArray(json);

        int subUnit = 100;
        int maxIdx = (int) Math.ceil(array.length() / subUnit);

        List<List<Point>> result = new ArrayList<>();

        for (int i = 0; i < maxIdx; i++) {
            List<Point> pointList = new ArrayList<>();

            for (int j = 0; j < subUnit; j++) {
                JSONArray pointJson = array.getJSONArray(i * subUnit + j);
                Point point = Point.fromLngLat(pointJson.getDouble(1), pointJson.getDouble(0));

                pointList.add(point);
            }

            result.add(pointList);
        }
*/

        return result;
    }

    @Override
    public void onBackPressed() {
        mapboxNavigation.unregisterLocationObserver(locationObserver);
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
        mapboxNavigation.unregisterOffRouteObserver(offRouteObserver);
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
//        mapboxNavigation.stopTripSession();
//        finish();
        super.onBackPressed();
    }
}