package com.huduck.application.fragment.navigation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.fragment.PageFragment;
import com.huduck.application.service.NavigationService;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapGpsManager;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;

public class NavigationSearchResultFragment extends PageFragment {

    NavigationSearchResultListViewAdapter listViewAdapter;
    String searchWord;
    TMapView tMapView;
    TMapMarkerItem marker = new TMapMarkerItem();


    public NavigationSearchResultFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_search_result, container, false);
        tMapView = new TMapView(getActivity().getApplicationContext());
        marker.setPosition(0.5f, 1f);
        marker.setVisible(TMapMarkerItem.VISIBLE);
        tMapView.addMarkerItem("target_point_marker", marker);

        LinearLayout mapView = view.findViewWithTag("map");
        mapView.addView(tMapView);

        TextView searchInput = view.findViewWithTag("search_input");
        searchInput.setText(searchWord);

        searchInput.setOnClickListener(view_ -> {
            ((MainActivity)getActivity()).changeFragment(NavigationSearchFragment.class);
            return;
        });

        ImageButton backButton = view.findViewWithTag("back_button");
        backButton.setOnClickListener(view_ -> {
            ((MainActivity)getActivity()).changeFragment(NavigationMainFragment.class);
            return;
        });

        listViewAdapter = new NavigationSearchResultListViewAdapter(getActivity());
        ListView resultListView = view.findViewWithTag("result_list_view");
        resultListView.setAdapter(listViewAdapter);

        TMapData tMapData = new TMapData();


        resultListView.setOnItemClickListener((parent, view1, position, id) -> {
            getActivity().runOnUiThread(() -> {
                TMapPOIItem targetPoi = (TMapPOIItem) listViewAdapter.getItem(position);
                changeTMapViewByTargetPoi(targetPoi);

                listViewAdapter.selectItem(position);
                resultListView.setSelection(position);
                listViewAdapter.notifyDataSetChanged();

                resultListView.requestFocusFromTouch();
                resultListView.smoothScrollToPositionFromTop(position, 0);
            });
        });

        tMapData.findAroundKeywordPOI(NavigationService.GetNavigationInfo().getCurrentPoint(), searchWord, Integer.MAX_VALUE, 30, new TMapData.FindAroundKeywordPOIListenerCallback() {
            @Override
            public void onFindAroundKeywordPOI(ArrayList<TMapPOIItem> arrayList) {
                if(arrayList == null) {
                    Log.d("NavigationSearchResultFragment", "검색 결과 없음");
                    return;
                }

                if(arrayList.size() == 0) {
                    tMapData.findAllPOI(searchWord, 30, arrayList1 -> {
                        getActivity().runOnUiThread(() -> {

                            if(arrayList1 == null) return;
                            if(arrayList1.size() == 0) {
                                view.findViewWithTag("none_result_layout").setVisibility(View.VISIBLE);
                                view.findViewWithTag("destination_map_layout").setVisibility(View.GONE);
                            }
                            else {
                                view.findViewWithTag("destination_map_layout").setVisibility(View.VISIBLE);
                                view.findViewWithTag("none_result_layout").setVisibility(View.GONE);

                                arrayList1.forEach(poi -> {
                                    listViewAdapter.addItem(poi);
                                });

                                listViewAdapter.notifyDataSetChanged();

                                // init first item
                                Object firstItem = listViewAdapter.getItem(0);
                                if(firstItem == null) return;

                                TMapPOIItem targetPoi = (TMapPOIItem) firstItem;
                                changeTMapViewByTargetPoi(targetPoi);

                            }
                        });
                    });
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    view.findViewWithTag("destination_map_layout").setVisibility(View.VISIBLE);
                    view.findViewWithTag("none_result_layout").setVisibility(View.GONE);

                    arrayList.forEach(poi -> {
                        listViewAdapter.addItem(poi);
                    });

                    listViewAdapter.notifyDataSetChanged();

                    // init first item
                    Object firstItem = listViewAdapter.getItem(0);
                    if(firstItem == null) return;

                    TMapPOIItem targetPoi = (TMapPOIItem) firstItem;
                    changeTMapViewByTargetPoi(targetPoi);
                });
            }
        });


//        tMapData.findAroundNamePOI(NavigationService.GetNavigationInfo().getCurrentPoint(), searchWord, Integer.MAX_VALUE, 10, new TMapData.FindAroundNamePOIListenerCallback() {
//            @Override
//            public void onFindAroundNamePOI(ArrayList<TMapPOIItem> arrayList) {
//                if(arrayList == null) return;
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        arrayList.forEach(poi -> {
//                            listViewAdapter.addItem(poi);
//                        });
//
//                        listViewAdapter.notifyDataSetChanged();
//                    }
//                });
//            }
//        });

//        tMapData.findAllPOI(searchWord, new TMapData.FindAllPOIListenerCallback() {
//            @Override
//            public void onFindAllPOI(ArrayList<TMapPOIItem> arrayList) {
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        arrayList.forEach(poi -> {
//                            listViewAdapter.addItem(poi);
//                        });
//
//                        listViewAdapter.notifyDataSetChanged();
//                    }
//                });
//            }
//        });

        return view;
    }

    private void changeTMapViewByTargetPoi(TMapPOIItem targetPoi) {
        TMapPoint targetPoint = targetPoi.getPOIPoint();
        marker.setTMapPoint(targetPoint);
        tMapView.setCenterPoint(targetPoint.getLongitude(), targetPoint.getLatitude());
    }

    @Override
    protected void onInit(Bundle bundle) {
        super.onInit(bundle);
        searchWord = bundle.getString("search_word");
        Log.d("SearchWord: ", searchWord);
    }

    public static class NavigationSearchResultListViewAdapter extends BaseAdapter {

        private ArrayList<TMapPOIItem> poiItems = new ArrayList<>();
        private int selectedItemPosition = 0;
        private Activity activity;
        private TMapView map;

        NavigationSearchResultListViewAdapter(Activity activity) {
            this.activity = activity;
            map = new TMapView(activity.getApplicationContext());
        }

        @Override
        public int getCount() {
            return poiItems.size();
        }

        @Override
        public Object getItem(int position) {
            if(poiItems.size() == 0) return null;
            return poiItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void addItem(TMapPOIItem poiItem) {
            poiItems.add(poiItem);
        }

        public void selectItem(int position) {
            selectedItemPosition = position;
        }

        //@SuppressLint("SetTextI18n")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.fragment_navigation_search_result_item, parent, false);
            }

            TMapPOIItem poi = poiItems.get(position);

            View item = convertView.findViewWithTag("navigation_search_result_item");

            // 이름
            TextView name = item.findViewWithTag("poi_name");
            name.setText(poi.getPOIName());
            if(selectedItemPosition == position)
                name.setTypeface(null, Typeface.BOLD);
            else
                name.setTypeface(null, Typeface.NORMAL);

            // 거리
            TextView distanceView = item.findViewWithTag("poi_distance");
            TMapPoint currentPoint = NavigationService.GetNavigationInfo().getCurrentPoint();
            if(currentPoint != null) {
                double distance = poi.getDistance(currentPoint);

                if(distance >= 1000.0) {
                    distance /= 1000.0;
                    distance = Math.round(distance * 10) / 10.0;
                    distanceView.setText(distance + "km");
                }
                else {
                    distanceView.setText((int)distance + "m");     // !!단위 확인하자
                }
            }

            // 업종
            TextView business = item.findViewWithTag("poi_business");
            business.setText(poi.lowerBizName);

            // 주소
            TextView addressView = item.findViewWithTag("poi_address");
            String address =
                    poi.upperAddrName
                    + ( (poi.middleAddrName != null) ? (" " + poi.middleAddrName) : "" )
                    + ( (poi.roadName != null) ? (" " + poi.roadName
                        + ( (poi.buildingNo1 != null) ? (" " + poi.buildingNo1
                            + ( (poi.buildingNo2 != null && !poi.buildingNo2.equals("0")) ? ("-" + poi.buildingNo2) : "" )
                        ) : "" )
                    ) : "" );
            addressView.setText(address);


//            // Map
//            if(selectedItemPosition == position) {
//                LinearLayout mapView = item.findViewWithTag("map");
////                TMapView tMapView = new TMapView(activity.getApplicationContext());
////                tMapView.setSKTMapApiKey( "l7xxf21cc9e0068d4fbbb7c939aa6bda5a25" );
//
//                if(map.getParent() != null)
//                    ((ViewGroup) map.getParent()).removeViewInLayout(map);
//                mapView.addView( map );
//
////
////                Bitmap markerBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.icon_location);
////                TMapPoint selectedPoint = poi.getPOIPoint();
////

////
////                tMapView.removeAllMarkerItem();
////                tMapView.addMarkerItem("myMarker", mapMarker);
////                tMapView.setCenterPoint(selectedPoint.getLongitude(), selectedPoint.getLatitude());
//            }

            return convertView;
        }
    }
}