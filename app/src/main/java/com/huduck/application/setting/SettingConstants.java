package com.huduck.application.setting;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingConstants {
    public static HashMap<String, Class> SETTING_ITEM_MAP = new HashMap<String, Class>() {{
        // 위험 상황 인식
        put("stop_front_car", Boolean.class);
        put("sensitivity", String.class);

        // 내비게이션
        put("speed", Boolean.class);
        put("turn_event", Boolean.class);

        // 알림
        put("call", Boolean.class);
        put("sms", Boolean.class);
        put("kakao", Boolean.class);
    }};

    public static HashMap<String, String> SETTING_DEFAULT_VALUE = new HashMap<String, String>() {{
        // 위험 상황 인식
        put("stop_front_car", "false");
        put("sensitivity", "보통");

        // 내비게이션
        put("speed", "false");
        put("turn_event", "false");

        // 알림
        put("call", "false");
        put("sms", "false");
        put("kakao", "false");
    }};
}
