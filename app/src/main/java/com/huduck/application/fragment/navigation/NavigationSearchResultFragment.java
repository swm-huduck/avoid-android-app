package com.huduck.application.fragment.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.huduck.application.Navigation.LocationProvider;
import com.huduck.application.Navigation.NavigationProvider;
import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.activity.NavigationRoutesActivity;
import com.huduck.application.fragment.PageFragment;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

public class NavigationSearchResultFragment extends PageFragment {
    NavigationSearchResultListViewAdapter listViewAdapter;
    String searchWord;

    private MapView mapView = null;
    private NaverMap naverMap = null;
    private Marker marker = new Marker();
    TMapData tMapData = new TMapData();

    private TMapPOIItem selectedPoi = null;

    public NavigationSearchResultFragment() {
    }

    public static NavigationSearchResultFragment newInstance() {
        NavigationSearchResultFragment fragment = new NavigationSearchResultFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    private void initNaverMap(NaverMap naverMap_) {
        naverMap = naverMap_;
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setLocationButtonEnabled(false);

        marker.setPosition(new LatLng(0, 0));
        new Handler().post(() -> {
            marker.setMap(naverMap);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation_search_result, container, false);

        mapView = view.findViewWithTag("map_view");

        mapView.getMapAsync(naverMap_ -> initNaverMap(naverMap_));

        // 검색어 표시
        TextView searchInput = view.findViewWithTag("search_input");
        searchInput.setText(searchWord);

        // 검색창 클릭 이벤트 등록
        searchInput.setOnClickListener(view_ -> {
            ((MainActivity) getActivity()).changeFragment(NavigationSearchFragment.class);
            return;
        });

        // 뒤로가기 버튼 클릭 이벤트 등록
        ImageButton backButton = view.findViewWithTag("back_button");
        backButton.setOnClickListener(view_ -> {
            ((MainActivity) getActivity()).changeFragment(NavigationMainFragment.class);
            return;
        });

        // 결과 리스트 어댑터 연결
        listViewAdapter = new NavigationSearchResultListViewAdapter(getActivity());
        ListView resultListView = view.findViewWithTag("result_list_view");
        resultListView.setAdapter(listViewAdapter);

        // 리스트 아이템 클릭 이벤트 등록
        resultListView.setOnItemClickListener((parent, view1, position, id) -> {
            TMapPOIItem targetPoi = (TMapPOIItem) listViewAdapter.getItem(position);
            selectedPoi = targetPoi;

            getActivity().runOnUiThread(() -> {
                updateNaverMapByTargetPoi(targetPoi);

                listViewAdapter.selectItem(position);
                resultListView.setSelection(position);
                listViewAdapter.notifyDataSetChanged();

                resultListView.requestFocusFromTouch();
                resultListView.smoothScrollToPositionFromTop(position, 0);
            });
        });

        // 경로 검색 및 리스트에 아이템 추가
        Location currentLocation = LocationProvider.getLastRowLocation(); // 현재위치 // NavigationManager.getInstance().getCurrentRowLocation();
        tMapData.findAroundKeywordPOI(
                new TMapPoint(currentLocation.getLatitude(), currentLocation.getLongitude()),
                searchWord, Integer.MAX_VALUE, 30,
                new TMapData.FindAroundKeywordPOIListenerCallback() {
                    @Override
                    public void onFindAroundKeywordPOI(ArrayList<TMapPOIItem> arrayList) {
                        if (arrayList == null) {
                            Log.d("NavigationSearchResultFragment", "검색 결과 없음");
                            return;
                        }

                        if (arrayList.size() == 0) {
                            tMapData.findAllPOI(searchWord, 30, arrayList1 -> {
                                getActivity().runOnUiThread(() -> {

                                    if (arrayList1 == null) return;
                                    if (arrayList1.size() == 0) {
                                        view.findViewWithTag("none_result_layout").setVisibility(View.VISIBLE);
                                        view.findViewWithTag("destination_map_layout").setVisibility(View.GONE);
                                    } else {
                                        view.findViewWithTag("destination_map_layout").setVisibility(View.VISIBLE);
                                        view.findViewWithTag("none_result_layout").setVisibility(View.GONE);

                                        arrayList1.forEach(poi -> {
                                            listViewAdapter.addItem(poi);
                                        });

                                        listViewAdapter.notifyDataSetChanged();

                                        // init first item
                                        Object firstItem = listViewAdapter.getItem(0);
                                        if (firstItem == null) return;

                                        TMapPOIItem targetPoi = (TMapPOIItem) firstItem;
                                        selectedPoi = targetPoi;
                                        updateNaverMapByTargetPoi(targetPoi);

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
                            if (firstItem == null) return;

                            TMapPOIItem targetPoi = (TMapPOIItem) firstItem;
                            selectedPoi = targetPoi;
                            updateNaverMapByTargetPoi(targetPoi);
                        });
                    }
                });

        return view;
    }

    private void updateNaverMapByTargetPoi(TMapPOIItem targetPoi) {
        if (naverMap == null) {
            mapView.getMapAsync(naverMap_ -> {
                initNaverMap(naverMap_);
                updateNaverMapByTargetPoi(targetPoi);
            });
            return;
        }

        TMapPoint targetPoint = targetPoi.getPOIPoint();
        LatLng latLng = new LatLng(targetPoint.getLatitude(), targetPoint.getLongitude());

        marker.setPosition(latLng);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(latLng);
        naverMap.moveCamera(cameraUpdate);
    }

    @Override
    protected void onInit(Bundle bundle) {
        super.onInit(bundle);
        searchWord = bundle.getString("search_word");
        Log.d("SearchWord: ", searchWord);
    }

    public class NavigationSearchResultListViewAdapter extends BaseAdapter {

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
            if (poiItems.size() == 0) return null;
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

        @SuppressLint("ResourceAsColor")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View item;
            TextView name;
            LinearLayout decideDestinationBtn;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.fragment_navigation_search_result_item, parent, false);

                TMapPOIItem poi = poiItems.get(position);

                item = convertView.findViewWithTag("navigation_search_result_item");

                // 이름
                name = item.findViewWithTag("poi_name");
                name.setText(poi.getPOIName());

                // 업종
                TextView business = item.findViewWithTag("poi_business");
                business.setText(poi.lowerBizName);

                // 주소
                TextView addressView = item.findViewWithTag("poi_address");
                String address =
                        poi.upperAddrName
                                + ((poi.middleAddrName != null) ? (" " + poi.middleAddrName) : "")
                                + ((poi.roadName != null) ? (" " + poi.roadName
                                + ((poi.buildingNo1 != null) ? (" " + poi.buildingNo1
                                + ((poi.buildingNo2 != null && !poi.buildingNo2.equals("0")) ? ("-" + poi.buildingNo2) : "")
                        ) : "")
                        ) : "");
                addressView.setText(address);

                // 목적지 버튼
                decideDestinationBtn = item.findViewWithTag("decide_destination_btn");
                decideDestinationBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity().getApplicationContext(), NavigationRoutesActivity.class);
                    intent.putExtra("target_poi_lat", poi.getPOIPoint().getLatitude());
                    intent.putExtra("target_poi_lng", poi.getPOIPoint().getLongitude());
                    NavigationProvider.setDestination(poi);
                    getActivity().startActivity(intent);
                });
            } else {
                item = convertView.findViewWithTag("navigation_search_result_item");
                name = item.findViewWithTag("poi_name");
                decideDestinationBtn = item.findViewWithTag("decide_destination_btn");
            }


            if (selectedItemPosition == position) {
                item.setBackgroundResource(R.drawable.border_bottom_gray_1px);
                name.setTypeface(null, Typeface.BOLD);
                name.setTextColor(getResources().getColor(R.color.indigo700));
                decideDestinationBtn.setVisibility(View.VISIBLE);
            } else {
                item.setBackgroundResource(R.drawable.bg_navigation_search_result_item_layout_idle);
                name.setTypeface(null, Typeface.NORMAL);
                name.setTextColor(getResources().getColor(R.color.default_text));
                decideDestinationBtn.setVisibility(View.GONE);
            }

            return convertView;
        }
    }
}