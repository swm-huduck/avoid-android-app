package com.huduck.application.setting.detail;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.huduck.application.setting.detail.item.SettingDetailItem;
import com.huduck.application.setting.detail.item.SettingDetailItemCheckBox;
import com.huduck.application.setting.detail.item.SettingDetailItemSwitch;
import com.huduck.application.setting.detail.item.SettingDetailItemType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingDetail {
    @NonNull
    protected String title;
    protected List<SettingDetailItem> items = new ArrayList<>();

    public SettingDetail(@NonNull String title) {
        this.title = title;
    }

    public SettingDetail(@NonNull String title, @NonNull List<SettingDetailItem> items) {
        this.title = title;
        this.items = items;
    }

    public SettingDetailItem addItem(SettingDetailItem item) {
        items.add(item);
        return item;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public List<SettingDetailItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public static SettingDetail fromJson(String string) throws JSONException {
        return fromJson(new JSONObject(string));
    }

    public static SettingDetail fromJson(JSONObject json) throws JSONException {
        String[] needKeys = {"title", "items"};
        for(String key : needKeys)
            if(!json.has(key)) return null;

        String title = json.optString("title", "None");
        JSONArray jsonItems = json.optJSONArray("items");
        List<SettingDetailItem> items = new ArrayList<SettingDetailItem>();
        for(int i = 0; i < jsonItems.length(); i++) {
            JSONObject jsonItem = jsonItems.getJSONObject(i);
            SettingDetailItemType type = SettingDetailItemType.valueOf(jsonItem.optString("type"));
            SettingDetailItem item;
            switch (type) {
                case SWITCH:
                    item = SettingDetailItemSwitch.fromJson(jsonItem);
                    break;
                case CHECKBOX:
                    item = SettingDetailItemCheckBox.fromJson(jsonItem);
                    break;
                case NONE:
                default:
                    item = SettingDetailItem.fromJson(jsonItem);
                    break;
            }
            items.add(item);
        }

        return new SettingDetail(title, items);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        // Put title
        json.put("title", title);

        // Put items
        JSONArray jsonItems = new JSONArray();
        items.forEach(item -> {
            try {
                jsonItems.put(item.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        json.put("items", jsonItems);

        return json;
    }
}
