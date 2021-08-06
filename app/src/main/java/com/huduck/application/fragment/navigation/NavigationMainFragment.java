package com.huduck.application.fragment.navigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.fragment.PageFragment;
import com.huduck.application.service.NavigationService;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.util.FusedLocationSource;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

public class NavigationMainFragment extends PageFragment {
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public NavigationMainFragment() {}

    public static NavigationMainFragment newInstance(/*String param1, String param2*/) {
        NavigationMainFragment fragment = new NavigationMainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_navigation_main, container, false);

        // T MAP API KEY 등록
        TMapView tMapView = new TMapView(view.getContext());
        tMapView.setSKTMapApiKey( getString(R.string.skt_map_api_key) );

        // 네이버 지도 현 위치 표시
        MapView mapView = view.findViewWithTag("map_view");
        mapView.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            naverMap.getUiSettings().setLocationButtonEnabled(true);
            locationSource = new FusedLocationSource(this, 1000);
            naverMap.setLocationSource(locationSource);
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            naverMap.getLocationOverlay().setVisible(true);
        });

        // 검색창 클릭 이벤트 등록
        TextView searchInput = view.findViewById(R.id.search_input);
        searchInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity)getActivity();
                mainActivity.changeFragment(NavigationSearchFragment.class, true);
            }
        });

        return view;
    }
}