package com.huduck.application.common;

import android.util.Log;

public class CLog {
    public static void d(String tag, String log) {
        final int MAX_LEN = 2000;
        int len = log.length();
        if(len > MAX_LEN) {
            int idx = 0, nextIdx = 0;
            while (idx < len) {
                if(idx != 0) tag = "";

                nextIdx += MAX_LEN;
                Log.d(tag, log.substring(idx, nextIdx > len ? len : nextIdx));
                idx = nextIdx;
            }
        }
        else {
            Log.d(tag, log);
        }
    }
}
