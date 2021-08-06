package com.huduck.application.fragment.myCar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.huduck.application.R;
import com.huduck.application.fragment.PageFragment;

public class MyCarFragment extends PageFragment {

    private MyCarItemFragment widthFragment;
    private MyCarItemFragment heightFragment;
    private MyCarItemFragment lengthFragment;
    private MyCarItemFragment truckWeightFragment;
    private MyCarItemFragment loadWeightFragment;
    private MyCarItemFragment totalWeightFragment;

    private SharedPreferences sharedPreferences;

    private ImageView successIcon;

    public MyCarFragment() { }

    public static MyCarFragment newInstance() {
        MyCarFragment fragment = new MyCarFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_car, container, false);

        successIcon = view.findViewWithTag("success_icon");

        FragmentManager fm = getChildFragmentManager();

        widthFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_width);
        widthFragment.init("차량 너비", "100~300cm", "너비", "cm", 100, 300)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkAllSuccess();
                });

        heightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_height);
        heightFragment.init("차량 높이", "100~600cm", "높이", "cm", 100, 600)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkAllSuccess();
                });

        lengthFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_length);
        lengthFragment.init("차량 길이", "200~4000cm", "길이", "cm", 200, 4000)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkAllSuccess();
                });

        totalWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_total_weight);
        totalWeightFragment
                .init("[자동 계산] 총 중량", "(차량 중량 + 화물 중량)이\n500~600000kg 사이여야 합니다.", "", "kg", 500, 600000, 0)
                .isEditable(false)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkAllSuccess();
                });

        truckWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_truck_weight);
        truckWeightFragment
                .init("차량 중량", "500~600000kg", "중량", "kg", 500, 600000)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkTotalWeight();
                    checkAllSuccess();
                });

        loadWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_load_weight);
        loadWeightFragment
                .init("화물 중량", "500~600000kg", "중량", "kg", 500, 600000)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkTotalWeight();
                    checkAllSuccess();
                });

        sharedPreferences = getActivity().getSharedPreferences("my_car", Context.MODE_PRIVATE);
        if(sharedPreferences.contains("saved")) {
            int width = sharedPreferences.getInt("width", 0);
            int height = sharedPreferences.getInt("height", 0);
            int length = sharedPreferences.getInt("length", 0);
            int truck_weight = sharedPreferences.getInt("truck_weight", 0);
            int load_weight = sharedPreferences.getInt("load_weight", 0);
            int total_weight = sharedPreferences.getInt("total_weight", 0);

            widthFragment.setValue(width);
            heightFragment.setValue(height);
            lengthFragment.setValue(length);
            truckWeightFragment.setValue(truck_weight);
            loadWeightFragment.setValue(load_weight);
            totalWeightFragment.setValue(total_weight);

            changeSuccessIcon(true);
        }

        return view;
    }

    private void checkTotalWeight() {
        int total = 0;
        boolean success = truckWeightFragment.isSuccess() && loadWeightFragment.isSuccess();

        if(success) {
            total = truckWeightFragment.getValue() + loadWeightFragment.getValue();
            if(500 <= total && total <= 600000) {
                totalWeightFragment.setValue(total);
                totalWeightFragment.isSuccess(true);
                return;
            }
        }

        totalWeightFragment.setValue(total);
        totalWeightFragment.isSuccess(false);
    }

    private void checkAllSuccess() {
        if(widthFragment.isSuccess() && heightFragment.isSuccess() && lengthFragment.isSuccess()
        && totalWeightFragment.isSuccess())
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("saved", true);
            editor.putInt("width", widthFragment.getValue());
            editor.putInt("height", heightFragment.getValue());
            editor.putInt("length", lengthFragment.getValue());
            editor.putInt("truck_weight", truckWeightFragment.getValue());
            editor.putInt("load_weight", loadWeightFragment.getValue());
            editor.putInt("total_weight", totalWeightFragment.getValue());
            editor.commit();
            changeSuccessIcon(true);
        }
        else
            changeSuccessIcon(false);
    }

    private void changeSuccessIcon(boolean success) {
        int icon = R.drawable.ic_outline_check_circle_24;
        int color = R.color.indigo500;

        if(!success) {
            icon = R.drawable.ic_outline_cancel_24;
            color = R.color.gray700;
        }

        successIcon.setImageResource(icon);
        successIcon.setImageTintList(
                ColorStateList.valueOf(ContextCompat.getColor(getActivity().getApplicationContext(), color))
        );
    }
}