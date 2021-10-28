package com.huduck.application.fragment.setting;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceFragmentCompat;

import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.device.DeviceService;
import com.huduck.application.fragment.PageFragment;
import com.huduck.application.setting.SettingConstants;

import lombok.Setter;

public class SettingFragment extends PageFragment {

    Preference preference;
    View view;

    DeviceService deviceService;
    boolean isService = false;
    ServiceConnection conn = new ServiceConnection() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            deviceService = binder.getService();
            isService = true;
            preference.setDeviceService(deviceService);

            checkDeviceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    public SettingFragment() {}

    public static SettingFragment newInstance() {
        SettingFragment fragment = new SettingFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden)
            checkDeviceConnected();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        preference = new Preference();
        getChildFragmentManager().beginTransaction().add(R.id.setting_container, preference).commit();

        Intent intent = new Intent(getActivity(), DeviceService.class);
        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preference.getView();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkDeviceConnected() {
        if(deviceService != null && !deviceService.isConnected()) {
            Toast toast = Toast.makeText(getActivity(), "디바이스 연결이 필요합니다.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.selectBottomNavigationBarItem(2);
        }
    }

    @SuppressLint("ValidFragment")
    public static class Preference extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        SharedPreferences sharedPreferences;
        @Setter DeviceService deviceService;
        SharedPreferences preferenceSharedPreferences; // 따로 저장하는 설정값

        Preference() {

        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            sharedPreferences = getPreferenceScreen().getSharedPreferences();

            preferenceSharedPreferences = getActivity().getSharedPreferences("preference", Context.MODE_PRIVATE);
            if(!preferenceSharedPreferences.contains("saved")) {
                SharedPreferences.Editor editor = preferenceSharedPreferences.edit();

                SettingConstants.SETTING_ITEM_MAP.forEach((String key, Class class_) -> {
                    if(class_ == String.class)
                        editor.putString(key, SettingConstants.SETTING_DEFAULT_VALUE.get(key));
                    else if(class_ == Boolean.class)
                        editor.putBoolean(key, SettingConstants.SETTING_DEFAULT_VALUE.get(key) == "true");
                });

                editor.putBoolean("saved", true);
                editor.commit();
            }

            return view;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onResume() {
            super.onResume();
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.setting);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            sendSettingValue(sharedPreferences, key);
        }

        private void sendSettingValue(SharedPreferences sharedPreferences, String key) {
            Class class_ = SettingConstants.SETTING_ITEM_MAP.get(key);

            String value = null;
            if(class_ == String.class) {
                value = sharedPreferences.getString(key, null);
                preferenceSharedPreferences.edit().putString(key, value).commit();
            }
            else if(class_ == Boolean.class) {
                boolean boolValue = sharedPreferences.getBoolean(key, false);
                value =  boolValue ? "true" : "false";
                preferenceSharedPreferences.edit().putBoolean(key, boolValue).commit();
            }

            if(value == null)           return;
            if(deviceService == null)   return;

            deviceService.updateSetting(key, value);
        }
    }
}
