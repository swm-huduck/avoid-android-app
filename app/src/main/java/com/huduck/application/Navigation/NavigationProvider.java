package com.huduck.application.Navigation;

import com.huduck.application.myCar.TruckInformation;
import com.skt.Tmap.TMapPOIItem;

import lombok.Getter;
import lombok.Setter;

public class NavigationProvider {
    private static NavigationProvider navigationProvider = new NavigationProvider();

    private NavigationProvider() {}

    // Navigation route
    private NavigationRoutes navigationRoute = null;

    public static void setNavigationRoute(NavigationRoutes navigationRoute) {
        navigationProvider.navigationRoute = navigationRoute;
    }

    public static NavigationRoutes getNavigationRoute() {
        return navigationProvider.navigationRoute;
    }

    // Destination
    private TMapPOIItem destination = null;

    public static void setDestination(TMapPOIItem destination) {
        navigationProvider.destination = destination;
    }

    public static TMapPOIItem getDestination() {
        return navigationProvider.destination;
    }

    // Search option
    private String searchOption = "0";

    public static void setSearchOption(String searchOption) {
        navigationProvider.searchOption = searchOption;
    }

    public static String getSearchOption() {
        return navigationProvider.searchOption;
    }

    // Truck information
    private TruckInformation truckInformation = TruckInformation.builder().build();

    public static void setTruckInformation(TruckInformation truckInformation) {
        navigationProvider.truckInformation = truckInformation;
    }

    public static TruckInformation getTruckInformation() {
        return navigationProvider.truckInformation;
    }
}
