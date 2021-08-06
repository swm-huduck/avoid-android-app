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
    @Getter private int minValue = 0;
    @Getter private int maxValue = 1000;
    @Getter private boolean success = false;

    // Callback
    private MyCarItemChanged changedCallback;

    // Value
    @Getter private Integer value = null;

    public MyCarItemFragment() {}

    public MyCarItemFragment init(String name, String description, String hint, String unit, int minValue, int maxValue) {
        return init(name, description, hint, unit, minValue, maxValue, null);
    }

    public MyCarItemFragment init(String name, String description, String hint, String unit, int minValue, int maxValue, Integer defaultValue) {
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
        inputView.setFilters(new InputFilter[] {new InputFilter.LengthFilter((int)(Math.log10(maxValue)+1))});
        if(defaultValue != null) {
            this.success = true;
            this.value = defaultValue;
            inputView.setText(defaultValue.toString());
        }
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

    public void setValue(int value) {
        this.value = value;
        inputView.setText(value+"");
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
                int val = 0;

                String str = s.toString();
                if(!str.equals(""))
                    val = Integer.parseInt(str);

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
        public void myCarItemChanged(boolean success, int value);
    }
}

