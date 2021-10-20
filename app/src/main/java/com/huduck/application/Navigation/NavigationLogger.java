package com.huduck.application.Navigation;

import android.app.Activity;
import android.location.Location;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.common.internal.service.Common;
import com.huduck.application.common.CommonMethod;
import com.naver.maps.map.NaverMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class NavigationLogger implements NaverMap.OnLocationChangeListener, Navigator.OnRouteChangedCallback {
    @Getter private List<RouteLog> routeLogList = new ArrayList<>();
    @Getter private List<LocationLog> locationLogList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRouteChanged(NavigationRoutes route, List<Navigator.NavigatorLineStringSegment> navigatorLineStringSegmentList) {
        RouteLog log = new RouteLog(LocalTime.now(), route);
        routeLogList.add(log);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onLocationChange(@NonNull Location location) {
        LocationLog log = new LocationLog(LocalTime.now(), location);
        locationLogList.add(log);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void writeFile(Activity activity) throws IOException, JSONException {
        File dir = Environment.getExternalStorageDirectory();
        String abPath = dir.getAbsolutePath();
        String packageName = activity.getPackageName();
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + ".dat";
        String path = abPath + "/android/data/" + packageName + "/" + fileName;
/*
        // 외부저장소 경로가 있는지 확인, 없으면 생성
        File file = new File(path);
        if(!file.exists()) {
            file.mkdir();
            file.createNewFile();
        }

        // 외부 저장소 저장
        FileOutputStream fos = new FileOutputStream(path);
        DataOutputStream dos = new DataOutputStream(fos);

        //데이터를 쓴다.
        dos.writeUTF(toJson().toString());
        dos.flush();
        dos.close();*/

        File file = new File(Environment.getExternalStorageDirectory(), fileName);

        try {
            if (!file.exists())
                file.createNewFile();

            FileWriter writer = new FileWriter(file, false);
            String str = toJson().toString();
            writer.write(str);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NavigationLogger readFile(File file) throws IOException, JSONException {
        // 외부 저장소 읽기

        FileInputStream stream = new FileInputStream(file);
        String jString = null;
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jString = Charset.defaultCharset().decode(bb).toString();
            JSONObject json = new JSONObject(jString);
            return fromJson(json);
        }
        finally {
            stream.close();
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();

        // Route
        JSONArray routeLogListJson = new JSONArray();
        for (RouteLog routeLog : routeLogList) {
            routeLogListJson.put(routeLog.toJson());
        }
        result.put("routeLogList", routeLogListJson);

        // Location
        JSONArray locationLogListJson = new JSONArray();
        for (LocationLog locationLog : locationLogList) {
            locationLogListJson.put(locationLog.toJson());
        }
        result.put("locationLogList", locationLogListJson);

        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NavigationLogger fromJson(JSONObject json) throws JSONException {
        NavigationLogger result = new NavigationLogger();

        JSONArray routeLogListJson = json.getJSONArray("routeLogList");
        for(int i = 0; i < routeLogListJson.length(); i++) {
            JSONObject routeLogJson = routeLogListJson.getJSONObject(i);
            RouteLog log = RouteLog.fromJson(routeLogJson);
            result.routeLogList.add(log);
        }

        JSONArray locationLogListJson = json.getJSONArray("locationLogList");
        for(int i = 0; i < locationLogListJson.length(); i++) {
            JSONObject locationLogJson = locationLogListJson.getJSONObject(i);
            LocationLog log = LocationLog.fromJson(locationLogJson);
            result.locationLogList.add(log);
        }

        return result;
    }

    public static class RouteLog {
        @Setter @Getter
        private LocalTime time;
        @Setter @Getter
        private NavigationRoutes route;

        public RouteLog(LocalTime time, NavigationRoutes route) {
            this.time = time;
            this.route = route;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("time", time.toString());
            result.put("route", route.toJson());
            return result;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public static RouteLog fromJson(JSONObject json) throws JSONException {
            LocalTime time = LocalTime.parse(json.getString("time"));
            NavigationRoutes route = NavigationRoutesParser.parserTruckRoutes(json.getString("route"));
            RouteLog result = new RouteLog(time, route);
            return result;
        }
    }

    public static class LocationLog {
        @Setter @Getter
        private LocalTime time;
        @Setter @Getter
        private Location location;

        public LocationLog(LocalTime time, Location location) {
            this.time = time;
            this.location = location;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("time", time.toString());
            result.put("location", locationToJson(location));
            return result;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public static LocationLog fromJson(JSONObject json) throws JSONException {
            LocalTime time = LocalTime.parse(json.getString("time"));
            Location location = jsonToLocation(json.getJSONObject("location"), time);
            LocationLog result = new LocationLog(time, location);
            return result;
        }

        private static JSONObject locationToJson(Location location) throws JSONException {
            JSONObject result = new JSONObject();
            result.put("longitude",     location.getLongitude());
            result.put("latitude",      location.getLatitude());
            result.put("speed",         location.getSpeed());
            result.put("bearing",       location.getBearing());
            return result;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private static Location jsonToLocation(JSONObject locationJson, LocalTime time) throws JSONException {
            Location result = new Location("");
            result.setTime(CommonMethod.LocalTimeToMiliSecond(time));
            result.setLongitude(locationJson.getDouble("longitude"));
            result.setLatitude(locationJson.getDouble("latitude"));
            result.setSpeed((float) locationJson.getDouble("speed"));
            result.setBearing((float) locationJson.getDouble("bearing"));
            return result;
        }
    }
}
