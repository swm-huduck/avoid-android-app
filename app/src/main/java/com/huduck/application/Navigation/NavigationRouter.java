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
    @NonNull @Getter
    private String searchOption = "0";

    @Builder
    public NavigationRouter(@NonNull LatLng currentLocation, @NonNull LatLng targetLocation, @NonNull TruckInformation truckInformation, @NonNull String searchOption) {
        this.currentLocation = currentLocation;
        this.targetLocation = targetLocation;
        this.truckInformation = truckInformation;
        setSearchOption(searchOption);
    }

    public void setSearchOption(String searchOption) {
        if(searchOption.equals("0") || searchOption.equals("1") ||searchOption.equals("2") ||
                searchOption.equals("3") || searchOption.equals("4") || searchOption.equals("10") ||
                searchOption.equals("12")) {
            this.searchOption = searchOption;
        }
        else
            this.searchOption = "0";
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
        values.put("totalValue", "1");
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
}
