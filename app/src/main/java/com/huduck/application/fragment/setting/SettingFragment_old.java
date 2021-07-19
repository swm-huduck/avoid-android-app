//package com.huduck.application.fragment.setting;
//
//import static androidx.core.content.ContextCompat.getSystemService;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.os.Build;
//import android.os.Bundle;
//
//import androidx.annotation.RequiresApi;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;
//
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.CompoundButton;
//import android.widget.LinearLayout;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.Switch;
//import android.widget.TextView;
//
//import com.huduck.application.R;
//import com.huduck.application.setting.Setting;
//import com.huduck.application.setting.detail.SettingDetail;
//import com.huduck.application.setting.detail.item.SettingDetailItemSwitch;
//import com.huduck.application.setting.detail.item.SettingDetailItemType;
//
//import org.json.JSONException;
//
//import java.util.zip.Inflater;
//
//public class SettingFragment extends Fragment {
//    private Setting setting;
//    private LinearLayout LinearLayout_detailItemView;
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//    }
//
//    @SuppressLint("ResourceType")
//    @RequiresApi(api = Build.VERSION_CODES.N)
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_setting, container, false);
//
//        // 세부 설정 아이템 레이아웃 view 가져오기
//        LinearLayout_detailItemView = view.findViewById(R.id.LinearLayout_detail_item_view);
//
//        // 저장되어 있던 설정 json 내용 가져오기
//        SharedPreferences sharedPreference = getActivity().getSharedPreferences("setting", Context.MODE_PRIVATE);
//        String jsonStr = sharedPreference.getString("jsonStr", "");
//
//        // 설정 json을 Setting 객체로 변환
//        try {
//            setting = Setting.fromJson(jsonStr);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        // Setting 객체를 기반으로 화면 UI 동적 생성
//        RadioGroup RadioGroup_settingDetailMenuBar = view.findViewById(R.id.RadioGroup_setting_detail_menu_bar);
//        for(int i = 0; i < setting.getDetails().size(); i++) {
//            SettingDetail detail = setting.getDetails().get(i);
//            LayoutInflater inflater1 = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            View rbtnView = inflater1.inflate(R.layout.view_setting_detail_menu_rbtn, RadioGroup_settingDetailMenuBar, false);
//            RadioButton rbtn = rbtnView.findViewById(R.id.RadioButton_setting_detail_menu_rbtn);
//            rbtn.setText(detail.getTitle());
//            rbtn.setId(i*i+i);
//            RadioGroup_settingDetailMenuBar.addView(rbtnView);
//            int finalI = i;
//            rbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    if(!isChecked) return;
//
//                    FragmentManager fm = getActivity().getSupportFragmentManager();
////                    getChildFragmentManager
//                    LinearLayout_detailItemView.removeAllViews();
//
//
//
//                    detail.getItems().forEach(item -> {
//                        SettingDetailItemType type = item.getType();
//                        FragmentTransaction ft = fm.beginTransaction();
//                        switch (type) {
//                            case SWITCH:
//                                SettingDetailItemSwitch itemSwitch = (SettingDetailItemSwitch)item;
//                                SettingDetailItemFragment itemFragment = SettingDetailItemFragment.newInstanceSwitch(itemSwitch.getTitle(),itemSwitch.getValue());
//                                ft.add(LinearLayout_detailItemView.getId(), itemFragment);
//                                ft.commitNow();
//                                View view1 = itemFragment.getView();
//                                Switch_itemValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                                    @Override
//                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                                        itemSwitch.setValue(isChecked);
//                                    }
//                                });
//                                break;
//                            default:
//                                break;
//                        }
//
//                   });
//                    //ft.commit();
//
//                }
//            });
//        }
//
//        TextView TextView_saveSetting = view.findViewById(R.id.TextView_save_setting);
//        TextView_saveSetting.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // 저장 버튼 누르면 json 저장
//                try {
//                    SharedPreferences.Editor editor = sharedPreference.edit();
//                    editor.putString("jsonStr", setting.toJson().toString());
//                    editor.commit();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        return view;
//    }
//
//    static final String DefJsonStr = "{\n" +
//            "    \"details\": [\n" +
//            "        {\n" +
//            "            \"title\": \"기본 정보\",\n" +
//            "            \"items\": [\n" +
//            "                {\n" +
//            "                    \"title\": \"속도\",\n" +
//            "                    \"type\": \"SWITCH\",\n" +
//            "                    \"value\": true\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"title\": \"시간\",\n" +
//            "                    \"type\": \"SWITCH\",\n" +
//            "                    \"value\": false\n" +
//            "                }\n" +
//            "            ]\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"title\": \"알림\",\n" +
//            "            \"items\": [\n" +
//            "                {\n" +
//            "                    \"title\": \"알림1\",\n" +
//            "                    \"type\": \"CHECKBOX\",\n" +
//            "                    \"value\": true\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"title\": \"알림 2\",\n" +
//            "                    \"type\": \"NONE\",\n" +
//            "                    \"value\": false\n" +
//            "                }\n" +
//            "            ]\n" +
//            "        }\n" +
//            "    ]\n" +
//            "}";
//}