package com.huduck.application.fragment.navigation;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.huduck.application.NavigationSearchItem;
import com.huduck.application.NavigationSearchResultItemFragment;
import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.fragment.PageFragment;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class NavigationSearchFragment extends PageFragment {
    ListView listView;
    NavigationSearchListViewAdapter adapter;

    private View view;

    public NavigationSearchFragment() {
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
        view = inflater.inflate(R.layout.fragment_navigation_search, container, false);

        // 검색 입력창에 포커스 상태로 놓고, 키보드 활성화
        EditText searchInput = view.findViewWithTag("search_input");
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                searchInput.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        },30);

        // 검색어 바뀔때 마다 연관 검색어 자동 생성
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                TMapData tMapData = new TMapData();
                tMapData.autoComplete(s.toString(), arrayList -> getActivity().runOnUiThread(() -> {
                    adapter.removeAllItem();

                    if(arrayList == null) return;
                    arrayList.forEach(item -> {
                        adapter.addItem(item);
                    });

                    adapter.notifyDataSetChanged();
                }));
            }
        });

        // 연관 검색어 클릭 이벤트
        adapter = new NavigationSearchListViewAdapter(getActivity());
        listView = view.findViewById(R.id.navigation_search_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            NavigationSearchItem item = adapter.getSearchItem(position);
            String poiName = item.getPoiName();
            searchInput.setText(poiName);
            searchInput.setSelection(searchInput.length());
            // 검색
            search(poiName);
        });

        // 엔터 버튼 눌렀을 때
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                String searchWord = searchInput.getText().toString();
                search(searchWord);
            }
            return true;
        });

        // 뒤로가기 버튼 클릭 이벤트
        ImageButton backButton = view.findViewWithTag("back_button");
        backButton.setOnClickListener(view_ -> {
            ((MainActivity)getActivity()).changeFragment(NavigationMainFragment.class);
            return;
        });

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void search(String searchWord) {
        Bundle bundle = new Bundle();
        bundle.putString("search_word", searchWord);
        ((MainActivity) getActivity()).changeFragment(NavigationSearchResultFragment.class, bundle,true);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            EditText searchInput = view.findViewWithTag("search_input");
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchInput.requestFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);

                }
            },30);
        }
    }

    // 검색어 리스트 어댑터
    public static class NavigationSearchListViewAdapter extends BaseAdapter {

        private ArrayList<NavigationSearchItem> searchItems = new ArrayList<>();
        private Activity activity;

        NavigationSearchListViewAdapter(Activity activity) {
            this.activity = activity;
        }

        @Override
        public int getCount() {
            return searchItems.size();
        }

        @Override
        public Object getItem(int position) {
            return searchItems.get(position);
        }

        public NavigationSearchItem getSearchItem(int position) {
            return searchItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void addItem(String poiName) {
            NavigationSearchItem item = new NavigationSearchItem();
            item.setPoiName(poiName);

            searchItems.add(item);
        }

        public void removeAllItem() {
            searchItems.clear();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.fragment_navigation_search_item, parent, false);
            }

            TextView searchText = convertView.findViewWithTag("search_text");
            NavigationSearchItem item = searchItems.get(position);
            searchText.setText(item.getPoiName());

            return convertView;
        }
    }
}
