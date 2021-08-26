package com.huduck.application.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.huduck.application.Navigation.NavigationRoutes;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.MapboxNavigationProvider;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class NavigationManager implements LocationObserver{
    private static NavigationManager instance = new NavigationManager();

    @Getter
    private boolean init = false;
    private Context context = null;

    // Mapbox
    private String accessToken;
    @Getter
    private MapboxNavigation navigation;

    // Routes
    @Setter
    @Getter
    NavigationRoutes routes;

    // Location
    private Location currentRowLocation = new Location("");
    private Location currentEnhancedLocation = new Location("");

    public Location getCurrentRowLocation() {
        return new Location(currentRowLocation);
    }

    public Location getCurrentEnhancedLocation() {
        return new Location(currentEnhancedLocation);
    }

    public static NavigationManager getInstance() {
        return instance;
    }

    public MapboxNavigation initNavigationManager(@NonNull Context context, @NonNull String accessToken) {
        this.context = context;
        this.accessToken = accessToken;

        NavigationOptions navigationOption
                =new NavigationOptions.Builder(context.getApplicationContext()).build();

        navigation = MapboxNavigationProvider.create(navigationOption);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }

        navigation.registerLocationObserver(this);

        navigation.startTripSession();

        init = true;
        return navigation;
    }

    @Override
    public void onEnhancedLocationChanged(@androidx.annotation.NonNull Location location, @androidx.annotation.NonNull List<? extends Location> list) {
        currentEnhancedLocation = location;
    }

    @Override
    public void onRawLocationChanged(@androidx.annotation.NonNull Location location) {
        currentRowLocation = location;
    }
}
