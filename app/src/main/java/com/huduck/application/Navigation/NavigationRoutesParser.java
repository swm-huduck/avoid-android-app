package com.huduck.application.Navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class NavigationRoutesParser {
    public static NavigationRoutes parserTruckRoutes(String jsonString) throws JSONException {

        NavigationRoutes routes = new NavigationRoutes();
        ArrayList<Integer>
                navigationSequence = routes.getNavigationSequence();
        HashMap<Integer, NavigationPoint>
                navigationPointHashMap = routes.getNavigationPointHashMap();
        HashMap<Integer, NavigationLineString>
                navigationLineStringHashMap = routes.getNavigationLineStringHashMap();

        JSONObject json;
        try {
            json = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
            return routes;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return routes;
        }
        JSONArray featuresJson = json.getJSONArray("features");

        for(int i = 0; i < featuresJson.length(); i++) {
            JSONObject featureJson = featuresJson.getJSONObject(i);

            JSONObject geometryJson = featureJson.getJSONObject("geometry");
            String type = geometryJson.getString("type");

            JSONObject propertiesJson = featureJson.getJSONObject("properties");
            if(!propertiesJson.has("index")) continue;
            int index = propertiesJson.getInt("index");
            String name = propertiesJson.getString("name");
            String description = propertiesJson.getString("description");

            navigationSequence.add(index);

            if (type.equals("Point")) {
                // Geometry
                JSONArray coordinatesJson = geometryJson.getJSONArray("coordinates");
                ArrayList<Double> coordinates = new ArrayList<Double>(){{
                    add(coordinatesJson.getDouble(1));  // Lat
                    add(coordinatesJson.getDouble(0));  // Lng
                }};
                NavigationPoint.Geometry geo = NavigationPoint.Geometry.builder()
                        .type(type).coordinates(coordinates)
                        .build();

                // Properties
                int totalTime = 0;
                if(propertiesJson.getString("pointType").equals("S")) {
                    totalTime = propertiesJson.getInt("totalTime");
                    routes.setTotalTime(totalTime);
                }
                int pointIndex = propertiesJson.getInt("pointIndex");
                String nextRoadName = propertiesJson.getString("nextRoadName");
                int turnType = propertiesJson.getInt("turnType");
                String pointType = propertiesJson.getString("pointType");

                NavigationPoint.Properties prop = NavigationPoint.Properties.builder()
                        .index(index).pointIndex(pointIndex)
                        .name(name).description(description)
                        .nextRoadName(nextRoadName).turnType(turnType).pointType(pointType)
                        .totalTime(totalTime)
                        .build();

                NavigationPoint point = new NavigationPoint(geo, prop);
                navigationPointHashMap.put(index, point);
            }
            else if(type.equals("LineString")) {
                // Geometry
                JSONArray coordinatesListJson = geometryJson.getJSONArray("coordinates");
                ArrayList<ArrayList<Double>> coordinates = new ArrayList<>();

                for(int idx = 0; idx < coordinatesListJson.length(); idx++) {
                    JSONArray coordinatesJson = coordinatesListJson.getJSONArray(idx);
                    coordinates.add(new ArrayList<Double>(){{
                        add(coordinatesJson.getDouble(1));  // Lat
                        add(coordinatesJson.getDouble(0));  // Lng
                    }});
                }

                NavigationLineString.Geometry geo = NavigationLineString.Geometry.builder()
                        .type(type).coordinates(coordinates)
                        .build();

                // Properties
                int lineIndex = propertiesJson.getInt("lineIndex");
                int distance = propertiesJson.getInt("distance");
                int time = propertiesJson.getInt("time");
                int roadType = propertiesJson.getInt("roadType");
                int facilityType = propertiesJson.getInt("facilityType");

                NavigationLineString.Properties prop = NavigationLineString.Properties.builder()
                        .index(index).lineIndex(lineIndex)
                        .name(name).description(description)
                        .distance(distance).time(time)
                        .roadType(roadType).facilityType(facilityType)
                        .build();

                NavigationLineString lineString = new NavigationLineString(geo, prop);
                navigationLineStringHashMap.put(index, lineString);
            }
        }

        return routes;
    }
}
