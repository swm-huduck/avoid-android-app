package com.huduck.application.fragment.device;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.huduck.application.Navigation.NavigationProvider;
import com.huduck.application.R;
import com.huduck.application.activity.NavigationRoutesActivity;
import com.huduck.application.device.DeviceService;
import com.huduck.application.fragment.PageFragment;
import com.skt.Tmap.TMapPOIItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceFragment extends PageFragment {
    ListView deviceListView;

    DeviceService deviceService;
    boolean isService = false;

    private DeviceService.OnFinishedScanCallback onFinishedScanCallback = new DeviceService.OnFinishedScanCallback() {
        @Override
        public void onFinishedScan(Map<String, BluetoothDevice> scanResult) {
            deviceListView.setAdapter(new DeviceListAdapter(getActivity(), scanResult));
        }
    };

    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DeviceService.DeviceServiceBinder binder = (DeviceService.DeviceServiceBinder)service;
            deviceService = binder.getService();
            deviceService.registerOnFinishedScanCallback(onFinishedScanCallback);
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

        deviceListView = view.findViewById(R.id.device_list);

        Intent intent = new Intent(
                getActivity(),
                DeviceService.class
        );

        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);

        return view;
    }

    private class DeviceListAdapter extends BaseAdapter {
        Context context;
        Map<String, BluetoothDevice> deviceMap;
        List<BluetoothDevice> deviceList = new ArrayList<>();

        public DeviceListAdapter(Context context, Map<String, BluetoothDevice> deviceMap) {
            this.context = context;
            this.deviceMap = deviceMap;
            for (String address : deviceMap.keySet()) {
                deviceList.add(deviceMap.get(address));
            }
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

                TextView nameTextView = convertView.findViewWithTag("device_name");
                nameTextView.setText(deviceList.get(position).getName());

                TextView addressTextView = convertView.findViewWithTag("device_address");
                addressTextView.setText(deviceList.get(position).getAddress());

                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deviceService.registerDevice(deviceList.get(position));
                    }
                });
            }

            return convertView;
        }
    }
}