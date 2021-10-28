package com.huduck.application.Navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;

@Getter
public class NavigationRoutes implements Serializable {
    @Setter
    private long totalTime = 0;
    private ArrayList<Integer> navigationSequence = new ArrayList<>();
    private HashMap<Integer, NavigationPoint> navigationPointHashMap = new HashMap<>();
    private HashMap<Integer, NavigationLineString> navigationLineStringHashMap = new HashMap<>();

    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("type", "FeatureCollection");

        JSONArray featuresJson = new JSONArray();

        for (Integer index : navigationSequence) {
            if(navigationPointHashMap.containsKey(index)) {
                JSONObject pointJson = navigationPointHashMap.get(index).toJson();
                featuresJson.put(pointJson);
            }
            else if(navigationLineStringHashMap.containsKey(index)) {
                JSONObject lineStringJson = navigationLineStringHashMap.get(index).toJson();
                featuresJson.put(lineStringJson);
            }
        }

        result.put("features", featuresJson);

        return result;
    }
}
