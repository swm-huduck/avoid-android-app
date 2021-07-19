package com.huduck.application.setting;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.huduck.application.setting.detail.SettingDetail;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Setting {
    List<SettingDetail> details = new ArrayList<>();

    public Setting() {}

    public Setting(List<SettingDetail> details) {
        if(details != null)
            this.details = details;
    }

    public List<SettingDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }

    public SettingDetail addDetail(SettingDetail detail) {
        details.add(detail);
        return detail;
    }

    public static Setting fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static Setting fromJson(JSONObject json) throws JSONException {
        String[] needKeys = {"details"};
        for(String key : needKeys)
            if(!json.has(key)) return null;

        JSONArray jsonDetails = json.optJSONArray("details");
        List<SettingDetail> details = new ArrayList<>();
        for(int i = 0; i < jsonDetails.length(); i++) {
            SettingDetail detail = SettingDetail.fromJson((JSONObject)jsonDetails.get(i));
            details.add(detail);
        }

        return new Setting(details);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray jsonDetails = new JSONArray();

        for(SettingDetail detail : details) {
            try {
                jsonDetails.put(detail.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        json.put("details", jsonDetails);
        return json;
    }
}
