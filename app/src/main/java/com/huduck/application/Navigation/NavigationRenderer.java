package com.huduck.application.Navigation;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;

import com.huduck.application.R;
import com.huduck.application.common.CommonMethod;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.GeometryUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;

public class NavigationRenderer implements Navigator.OnRouteChangedCallback, Navigator.OnEnhancedLocationChangedCallback, Navigator.OnProgressChangedCallback {
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

    private double speedKh = 0;
    public void setSpeed(double speedKh) {
        this.speedKh = speedKh;
    }

    private RendererTimerTask rendererTimerTask = new RendererTimerTask();
    private class RendererTimerTask extends TimerTask {
        private LatLng targetPuckPosition = null;
        private double targetPuckBearing = 0;
        private int targetSegIdx = 0;
        private double targetSegPassedDist = 0;

        private LatLng currentPuckPosition = null;
        private double currentPuckBearing = 0;
        private int currentSegIdx = 0;
        private double currentSegPassedDist = 0;

        @Getter private List<Navigator.NavigatorLineStringSegment> lineStringSegmentList = null;

        @Setter private double moveSpeed = 0.0001;
        private LatLng moveDir;

        private int bearingDir = 0;
        private int bearingDirOrigin = 0;
        private boolean convertedBearing = false;
        private double bearingSpeed = 0;

        private double totalDistanceM = 0;
        private double totalPassedDistanceM = 0;

        private void setMoveDir(LatLng moveDir, double targetPuckBearing) {
            this.moveDir = moveDir;
            this.targetPuckBearing = targetPuckBearing;

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
        }

        public void setTargetPuckTransform(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng targetPuckPosition, double targetPuckBearing) {
            this.targetSegIdx = currentLineStringSegIdx;
            this.targetSegPassedDist = currentLineStringSegPassedDistance;
            this.targetPuckPosition = targetPuckPosition;
        }

        public void setLineStringSegmentList(List<Navigator.NavigatorLineStringSegment> lineStringSegmentList) {
            this.lineStringSegmentList = lineStringSegmentList;
            Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(0);
            currentPuckPosition = lineStringSegmentList.get(0).getStartPoint();
            currentPuckBearing = seg.getDirectionBearing();
            currentSegIdx = 0;
            currentSegPassedDist = 0;
            setMoveDir(seg.getDirection(), seg.getDirectionBearing());

            totalDistanceM = 0;
            for (Navigator.NavigatorLineStringSegment lineStringSegment : lineStringSegmentList)
                totalDistanceM += lineStringSegment.getDistanceMeter();
        }

        private boolean started = false;
        private void start() {
            if(targetPuckPosition == null || lineStringSegmentList == null) return;
            started = true;
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if(!started) {
                start();
                if(!started) return;
            }

            updateDeltaTime();
            calcMoveSpeed();
            calcPuckPosition();
            calcBearing();
            calcProgress();
            render();
        }

        private void calcMoveSpeed() {
            // 남은 거리 계산
            double leftDist = 0;
            for(int i = currentSegIdx; i <= targetSegIdx; i++) {
                Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(i);
                if(currentSegIdx == targetSegIdx) {
                    leftDist = targetSegPassedDist - currentSegPassedDist;
                }
                else if(i == currentSegIdx)
                    leftDist += seg.getDistance() - currentSegPassedDist;
                else if(i == targetSegIdx)
                    leftDist += targetSegPassedDist;
                else
                    leftDist += seg.getDistance();
            }

            // Log.d("LeftDist", BigDecimal.valueOf(leftDist).toString());
            moveSpeed = CommonMethod.lerp(0, 0.0003, leftDist/0.0004 - 0.001);
//            moveSpeed = 0.000001;
        }

        private void calcPuckPosition() {
//            Log.d("passedDist", targetSegPassedDist+"");

            if(currentSegIdx >= lineStringSegmentList.size()) return;

            Navigator.NavigatorLineStringSegment seg = lineStringSegmentList.get(currentSegIdx);
            double mag = LatLngTool.mag(seg.getEndPoint(), seg.getStartPoint());

            double leftDistance = moveSpeed * deltaTime;
            while (leftDistance > 0) {
                if(currentSegIdx == targetSegIdx && currentSegPassedDist < targetSegPassedDist) {
                    leftDistance = (currentSegPassedDist + leftDistance > targetSegPassedDist)
                            ? targetSegPassedDist - currentSegPassedDist : leftDistance;

                    currentPuckPosition = LatLngTool.add(currentPuckPosition, LatLngTool.mul(moveDir, leftDistance));
                    currentSegPassedDist += leftDistance;
                    leftDistance = 0;
                }
                else if(currentSegIdx < targetSegIdx) {
                    if(currentSegPassedDist + leftDistance >= mag) {
                        leftDistance -= mag - currentSegPassedDist;
                        currentSegIdx++;
                        currentSegPassedDist = 0;
                        seg = lineStringSegmentList.get(currentSegIdx);
                        setMoveDir(seg.getDirection(), seg.getDirectionBearing());
                        mag = LatLngTool.mag(seg.getEndPoint(), seg.getStartPoint());
                        currentPuckPosition = seg.getStartPoint();
                    }
                    else {
                        currentPuckPosition = LatLngTool.add(currentPuckPosition, LatLngTool.mul(moveDir, leftDistance));
                        currentSegPassedDist += leftDistance;
                        leftDistance = 0;
                    }
                }
            }

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
//                Log.d("moveDir", LatLngTool.mag(moveDir) + "  " + moveDir.toString());

                graphicResources.getPuckMarker().setPosition(currentPuckPosition);
                double bearing = lineStringSegmentList.get(currentSegIdx).getDirectionBearing();
                graphicResources.getPuckMarker().setAngle((float) bearing);
                graphicResources.routePathOverlay.setProgress(/*GeometryUtils.getProgress(routeLatLngList, currentPuckPosition)*/totalPassedDistanceM / totalDistanceM);
                naverMap.moveCamera(cameraUpdate);

//                naverMap.setCameraPosition(cameraPosition);

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

    @Override
    public void onEnhancedLocationChange(int currentLineStringSegIdx, double currentLineStringSegPassedDistance, LatLng currentPosition, double currentBearing) {
        rendererTimerTask.setTargetPuckTransform(currentLineStringSegIdx, currentLineStringSegPassedDistance, currentPosition, currentBearing);
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
