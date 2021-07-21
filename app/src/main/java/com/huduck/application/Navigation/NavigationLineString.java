package com.huduck.application.Navigation;

import java.util.ArrayList;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
public class NavigationLineString extends NavigationFeature {
    private Geometry geometry;
    private Properties properties;

    public NavigationLineString(Geometry geometry, Properties properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    @Getter
    @SuperBuilder
    public static class Geometry extends NavigationFeature.Geometry {
        private final String type = "LineString";
        private ArrayList<ArrayList<Double>> coordinates;
//        whr
//        htr
//        wtr
//        wpz
//        ttr
    }

    @Getter
    @SuperBuilder
    public static class Properties extends NavigationFeature.Properties {
        private int lineIndex;
        private int distance;
        private int time;
        private int roadType;
        private int facilityType;
    }
}
