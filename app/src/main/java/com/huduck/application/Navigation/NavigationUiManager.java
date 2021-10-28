package com.huduck.application.Navigation;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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
        // 속도 업데이트
        Runnable runnable = new TimerTask() {
            @Override
            public void run() {
                int speedKh = (int) (location.getSpeed() * 3.6);
                binding.currentSpeed.setText(speedKh + "");
            }
        };
        activity.runOnUiThread(runnable);

        // 현재 위치(주소) 업데이트
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

        // 예상 도착 시간
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
                // 다음 회전 이벤트
                String turnEvent = NavigationPoint.TurnType.get(nextTurnEvent.getProperties().getTurnType());
                binding.nextTurnEvent.setText(turnEvent);

                int nextTurnEventLeftDistance = (int) (Math.floor(nextTurnEventLeftDistanceMeter / 10) * 10);   // 10m 단위로 변환
                if(nextTurnEventLeftDistance > 1000) {
                    binding.nextTurnEventLeftDistance.setText(
                            String.format(
                                    "%.2f",
                                    Math.round((nextTurnEventLeftDistance / 1000.0) * 100) / 100.0  // 소수점 아래 2자리까지
                            ) + "km"
                    );
                }
                else {
                    binding.nextTurnEventLeftDistance.setText(nextTurnEventLeftDistance + "m");
                }

                // 다다음 회전 이벤트
                int nextNextTurnEventLeftDistance = (int) (Math.floor(nextNextTurnEventLeftDistanceMeter / 10) * 10);   // 10m 단위로 변환
                if(nextNextTurnEventLeftDistance > 1000) {
                    binding.nextNextTurnEventLeftDistance.setText(
                            String.format(
                                    "%.1f",
                                    Math.round((nextNextTurnEventLeftDistance / 1000.0) * 10) / 10.0    // 소수점 아래 1자리까지
                            ) + "km"
                    );
                }
                else {
                    binding.nextNextTurnEventLeftDistance.setText(nextNextTurnEventLeftDistance + "m");
                }

                // 총 남은 거리
                int totalLeftDistance = (int) (routeTotalDistance - (totalProgress * routeTotalDistance));
                if(totalLeftDistance > 1000) {
                    binding.leftDistanceUnit.setText("km");
                    String text = String.format("%.1f", Math.round((totalLeftDistance / 1000.0) * 10) / 10.0);    // 소수점 아래 1자리까지
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

    private String destination = "";
    public void setDestination(String destination) {
        this.destination = destination;
        swapBottomInfoBar();
    }
}
