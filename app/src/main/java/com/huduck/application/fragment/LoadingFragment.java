package com.huduck.application.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.huduck.application.R;

public class LoadingFragment extends Fragment {
    RelativeLayout loadingLayout;
    ImageView logo;

    public LoadingFragment() {}

    public static LoadingFragment newInstance() {
        LoadingFragment fragment = new LoadingFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loading, container, false);

        loadingLayout = view.findViewWithTag("loading_layout");
        logo = view.findViewWithTag("logo");

        // Loading
        Glide.with(this)
                .asGif()    // GIF 로딩
                .load(R.raw.logo_loading)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)    // Glide에서 캐싱한 리소스와 로드할 리소스가 같을때 캐싱된 리소스 사용
                .into(logo);

        // Loading 뒤에 터치 무시
        loadingLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        return view;
    }

    public void isVisible(boolean value) {
        loadingLayout.setVisibility(value ? View.VISIBLE : View.GONE);
    }
}