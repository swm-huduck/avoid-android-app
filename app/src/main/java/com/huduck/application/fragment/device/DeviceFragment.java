package com.huduck.application.fragment.device;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.huduck.application.R;
import com.huduck.application.activity.DeviceDebugActivity;
import com.huduck.application.bleCentral.CentralCallback;
import com.huduck.application.device.DeviceService;
import com.huduck.application.fragment.PageFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceFragment extends PageFragment {
    private ImageView deviceStateImageView;
    private TextView deviceStateTextView;
    private TextView deviceStateDescriptionTextView;

    ImageView updateBtn;

    ListView deviceListView;

    DeviceService deviceService;
    boolean isService = false;

    DeviceListAdapter deviceListAdapter;

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            deviceService = binder.getService();

            changeDeviceConnectionState(deviceService.isConnected());
            deviceService.registerCentralCallback(centralCallback);
            deviceListAdapter = new DeviceListAdapter(getActivity());
            deviceListView.setAdapter(deviceListAdapter);

            if(deviceService.isConnected()) {
                moveConnectedDeviceToTop();
            }
            else {
                if (deviceService.isScanning())
                    setUpdateBtnAnimation(true);

                new Handler(Looper.getMainLooper()).post(() -> {
                    deviceListAdapter.updateList(deviceService.getScanDeviceList());
                    deviceListAdapter.notifyDataSetChanged();
                });
            }
            isService = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    public DeviceFragment() { }

    public static DeviceFragment newInstance() {
        DeviceFragment fragment = new DeviceFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device, container, false);

        deviceStateImageView = view.findViewWithTag("debug");
        deviceStateTextView = view.findViewWithTag("device_state_textview");
        deviceStateDescriptionTextView = view.findViewWithTag("device_state_description_textview");

        view.findViewWithTag("debug").setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if(deviceService == null) return;
                deviceService.updateCall("010-6294-5186", 1);
                deviceService.updateCall("?????????", 2);
                deviceService.updateSpeed(123);
                deviceService.updateSms("??????????????????.", "???????????????.????????? ???????????????. ???????????????. ?????????.??????????????????????????????");
                deviceService.updateKakaoTalk("??????????????????.", "?????????????????????.????????? ?????????????????????. ?????????????????????. ???????????????.????????????????????????????????????????????????????????????");
            }
        });

        view.findViewWithTag("debug").setOnLongClickListener(v -> {
            if(!deviceService.isConnected()) {
                Toast.makeText(getContext(), "???????????? ?????? ??? ?????? ????????????.", Toast.LENGTH_SHORT).show();
                return true;
            }
            startActivity(new Intent(getActivity(), DeviceDebugActivity.class));
            return true;
        });

        updateBtn = view.findViewWithTag("update");
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceService.refreshDeviceList();
            }
        });

        deviceListView = view.findViewById(R.id.device_list);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                deviceService.registerDevice((BluetoothDevice) deviceListAdapter.getItem(i));
            }
        });

        Intent intent = new Intent(
                getActivity(),
                DeviceService.class
        );

        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        return view;
    }

    private void setUpdateBtnAnimation(boolean value) {
        if(updateBtn == null) return;

        if(value) {
            Animation anim = AnimationUtils.loadAnimation(
                    getActivity(), // ?????? ????????? ????????????
                    R.anim.anim_rotate);    // ????????? ??????????????? ??????
            updateBtn.startAnimation(anim);
        }
        else
            updateBtn.clearAnimation();
    }

    private void moveConnectedDeviceToTop() {
        List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();

        for (BluetoothDevice device : deviceService.getScanDeviceList())
            if(device.getAddress().equals(deviceService.getRegisteredDeviceAddress()))
                bluetoothDeviceList.add(device);

        for (BluetoothDevice device : deviceService.getScanDeviceList())
            if(!bluetoothDeviceList.contains(device))
                bluetoothDeviceList.add(device);

        new Handler(Looper.getMainLooper()).post(() -> {
            deviceListAdapter.updateList(bluetoothDeviceList);
            deviceListAdapter.notifyDataSetChanged();
            deviceListView.smoothScrollToPosition(0);
        });
    }

    private class DeviceListAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> deviceList = new ArrayList<>();

        public DeviceListAdapter(Context context) {
            this.context = context;
        }

        public void clearDevice() {
            deviceList.clear();
            notifyDataSetChanged();
        }

        public void updateList(List<BluetoothDevice> bluetoothDeviceList) {
            deviceList = bluetoothDeviceList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.view_device_list_item, parent, false);
            }

            LinearLayout layout = convertView.findViewWithTag("device_list_item");

            TextView nameTextView = convertView.findViewWithTag("device_name");
            nameTextView.setText(deviceList.get(position).getName());

            TextView addressTextView = convertView.findViewWithTag("device_address");
            addressTextView.setText(deviceList.get(position).getAddress());

            BluetoothDevice device = (BluetoothDevice) getItem(position);
            if(deviceService.isConnected() && device.getAddress().equals(deviceService.getRegisteredDeviceAddress())) {
                layout.setBackground(getResources().getDrawable(R.drawable.bg_selected_round_white_box_100dp, context.getTheme()));
                nameTextView.setTextColor(getResources().getColor(R.color.indigo700, context.getTheme()));
//                nameTextView.setTypeface(null, Typeface.BOLD);
            }
            else {
                layout.setBackground(getResources().getDrawable(R.drawable.bg_unselected_round_white_box_100dp, context.getTheme()));
                nameTextView.setTextColor(getResources().getColor(R.color.default_text, context.getTheme()));
//                nameTextView.setTypeface(null, Typeface.NORMAL);
            }

            return convertView;
        }
    }

    private CentralCallback centralCallback = new CentralCallback() {
        @Override
        public void requestEnableBLE() {

        }

        @Override
        public void requestLocationPermission() {

        }

        @Override
        public void onStartScan() {
            setUpdateBtnAnimation(true);
            deviceListAdapter.clearDevice();
        }

        @Override
        public void onFindNewDevice(BluetoothDevice bluetoothDevice) {
            deviceListAdapter.updateList(deviceService.getScanDeviceList());
        }

        @Override
        public void onFinishScan(Map<String, BluetoothDevice> scanResult) {
            setUpdateBtnAnimation(false);
            updateBtn.clearAnimation();
        }

        @Override
        public void connectedGattServer() {
            setUpdateBtnAnimation(false);
            changeDeviceConnectionState(true);
            moveConnectedDeviceToTop();
        }

        @Override
        public void disconnectedGattServer() {
            changeDeviceConnectionState(false);
            new Handler(Looper.getMainLooper()).post(() -> {
                deviceListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onWrite() {

        }
    };

    private void changeDeviceConnectionState(boolean state) {
        new Handler(Looper.getMainLooper()).post(() -> {
            String stateText = state ? "HUD ?????? ??????" : "HUD ?????? ??????";
            String stateDescriptionText = state ? "??????????????? ?????????????????????." : "??????????????? ????????? ???????????????.";

            try {
                deviceStateImageView.setImageTintList(ColorStateList.valueOf(state ? getResources().getColor(R.color.indigo700, getActivity().getTheme()) : getResources().getColor(R.color.gray500, getActivity().getTheme())));
                deviceStateTextView.setText(stateText);
                deviceStateDescriptionTextView.setText(stateDescriptionText);
            }
            catch (IllegalStateException e) {}
        });
    }
}