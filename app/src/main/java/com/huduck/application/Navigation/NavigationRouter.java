package com.huduck.application.Navigation;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.huduck.application.common.NetworkTask;
import com.huduck.application.myCar.TruckInformation;
import com.naver.maps.geometry.LatLng;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NavigationRouter{
    @NonNull @Getter @Setter
    private LatLng currentLocation;
    @NonNull @Getter @Setter
    private LatLng targetLocation;
    @NonNull @Getter @Setter
    private TruckInformation truckInformation;
    @Builder.Default @NonNull @Getter
    private String searchOption = "0";

    @Builder
    public NavigationRouter(@NonNull LatLng currentLocation, @NonNull LatLng targetLocation, @NonNull TruckInformation truckInformation, @NonNull String searchOption) {
        this.currentLocation = currentLocation;
        this.targetLocation = targetLocation;
        this.truckInformation = truckInformation;
        this.searchOption = searchOption;
    }

    public void setSearchOption(String searchOption) {
        if(searchOption.equals("0") || searchOption.equals("1") ||searchOption.equals("2") ||
                searchOption.equals("3") || searchOption.equals("4") || searchOption.equals("10") ||
                searchOption.equals("12")) {
            this.searchOption = searchOption;
        }
    }

    public void findRoutes(String sktMapApiKey, @Nullable LatLng lastLastLocation, OnFoundRoutesCallback onFoundRoutesCallback) {
        NavigationRoutes navigationRoutes = new NavigationRoutes();

        String url = "https://apis.openapi.sk.com/tmap/truck/routes?version=1&format=json&callback=result&appKey=" + sktMapApiKey;

        // AsyncTask를 통해 HttpURLConnection 수행.

        ContentValues values = new ContentValues();

        if (lastLastLocation != null) {
            values.put("passList", currentLocation.longitude+","+currentLocation.latitude);
            values.put("startX", lastLastLocation.longitude);
            values.put("startY", lastLastLocation.latitude);
        }
        else {
            values.put("startX", currentLocation.longitude);
            values.put("startY", currentLocation.latitude);
        }
        values.put("endX", targetLocation.longitude);
        values.put("endY", targetLocation.latitude);
        values.put("reqCoordType", "WGS84GEO");
        values.put("resCoordType", "WGS84GEO");
        values.put("angle", "172");
        values.put("directionOption", "1");
        values.put("searchOption", searchOption);
        values.put("trafficInfo", "Y");
        values.put("truckType", "1");
        values.put("truckWidth", truckInformation.getTruckWidth());
        values.put("truckHeight", truckInformation.getTruckHeight());
        values.put("truckLength", truckInformation.getTruckLength());
        values.put("truckWeight", truckInformation.getLoadWeight());
        values.put("truckTotalWeight", truckInformation.getTotalWeight());



        NetworkTask networkTask = new NetworkTask(url, values) {
            @SuppressLint("StaticFieldLeak")
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            protected void onPostExecute(String s) {
                try {
                    NavigationRoutes routes = NavigationRoutesParser.parserTruckRoutes(s);
                    onFoundRoutesCallback.OnSuccess(routes);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        networkTask.execute();
    }

    public static interface OnFoundRoutesCallback {
        public void OnSuccess(NavigationRoutes navigationRoutes);
    }

  /*  // 맵 매칭 뷰분 만들어야함
    private List<List<Point>> points;
    private List<Point> point100List;
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void findMapMatchingRoutesByNavigationRoutes(String mapboxApiKey, NavigationRoutes navigationRoutes, OnFoundMapMatchingRoutesCallback callback) {
        points = new ArrayList<>();
        point100List = new ArrayList<>();

        points.add(point100List);

        navigationRoutes.getNavigationLineStringHashMap().forEach((integer, navigationLineString) -> {
            navigationLineString.getGeometry().getCoordinates().forEach(doubles -> {
                if(point100List.size() == 100) {
                    point100List = new ArrayList<>();
                    points.add(point100List);
                }

                Point point = Point.fromLngLat(doubles.get(1), doubles.get(0));
                point100List.add(point);
            });
        });

        findMapMatchingRoutesByPoints(mapboxApiKey, points, callback);
    }

    private void findMapMatchingRoutesByPoints(String mapboxApiKey,
                                               List<List<Point>> points,
                                               OnFoundMapMatchingRoutesCallback callback) {
        List<DirectionsRoute> directionsRoutes = new ArrayList<>();
        findMapMatchingRoutesByPointsRoutine(mapboxApiKey, 0, points, directionsRoutes, callback);
    }

    private void findMapMatchingRoutesByPointsRoutine(String mapboxApiKey,
                                                      int idx, List<List<Point>> points,
                                                      List<DirectionsRoute> directionsRoutes,
                                                      OnFoundMapMatchingRoutesCallback callback) {
        List<Point> point100List = points.get(idx);
        MapboxMapMatching mapboxMapMatching = MapboxMapMatching.builder()
                .accessToken(mapboxApiKey)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .steps(true)
                .roundaboutExits(true)
                .annotations(
                        DirectionsCriteria.ANNOTATION_CONGESTION,
                        DirectionsCriteria.ANNOTATION_DURATION,
                        DirectionsCriteria.ANNOTATION_MAXSPEED,
                        DirectionsCriteria.ANNOTATION_SPEED,
                        DirectionsCriteria.ANNOTATION_DISTANCE
                )
                .voiceInstructions(true)
                .bannerInstructions(true)
                .language(Locale.KOREA)
                .voiceUnits(DirectionsCriteria.METRIC)
                .coordinates(point100List)
                .baseUrl(Constants.BASE_API_URL)
                .user(Constants.MAPBOX_USER)
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .build();

        mapboxMapMatching.enqueueCall(new Callback<MapMatchingResponse>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<MapMatchingResponse> call, Response<MapMatchingResponse> response) {
                if(!response.isSuccessful())
                    return;

                List<MapMatchingMatching> matchings = response.body().matchings();
                if(matchings.size() == 0)
                    return;

                DirectionsRoute directionsRoute = matchings.get(0).toDirectionRoute();
                directionsRoutes.add(directionsRoute);

                int nextIdx = idx + 1;
                if(nextIdx < points.size())
                    findMapMatchingRoutesByPointsRoutine(mapboxApiKey, nextIdx, points, directionsRoutes, callback);
                else {
                    callback.OnSuccess(directionsRoutes);
                }
            }

            @Override
            public void onFailure(Call<MapMatchingResponse> call, Throwable t) {

            }
        });
    }

    public static interface OnFoundMapMatchingRoutesCallback{
        public void OnSuccess(List<DirectionsRoute> directionsRoutes);
    }*/
}
