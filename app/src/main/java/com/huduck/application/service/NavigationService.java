package com.huduck.application.service;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapPoint;

import java.util.ArrayList;

public class NavigationService extends Service implements LocationListener {
    private final String TAG = "NavigationService";

    private TMapGpsManager gps;

    private static NavigationInfo info = null;

    public static NavigationInfo GetNavigationInfo() {
        if (info == null)
            info = new NavigationInfo();
        return info;
    }

    public NavigationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return Service.START_STICKY;
        else {
            Log.d(TAG, "Service 시작");
            processCommand(intent);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void processCommand(Intent intent) {
        GetNavigationInfo();
        gps = new TMapGpsManager(this);
        gps.setMinTime(1000);
        gps.setMinDistance(1);
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this::onLocationChanged);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this::onLocationChanged);

        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            gps.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
        Log.d(TAG, "GPS 시작");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static ArrayList<NavigationLocationChangeCallback> locationChangeCallbacks =  new ArrayList<>();
    public static void addNavigationLocationChangeEvent(NavigationLocationChangeCallback changeCallback) {
        locationChangeCallbacks.add(changeCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onLocationChanged(@NonNull Location location) {
        info.currentPoint = new TMapPoint(location.getLatitude(), location.getLongitude());
        locationChangeCallbacks.forEach(callback -> {
            callback.onLocationChange(info.currentPoint, info);
        });

        // 로그
        Log.d(TAG, "LocationChange: " + info.currentPoint.getLatitude() + ", " + info.currentPoint.getLongitude());
    }

    public static class NavigationInfo {
        private TMapPoint currentPoint = null;
        private Double currentSpeed = 0.0;

        private NavigationInfo() {}

        public TMapPoint getCurrentPoint() {
            if(currentPoint != null) {
                TMapPoint newInstance = new TMapPoint(currentPoint.getLatitude(), currentPoint.getLongitude());
                return newInstance;
            }
            else  {
                return null;
            }
        }

        public Double getCurrentSpeed() {
            return currentSpeed;
        }
    }

    public static interface NavigationLocationChangeCallback {
        public void onLocationChange(TMapPoint currentPoint, NavigationInfo info);
    }
}