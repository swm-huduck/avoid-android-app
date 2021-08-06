package com.huduck.application.fragment.navigation;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.huduck.application.Navigation.NavigationSearchItem;
import com.huduck.application.R;
import com.huduck.application.activity.MainActivity;
import com.huduck.application.fragment.PageFragment;
import com.skt.Tmap.TMapData;

import java.util.ArrayList;

public class NavigationSearchFragment extends PageFragment implements TextWatcher{
    ListView listView;
    NavigationSearchListViewAdapter adapter;

    private View view;
    TMapData tMapData = new TMapData();

    public NavigationSearchFragment() {}

    public static NavigationSearchFragment newInstance() {
        NavigationSearchFragment fragment = new NavigationSearchFragment();
        return fragment;
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
        searchInput.addTextChangedListener(this);

        // 연관 검색어 클릭 이벤트
        NavigationSearchFragment it = this;
        adapter = new NavigationSearchListViewAdapter(getActivity());
        listView = view.findViewById(R.id.navigation_search_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            NavigationSearchItem item = adapter.getSearchItem(position);
            String poiName = item.getPoiName();

            // 검색
            searchInput.removeTextChangedListener(it);
            searchInput.setText(poiName);
            searchInput.setSelection(searchInput.length());
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
        if (!hidden) {
            EditText searchInput = view.findViewWithTag("search_input");
            searchInput.addTextChangedListener(this);
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchInput.requestFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);

                }
            },30);
        super.onHiddenChanged(hidden);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void afterTextChanged(Editable s) {

        tMapData.autoComplete(s.toString(), arrayList -> getActivity().runOnUiThread(() -> {
            adapter.removeAllItem();

            if(arrayList == null) return;
            arrayList.forEach(item -> {
                adapter.addItem(item);
            });

            adapter.notifyDataSetChanged();
        }));
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
