package com.huduck.application.Navigation;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.Tm128;
import com.naver.maps.map.NaverMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class Navigator implements NaverMap.OnLocationChangeListener {
    /* Route */
    @Getter
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
    private LatLng currentRowPosition;

    // Event
    private List<OnRouteChangedCallback> routeChangedCallbackList = new ArrayList<>();
    private List<OnEnhancedLocationChangedCallback> enhancedLocationChangedCallbackList = new ArrayList<>();
    private List<OnProgressChangedCallback> progressChangedCallbackList = new ArrayList<>();
    private List<OnOffRouteCallback> offRouteCallbackList = new ArrayList<>();


    // Speed
    double currentSpeedKmH = 0;
    double currentSpeedMS = 0;
    double lastTime = -1;
    double deltaTime = 0;


    public void startNavigator() {
        startedNavigator = true;

        if (currentLocation != null)
            onLocationChanged(currentLocation);
    }

    public void stopNavigator() {
        startedNavigator = false;
    }

    private NavigationRoutes modifyRouteByMoveDirection(NavigationRoutes navigationRoutes) {
        // 현재 및 직전 위치 가져오기
        int locationSize = LocationProvider.getLastRowLocationSize();

        if(locationSize < 2) return navigationRoutes;

        Location curLocation    = LocationProvider.getLastRowLocation(locationSize - 1);
        Location prevLocation   = LocationProvider.getLastRowLocation(locationSize - 2);

        if(curLocation == null || prevLocation == null || curLocation == prevLocation || curLocation.equals(prevLocation))
            return navigationRoutes;

        LatLng curPosition      = new LatLng(curLocation.getLatitude(),     curLocation.getLongitude());
        LatLng prevPosition     = new LatLng(prevLocation.getLatitude(),    prevLocation.getLongitude());

        // 10미터 범위 안에 있는 세그먼트 수집
        List<NavigatorLineStringSegment> segList = navigationRouteToLineStringSegment(navigationRoutes);
        List<NavigatorLineStringSegment> closeSegList = new ArrayList<>();
        for (NavigatorLineStringSegment seg : segList) {
            NavigatorLineStringSegment.DistanceAndPoint dap = seg.getDistanceAndPoint(curPosition);

            if(dap.distance < 10)
                closeSegList.add(seg);
        }

        if(closeSegList.size() == 0) return navigationRoutes;

        // 비슷한 방향의 진행 방향을 가진 세그먼트 찾기
        double moveDirDeg = LatLngTool.deg(prevPosition, curPosition);

        NavigatorLineStringSegment flagSeg = null;
        double minGap = Double.MAX_VALUE;

        for (NavigatorLineStringSegment seg : closeSegList) {
            double segDirDeg = LatLngTool.deg(seg.startPoint, seg.endPoint);

            double gap = Math.abs(moveDirDeg - segDirDeg);

            if (gap < minGap) {
                minGap = gap;
                flagSeg = seg;
            }
        }

        // 목표 세그먼트에 맞게 경로 수정
        List<Integer> routeSeq = navigationRoutes.getNavigationSequence();
        Map<Integer, NavigationPoint> pointMap = navigationRoutes.getNavigationPointHashMap();
        Map<Integer, NavigationLineString> lineStringMap = navigationRoutes.getNavigationLineStringHashMap();

        NavigationLineString flagSegParentLineString = flagSeg.getParentLineString();

        int flagIndex = flagSegParentLineString.getProperties().getIndex();

        for (Integer key : routeSeq) {
            // 새로운 시작 지점 전의 포인트와 라인 스트링은 삭제
            if (key < flagIndex) {
                if (pointMap.containsKey(key))
                    pointMap.remove(key);
                else if (lineStringMap.containsKey(key))
                    lineStringMap.remove(key);
            }

            // 시작 지점 라인 스트링이면 본 라인 스트링 수정
            if (key == flagIndex) {
                NavigationLineString lineString = lineStringMap.get(flagIndex);

                // 라인 스트링에서 몇 번째 Seg인지 검색
                int segIndex = 0;

                for (NavigatorLineStringSegment seg : segList) {
                    if(!seg.getParentLineString().equals(flagSegParentLineString)) continue;

                    if (seg.equals(flagSeg))
                        break;

                    segIndex++;
                }

                ArrayList<ArrayList<Double>> coordList = lineString.getGeometry().getCoordinates();

                // 기준 Seg 전의 Seg는 삭제
                for (int i = 0; i < segIndex; i++) {
                    coordList.remove(0);
                }

                // 기준 Seg 좌표
                ArrayList<Double> targetCoord = coordList.get(0);

                // 기준 Seg 좌표 수정
                NavigatorLineStringSegment.DistanceAndPoint dap = flagSeg.getDistanceAndPoint(curPosition);

                targetCoord.set(0, dap.point.latitude);
                targetCoord.set(1, dap.point.longitude);

                break;
            }
        }

        Log.d("Modified Route", "Modified Route close seg count: " + closeSegList.size());

        return navigationRoutes;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setRoute(NavigationRoutes navigationRoute) {
        this.navigationRoute = modifyRouteByMoveDirection(navigationRoute);
        lineStringSegmentList = navigationRouteToLineStringSegment(this.navigationRoute);

        // init lineStringSeg parm
        currentLineStringSegIdx = 0;
        currentLineStringSegPassedDistance = 0;
        currentPositionOnRoute = lineStringSegmentList.get(0).getStartPoint();

        // init TotalDistance
        routeTotalDistance = 0;
        lineStringSegmentList.forEach(seg -> routeTotalDistance += seg.getDistanceMeter());

        // Call event
        for (OnRouteChangedCallback onRouteChangedCallback : routeChangedCallbackList) {
            onRouteChangedCallback.onRouteChanged(this.navigationRoute, routeTotalDistance, lineStringSegmentList);
        }

        /*
        // startNavigator()에서 호출해서 주석 처리함 (2중 호출)
        if (currentLocation != null)
            onLocationChanged(currentLocation);*/
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
                    NavigatorLineStringSegment lineStringSegment = new NavigatorLineStringSegment(lastPoint, point, lineString);
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

    private double distanceMBetweenRouteAndRowLocation = 0;
    private LatLng findCurrentPositionOnRoute(LatLng rowPosition) {
        int minDistIdx = -1;
        double minDist = Double.MAX_VALUE;
        NavigatorLineStringSegment targetSeg = lineStringSegmentList.get(currentLineStringSegIdx);
        LatLng positionOnRoute = currentPositionOnRoute;

        double movedM = 0;

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

            if(i == currentLineStringSegIdx)
                movedM += LatLngTool.mag(currentPositionOnRoute, seg.getEndPoint(), true);
            else
                movedM += seg.distanceMeter;

            if(movedM >= maxMovableM)
                break;
        }

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

        distanceMBetweenRouteAndRowLocation = rowPosition.distanceTo(positionOnRoute);
        Log.d("distanceMBetweenRouteAndRowLocation", distanceMBetweenRouteAndRowLocation+"");

        return this.currentPositionOnRoute;
    }

    private NavigationPoint nextTurnEvent = null;
    private double nextTurnEventLeftDistanceMeter = 0;
    private NavigationPoint nextNextTurnEvent = null;
    private double nextNextTurnEventLeftDistanceMeter = 0;
    private double routeTotalLeftDistance = 0;

    private void updateNextTurnEvent() {
        double leftDistance = 0;    // M
        NavigationPoint nextTurnEvent = null;
        NavigationPoint nextNextTurnEvent = null;

        for (int i = currentLineStringSegIdx; i < lineStringSegmentList.size(); i++) {
            NavigatorLineStringSegment seg = lineStringSegmentList.get(i);

            if (i == currentLineStringSegIdx) {
                leftDistance += currentPositionOnRoute.distanceTo(seg.endPoint);
            } else {
                leftDistance += seg.getDistanceMeter();
            }

            if (seg.endPointEvent != null) {
                if(nextTurnEvent == null) {
                    nextTurnEvent = seg.endPointEvent;
                    nextTurnEventLeftDistanceMeter = leftDistance;
                }
                else if(nextNextTurnEvent == null) {
                    nextNextTurnEvent = seg.endPointEvent;
                    nextNextTurnEventLeftDistanceMeter = leftDistance;
                }
            }
        }

        this.nextTurnEvent = nextTurnEvent;
        this.nextNextTurnEvent = nextNextTurnEvent;

        routeTotalLeftDistance = leftDistance;
    }

    public double getRouteTotalLeftDistance() {
        return routeTotalLeftDistance;
    }

    private double maxMovableM = 0;
    public void onLocationChanged(@NonNull Location location) {
        if (location.getLongitude() < 0) return;
        if (lineStringSegmentList.size() == 0) return;

        // Delta Time
        if(lastTime < 0)
            lastTime = System.currentTimeMillis();
        double currentTime = System.currentTimeMillis();
        deltaTime = (currentTime - lastTime) * 0.001;
        lastTime = currentTime;

        // Speed
        currentSpeedMS = location.getSpeed();
        currentSpeedKmH = currentSpeedMS * 3.6;

        // Max movable M
        maxMovableM = (currentSpeedMS + 30/3.6) * deltaTime;

        // Location
        currentLocation = location;
        currentRowPosition = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        NavigatorLineStringSegment currentLineStringSeg = lineStringSegmentList.get(currentLineStringSegIdx);

        if (!startedNavigator || navigationRoute == null) return;

        findCurrentPositionOnRoute(currentRowPosition);

        if (currentPositionOnRoute == null) return;

        for (OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback : enhancedLocationChangedCallbackList) {
            onEnhancedLocationChangedCallback.onEnhancedLocationChange
                    (currentLineStringSegIdx, currentLineStringSegPassedDistance, currentPositionOnRoute, lineStringSegmentList.get(currentLineStringSegIdx).directionBearing);
        }

        updateNextTurnEvent();
        double progress = (routeTotalDistance - routeTotalLeftDistance) / routeTotalDistance;

        NavigationTurnEventCalc.NavigationTurnEventData nextTurnEventData = NavigationTurnEventCalc.calc(currentLineStringSeg.getStartPoint(), currentLineStringSeg.getEndPoint(), currentPositionOnRoute, nextTurnEvent);

        for (OnProgressChangedCallback onProgressChangedCallback : progressChangedCallbackList) {
            onProgressChangedCallback.onProgressChanged(
                    progress,
                    nextTurnEvent, nextTurnEventLeftDistanceMeter,
                    nextTurnEventData,
                    nextNextTurnEvent, nextNextTurnEvent != null ? nextNextTurnEventLeftDistanceMeter : -1);
        }

        boolean checkOffRoute = checkOffRoute();
        if(checkOffRoute)
            for (OnOffRouteCallback onOffRouteCallback : offRouteCallbackList) {
                onOffRouteCallback.onOffRoute();
            }
    }

    private int offRouteCount = 0;
    private boolean checkOffRoute() {
        if(distanceMBetweenRouteAndRowLocation > 15) return true;

        else if (distanceMBetweenRouteAndRowLocation > 10)
            if (++offRouteCount > 2) return true;

            else
                offRouteCount = 0;

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
        void onRouteChanged(NavigationRoutes route, double routeTotalDistance, List<NavigatorLineStringSegment> navigatorLineStringSegmentList);
    }

    public interface OnEnhancedLocationChangedCallback {
        void onEnhancedLocationChange(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng currentPosition, double currentBearing);
    }

    public void addOnEnhancedLocationChangedCallback(OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback) {
        enhancedLocationChangedCallbackList.add(onEnhancedLocationChangedCallback);
    }

    public void removeOnEnhancedLocationChangedCallback(OnEnhancedLocationChangedCallback onEnhancedLocationChangedCallback) {
        enhancedLocationChangedCallbackList.remove(onEnhancedLocationChangedCallback);
    }

    public interface OnProgressChangedCallback {
        void onProgressChanged(double totalProgress,
                               NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter,
                               NavigationTurnEventCalc.NavigationTurnEventData nextTurnEventData,
                               NavigationPoint nextNextTurnEvent, double nextNextTurnEventLeftDistanceMeter);
    }

    public void addOnProgressChangedCallback(OnProgressChangedCallback onProgressChangedCallback) {
        progressChangedCallbackList.add(onProgressChangedCallback);
    }

    public void removeOnProgressChangedCallback(OnProgressChangedCallback onProgressChangedCallback) {
        progressChangedCallbackList.remove(onProgressChangedCallback);
    }

    public interface OnOffRouteCallback {
        void onOffRoute();
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
        @Getter
        private NavigationLineString parentLineString;

        NavigatorLineStringSegment(@NonNull LatLng startPoint, @NonNull LatLng endPoint, NavigationLineString parentLineString) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            LatLng dir = LatLngTool.sub(endPoint, startPoint);
            this.distance = LatLngTool.mag(dir);
            this.direction = LatLngTool.normalize(dir);
            this.directionBearing = LatLngTool.deg(this.startPoint, this.endPoint);
            this.distanceMeter = startPoint.distanceTo(endPoint);
            this.parentLineString = parentLineString;
        }

        public DistanceAndPoint getDistanceAndPoint(@NonNull LatLng point) {
            Tm128 startPointTm    = Tm128.valueOf(this.startPoint);
            Tm128 endPointTm      = Tm128.valueOf(this.endPoint);

            double x1 = startPointTm.x;
            double y1 = startPointTm.y;
            double x2 = endPointTm.x;
            double y2 = endPointTm.y;

            Tm128 pointTm = Tm128.valueOf(point);

            double x = pointTm.x;
            double y = pointTm.y;
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

            Tm128 targetPointTm = new Tm128(xx, yy);
            LatLng targetPoint = targetPointTm.toLatLng();

            double distance = point.distanceTo(targetPoint);

            return new DistanceAndPoint(distance, targetPoint);
        }

        @AllArgsConstructor
        public static class DistanceAndPoint {
            public double distance;
            public LatLng point;
        }
    }
}