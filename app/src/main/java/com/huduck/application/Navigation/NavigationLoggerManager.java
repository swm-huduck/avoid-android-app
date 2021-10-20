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
    }

    private int routeIdx    = 0;
    private int locIdx      = 0;
    private boolean start = false;

    Runnable routeRunnable = new TimerTask() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if(!start) return;

            NavigationLogger.RouteLog curRouteLog = logger.getRouteLogList().get(routeIdx);
            int curTime = CommonMethod.LocalTimeToMiliSecond(curRouteLog.getTime());

            onRouteChangedCallback.onRouteChanged(curRouteLog.getRoute());

            routeIdx++;
            if(routeIdx < logger.getRouteLogList().size()) {
                NavigationLogger.RouteLog nextRouteLog = logger.getRouteLogList().get(routeIdx);
                int nextTime = CommonMethod.LocalTimeToMiliSecond(nextRouteLog.getTime());

                handler.postDelayed(this, nextTime - curTime);
            }
        }
    };

    Runnable locRunnable = new TimerTask() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if(!start) return;

            NavigationLogger.LocationLog curLocLog = logger.getLocationLogList().get(locIdx);
            int curTime = CommonMethod.LocalTimeToMiliSecond(curLocLog.getTime());

            listener.onLocationChanged(curLocLog.getLocation());

            locIdx++;
            if(locIdx < logger.getLocationLogList().size()) {
                NavigationLogger.LocationLog nextLocLog = logger.getLocationLogList().get(locIdx);
                int nextTime = CommonMethod.LocalTimeToMiliSecond(nextLocLog.getTime());

                handler.postDelayed(this, nextTime - curTime);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start() {
        if(logger.getRouteLogList().size()      <= 0) return;
        if(logger.getLocationLogList().size()   <= 0) return;

        int routeFirstTime  = CommonMethod.LocalTimeToMiliSecond(logger.getRouteLogList().get(0).getTime());
        int locFirstTime    = CommonMethod.LocalTimeToMiliSecond(logger.getLocationLogList().get(0).getTime());

        int firstTime = Math.min(routeFirstTime, locFirstTime);

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