package com.huduck.application.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.huduck.application.R;
import com.huduck.application.databinding.ActivityMapboxTestBinding;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.bindgen.Expected;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.StyleManagerInterface;
import com.mapbox.maps.extension.localization.LocalizationKt;
import com.mapbox.maps.plugin.LocationPuck;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImpl;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPluginImpl;
import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions;
import com.mapbox.navigation.base.formatter.UnitType;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.RouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.base.route.RouterOrigin;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer;
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi;
import com.mapbox.navigation.ui.maneuver.model.Maneuver;
import com.mapbox.navigation.ui.maneuver.model.ManeuverError;
import com.mapbox.navigation.ui.maps.camera.NavigationCamera;
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource;
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler;
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState;
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver;
import com.mapbox.navigation.ui.maps.camera.transition.MapboxNavigationCameraStateTransition;
import com.mapbox.navigation.ui.maps.camera.transition.MapboxNavigationCameraTransition;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi;
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView;
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError;
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions;
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine;
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError;
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue;
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi;
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter;
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter;
import com.mapbox.navigation.ui.tripprogress.model.PercentDistanceTraveledFormatter;
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter;
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter;
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi;
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer;
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement;
import com.mapbox.navigation.ui.voice.model.SpeechError;
import com.mapbox.navigation.ui.voice.model.SpeechValue;
import com.mapbox.navigation.ui.voice.model.SpeechVolume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;

public class MapboxTest extends AppCompatActivity {
    private ActivityMapboxTestBinding binding;

    private MapboxMap mapboxMap;

    private MapboxNavigation mapboxNavigation;

    private NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();

    private NavigationCamera navigationCamera;
    private MapboxNavigationViewportDataSource viewportDataSource;
    private float pixelDensity = Resources.getSystem().getDisplayMetrics().density;
    private EdgeInsets overviewPadding =  new EdgeInsets (
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
    );
    private EdgeInsets landscapeOverviewPadding = new EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            20.0 * pixelDensity,
            20.0 * pixelDensity
    );
    private EdgeInsets followingPadding = new EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
    );
    private EdgeInsets landscapeFollowingPadding = new EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
    );

    private MapboxTripProgressApi tripProgressApi;

    // voice instructions
    private boolean isVoiceInstructionsMuted = false;
    private MapboxManeuverApi maneuverApi;
    private MapboxSpeechApi speechAPI;
    private MapboxVoiceInstructionsPlayer voiceInstructionsPlayer;

    // route line
    private MapboxRouteLineApi routeLineAPI;
    private MapboxRouteLineView routeLineView;
    private MapboxRouteArrowView routeArrowView;
    private MapboxRouteArrowApi routeArrowAPI = new MapboxRouteArrowApi();

    private VoiceInstructionsObserver voiceInstructionsObserver = new VoiceInstructionsObserver() {
        @Override
        public void onNewVoiceInstructions(@NonNull VoiceInstructions voiceInstructions) {
            speechAPI.generate(voiceInstructions, speechCallback);
        }
    };

    private MapboxNavigationConsumer<SpeechAnnouncement> voiceInstructionsPlayerCallback =
            new MapboxNavigationConsumer<SpeechAnnouncement>() {
                @Override
                public void accept(SpeechAnnouncement speechAnnouncement) {
                    speechAPI.clean(speechAnnouncement);
                }
            };

    private MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> speechCallback = new MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>>() {
        @Override
        public void accept(Expected<SpeechError, SpeechValue> speechErrorSpeechValueExpected) {
           speechErrorSpeechValueExpected.fold(
                   err -> {
                       voiceInstructionsPlayer.play(
                               err.getFallback(),
                               voiceInstructionsPlayerCallback
                       );
                       return null;
                   },
                   value -> {
                       voiceInstructionsPlayer.play(
                               value.getAnnouncement(),
                               voiceInstructionsPlayerCallback
                       );
                       return null;
                   }
           );
        }
    };

    private LocationObserver locationObserver = new LocationObserver() {
        @Override
        public void onRawLocationChanged(@NonNull Location location) {

        }

        @Override
        public void onEnhancedLocationChanged(@NonNull Location location, @NonNull List<? extends Location> keyPoints) {
            navigationLocationProvider.changePosition(
                    location, keyPoints, null, null
            );
        }
    };

    private RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
        @Override
        public void onRouteProgressChanged(@NonNull RouteProgress routeProgress) {
            viewportDataSource.onRouteProgressChanged(routeProgress);
            viewportDataSource.evaluate();

            Expected<InvalidPointError, UpdateManeuverArrowValue> maneuverArrowResult
                    = routeArrowAPI.addUpcomingManeuverArrow(routeProgress);
            Style style = mapboxMap.getStyle();
            if(style != null) {
                routeArrowView.renderManeuverUpdate(style, maneuverArrowResult);
            }

//            Expected<ManeuverError, List<Maneuver>> maneuvers = maneuverApi.getManeuvers(routeProgress);
//            maneuvers.fold(
//                    new Expected.Transformer<ManeuverError, Object>() {
//                        @NonNull
//                        @Override
//                        public Object invoke(@NonNull ManeuverError input) {
//                            Log.d("error", input.getErrorMessage());
//                            return null;
//                        }
//                    },
//                    new Expected.Transformer<List<Maneuver>, Object>() {
//                        @NonNull
//                        @Override
//                        public Object invoke(@NonNull List<Maneuver> input) {
//                            binding.maneuverView.setVisibility(View.VISIBLE);
//                            binding.maneuverView.renderManeuvers(maneuvers);
//                            return null;
//                        }
//                    }
//            );

            binding.tripProgressView.render(tripProgressApi.getTripProgress(routeProgress));
        }
    };

    private RoutesObserver routesObserver = new RoutesObserver() {
        @Override
        public void onRoutesChanged(@NonNull List<? extends DirectionsRoute> routes) {
            if(!routes.isEmpty()) {
                new Handler().post(() -> {
                    routeLineAPI.setRoutes(
                        Arrays.asList(new RouteLine(routes.get(0), null)),
                        new MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>() {
                            @Override
                            public void accept(Expected<RouteLineError, RouteSetValue> result) {
                                Style style = mapboxMap.getStyle();
                                if(style != null)
                                    routeLineView.renderRouteDrawData(style, result);
                            }
                        }
                    );
                });

                viewportDataSource.onRouteChanged(routes.get(0));
                viewportDataSource.evaluate();
            }
            else {
                Style style = mapboxMap.getStyle();
                if(style != null) {
                    routeLineAPI.clearRouteLine( value -> {
                        routeLineView.renderClearRouteLineValue(
                            style,
                            value
                        );
                    });
                    routeArrowView.render(style, routeArrowAPI.clearArrows());
                }

                viewportDataSource.clearRouteData();
                viewportDataSource.evaluate();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapboxTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mapboxMap = binding.mapView.getMapboxMap();

        // initialize the location puck
        LocationComponentPlugin locationComponentPlugin
                = binding.mapView.getPlugin(LocationComponentPluginImpl.class);
        locationComponentPlugin.setLocationPuck (
            new LocationPuck2D(
                null,
                ContextCompat.getDrawable(
                        this,
                        R.drawable.mapbox_navigation_puck_icon
                ),
                null,
                null
            )
        );
        locationComponentPlugin.setLocationProvider(navigationLocationProvider);
        locationComponentPlugin.setEnabled(true);

        // initialize Mapbox Navigation
        mapboxNavigation = new MapboxNavigation(
            new NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .build()
        );

        // move the camera to current location on the first update
        mapboxNavigation.registerLocationObserver(
            new LocationObserver() {
                @Override
                public void onRawLocationChanged(@NonNull Location location) {
                    Point point = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                    CameraOptions cameraOptions = new CameraOptions.Builder()
                            .center(point)
                            .zoom(13.0)
                            .build();
                    mapboxMap.setCamera(cameraOptions);
                    mapboxNavigation.unregisterLocationObserver(this);
                }

                @Override
                public void onEnhancedLocationChanged(@NonNull Location location, @NonNull List<? extends Location> list) {

                }
        });

        // initialize Navigation Camera
        viewportDataSource = new MapboxNavigationViewportDataSource(
                binding.mapView.getMapboxMap()
        );
        CameraAnimationsPluginImpl cameraPlugin = binding.mapView.getPlugin(CameraAnimationsPluginImpl.class);
        navigationCamera = new NavigationCamera(
                binding.mapView.getMapboxMap(),
                cameraPlugin,
                viewportDataSource,
                new MapboxNavigationCameraStateTransition(
                        mapboxMap,
                        cameraPlugin,
                        new MapboxNavigationCameraTransition(mapboxMap, cameraPlugin)
                )
        );
        cameraPlugin.addCameraAnimationsLifecycleListener(
                new NavigationBasicGesturesHandler(navigationCamera)
        );
        navigationCamera.registerNavigationCameraStateChangeObserver(
                new NavigationCameraStateChangedObserver() {
                    @Override
                    public void onNavigationCameraStateChanged(@NonNull NavigationCameraState navigationCameraState) {
                        if(navigationCameraState == NavigationCameraState.FOLLOWING || navigationCameraState == NavigationCameraState.TRANSITION_TO_FOLLOWING) {
                            binding.recenter.setVisibility(View.INVISIBLE);
                        }
                        else {
                            binding.recenter.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.setOverviewPadding(landscapeOverviewPadding);
        }
        else {
            viewportDataSource.setOverviewPadding(overviewPadding);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.setFollowingPadding(landscapeFollowingPadding);
        }
        else {
            viewportDataSource.setFollowingPadding(followingPadding);
        }

        // initialize top maneuver view
        maneuverApi = new MapboxManeuverApi(
            new MapboxDistanceFormatter(
                new DistanceFormatterOptions.Builder(this).build()
            )
        );

        // initialize bottom progress view
        tripProgressApi = new MapboxTripProgressApi(
            new TripProgressUpdateFormatter.Builder(this)
                .distanceRemainingFormatter(
                    new DistanceRemainingFormatter(
                        mapboxNavigation.getNavigationOptions().getDistanceFormatterOptions()
                    )
                )
                .timeRemainingFormatter(new TimeRemainingFormatter(this, null))
                .percentRouteTraveledFormatter(new PercentDistanceTraveledFormatter())
                .estimatedTimeToArrivalFormatter(
                        new EstimatedTimeToArrivalFormatter(this, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        );

        // initialize voice instructions
        speechAPI = new MapboxSpeechApi(
                this,
                getMapboxAccessTokenFromResources(),
                Locale.KOREA.getLanguage()
        );
        voiceInstructionsPlayer = new MapboxVoiceInstructionsPlayer(
                this,
                getMapboxAccessTokenFromResources(),
                Locale.KOREA.getLanguage()
        );

        // initialize route line
        MapboxRouteLineOptions mapboxRouteLineOptions = new MapboxRouteLineOptions.Builder(this)
                .withRouteLineBelowLayerId("road-label")
                .build();
        routeLineAPI = new MapboxRouteLineApi(mapboxRouteLineOptions);
        routeLineView = new MapboxRouteLineView(mapboxRouteLineOptions);
        RouteArrowOptions routeArrowOptions = new RouteArrowOptions.Builder(this).build();
        routeArrowView = new MapboxRouteArrowView(routeArrowOptions);

        // load map style
        mapboxMap.loadStyleUri(
            Style.MAPBOX_STREETS,
            style -> binding.mapView.getPlugin(GesturesPluginImpl.class).addOnMapLongClickListener(
                point -> {
                    findRoute(point);
                    return true;
                }
            )
        );

        // initialize view interactions
        binding.stop.setOnClickListener(
                v -> clearRouteAndStopNavigation()
        );

        binding.recenter.setOnClickListener(
                v -> navigationCamera.requestNavigationCameraToFollowing()
        );

        binding.routeOverview.setOnClickListener(
                v -> {
                    navigationCamera.requestNavigationCameraToOverview();
                    binding.recenter.showTextAndExtend(2000L);
                }
        );

        binding.soundButton.setOnClickListener(
                v -> {
                    // mute/unmute voice instructions
                    isVoiceInstructionsMuted = !isVoiceInstructionsMuted;
                    if (isVoiceInstructionsMuted) {
                        binding.soundButton.muteAndExtend(2000L);
                        voiceInstructionsPlayer.volume(new SpeechVolume(0f));
                    } else {
                        binding.soundButton.unmuteAndExtend(2000L);
                        voiceInstructionsPlayer.volume(new SpeechVolume(1f));
                    }
                }
        );

        // start the trip session to being receiving location updates in free drive
        // and later when a route is set, also receiving route progress updates
        mapboxNavigation.startTripSession();
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.mapView.onStart();
        mapboxNavigation.registerRoutesObserver(routesObserver);
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
        mapboxNavigation.registerLocationObserver(locationObserver);
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.mapView.onStop();
        mapboxNavigation.unregisterRoutesObserver(routesObserver);
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
        mapboxNavigation.unregisterLocationObserver(locationObserver);
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.mapView.onDestroy();
        mapboxNavigation.onDestroy();
        speechAPI.cancel();
        voiceInstructionsPlayer.shutdown();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void findRoute(Point destination) {
        Location originLocation = navigationLocationProvider.getLastLocation();
        if(originLocation == null) return;
        Point origin = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());

        mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .steps(true)
                        .continueStraight(true)
                        .roundaboutExits(true)
                        .annotationsList(
                            Arrays.asList(
                                DirectionsCriteria.ANNOTATION_CONGESTION,
                                DirectionsCriteria.ANNOTATION_MAXSPEED,
                                DirectionsCriteria.ANNOTATION_SPEED,
                                DirectionsCriteria.ANNOTATION_DURATION,
                                DirectionsCriteria.ANNOTATION_DISTANCE,
                                DirectionsCriteria.ANNOTATION_CLOSURE
                            )
                        )
                        .voiceInstructions(true)
                        .bannerInstructions(true)
                        .enableRefresh(true)
                        .language("ko")
                        .voiceUnits(UnitType.METRIC.getValue())
                        .coordinatesList(Arrays.asList(origin, destination))
                        .build(),
//                        .accessToken(getMapboxAccessTokenFromResources())
                new RouterCallback() {
                    @Override
                    public void onRoutesReady(@NonNull List<? extends DirectionsRoute> routes, @NonNull RouterOrigin routerOrigin) {
                        setRouteAndStartNavigation(routes.get(0), routerOrigin);
                    }

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> list, @NonNull RouteOptions routeOptions) {

                    }

                    @Override
                    public void onCanceled(@NonNull RouteOptions routeOptions, @NonNull RouterOrigin routerOrigin) {

                    }
                }
        );
    }

    private void setRouteAndStartNavigation(DirectionsRoute route, RouterOrigin routerOrigin) {
        // set route
        mapboxNavigation.setRoutes(Arrays.asList(route));

        // show UI elements
        binding.soundButton.setVisibility(View.VISIBLE);
        binding.routeOverview.setVisibility(View.VISIBLE);
        binding.tripProgressCard.setVisibility(View.VISIBLE);
        binding.routeOverview.showTextAndExtend(2000L);
        binding.soundButton.unmuteAndExtend(2000L);

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview();
    }

    private void clearRouteAndStopNavigation() {
        // clear
        mapboxNavigation.setRoutes(Arrays.asList());

        // hide UI elements
        binding.soundButton.setVisibility(View.INVISIBLE);
        binding.maneuverView.setVisibility(View.INVISIBLE);
        binding.routeOverview.setVisibility(View.INVISIBLE);
        binding.tripProgressCard.setVisibility(View.INVISIBLE);
    }

    private String getMapboxAccessTokenFromResources() {
        return getString(R.string.mapbox_access_token);
    }
}