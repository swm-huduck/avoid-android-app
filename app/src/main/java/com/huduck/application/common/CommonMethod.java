package com.huduck.application.common;

import android.content.res.Resources;

public class CommonMethod {
    public static int dpToPx(Resources res, int dp) {
        float density = res.getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
