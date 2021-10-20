package com.huduck.application.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.huduck.application.R;
import com.huduck.application.databinding.ActivityDeviceDebugBinding;
import com.huduck.application.device.DeviceService;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import lombok.Builder;

public class DeviceDebugActivity extends AppCompatActivity {
    private ActivityDeviceDebugBinding binding;

    private DeviceService deviceService;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            deviceService = binder.getService();

            initActivity();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private List<Test> testList = new ArrayList<>();
    private List<String> callStateStringList = new ArrayList<String>(){{
        add("전화 종료");
        add("전화 옴");
        add("전화 중");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDebugBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = new Intent(
                this,
                DeviceService.class
        );

        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private void initActivity() {
        // sms 추가
        binding.addSms.setOnClickListener(v -> {
            Test test;

            if (binding.radioSms.isSelected()) {
                test = SmsTest.builder()
                        .deviceService(deviceService)
                        .nextDelay(!binding.delaySms.getText().toString().isEmpty() ? Integer.parseInt(binding.delaySms.getText().toString()) : 0)
                        .name(binding.smsName.getText().toString())
                        .content(binding.smsContent.getText().toString())
                        .build();

            }
            else {
                test = KakaoTest.builder()
                        .deviceService(deviceService)
                        .nextDelay(!binding.delaySms.getText().toString().isEmpty() ? Integer.parseInt(binding.delaySms.getText().toString()) : 0)
                        .name(binding.smsName.getText().toString())
                        .content(binding.smsContent.getText().toString())
                        .build();
            }
            addTestItem(test);
        });

        // 속도 추가
        binding.addSpeed.setOnClickListener(v -> {
            Test test = SpeedTest.builder()
                    .deviceService(deviceService)
                    .nextDelay(!binding.delaySpeed.getText().toString().isEmpty() ? Integer.parseInt(binding.delaySpeed.getText().toString()) : 0)
                    .value(binding.seekbarSpeed.getProgress())
                    .build();

            addTestItem(test);
        });

        // 전화 추가
        binding.addCall.setOnClickListener(v -> {
            Test test = CallTest.builder()
                    .deviceService(deviceService)
                    .nextDelay(!binding.delayCall.getText().toString().isEmpty() ? Integer.parseInt(binding.delayCall.getText().toString()) : 0)
                    .name(binding.callName.getText().toString())
                    .callState(binding.spinnerCall.getSelectedItemPosition())
                    .build();

            addTestItem(test);
        });

        // 속도 seekbar 값 디스플레이 업데이트
        binding.seekbarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                binding.viewerSpeed.setText(i + "km/h");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 스피너 아이템 추가
        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, callStateStringList);
        binding.spinnerCall.setAdapter(adapter);
        binding.spinnerCall.setSelection(0);

        // 테스트 리스트뷰
        binding.testList.setAdapter(testListAdapter);

        // 전송
        binding.send.setOnClickListener(v -> sendTest());
    }

    BaseAdapter testListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return testList.size();
        }

        @Override
        public Object getItem(int i) {
            return testList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            return ((Test) getItem(i)).getView(getApplicationContext(), viewGroup);
        }
    };

    public void addTestItem(Test test) {
        testList.add(test);
        test.setOnCancelListener(v -> removeTestItem(test));
        testListAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(binding.testList);
    }

    public void removeTestItem(Test test) {
        testList.remove(test);
        testListAdapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(binding.testList);
    }

    public void sendTest() {
        if(testList.size() == 0) return;

        binding.send.setEnabled(false);

        Handler handler = new Handler();

        Runnable send = new TimerTask() {
            int index = 0;

            @Override
            public void run() {
                Test test = testList.get(index);
                test.send();

                index++;
                if(index < testList.size())
                    handler.postDelayed(this, test.nextDelay * 1000);
                else
                    binding.send.setEnabled(true);
            }
        };

        handler.post(send);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        BaseAdapter listAdapter = (BaseAdapter) listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);

        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    abstract static class Test {
        protected Context context;
        protected DeviceService deviceService;
        protected int nextDelay = 0;
        protected View view;
        protected boolean initView = false;
        protected abstract void send();
        protected View.OnClickListener listener;

        public View getView(Context context, ViewGroup container){
            if(view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.view_device_debug_list_item, container, false);
                ((ImageButton) view.findViewWithTag("cancel")).setOnClickListener(v -> {
                    if(listener != null)
                        listener.onClick(v);
                });
            }

            return view;
        }

        public Test(DeviceService deviceService, int nextDelay) {
            this.deviceService = deviceService;
            this.nextDelay = nextDelay;
        }

        public void setOnCancelListener(View.OnClickListener listener) {
            this.listener = listener;
        }
    }

    private static class SmsTest extends Test {
        private String name;
        private String content;

        @Builder
        public SmsTest(DeviceService deviceService, int nextDelay, String name, String content) {
            super(deviceService, nextDelay);
            this.name = name == null ? "" : name;
            this.content = content == null ? "" : content;
        }

        @Override
        protected void send() {
            deviceService.updateSms(name, content);
        }

        @Override
        public View getView(Context context, ViewGroup container) {
            super.getView(context, container);
            if(!initView) {
                ((TextView) view.findViewWithTag("header")).setText("[SMS]");
                ((TextView) view.findViewWithTag("content")).setText("이름: " + name + ", 내용: " + content);
                ((TextView) view.findViewWithTag("delay")).setText(nextDelay+"초 뒤, 다음 실행");
                initView = true;
            }

            return view;
        }
    }

    private static class KakaoTest extends Test {
        private String name;
        private String content;

        @Builder
        public KakaoTest(DeviceService deviceService, int nextDelay, String name, String content) {
            super(deviceService, nextDelay);
            this.name = name == null ? "" : name;
            this.content = content == null ? "" : content;
        }

        @Override
        protected void send() {
            deviceService.updateKakaoTalk(name, content);
        }

        @Override
        public View getView(Context context, ViewGroup container) {
            super.getView(context, container);
            if(!initView) {
                ((TextView) view.findViewWithTag("header")).setText("[KakaoTalk]");
                ((TextView) view.findViewWithTag("content")).setText("이름: " + name + ", 내용: " + content);
                ((TextView) view.findViewWithTag("delay")).setText(nextDelay+"초 뒤, 다음 실행");
                initView = true;
            }
            return view;
        }
    }

    private static class CallTest extends Test {
        private String name;
        private int callState;

        @Builder
        public CallTest(DeviceService deviceService, int nextDelay, String name, int callState) {
            super(deviceService, nextDelay);
            this.name = name == null ? "" : name;
            this.callState = callState;
        }

        @Override
        protected void send() {
            deviceService.updateCall(name, callState);
        }

        @Override
        public View getView(Context context, ViewGroup container) {
            super.getView(context, container);
            if(!initView) {
                ((TextView) view.findViewWithTag("header")).setText("[Call]");
                ((TextView) view.findViewWithTag("content")).setText(
                        "이름: " + name +
                        ", 상태: "
                        + (callState == 0 ? "전화 종료" : callState == 1 ? "전화 옴" : "전화 중")
                );
                ((TextView) view.findViewWithTag("delay")).setText(nextDelay+"초 뒤, 다음 실행");
                initView = true;
            }
            return view;
        }
    }

    private static class SpeedTest extends Test {
        private int value;

        @Builder
        public SpeedTest(DeviceService deviceService, int nextDelay, int value) {
            super(deviceService, nextDelay);
            this.value = value;
        }

        @Override
        protected void send() {
            deviceService.updateSpeed(value);
        }

        @Override
        public View getView(Context context, ViewGroup container) {
            super.getView(context, container);
            if(!initView) {
                ((TextView) view.findViewWithTag("header")).setText("[Speed]");
                ((TextView) view.findViewWithTag("content")).setText(value + "km/h");
                ((TextView) view.findViewWithTag("delay")).setText(nextDelay+"초 뒤, 다음 실행");
                initView = true;
            }
            return view;
        }
    }
}