package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.bottomnavigation.BottomNavigationMenu;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.huduck.application.R;
import com.huduck.application.databinding.ActivityMainBinding;
import com.huduck.application.fragment.PageFragment;
import com.huduck.application.fragment.myCar.MyCarFragment;
import com.huduck.application.fragment.navigation.NavigationMainFragment;
import com.huduck.application.fragment.navigation.NavigationSearchFragment;
import com.huduck.application.fragment.navigation.NavigationSearchResultFragment;
import com.huduck.application.setting.Setting;
import com.huduck.application.setting.detail.SettingDetail;
import com.huduck.application.setting.detail.item.SettingDetailItem;
import com.huduck.application.setting.detail.item.SettingDetailItemCheckBox;
import com.huduck.application.setting.detail.item.SettingDetailItemSwitch;

import org.json.JSONException;

import com.huduck.application.fragment.device.DeviceFragment;
import com.huduck.application.fragment.setting.SettingFragment;
import com.skt.Tmap.TMapPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FragmentManager fragmentManager;
    private InputMethodManager inputMethodManager;

    HashMap<Class, PageFragment> fragmentHashMap = new HashMap<>();
    Integer currentBottomNaviItemId = 0;
    Class currentFragmentClass = null;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 바인딩
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 멤버변수 초기화
        fragmentManager = getSupportFragmentManager();
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // 하단 내비바 선택 이벤트 설정
        binding.bottomNaviBar.setOnNavigationItemSelectedListener(item -> {
            Integer selectedBottomNaviItemId = item.getItemId();

            // 저장된 하단 내비바 위치와 동일한 버튼 선택 금지
            if (currentBottomNaviItemId.equals(selectedBottomNaviItemId)) return false;

            // 각 버튼별 처리 로직
            switch (selectedBottomNaviItemId) {
                case R.id.page_navigation:
                    changeFragment(NavigationMainFragment.class);
                    break;
                case R.id.page_my_car:
                    changeFragment(MyCarFragment.class);
                    break;
                case R.id.page_device:
                    changeFragment(DeviceFragment.class);
                    break;
                case R.id.page_setting:
                    changeFragment(SettingFragment.class);
                    break;
                default:
                    return false;
            }

            // 키보드 내리기
            hideKeyboard();

            // 현재 하단 내비바 위치 저장
            currentBottomNaviItemId = selectedBottomNaviItemId;
            return true;
        });

        // 초기 선택 되어있는 하단 내비바 아이템 지정
        binding.bottomNaviBar.setSelectedItemId(R.id.page_navigation);
    }

    // container에 표시할 fragment 변경 (overwrite 하지 않음)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void changeFragment(Class<? extends PageFragment> fragmentClass) {
        changeFragment(fragmentClass, null, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void changeFragment(Class<? extends PageFragment> fragmentClass, Bundle bundle) {
        changeFragment(fragmentClass, bundle, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void changeFragment(Class<? extends PageFragment> fragmentClass, @NonNull Boolean overwrite) {
        changeFragment(fragmentClass, null, overwrite);
    }

    // container에 표시할 fragment 변경 (overwrite 설정 가능)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void changeFragment(Class<? extends PageFragment> fragmentClass, Bundle bundle, Boolean overwrite) {
        hideKeyboard();

        // 덮어쓰기이고, 객체가 있을 경우
        if (overwrite && fragmentHashMap.containsKey(fragmentClass)) {
            PageFragment removeFragment = fragmentHashMap.get(fragmentClass);
            fragmentHashMap.remove(fragmentClass);
            fragmentManager.beginTransaction().remove(removeFragment).commit();
        }

        // 만들어진 객체가 없을 경우
        if (!fragmentHashMap.containsKey(fragmentClass)) {
            PageFragment newFragment = createFragment(fragmentClass);
            if(newFragment == null) return;
            fragmentHashMap.put(fragmentClass, newFragment);
            fragmentManager.beginTransaction().add(binding.containerFrameLayout.getId(), newFragment).commit();
        }

        // 모든 페이지 객체에서 선택된 객체만 활성화 및 번들 삽입
        fragmentHashMap.forEach((class_, fragment) -> {
            if (class_.equals(fragmentClass)) {
                fragmentManager.beginTransaction().show(fragment).commit();
                if(bundle != null)
                    fragment.init(bundle);
            }
            else
                fragmentManager.beginTransaction().hide(fragment).commit();
        });

        currentFragmentClass = fragmentClass;
    }

    // container에 띄워줄 새로운 Fragment 객체를 만듦
    private PageFragment createFragment(Class<? extends PageFragment> fragmentClass) {
        PageFragment fragment = null;

        try {
            fragment = fragmentClass.newInstance();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return (PageFragment) fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBackPressed() {
        if(currentFragmentClass.equals(NavigationMainFragment.class)) {
            super.onBackPressed();
        }
        else if(currentFragmentClass.equals(NavigationSearchFragment.class)) {
            changeFragment(NavigationMainFragment.class);
        }
        else if(currentFragmentClass.equals(NavigationSearchResultFragment.class)) {
            changeFragment(NavigationSearchFragment.class);
        }
        else if(currentFragmentClass.equals(MyCarFragment.class)) {
            binding.bottomNaviBar.setSelectedItemId(R.id.page_navigation);
        }
        else if(currentFragmentClass.equals(DeviceFragment.class)) {
            binding.bottomNaviBar.setSelectedItemId(R.id.page_navigation);
        }
        else if(currentFragmentClass.equals(SettingFragment.class)) {
            binding.bottomNaviBar.setSelectedItemId(R.id.page_navigation);
        }
        else {
            changeFragment(NavigationMainFragment.class);
        }
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if(inputMethodManager.isAcceptingText()){
            View focusedView = getCurrentFocus();
            if(focusedView != null)
                inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }
}