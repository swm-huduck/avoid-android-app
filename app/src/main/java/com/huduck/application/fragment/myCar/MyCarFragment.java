package com.huduck.application.fragment.myCar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

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


    private Button saveButton;
    
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

        saveButton = view.findViewById(R.id.save_my_car);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMyCarInformation();
            }
        });

        FragmentManager fm = getChildFragmentManager();

        widthFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_width);
        widthFragment.init("차량 너비", "1~3m", "너비", "m", 1, 3)
                .setMyCarItemChangedCallback((success, value) -> {
                    changeSaveButtonState(checkSavable());
                });

        heightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_height);
        heightFragment.init("차량 높이", "1~6m", "높이", "m", 1, 6)
                .setMyCarItemChangedCallback((success, value) -> {
                    changeSaveButtonState(checkSavable());
                });

        lengthFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_length);
        lengthFragment.init("차량 길이", "2~40m", "길이", "m", 2, 40)
                .setMyCarItemChangedCallback((success, value) -> {
                    changeSaveButtonState(checkSavable());
                });

        totalWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_total_weight);
        totalWeightFragment
                .init("[자동 계산] 총 중량", "(차량 중량 + 화물 중량)이\n0.5~600t 사이여야 합니다.", "", "t", 0.5f, 600, 0.0f, 2)
                .isEditable(false)
                .setMyCarItemChangedCallback((success, value) -> {
                });

        truckWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_truck_weight);
        truckWeightFragment
                .init("차량 중량", "0.5~600t", "중량", "t", 0.5f, 600, null, 2)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkTotalWeight();
                    changeSaveButtonState(checkSavable());
                });

        loadWeightFragment = (MyCarItemFragment) fm.findFragmentById(R.id.my_car_load_weight);
        loadWeightFragment
                .init("화물 중량", "0.5~600t", "중량", "t", 0.5f, 600, null, 2)
                .setMyCarItemChangedCallback((success, value) -> {
                    checkTotalWeight();
                    changeSaveButtonState(checkSavable());
                });

        sharedPreferences = getActivity().getSharedPreferences("my_car", Context.MODE_PRIVATE);
        boolean saved = sharedPreferences.contains("saved");
        changeSaveButtonState(saved);

        if(saved) {
            float width = sharedPreferences.getFloat("width", 0);
            float height = sharedPreferences.getFloat("height", 0);
            float length = sharedPreferences.getFloat("length", 0);
            float truck_weight = sharedPreferences.getFloat("truck_weight", 0);
            float load_weight = sharedPreferences.getFloat("load_weight", 0);
            float total_weight = sharedPreferences.getFloat("total_weight", 0);

            widthFragment.setValue(width);
            heightFragment.setValue(height);
            lengthFragment.setValue(length);
            truckWeightFragment.setValue(truck_weight);
            loadWeightFragment.setValue(load_weight);
            totalWeightFragment.setValue(total_weight);
        }

        return view;
    }

    private boolean checkTotalWeight() {
        float total = 0;
        boolean success = truckWeightFragment.isSuccess() && loadWeightFragment.isSuccess();

        if(success) {
            total = truckWeightFragment.getValue() + loadWeightFragment.getValue();
            if(0.5 <= total && total <= 600) {
                totalWeightFragment.setValue(total);
                totalWeightFragment.isSuccess(true);
                return true;
            }
        }

        totalWeightFragment.setValue(total);
        totalWeightFragment.isSuccess(false);
        return false;
    }

    private void saveMyCarInformation() {
        if(checkSavable())
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor
                    .putBoolean("saved", true)
                    .putFloat("width", widthFragment.getValue())
                    .putFloat("height", heightFragment.getValue())
                    .putFloat("length", lengthFragment.getValue())
                    .putFloat("truck_weight", truckWeightFragment.getValue())
                    .putFloat("load_weight", loadWeightFragment.getValue())
                    .putFloat("total_weight", totalWeightFragment.getValue())
                    .commit();

            Toast toast = Toast.makeText(getActivity(), "저장되었습니다.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private boolean checkSavable() {
        return widthFragment.isSuccess() && heightFragment.isSuccess() && lengthFragment.isSuccess()
                && totalWeightFragment.isSuccess();
    }

    private void changeSaveButtonState(boolean success) {
        saveButton.setEnabled(success);
        if(success) {
            saveButton.setBackgroundColor(getResources().getColor(R.color.indigo700, getActivity().getTheme()));
        }
        else {
            saveButton.setBackgroundColor(getResources().getColor(R.color.gray700, getActivity().getTheme()));
        }
    }
}