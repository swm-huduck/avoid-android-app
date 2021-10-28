package com.huduck.application.Navigation;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.naver.maps.map.NaverMap;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;

public class LocationProvider {
    private static LocationProvider locationProvider = new LocationProvider();

    private List<Location> rowLocationList = new ArrayList<>();

    private LocationProvider() {

    }

    private static int maxLen = 5;
    private static void updateRowLocation(Location location) {
        if(location == null) return;
        List<Location> list = locationProvider.rowLocationList;
        if(list.size() >= maxLen) {
            for(int i = 0; i < maxLen - 1; i++) {
                Location item = list.get(i + 1);
                list.set(i, item);
            }
            list.set(maxLen - 1, location);
        }
        else {
            list.add(location);
        }
    }

    public static Location getLastRowLocation() {
        return getLastRowLocation(maxLen);
    }

    public static Location getLastRowLocation(int index) {
        List<Location> list = locationProvider.rowLocationList;
        if(list.size() == 0) return null;
        index = Math.min(index, list.size() - 1);
        return list.get(index);
    }

    public static int getLastRowLocationSize() {
        return locationProvider.rowLocationList.size();
    }

    public static NaverMap.OnLocationChangeListener locationChangeListener = new NaverMap.OnLocationChangeListener() {
        @Override
        public void onLocationChange(@NonNull Location location) {
            updateRowLocation(location);
        }
    };
}
