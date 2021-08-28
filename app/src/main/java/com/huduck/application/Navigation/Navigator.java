package com.huduck.application.Navigation;

import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.huduck.application.Navigation.LatLngTool;
import com.huduck.application.Navigation.NavigationLineString;
import com.huduck.application.Navigation.NavigationPoint;
import com.huduck.application.Navigation.NavigationRoutes;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.NaverMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class Navigator implements LocationListener, NaverMap.OnLocationChangeListener {
    /* Route */
    private NavigationRoutes navigationRoute = null;
    private List<NavigatorLineStringSegment> lineStringSegmentList = new ArrayList<>();

    // Route Information
    private double routeTotalDistance = 0;

    /* Navigator */
    // State
    private boolean startedNavigator = false;

    // Route
    @Getter
    private LatLng currentPositionOnRoute;
    private int currentLineStringSegIdx = 0;       // 몇 번째 Segment인지
    private double currentLineStringSegPassedDistance = -99999999;  // Segment 시작점에서 얼마나 떨어져 있었는지

    // Row Location
    private Location currentLocation;
    private Location lastLocation;

    // Event
    private List<OnRouteChangedCallback> routeChangedCallbackList = new ArrayList<>();
    private List<OnEnhancedLocationChangedCallback> enhancedLocationChangedCallbackList = new ArrayList<>();
    private List<OnProgressChangedCallback> progressChangedCallbackList = new ArrayList<>();
    private List<OnOffRouteCallback> offRouteCallbackList = new ArrayList<>();

    public void startNavigator() {
        startedNavigator = true;

        if (currentLocation != null)
            onLocationChanged(currentLocation);
    }

    public void stopNavigator() {
        startedNavigator = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setRoute(NavigationRoutes navigationRoute) {
        this.navigationRoute = navigationRoute;
        lineStringSegmentList = navigationRouteToLineStringSegment(navigationRoute);

        // init lineStringSeg parm
        currentLineStringSegIdx = 0;
        currentLineStringSegPassedDistance = 0;

        // init TotalDistance
        routeTotalDistance = 0;
        lineStringSegmentList.forEach(seg -> routeTotalDistance += seg.getDistanceMeter());

        // Call event
        for (OnRouteChangedCallback onRouteChangedCallback : routeChangedCallbackList) {
            onRouteChangedCallback.onRouteChanged(navigationRoute, lineStringSegmentList);
        }

        if (currentLocation != null)
            onLocationChanged(currentLocation);
    }

    /* LineString 리스트를 LineStringSegment 리스트로 바꿔 줌 */
    private List<NavigatorLineStringSegment> navigationRouteToLineStringSegment(NavigationRoutes navigationRoutes) {
        List<NavigatorLineStringSegment> result = new ArrayList<>();

        List<Integer> seq = navigationRoutes.getNavigationSequence();
        HashMap<Integer, NavigationLineString> lineStringHashMap = navigationRoutes.getNavigationLineStringHashMap();
        HashMap<Integer, NavigationPoint> pointHashMap = navigationRoutes.getNavigationPointHashMap();

        LatLng lastPoint = null;
        NavigatorLineStringSegment lastSeg = null;


        for (Integer key : seq) {
            if (lineStringHashMap.containsKey(key)) {
                NavigationLineString lineString = lineStringHashMap.get(key);
                for (ArrayList<Double> coordinate : lineString.getGeometry().getCoordinates()) {
                    LatLng point = new LatLng(coordinate.get(0), coordinate.get(1));
                    if (lastPoint == null) {
                        lastPoint = point;
                        continue;
                    }
                    NavigatorLineStringSegment lineStringSegment = new NavigatorLineStringSegment(lastPoint, point);
                    result.add(lineStringSegment);
                    lastPoint = point;
                    lastSeg = lineStringSegment;
                }
            } else if (pointHashMap.containsKey(key)) {
                if (lastSeg == null) continue;
                NavigationPoint nPoint = pointHashMap.get(key);
                lastSeg.setEndPointEvent(nPoint);
            }
        }

        return result;
    }

    private double distanceBetweenRouteAndRowLocation = 0;
    private LatLng findCurrentPositionOnRoute(Location currentLocation) {
        LatLng rowPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        int minDistIdx = -1;
        double minDist = Double.MAX_VALUE;
        NavigatorLineStringSegment targetSeg = null;
        LatLng positionOnRoute = null;

        for (int i = currentLineStringSegIdx; i < lineStringSegmentList.size(); i++) {
            NavigatorLineStringSegment seg = lineStringSegmentList.get(i);

            // 현재 위치와 Segment 사이의 거리 계산
            // 현재 위치를 경로위의 위치로 바꾸는 것
            NavigatorLineStringSegment.DistanceAndPoint distanceAndPoint = seg.getDistanceAndPoint(rowPosition);

            if (distanceAndPoint.distance < minDist) {
                minDistIdx = i;
                minDist = distanceAndPoint.distance;
                targetSeg = seg;
                positionOnRoute = distanceAndPoint.point;
            }
        }

        distanceBetweenRouteAndRowLocation = minDist;

        double passedDistance = LatLngTool.mag(targetSeg.startPoint, positionOnRoute);  // Deg

        if (minDistIdx > currentLineStringSegIdx) {
            currentLineStringSegIdx = minDistIdx;
            currentLineStringSegPassedDistance = passedDistance;
            currentPositionOnRoute = positionOnRoute;
        } else if (minDistIdx == currentLineStringSegIdx && passedDistance >= currentLineStringSegPassedDistance) {
            currentLineStringSegPassedDistance = passedDistance;
            currentPositionOnRoute = positionOnRoute;
        } else {

        }

        return this.currentPositionOnRoute;
    }

    private NavigationPoint nextTurnEvent = null;
    private double nextTurnEventLeftDistanceMeter = 0;
    private double routeTotalLeftDistance = 0;

    private void updateNextTurnEvent() {
        double leftDistance = 0;    // M
        NavigationPoint nextTurnEvent = null;

        for (int i = currentLineStringSegIdx; i < lineStringSegmentList.size(); i++) {
            NavigatorLineStringSegment seg = lineStringSegmentList.get(i);

            if (i == currentLineStringSegIdx) {
                leftDistance += LatLngTool.mag(LatLngTool.sub(seg.endPoint, currentPositionOnRoute), true);
            } else {
                leftDistance += seg.getDistanceMeter();
            }

            if (seg.endPointEvent != null && nextTurnEvent == null) {
                nextTurnEvent = seg.endPointEvent;
                nextTurnEventLeftDistanceMeter = leftDistance;
            }
        }

        this.nextTurnEvent = nextTurnEvent;
        routeTotalLeftDistance = leftDistance;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("Location", location.toString());
        if (location.getLongitude() < 0) return;

        currentLocation = location;

        if (!startedNavigator || navigationRoute == null) return;
        findCurrentPositionOnRoute(location);

        if (currentPositionOnRoute == null) return;

        for (OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback : enhancedLocationChangedCallbackList) {
            onEnhancedLocationChangedCallback.onEnhancedLocationChange
                    (currentLineStringSegIdx, currentLineStringSegPassedDistance, currentPositionOnRoute, lineStringSegmentList.get(currentLineStringSegIdx).directionBearing);
        }

        updateNextTurnEvent();

        double progress = (routeTotalDistance - routeTotalLeftDistance) / routeTotalDistance;
        for (OnProgressChangedCallback onProgressChangedCallback : progressChangedCallbackList) {
            onProgressChangedCallback.onProgressChanged(progress, nextTurnEvent, nextTurnEventLeftDistanceMeter);
        }

        boolean checkOffRoute = checkOffRoute();
        if(checkOffRoute)
            for (OnOffRouteCallback onOffRouteCallback : offRouteCallbackList) {
                onOffRouteCallback.onOffRoute();
            }
    }

    private int offRouteCount = 0;
    private boolean checkOffRoute() {
        if (distanceBetweenRouteAndRowLocation > 0.0003) {
            offRouteCount++;
            if (offRouteCount > 8) {
                return true;
            }
        } else {
            offRouteCount = 0;
        }
        return false;
    }

    public void addOnRouteChangedCallback(OnRouteChangedCallback onRouteChangedCallback) {
        routeChangedCallbackList.add(onRouteChangedCallback);
    }

    public void removeOnRouteChangedCallback(OnRouteChangedCallback onRouteChangedCallback) {
        routeChangedCallbackList.remove(onRouteChangedCallback);
    }

    @Override
    public void onLocationChange(@NonNull Location location) {
        onLocationChanged(location);
    }

    public interface OnRouteChangedCallback {
        public void onRouteChanged(NavigationRoutes route, List<NavigatorLineStringSegment> navigatorLineStringSegmentList);
    }

    public interface OnEnhancedLocationChangedCallback {
        public void onEnhancedLocationChange(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng currentPosition, double currentBearing);
    }

    public void addOnEnhancedLocationChangedCallback(OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback) {
        enhancedLocationChangedCallbackList.add(onEnhancedLocationChangedCallback);
    }

    public void removeOnEnhancedLocationChangedCallback(OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback) {
        enhancedLocationChangedCallbackList.remove(onEnhancedLocationChangedCallback);
    }

    public interface OnProgressChangedCallback {
        public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter);
    }

    public void addOnProgressChangedCallback(OnProgressChangedCallback onProgressChangedCallback) {
        progressChangedCallbackList.add(onProgressChangedCallback);
    }

    public void removeOnProgressChangedCallback(OnProgressChangedCallback onProgressChangedCallback) {
        progressChangedCallbackList.remove(onProgressChangedCallback);
    }

    public interface OnOffRouteCallback {
        public void onOffRoute();
    }

    public void addOnOffRouteCallback(OnOffRouteCallback onOffRouteCallback) {
        offRouteCallbackList.add(onOffRouteCallback);
    }

    public void removeOnOffRouteCallback(OnOffRouteCallback onOffRouteCallback) {
        offRouteCallbackList.remove(onOffRouteCallback);
    }

    public static class NavigatorLineStringSegment {
        @Getter
        private LatLng startPoint;
        @Getter
        private LatLng endPoint;
        @Getter
        @Setter
        private NavigationPoint endPointEvent = null;
        @Getter
        private double distance;
        @Getter
        private LatLng direction;
        @Getter
        private double directionBearing;
        @Getter
        private double distanceMeter;

        NavigatorLineStringSegment(@NonNull LatLng startPoint, @NonNull LatLng endPoint) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            LatLng dir = LatLngTool.sub(endPoint, startPoint);
            this.distance = LatLngTool.mag(dir);
            this.direction = LatLngTool.normalize(dir);
            this.directionBearing = LatLngTool.deg(dir);
            this.distanceMeter = LatLngTool.mag(dir, true);
        }

        public DistanceAndPoint getDistanceAndPoint(@NonNull LatLng point) {
            double x1 = startPoint.longitude;
            double y1 = startPoint.latitude;
            double x2 = endPoint.longitude;
            double y2 = endPoint.latitude;

            double x = point.longitude;
            double y = point.latitude;
            double A = x - x1;
            double B = y - y1;
            double C = x2 - x1;
            double D = y2 - y1;

            double dot = A * C + B * D;
            double len_sq = C * C + D * D;
            double param = -1;
            if (len_sq != 0) //in case of 0 length line
                param = dot / len_sq;

            double xx, yy;

            if (param < 0) {
                xx = x1;
                yy = y1;
            } else if (param > 1) {
                xx = x2;
                yy = y2;
            } else {
                xx = x1 + param * C;
                yy = y1 + param * D;
            }

            double dx = x - xx;
            double dy = y - yy;
            double distance = Math.sqrt(dx * dx + dy * dy);

            return new DistanceAndPoint(distance, new LatLng(yy, xx));
        }

        @AllArgsConstructor
        public static class DistanceAndPoint {
            public double distance;
            public LatLng point;
        }
    }
}