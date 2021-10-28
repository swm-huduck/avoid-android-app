package com.huduck.application.Navigation;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.Tm128;

import lombok.Getter;
import lombok.Setter;

public class NavigationTurnEventCalc {
    public static class NavigationTurnEventData {
        @Setter @Getter
        private double distanceFromCurrentPositionToFootOfPerpendicular = 0;    // 현재 위치 -> 수선의 발
        @Setter @Getter
        private double distanceFromEventToFootOfPerpendicular = 0;              // 이벤트 -> 수선의 발
        @Setter @Getter
        private double distanceFromCurrentPositionToEvent = 0;                  // 현재 위치 -> 이벤트
        @Setter @Getter
        private LatLng footOfPerpendicular = null;                              // 수선의 발
    }

    public static NavigationTurnEventData calc
            (LatLng lineStringSegStartPosition, LatLng lineStringSegEndPosition,
             LatLng currentPosition, NavigationPoint event) {
        NavigationTurnEventData result = new NavigationTurnEventData();
        Tm128 lineStringSegStartPositionTm = Tm128.valueOf(lineStringSegStartPosition);
        Tm128 lineStringSegEndPositionTm = Tm128.valueOf(lineStringSegEndPosition);
        LatLng eventPosition = new LatLng(event.getGeometry().getCoordinates().get(0), event.getGeometry().getCoordinates().get(1));
        Tm128 eventPositionTm = Tm128.valueOf(eventPosition);

        double x1 = lineStringSegStartPositionTm.x;
        double x2 = lineStringSegEndPositionTm.x;
        double xe = eventPositionTm.x;

        double y1 = lineStringSegStartPositionTm.y;
        double y2 = lineStringSegEndPositionTm.y;
        double ye = eventPositionTm.y;

        double delX = x2 - x1;
        double delY = y2 - y1;

        double a = delX == 0 ? 0 : delY / delX;
        double b = -1;
        double c = y1 - (a * x1);

        double xf = (b * (b * xe - a * ye) - a * c)
                    / (a * a + b * b);
        double yf = (a * (-b * xe + a * ye) - b * c)
                    / (a * a + b * b);

        Tm128 footOfPerpendicularTm = new Tm128(xf, yf);
        LatLng footOfPerpendicular = footOfPerpendicularTm.toLatLng();

        result.setFootOfPerpendicular(footOfPerpendicular);
        result.setDistanceFromCurrentPositionToFootOfPerpendicular(currentPosition.distanceTo(footOfPerpendicular));
        result.setDistanceFromEventToFootOfPerpendicular(eventPosition.distanceTo(footOfPerpendicular));
        result.setDistanceFromCurrentPositionToEvent(currentPosition.distanceTo(eventPosition));

        return result;
    }
}
