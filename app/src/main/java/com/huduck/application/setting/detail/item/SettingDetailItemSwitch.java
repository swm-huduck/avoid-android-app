package com.huduck.application.setting.detail.item;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingDetailItemSwitch extends SettingDetailItem {
    public static SettingDetailItemType type = SettingDetailItemType.SWITCH;
    @NonNull
    boolean value = false;

    public SettingDetailItemSwitch(@NonNull String title, boolean value) {
        super(title);
        setValue(value);
    }

    @Override
    public SettingDetailItemType getType() {
        return SettingDetailItemType.SWITCH;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public static SettingDetailItemSwitch fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static SettingDetailItemSwitch fromJson(JSONObject json) throws JSONException {
        String[] needKeys = {"title", "type", "value"};
        for(String key : needKeys)
            if(!json.has(key)) return null;

        String needType = type.toString();
        String jsonType = json.getString("type");
        if(!jsonType.equals(needType)) return null;

        String title = json.optString("title", "None");
        boolean value = json.optBoolean("value", false);
        SettingDetailItemSwitch item = new SettingDetailItemSwitch(title, value);
        return item;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("title", title);
        json.put("type", type.toString());
        json.put("value", value);
        return json;
    }
}
