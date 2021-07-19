package com.huduck.application.setting.detail.item;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingDetailItem {
    public static SettingDetailItemType type = SettingDetailItemType.NONE;
    @NonNull
    protected String title;

    public SettingDetailItem(String title) {
        setTitle(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public SettingDetailItemType getType() {
        return SettingDetailItemType.NONE;
    }

    public static SettingDetailItem fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static SettingDetailItem fromJson(JSONObject json) throws JSONException {
        String[] needKeys = {"title", "type"};
        for(String key : needKeys)
            if(!json.has(key)) return null;

        String needType = type.toString();
        String jsonType = json.getString("type");
        if(!jsonType.equals(needType)) return null;

        String title = json.optString("title", "None");
        SettingDetailItem item = new SettingDetailItem(title);
        return item;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", title);
        json.put("type", type.toString());
        return json;
    }
}
