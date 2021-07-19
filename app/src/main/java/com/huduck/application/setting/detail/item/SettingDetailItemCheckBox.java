package com.huduck.application.setting.detail.item;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingDetailItemCheckBox extends SettingDetailItem {
    public static SettingDetailItemType type = SettingDetailItemType.CHECKBOX;
    @NonNull
    boolean value = false;

    public SettingDetailItemCheckBox(@NonNull String title, boolean value) {
        super(title);
        setValue(value);
    }

    @Override
    public SettingDetailItemType getType() {
        return SettingDetailItemType.CHECKBOX;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public static SettingDetailItemCheckBox fromJson(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    public static SettingDetailItemCheckBox fromJson(JSONObject json) throws JSONException {
        String[] needKeys = {"title", "type", "value"};
        for(String key : needKeys)
            if(!json.has(key)) return null;

        String needType = type.toString();
        String jsonType = json.getString("type");
        if(!jsonType.equals(needType)) return null;

        String title = json.optString("title", "None");
        boolean value = json.optBoolean("value", false);
        SettingDetailItemCheckBox item = new SettingDetailItemCheckBox(title, value);
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
