package com.huduck.application.fragment.myCar;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.huduck.application.R;

import java.text.DecimalFormat;

import lombok.Getter;

public class MyCarItemFragment extends Fragment {

    // View
    private TextView nameView;
    private TextView descriptionView;
    private TextView unitView;
    private EditText inputView;
    private LinearLayout checkLayout;


    // Setting
    @Getter private String name;
    @Getter private String description;
    @Getter private String hint;
    @Getter private String unit;
    @Getter private float minValue = 0;
    @Getter private float maxValue = 1000;
    @Getter private boolean success = false;

    // Callback
    private MyCarItemChanged changedCallback;

    // Value
    @Getter private Float value = null;

    public MyCarItemFragment() {}

    public MyCarItemFragment init(String name, String description, String hint, String unit, float minValue, float maxValue) {
        return init(name, description, hint, unit, minValue, maxValue, null, null);
    }

    public MyCarItemFragment init(String name, String description, String hint, String unit, float minValue, float maxValue, Float defaultValue, Integer decimalLength) {
        this.name = name;
        nameView.setText(name);

        this.description = description;
        descriptionView.setText(description);

        this.hint = hint;
        inputView.setHint(hint);

        this.unit = unit;
        unitView.setText(unit);

        this.minValue = minValue;
        this.maxValue = maxValue;


        if(defaultValue != null) {
            this.success = true;
            this.value = defaultValue;
            inputView.setText(defaultValue.toString());
        }

        if(decimalLength != null && decimalLength >= 0)
            inputView.setFilters(new InputFilter[] {new InputFilter.LengthFilter((int)(Math.log10(maxValue)+1+decimalLength))});
        else
            inputView.setFilters(new InputFilter[] {new InputFilter.LengthFilter((int)(Math.log10(maxValue)+1+2))});

        return this;
    }

    public MyCarItemFragment setMyCarItemChangedCallback(MyCarItemChanged callback) {
        changedCallback = callback;
        return this;
    }

    public MyCarItemFragment isEditable(boolean value) {
        inputView.setEnabled(value);
        return this;
    }

    public void setValue(float value) {
        this.value = value;
        inputView.setText(new DecimalFormat("#.######").format(value));
    }

    public void isSuccess(boolean value) {
        success = value;
        if(success)
            checkLayout.setBackgroundResource(R.drawable.bg_my_car_info_success);
        else
            checkLayout.setBackgroundResource(R.drawable.bg_my_car_info_danger);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_car_item, container, false);

        nameView = view.findViewWithTag("name");

        descriptionView = view.findViewWithTag("description");

        checkLayout = view.findViewWithTag("check_layout");

        unitView = view.findViewWithTag("unit");

        inputView = view.findViewWithTag("input");
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                float val = 0;

                String str = s.toString();
                if(!str.equals(""))
                    val = Float.parseFloat(str);

                boolean success = true;

                // 범위 안에 포함되지 않을 경우
                if(val < minValue || val > maxValue)
                    success = false;

                value = val;
                isSuccess(success);

                if(changedCallback != null)
                    changedCallback.myCarItemChanged(success, val);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    public static interface MyCarItemChanged {
        public void myCarItemChanged(boolean success, float value);
    }
}

