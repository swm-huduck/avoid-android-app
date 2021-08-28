package com.huduck.application.fragment.navigation;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huduck.application.Navigation.LocationProvider;
import com.huduck.application.common.CommonMethod;
import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.fragment.PageFragment;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.util.FusedLocationSource;
import com.skt.Tmap.TMapView;

public class NavigationMainFragment extends PageFragment {
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    public NavigationMainFragment() {}

    public static NavigationMainFragment newInstance(/*String param1, String param2*/) {
        NavigationMainFragment fragment = new NavigationMainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_navigation_main, container, false);

        // T MAP API KEY 등록
        TMapView tMapView = new TMapView(getActivity().getApplicationContext());
        tMapView.setSKTMapApiKey( getString(R.string.skt_map_api_key) );

        // 네이버 지도 현 위치 표시
        MapView mapView = view.findViewWithTag("map_view");
        mapView.getMapAsync(naverMap_ -> {
            naverMap = naverMap_;
            naverMap.getUiSettings().setLocationButtonEnabled(true);
            int[] paddings = naverMap.getContentPadding();
            naverMap.setContentPadding(paddings[0], CommonMethod.dpToPx(getResources(), 56) ,paddings[2], paddings[3]);

            locationSource = new FusedLocationSource(this, 1000);
            naverMap.setLocationSource(locationSource);
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

            naverMap.addOnLocationChangeListener(LocationProvider.locationChangeListener);
        });

        // 검색창 클릭 이벤트 등록
        View searchInput = view.findViewById(R.id.search_input);
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