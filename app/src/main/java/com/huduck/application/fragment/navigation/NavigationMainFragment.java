package com.huduck.application.fragment.navigation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

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
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

public class NavigationMainFragment extends PageFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NavigationMainFragment() {
        // Required empty public constructor
    }

    public static NavigationMainFragment newInstance(/*String param1, String param2*/) {
        NavigationMainFragment fragment = new NavigationMainFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
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

        LinearLayout linearLayoutTmap = view.findViewById(R.id.LinearLayout_map);
        TMapView tMapView = new TMapView(view.getContext());

        tMapView.setSKTMapApiKey( "l7xxf21cc9e0068d4fbbb7c939aa6bda5a25" );
        linearLayoutTmap.addView( tMapView );

        TMapMarkerItem mapMarker = new TMapMarkerItem();
        mapMarker.setPosition(0.5f, 1f);
        mapMarker.setVisible(TMapMarkerItem.VISIBLE);


        tMapView.addMarkerItem("myMarker", mapMarker);
        Fragment fragment = this;

        NavigationService.addNavigationLocationChangeEvent((currentPoint, info) -> {
            if(!fragment.isVisible()) return;
            mapMarker.setTMapPoint(currentPoint);
            tMapView.setCenterPoint(currentPoint.getLongitude(), currentPoint.getLatitude());
        });

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