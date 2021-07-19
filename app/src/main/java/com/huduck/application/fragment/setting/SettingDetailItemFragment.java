package com.huduck.application.fragment.setting;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.huduck.application.R;
import com.huduck.application.setting.detail.item.SettingDetailItemType;

public class SettingDetailItemFragment extends Fragment {
    // Static

    public static final String ItemTitle = "itemTitle";
    public static final String ItemType = "itemType";
    public static final String ItemValue = "itemValue";

    // View
    private TextView TextView_itemTitle;
    private Switch Switch_itemValue;
    private CheckBox CheckBox_itemValue;

    //
    private String itemTitle;
    private SettingDetailItemType itemType;
    private boolean itemValue;
    private View view;
    public static SettingDetailItemFragment newInstanceSwitch(String itemTitle, boolean itemValue) {
        SettingDetailItemFragment fragment = new SettingDetailItemFragment();

        Bundle args = new Bundle();
        args.putString(ItemTitle, itemTitle);
        args.putString(ItemType, SettingDetailItemType.SWITCH.toString());
        args.putBoolean(ItemValue, itemValue);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle args = getArguments();
            itemTitle = args.getString(ItemTitle);
            itemType = SettingDetailItemType.valueOf(args.getString(ItemType));
            itemValue = args.getBoolean(ItemValue);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting_detail_item, container, false);
//        TextView_itemTitle = view.findViewById(R.id.TextView_item_title);
//        Switch_itemValue = view.findViewById(R.id.Switch_item_value);
//        CheckBox_itemValue = view.findViewById(R.id.CheckBox_item_value);
//
//        // Init item title
//        TextView_itemTitle.setText(itemTitle);

        // Show/Fill input by item type and value
        initInput(view);


        return view;
    }

    private void initInput(View v) {
        switch (itemType) {
            case SWITCH:
                Switch_itemValue.setVisibility(View.VISIBLE);
                Switch_itemValue.setChecked(itemValue);
                break;
            case CHECKBOX:
                CheckBox_itemValue.setVisibility(View.VISIBLE);
                CheckBox_itemValue.setChecked(itemValue);
                break;
            case NONE:
            default:
                break;
        }
    }
}