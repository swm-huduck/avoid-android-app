package com.huduck.application.Navigation;

import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.huduck.application.R;
import com.huduck.application.common.CommonMethod;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.MathUtils;
import com.naver.maps.geometry.Tm128;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;

public class NavigationRenderer implements Navigator.OnRouteChangedCallback, Navigator.OnEnhancedLocationChangedCallback, Navigator.OnProgressChangedCallback, NaverMap.OnLocationChangeListener {
    private Handler handler;
    private NaverMap naverMap;
    private Context context;

    private Timer timer = null;

    private NavigationGraphicResources graphicResources = new NavigationGraphicResources();

    public NavigationRenderer(Context context, Handler handler, NaverMap naverMap) {
        this.context = context;
        this.handler = handler;
        this.naverMap = naverMap;

        initNavigationGraphicResources();

        this.timer = new Timer();
        timer.schedule(rendererTimerTask, 0, 33);
    }

    private void initNavigationGraphicResources() {
        handler.post(() -> {
            // Init puck
            Marker puck = graphicResources.getPuckMarker();
            puck.setIcon(OverlayImage.fromResource(R.drawable.navigation_puck));
            puck.setFlat(true);
            int puckSize = CommonMethod.dpToPx(context.getResources(), 70);
            puck.setWidth(puckSize);
            puck.setHeight(puckSize);
            puck.setAnchor(new PointF(0.5f, 0.5f));
            puck.setMap(naverMap);
        });
    }

    private double speedMs = 0;

    private RendererTimerTask rendererTimerTask = new RendererTimerTask();

    public void setSpeed(double speedMs) {
        this.speedMs = speedMs;
        rendererTimerTask.updateMoveSpeed(speedMs);
    }

    private Location lastLoc = null;
    @Override
    public void onLocationChange(@NonNull Location location) {
        if(lastLoc == null) {
            lastLoc = location;
            return;
        }

        double time = location.getTime() - lastLoc.getTime();
        time *= 0.001;
        LatLng a = new LatLng(location.getLatitude(), location.getLongitude());
        LatLng b = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
        double ms = a.distanceTo(b) / time;
        if(location.getSpeed() == 0)
            setSpeed(0);
        else
            setSpeed(ms);
        Log.d("BBBB", "ms: " + ms + ", currentSpeed:" + location.getSpeed() + ", lastSpeed: " + lastLoc.getSpeed());

        lastLoc = location;
    }

    private class RendererTimerTask extends TimerTask implements Navigator.OnEnhancedLocationChangedCallback{
        private double targetPuckBearing;

        private LatLng moveDir;
        private LatLng currentPuckPosition = null;
        private double currentPuckBearing = 0;
        private int currentSegIdx = 0;
        private Navigator.NavigatorLineStringSegment currentSeg = null;
        private double currentSegPassedDist = 0;

        @Getter private List<Navigator.NavigatorLineStringSegment> lineStringSegmentList = null;

        private int bearingDir = 0;
        private int bearingDirOrigin = 0;
        private boolean convertedBearing = false;
        private double bearingSpeed = 0;

        private double totalDistanceM = 0;
        private double totalPassedDistanceM = 0;

        private double distanceLatLngPerM = 0;
        private double moveSpeed = 0;

        //TM
        Tm128 startTm;
        Tm128 endTm;
        Tm128 currentTm;
        Tm128 dirTm;
        double dirTmLen;
        Tm128 norDirTm;
        double tmPerM;

        LatLng lastPoint;
        double lastPointTime;

        private void changeLineStringSegmentListIndex(int index) {
            if(index == lineStringSegmentList.size())
                ended = true;

            if(index < 0 || lineStringSegmentList.size()-1 < index) return;
            this.currentSegIdx = index;
            currentSeg = lineStringSegmentList.get(index);

            // Distance LatLngPerM
            double distanceM = currentSeg.getDistanceMeter();
            double distanceLatLng = currentSeg.getDistance();
            distanceLatLngPerM = distanceLatLng / distanceM;

            // Position
            currentPuckPosition = currentSeg.getStartPoint();

            // Bearing
            this.targetPuckBearing = currentSeg.getDirectionBearing();
            bearingDirOrigin = 1;
            if (targetPuckBearing - currentPuckBearing < 0) bearingDirOrigin = -1;

            if (Math.abs(targetPuckBearing - currentPuckBearing) >= 180) {
                double angle = 360 - Math.abs(targetPuckBearing - currentPuckBearing);
                bearingSpeed = CommonMethod.lerp(20, 150, angle / 180);
                bearingDir = -bearingDirOrigin;
            } else {
                double angle = Math.abs(targetPuckBearing - currentPuckBearing);
                bearingSpeed = CommonMethod.lerp(20, 150, angle / 180);
                bearingDir = bearingDirOrigin;
            }
            convertedBearing = false;

            // Move Direction
//            this.moveDir = currentSeg.getDirection();
            startTm = Tm128.valueOf(currentSeg.getStartPoint());
            endTm = Tm128.valueOf(currentSeg.getEndPoint());
            currentTm = Tm128.valueOf(currentSeg.getStartPoint());
            dirTm = new Tm128(endTm.x - startTm.x, endTm.y - startTm.y);
            dirTmLen = Math.sqrt(dirTm.x * dirTm.x + dirTm.y * dirTm.y);
            norDirTm = new Tm128(dirTm.x / dirTmLen, dirTm.y / dirTmLen);
            tmPerM = dirTmLen / distanceM;

            // Move Speed
            updateMoveSpeed(speedMs);
        }

        private void updateMoveSpeed(double speedMs) {
            if(speedMs <= 0)
                moveSpeed = 0;
            else
//                moveSpeed = distanceLatLngPerM * speedMs;
                moveSpeed = tmPerM * speedMs;
            calibratedMoveSpeed = moveSpeed;
        }

        public void setLineStringSegmentList(List<Navigator.NavigatorLineStringSegment> lineStringSegmentList) {
            this.lineStringSegmentList = lineStringSegmentList;
            Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(0);

            currentPuckPosition = lineStringSegmentList.get(0).getStartPoint();
            currentPuckBearing = seg.getDirectionBearing();
            currentSegIdx = 0;
            currentSegPassedDist = 0;
            totalDistanceM = 0;

            lastPoint = seg.getStartPoint();
            lastPointTime = 0;

            for (Navigator.NavigatorLineStringSegment lineStringSegment : lineStringSegmentList)
                totalDistanceM += lineStringSegment.getDistanceMeter();

            changeLineStringSegmentListIndex(0);
        }

        private boolean started = false;
        private boolean ended = false;
        private void start() {
            if(lineStringSegmentList == null) return;
            started = true;
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if(!started) {
                start();
                if(!started) return;
            }

            if(ended) return;

            try {
                updateDeltaTime();

                lastPointTime += deltaTime;
                if(lastPointTime >= 1) {
                    double moveM = lastPoint.distanceTo(currentPuckPosition);
                    Log.d("AAAA", "moved M: " + moveM+", m/s: " + (moveM / lastPointTime) + "speed: " + speedMs);
                    lastPoint = currentPuckPosition;
                    lastPointTime = 0;
                }


                calcMoveSpeed();
                calcPuckPosition();

                if(ended) {
                    cancel();
                    return;
                }

                calcBearing();
                calcProgress();
                render();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private int realSegIdx = 0;
        private LatLng realPosition;
        private double betweenRealAndPuckGapM;
        @Override
        public void onEnhancedLocationChange(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng currentPosition, double currentBearing) {
            if(ended) return;

            realSegIdx = currentLineStringSegIdx;
            realPosition = currentPosition;

            // 실제보다 puck이 덜 갔을 경우
            double gapM = 0;
            if(realSegIdx > currentSegIdx) {
                for(int i = currentSegIdx; i <= realSegIdx; i++) {
                    Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(i);
                    if(i == currentSegIdx)
                        gapM += currentPuckPosition.distanceTo(seg.getEndPoint());
                    else if(i == realSegIdx)
                        gapM += realPosition.distanceTo(seg.getStartPoint());
                    else
                        gapM += seg.getDistanceMeter();
                }
                gapM *= -1;
            }
            // 실제보다 puck이 더 갔을 경우
            else if(realSegIdx < currentSegIdx) {
                for(int i = realSegIdx; i <= currentSegIdx; i++) {
                    Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(i);
                    if(i == realSegIdx)
                        gapM += realPosition.distanceTo(seg.getEndPoint());
                    else if(i == currentSegIdx)
                        gapM += currentPuckPosition.distanceTo(seg.getStartPoint());
                    else
                        gapM += seg.getDistanceMeter();
                }
            }
            else {
                gapM = currentSeg.getEndPoint().distanceTo(realPosition) - currentSeg.getEndPoint().distanceTo(currentPuckPosition);
            }
            betweenRealAndPuckGapM = gapM;
            Log.d("Gap", gapM+"");
        }

        private int lastGapDir = 0;
        private double calibratedMoveSpeed = 0;
        private double calAddSpeed = 0;
        private void calcMoveSpeed() {
            if(betweenRealAndPuckGapM  <= 5) return;
            else if(lastGapDir == 0)
                lastGapDir = (int) betweenRealAndPuckGapM;
            else if(lastGapDir < 0 && betweenRealAndPuckGapM > 0) {
                calAddSpeed = 0;
                lastGapDir = 1;
            }
            else if(lastGapDir > 0 && betweenRealAndPuckGapM < 0) {
                calAddSpeed = 0;
                lastGapDir = -1;
            }
            else if(speedMs == 0)
                calAddSpeed = 0;
            else {
                lastGapDir = (int) betweenRealAndPuckGapM;
            }

            double addValue = betweenRealAndPuckGapM < 0 ? 1 : -1;
            if(betweenRealAndPuckGapM > 20)
                addValue = speedMs * -0.6;
            addValue = MathUtils.clamp(addValue, -speedMs, speedMs);

            calAddSpeed += addValue * deltaTime;
            calAddSpeed = MathUtils.clamp(calAddSpeed, -moveSpeed, Double.MAX_VALUE);
            calibratedMoveSpeed = moveSpeed + calAddSpeed;
        }

        private void calcPuckPosition() {
            if(currentSegIdx >= lineStringSegmentList.size()) return;

            double leftDistance = calibratedMoveSpeed * deltaTime;
            while (leftDistance > 0) {
                double leftSegDist = Math.sqrt(
                        Math.pow(endTm.x - currentTm.x, 2)
                        + Math.pow(endTm.y - currentTm.y, 2)
                );

                if (leftSegDist > leftDistance) {
                    double currentTmX = currentTm.x + norDirTm.x * leftDistance;
                    double currentTmY = currentTm.y + norDirTm.y * leftDistance;
                    currentTm = new Tm128(currentTmX, currentTmY);

                    leftDistance = 0;
                } else {
                    currentSegIdx++;
                    changeLineStringSegmentListIndex(currentSegIdx);
                    leftDistance -= leftSegDist;
                }

            }
            currentPuckPosition = currentTm.toLatLng();
//            Log.d("currentPos", currentPuckPosition.toString());
        }

        private void calcBearing() {
            if (currentPuckBearing != targetPuckBearing) {
                currentPuckBearing += bearingDir * bearingSpeed * deltaTime;

                if (bearingDir == 1 && currentPuckBearing >= 360) {
                    currentPuckBearing = 0;
                    convertedBearing = true;
                } else if (bearingDir == -1 && currentPuckBearing < 0) {
                    currentPuckBearing = 360 + currentPuckBearing;
                    convertedBearing = true;
                }

                if (bearingDir == 1 && currentPuckBearing > targetPuckBearing) {
                    if (bearingDirOrigin == 1)
                        currentPuckBearing = targetPuckBearing;
                    else if (bearingDirOrigin == -1 && convertedBearing)
                        currentPuckBearing = targetPuckBearing;
                } else if (bearingDir == -1 && currentPuckBearing < targetPuckBearing) {
                    if (bearingDirOrigin == 1 && convertedBearing)
                        currentPuckBearing = targetPuckBearing;
                    else if (bearingDirOrigin == -1)
                        currentPuckBearing = targetPuckBearing;
                }
            }
        }

        private void calcProgress() {
            totalPassedDistanceM = 0;
            for(int i = 0; i <= currentSegIdx; i++) {
                Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(i);
                if(i == currentSegIdx)
                    totalPassedDistanceM += currentPuckPosition.distanceTo(seg.getStartPoint());
                else
                    totalPassedDistanceM += seg.getDistanceMeter();
            }
        }

        private void render() {
            CameraPosition cameraPosition = new CameraPosition(
                    currentPuckPosition,
                    17,
                    20,
                    currentPuckBearing
            );

            CameraUpdate cameraUpdate = CameraUpdate
                    .toCameraPosition(cameraPosition)
                    .pivot(new PointF(0.5f, 0.8f));

            handler.post(() -> {
                graphicResources.getPuckMarker().setPosition(currentPuckPosition);
                double bearing = lineStringSegmentList.get(currentSegIdx).getDirectionBearing();
                graphicResources.getPuckMarker().setAngle((float) bearing);
                graphicResources.routePathOverlay.setProgress(/*GeometryUtils.getProgress(routeLatLngList, currentPuckPosition)*/totalPassedDistanceM / totalDistanceM);
                naverMap.moveCamera(cameraUpdate);
            });
        }

        private long lastTime;
        private double deltaTime;
        private void updateDeltaTime() {
            long currentTime = System.currentTimeMillis();
            deltaTime = (currentTime - lastTime) * 0.001;
            lastTime = currentTime;
        }
    };

    @Override
    public void onRouteChanged(NavigationRoutes route, List<Navigator.NavigatorLineStringSegment> navigatorLineStringSegmentList) {
        rendererTimerTask.setLineStringSegmentList(navigatorLineStringSegmentList);
        drawRoute(route);
    }

    private List<LatLng> routeLatLngList = new ArrayList<>();
    private void drawRoute(NavigationRoutes route) {
        if (graphicResources.routePathOverlay.getMap() != null) {
            graphicResources.routePathOverlay.setMap(null);
            graphicResources.routePathOverlay = new PathOverlay();
        }
        PathOverlay path = graphicResources.routePathOverlay;

        List<Integer> seq = route.getNavigationSequence();
        HashMap<Integer, NavigationLineString> lineStringHashMap = route.getNavigationLineStringHashMap();
        HashMap<Integer, NavigationPoint> pointHashMap = route.getNavigationPointHashMap();

        routeLatLngList = new ArrayList<>();

        for (Integer key : seq) {
            if (lineStringHashMap.containsKey(key)) {
                for (ArrayList<Double> coordinate : lineStringHashMap.get(key).getGeometry().getCoordinates()) {
                    routeLatLngList.add(new LatLng(coordinate.get(0), coordinate.get(1)));
                }
            }
        }

        handler.post(() -> {
            path.setCoords(routeLatLngList);
            path.setColor(context.getColor(R.color.indigo500));
            path.setPatternImage(OverlayImage.fromResource(R.drawable.ic_baseline_arrow_drop_up_24));
            path.setPatternInterval(80);
            path.setPassedColor(context.getColor(R.color.gray500));
            path.setWidth(30);
            path.setCoords(routeLatLngList);
            path.setMap(naverMap);
        });
    }

    private Marker debug = new Marker(new LatLng(0,0));
    @Override
    public void onEnhancedLocationChange(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng currentPosition, double currentBearing) {
//        rendererTimerTask.setTargetPuckTransform(currentLineStringSegIdx, currentLineStringSegPassedDistance, currentPosition, currentBearing);
        rendererTimerTask.onEnhancedLocationChange(currentLineStringSegIdx, currentLineStringSegPassedDistance, currentPosition, currentBearing);

        new Handler(Looper.getMainLooper()).post(() -> {
            if (debug.getMap() == null)
                debug.setMap(naverMap);
            debug.setPosition(currentPosition);
        });

    }

    @Override
    public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter) {

    }

    private class NavigationGraphicResources {
        @Getter
        private Marker puckMarker = new Marker(new LatLng(0, 0));
        @Getter
        private PathOverlay routePathOverlay = new PathOverlay();
    }

    public void destroy() {
        timer.cancel();
        rendererTimerTask.cancel();
    }
}
