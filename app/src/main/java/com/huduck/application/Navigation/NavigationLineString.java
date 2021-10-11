package com.huduck.application.Navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
public class NavigationLineString implements NavigationFeature {
    private Geometry geometry;
    private Properties properties;

    public NavigationLineString(Geometry geometry, Properties properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    @Getter
    @SuperBuilder
    public static class Geometry extends NavigationFeature.Geometry {
        private final String type = "LineString";
        private ArrayList<ArrayList<Double>> coordinates;
//        whr
//        htr
//        wtr
//        wpz
//        ttr
    }

    @Getter
    @SuperBuilder
    public static class Properties extends NavigationFeature.Properties {
        private int lineIndex;
        private int distance;
        private int time;
        private int roadType;
        private int facilityType;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("type", "Feature");

        // Geometry
        JSONObject geometryJson = new JSONObject();

        geometryJson.put("type", "LineString");

        JSONArray coordinatesJson = new JSONArray();
        for (ArrayList<Double> coordinate : geometry.getCoordinates()) {
            JSONArray coordinateJson = new JSONArray();
            coordinateJson.put(coordinate.get(1));
            coordinateJson.put(coordinate.get(0));
            coordinatesJson.put(coordinateJson);
        }
        geometryJson.put("coordinates", coordinatesJson);

        result.put("geometry", geometryJson);

        // Properties
        JSONObject propertiesJson = new JSONObject();

        propertiesJson.put("index",         properties.index);
        propertiesJson.put("lineIndex",     properties.lineIndex);
        propertiesJson.put("name",          properties.name);
        propertiesJson.put("description",   properties.description);
        propertiesJson.put("distance",      properties.distance);
        propertiesJson.put("time",          properties.time);
        propertiesJson.put("roadType",      properties.roadType);
        propertiesJson.put("facilityType",  properties.facilityType);

        result.put("properties", propertiesJson);

        return result;
    }
}
