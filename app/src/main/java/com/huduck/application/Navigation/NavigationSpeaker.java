package com.huduck.application.Navigation;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NavigationSpeaker implements Navigator.OnProgressChangedCallback, Navigator.OnOffRouteCallback {
    TextToSpeech tts;
    private boolean ttsIsInitialized = false;

    public NavigationSpeaker(Context context) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.ERROR) {
                    Log.d("state", status+"");
                    return;
                }
                int result = tts.setLanguage(Locale.KOREAN);
                if(result ==TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return;
                ttsIsInitialized = true;
                onStart();
                Log.d("TTS", "INit!");
            }
        });
    }

    private List<NavigationPoint> evnets = new ArrayList<>();
    private boolean spookGoStraight = false;
    @Override
    public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter) {
        if(!ttsIsInitialized) return;

        int speechType = TextToSpeech.QUEUE_FLUSH;
        if(refreshed)
            speechType = TextToSpeech.QUEUE_ADD;

        if(nextTurnEventLeftDistanceMeter > 300 && !spookGoStraight) {
            tts.speak("다음 안내 전까지 직진합니다.", speechType, null, "");
            spookGoStraight = true;
            if(refreshed) refreshed = false;
        }
        if(nextTurnEventLeftDistanceMeter > 110) return;
        if(evnets.contains(nextTurnEvent)) return;

        StringBuilder stringBuilder = new StringBuilder(((int)(nextTurnEventLeftDistanceMeter / 10) * 10) + "미터 앞에서 ");
        String desc = stringBuilder.append(nextTurnEvent.getProperties().getDescription()).append("합니다.").toString();
        CharSequence text = desc;
        tts.speak(text, speechType, null, "id1");
        evnets.add(nextTurnEvent);
        spookGoStraight = false;
        if(refreshed) refreshed = false;
    }

    private boolean refreshed = false;
    @Override
    public void onOffRoute() {
        if(!ttsIsInitialized) return;
        tts.speak("경로를 재탐색합니다.", TextToSpeech.QUEUE_FLUSH, null, "");
        refreshed = true;
    }

    private void onStart() {
        if(!ttsIsInitialized) return;
        tts.speak("경로 안내를 시작합니다.", TextToSpeech.QUEUE_FLUSH, null, "");
        refreshed = true;
    }
}
