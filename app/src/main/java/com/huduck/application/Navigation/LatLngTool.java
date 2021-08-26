package com.huduck.application.Navigation;

import android.location.Location;

import com.naver.maps.geometry.LatLng;

public class LatLngTool {
    public static LatLng add(LatLng a, LatLng b) {
        return new LatLng(a.latitude + b.latitude, a.longitude + b.longitude);
    }

    public static LatLng sub(LatLng a, LatLng b) {
        return new LatLng(a.latitude - b.latitude, a.longitude - b.longitude);
    }

    public static LatLng mul(LatLng a, double m) {
        return new LatLng(a.latitude * m, a.longitude * m);
    }

    public static LatLng div(LatLng a, double d) {
        if (d == 0) return new LatLng(0, 0);
        return new LatLng(a.latitude / d, a.longitude / d);
    }

//    public static double sqrMag(LatLng latLng) {
//        return latLng.latitude * latLng.latitude + latLng.longitude * latLng.longitude;
//    }

    public static double mag(LatLng latLng, boolean meter) {
        double R = 6371e3;
        double lat = latLng.latitude * Math.PI / 180;
        double lng = latLng.longitude * Math.PI / 180;

        double a =  Math.sin(lat * 0.5) * Math.sin(lat * 0.5) +
                    Math.cos(0) * Math.cos(lat) *
                    Math.sin(lng * 0.5) * Math.sin(lng * 0.5);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return meter ? c * R : c;
    }

    public static double mag(LatLng latLng) {
        return mag(latLng, false);
    }

    public static double mag(LatLng latLng1, LatLng latLng2) {
        return mag(sub(latLng1, latLng2));
    }

    public static double mag(LatLng latLng1, LatLng latLng2, boolean meter) {
        return mag(sub(latLng1, latLng2), meter);
    }

   /* public static double sqrMag(LatLng latLng) {
        return latLng.latitude * latLng.latitude + latLng.longitude * latLng.longitude;
    }

    public static double mag(LatLng latLng) {
        return Math.sqrt(sqrMag(latLng));
    }

    public static double mag(LatLng latLng1, LatLng latLng2) {
        return mag(sub(latLng1, latLng2));
    }*/

    public static LatLng normalize(LatLng latLng) {
        double len = mag(latLng); //Math.sqrt(latLng.latitude * latLng.latitude + latLng.longitude * latLng.longitude);
        if(len == 0)
            return new LatLng(0, 0);

        return div(latLng, len);
    }

    public static double deg(LatLng latLng) {
//        double ang = Math.atan2(latLng.longitude, latLng.latitude);
//        double deg = 180 * ang / Math.PI;
//        return (360.0+ deg) % 360.0;

//        Location zero = new Location("");
//        zero.setLatitude(0);
//        zero.setLongitude(0);
//
//        Location target = new Location("");
//        target.setLatitude(latLng.latitude);
//        target.setLongitude(latLng.longitude);
//
//        return zero.bearingTo(target);

        double y = Math.sin(latLng.longitude) * Math.cos(latLng.latitude);
        double x = Math.cos(0) * Math.sin(latLng.latitude) - Math.sin(0) * Math.cos(latLng.latitude) * Math.cos(latLng.longitude);

        double ang = Math.atan2(y, x);
        double deg = (ang * 180 / Math.PI + 360) % 360;
        return deg;
    }

    public static final double latlngToMeterConst = new LatLng(0,0).distanceTo(new LatLng(0, 1));

}
