package com.huduck.application.Navigation;

import android.location.Location;

import com.naver.maps.geometry.LatLng;
import com.skt.Tmap.TMapPoint;

public class LatLngTool {
    public static LatLng locationToLatlng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public static LatLng tMapPointToLatlng(TMapPoint tMapPoint) {
        return new LatLng(tMapPoint.getLatitude(), tMapPoint.getLongitude());
    }

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
        /*if (latLng.latitude == 0 && latLng.longitude == 0) {
            return 0.0D;
        } else {
            double lat1 = Math.toRadians(latLng.latitude);
            double lng1 = Math.toRadians(latLng.longitude);
            double lat2 = Math.toRadians(0);
            double lng2 = Math.toRadians(0);
            return 1.2756274E7D *
                    Math.asin(
                            Math.sqrt(
                                    Math.pow(
                                            Math.sin(
                                                    (lat1 - lat2) / 2.0D
                                            ), 2.0D
                                    )
                                    + Math.cos(lat1)
                                    * Math.cos(lat2)
                                    * Math.pow(
                                            Math.sin(
                                                    (lng1 - lng2) / 2.0D
                                            ), 2.0D
                                    )
                            )
                    );
        }


*/

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

    public static double deg(LatLng startLatLng, LatLng endLatLng) {
        double deg2rad = Math.PI / 180;
        LatLng radLatlng1 = new LatLng(startLatLng.latitude * deg2rad, startLatLng.longitude * deg2rad);
        LatLng radLatlng2 = new LatLng(endLatLng.latitude * deg2rad, endLatLng.longitude * deg2rad);

        double x = Math.cos(radLatlng2.latitude) * Math.sin(radLatlng2.longitude - radLatlng1.longitude);
        double y = Math.cos(radLatlng1.latitude) * Math.sin(radLatlng2.latitude)
                - Math.sin(radLatlng1.latitude) * Math.cos(radLatlng2.latitude) * Math.cos(radLatlng2.longitude - radLatlng1.longitude);
        double rad = Math.atan2(y, x);
        double deg = rad * 180 / Math.PI;
        deg = (deg + 360) % 360;
        deg = (90 + (360 - deg)) % 360;
        return deg;
    }

    public static LatLng zero = new LatLng(0,0);

//    public static double deg(LatLng latLng) {
//        return deg(latLng, zero);
//    }

    public static final double latlngToMeterConst = new LatLng(0,0).distanceTo(new LatLng(0, 1));

}
