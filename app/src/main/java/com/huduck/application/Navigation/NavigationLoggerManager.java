package com.huduck.application.Navigation;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.huduck.application.common.CommonMethod;
import com.naver.maps.map.LocationSource;

import java.time.LocalTime;
import java.util.TimerTask;
import java.util.logging.Logger;

import lombok.Setter;

public class NavigationLoggerManager implements LocationSource {
    private NavigationLogger logger;

    @NonNull
    private final Context context;
    @Nullable
    private LocationSource.OnLocationChangedListener listener;
    private OnRouteChangedCallback onRouteChangedCallback;

    private Handler handler;


    public NavigationLoggerManager(@NonNull Context context, @NonNull NavigationLogger logger, @NonNull OnRouteChangedCallback onRouteChangedCallback) {
        this.context = context;
        this.logger = logger;
        this.onRouteChangedCallback = onRouteChangedCallback;
        handler = new Handler();
    }

    public void setLogger(NavigationLogger logger) {
        this.logger = logger;
        routeIdx = 0;
        locIdx = 0;
        start = false;

        routeRunnable.setStop(true);
        locRunnable.setStop(true);

        routeRunnable = new RouteRunnable();
        locRunnable = new LocRunnable();
    }

    private int routeIdx    = 0;
    private int locIdx      = 0;
    private boolean start = false;

    private class RouteRunnable extends TimerTask {
        @Setter
        private boolean stop = false;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if(stop) return;
            if(!start) return;
            if(routeIdx == logger.getRouteLogList().size()) return;

            NavigationLogger.RouteLog curRouteLog = logger.getRouteLogList().get(routeIdx);
            long curTime = CommonMethod.LocalTimeToMiliSecond(curRouteLog.getTime());

            onRouteChangedCallback.onRouteChanged(curRouteLog.getRoute());

            routeIdx++;
            if(routeIdx < logger.getRouteLogList().size()) {
                NavigationLogger.RouteLog nextRouteLog = logger.getRouteLogList().get(routeIdx);
                long nextTime = CommonMethod.LocalTimeToMiliSecond(nextRouteLog.getTime());

                handler.postDelayed(this, nextTime - curTime);
            }
        }
    }

    RouteRunnable routeRunnable = new RouteRunnable();

    private class LocRunnable extends TimerTask {
        @Setter
        private boolean stop = false;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if(stop) return;
            if(!start) return;
            if(locIdx == logger.getLocationLogList().size()) return;

            NavigationLogger.LocationLog curLocLog = logger.getLocationLogList().get(locIdx);
            long curTime = CommonMethod.LocalTimeToMiliSecond(curLocLog.getTime());

            listener.onLocationChanged(curLocLog.getLocation());

            locIdx++;
            if(locIdx < logger.getLocationLogList().size()) {
                NavigationLogger.LocationLog nextLocLog = logger.getLocationLogList().get(locIdx);
                long nextTime = CommonMethod.LocalTimeToMiliSecond(nextLocLog.getTime());

                handler.postDelayed(this, nextTime - curTime);
            }
        }
    }

    LocRunnable locRunnable = new LocRunnable();

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start() {
        if(start) return;

        if(logger.getRouteLogList().size()      <= 0) return;
        if(logger.getLocationLogList().size()   <= 0) return;

        for (int i = 0; i < 2; i++) {
            NavigationLogger.LocationLog curLocLog = logger.getLocationLogList().get(0);
            listener.onLocationChanged(curLocLog.getLocation());
        }

        long routeFirstTime  = CommonMethod.LocalTimeToMiliSecond(logger.getRouteLogList().get(0).getTime());
        long locFirstTime    = CommonMethod.LocalTimeToMiliSecond(logger.getLocationLogList().get(0).getTime());

        long firstTime = Math.min(routeFirstTime, locFirstTime);

        start = true;

        handler.postDelayed(routeRunnable,  routeFirstTime - firstTime);
        handler.postDelayed(locRunnable,    locFirstTime - firstTime);
    }

    public void stop() {
        start = false;
    }

    @Override
    public void activate(@NonNull LocationSource.OnLocationChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void deactivate() {
    }

    public interface OnRouteChangedCallback {
        public void onRouteChanged(NavigationRoutes route);
    }
}