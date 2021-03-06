package com.huduck.application.Navigation;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;

import com.huduck.application.R;
import com.huduck.application.databinding.ActivityNavigationTestBinding;
import com.naver.maps.map.NaverMap;
import com.skt.Tmap.TMapAddressInfo;
import com.skt.Tmap.TMapData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import okhttp3.internal.http2.Header;

public class NavigationUiManager
        implements NaverMap.OnLocationChangeListener, Navigator.OnProgressChangedCallback,
                    Navigator.OnRouteChangedCallback{

    private Activity activity;
    private ActivityNavigationTestBinding binding;
    private TMapData tMapData;

    private Handler handler = new Handler();

    public NavigationUiManager(Activity activity, ActivityNavigationTestBinding binding, TMapData tMapData) {
        this.activity = activity;
        this.binding = binding;
        this.tMapData = tMapData;
    }

    private boolean startedSwap = false;
    private void swapBottomInfoBar() {
        if(startedSwap) return;
        startedSwap = true;

        Runnable runnable = new TimerTask() {
            boolean displayCurrentAddress = false;

            @Override
            public void run() {
                if(displayCurrentAddress) {
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            binding.bottomInformationBarIcon.setImageResource(R.drawable.ic_current_location);
                            binding.bottomInformationBarText.setText(currentAddress);
                        }
                    });
                }
                else {
                    activity.runOnUiThread(new TimerTask() {
                        @Override
                        public void run() {
                            binding.bottomInformationBarIcon.setImageResource(R.drawable.ic_navigation_mark);
                            binding.bottomInformationBarText.setText(destination);
                        }
                    });
                }

                displayCurrentAddress = !displayCurrentAddress;
                handler.postDelayed(this, 10000);
            }
        };

        handler.post(runnable);
    }

    private int updateAddressCnt = 0;
    private int updateAddressOrigin = 30;
    private String currentAddress = "";
    @Override
    public void onLocationChange(@NonNull Location location) {
        // ?????? ????????????
        Runnable runnable = new TimerTask() {
            @Override
            public void run() {
                int speedKh = (int) (location.getSpeed() * 3.6);
                binding.currentSpeed.setText(speedKh + "");
            }
        };
        activity.runOnUiThread(runnable);

        // ?????? ??????(??????) ????????????
        updateAddressCnt--;
        if (updateAddressCnt > 0) return;

        updateAddressCnt = updateAddressOrigin;
        tMapData.reverseGeocoding(location.getLatitude(), location.getLongitude(), "A02", new TMapData.reverseGeocodingListenerCallback() {
            @Override
            public void onReverseGeocoding(TMapAddressInfo tMapAddressInfo) {
                if (tMapAddressInfo == null) return;
                currentAddress = tMapAddressInfo.strFullAddress;
                swapBottomInfoBar();
            }
        });
    }

    double routeTotalDistance = 0;
    @Override
    public void onRouteChanged(NavigationRoutes route, double routeTotalDistance, List<Navigator.NavigatorLineStringSegment> navigatorLineStringSegmentList) {
        this.routeTotalDistance = routeTotalDistance;

        // ?????? ?????? ??????
        Date arrivedDate = new Date(System.currentTimeMillis() + (route.getTotalTime() * 1000));
        activity.runOnUiThread(new TimerTask() {
            @Override
            public void run() {
                SimpleDateFormat format = new SimpleDateFormat("hh:mm");
                binding.arrivedTime.setText(format.format(arrivedDate));

                format = new SimpleDateFormat("a");
                binding.amOrPm.setText(format.format(arrivedDate));
            }
        });
    }

    @Override
    public void onProgressChanged(double totalProgress, NavigationPoint nextTurnEvent, double nextTurnEventLeftDistanceMeter, NavigationTurnEventCalc.NavigationTurnEventData nextTurnEventData, NavigationPoint nextNextTurnEvent, double nextNextTurnEventLeftDistanceMeter) {
        Runnable runnable = new TimerTask() {
            @Override
            public void run() {
                // ?????? ?????? ?????????
                int turnEventIndex = nextTurnEvent.getProperties().getTurnType();
                String turnEvent = NavigationPoint.TurnType.get(turnEventIndex);
                binding.nextTurnEvent.setText(turnEvent);

                int nextTurnEventLeftDistance = (int) (Math.floor(nextTurnEventLeftDistanceMeter / 10) * 10);   // 10m ????????? ??????
                if(nextTurnEventLeftDistance > 1000) {
                    binding.nextTurnEventLeftDistance.setText(
                            String.format(
                                    "%.2f",
                                    Math.round((nextTurnEventLeftDistance / 1000.0) * 100) / 100.0  // ????????? ?????? 2????????????
                            ) + "km"
                    );

                }
                else {
                    binding.nextTurnEventLeftDistance.setText(nextTurnEventLeftDistance + "m");
                }

                if(NavigationPoint.TurnIcon.containsKey(turnEventIndex)) {
                    binding.nextTurnEventIcon.setImageResource(NavigationPoint.TurnIcon.get(turnEventIndex));
                }
                else {
                    binding.nextTurnEventIcon.setImageResource(R.drawable.icon_null);
                }

                // ????????? ?????? ?????????
                if(nextNextTurnEvent != null) {
                    int nextNextTurnEventIndex = nextNextTurnEvent.getProperties().getTurnType();
                    int nextNextTurnEventLeftDistance = (int) (Math.floor(nextNextTurnEventLeftDistanceMeter / 10) * 10);   // 10m ????????? ??????
                    if (nextNextTurnEventLeftDistance > 1000) {
                        binding.nextNextTurnEventLeftDistance.setText(
                                String.format(
                                        "%.1f",
                                        Math.round((nextNextTurnEventLeftDistance / 1000.0) * 10) / 10.0    // ????????? ?????? 1????????????
                                ) + "km"
                        );
                    } else {
                        binding.nextNextTurnEventLeftDistance.setText(nextNextTurnEventLeftDistance + "m");
                    }

                    if (NavigationPoint.TurnIcon.containsKey(nextNextTurnEventIndex)) {
                        binding.nextNextTurnEventIcon.setImageResource(NavigationPoint.TurnIcon.get(nextNextTurnEventIndex));
                    }
                    else {
                        binding.nextNextTurnEventIcon.setImageResource(R.drawable.icon_null);
                    }
                }
                else {
                    binding.nextNextTurnEventLayout.setVisibility(View.INVISIBLE);
                }

                // ??? ?????? ??????
                int totalLeftDistance = (int) (routeTotalDistance - (totalProgress * routeTotalDistance));
                if(totalLeftDistance > 1000) {
                    binding.leftDistanceUnit.setText("km");
                    String text = String.format("%.1f", Math.round((totalLeftDistance / 1000.0) * 10) / 10.0);    // ????????? ?????? 1????????????
                    binding.leftDistance.setText(text);
                }
                else {
                    binding.leftDistanceUnit.setText("m");
                    binding.leftDistance.setText(String.format("%d", totalLeftDistance));
                }
            }
        };
        activity.runOnUiThread(runnable);
    }

    private String destination = "????????????";
    public void setDestination(String destination) {
        this.destination = destination;
        swapBottomInfoBar();
    }
}
