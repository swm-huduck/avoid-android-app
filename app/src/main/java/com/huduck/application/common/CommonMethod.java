package com.huduck.application.common;

import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalTime;

public class CommonMethod {
    public static int dpToPx(Resources res, int dp) {
        float density = res.getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static double lerp(double a, double b, double t) {
        return a * (1-t) + b * t;
    }

    public static String subStringBytes(String str, int byteLength, int sizePerLetter) {
        int retLength = 0;
        int tempSize = 0;
        int asc;
        if (str == null || "".equals(str) || "null".equals(str)) {
            str = "";
        }

        int length = str.length();

        for (int i = 1; i <= length; i++) {
            asc = (int) str.charAt(i - 1);
            if (asc > 127) {
                if (byteLength >= tempSize + sizePerLetter) {
                    tempSize += sizePerLetter;
                    retLength++;
                }
            } else {
                if (byteLength > tempSize) {
                    tempSize++;
                    retLength++;
                }
            }
        }

        return str.substring(0, retLength);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static long LocalTimeToMiliSecond(LocalTime localTime) {
        return (long) (localTime.toNanoOfDay() * 1e-6);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static LocalTime MiliSecondToLocalTime(long miliSecond) {
        return LocalTime.ofNanoOfDay((long) (miliSecond * 1e+6));
    }
}
