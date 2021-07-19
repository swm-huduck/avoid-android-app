package com.huduck.application.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

public class PageFragment extends Fragment {
    protected Bundle initBundle = null;

    public final void init(Bundle bundle) {
        initBundle = bundle;
        onInit(bundle);
    }

    protected void onInit(Bundle bundle) {}
}
