package com.huduck.application.Navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
public class NavigationPoint implements NavigationFeature {
    private Geometry geometry;
    private Properties properties;

    public NavigationPoint(Geometry geometry, Properties properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    @Getter
    @SuperBuilder
    public static class Geometry extends NavigationFeature.Geometry {
        private final String type = "Point";
        private ArrayList<Double> coordinates; // Lat, Lng
    }
    @Getter
    @SuperBuilder
    public static class Properties extends NavigationFeature.Properties {

        private int pointIndex;
        private String nextRoadName;
        private int turnType;
        private String pointType;
        private int totalTime;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("type", "Feature");

        // Geometry
        JSONObject geometryJson = new JSONObject();

        geometryJson.put("type", "Point");

        JSONArray coordinatesJson = new JSONArray();
        coordinatesJson.put(geometry.getCoordinates().get(1));
        coordinatesJson.put(geometry.getCoordinates().get(0));
        geometryJson.put("coordinates", coordinatesJson);

        result.put("geometry", geometryJson);

        // Properties
        JSONObject propertiesJson = new JSONObject();

        propertiesJson.put("index",         properties.index);
        propertiesJson.put("pointIndex",    properties.pointIndex);
        propertiesJson.put("name",          properties.name);
        propertiesJson.put("description",   properties.description);
        propertiesJson.put("nextRoadName",  properties.nextRoadName);
        propertiesJson.put("turnType",      properties.turnType);
        propertiesJson.put("pointType",     properties.pointType);
        propertiesJson.put("totalTime",     properties.totalTime);

        result.put("properties", propertiesJson);

        return result;
    }

    public static HashMap<Integer, String> TurnType = new HashMap<Integer, String>() {{
        put(0, "휴게소");
        put(1, "도곽에 의한 점");
        put(2, "타일에 의한 점");
        put(3, "고속도로에 의한 안내없음");
        put(4, "일반도로에 의한 안내없음");
        put(5, "특수한 경우 안내없음");
        put(6, "Y자 오른쪽 안내없음");
        put(7, "Y자 왼쪽 안내없음");
        put(11, "직진");
        put(12, "좌회전");
        put(13, "우회전");
        put(14, "U턴");
        put(15, "P턴");
        put(16, "8시 방향 좌회전");
        put(17, "10시 방향 좌회전");
        put(18, "2시 방향 우회전");
        put(19, "4시 방향 우회전");
        put(43, "오른쪽");
        put(44, "왼쪽");
        put(51, "직진 방향");
        put(52, "왼쪽 차선");
        put(53, "오른쪽 차선");
        put(54, "1차선");
        put(55, "2차선");
        put(56, "3차선");
        put(57, "4차선");
        put(58, "5차선");
        put(59, "6차선");
        put(60, "7차선");
        put(61, "8차선");
        put(62, "9차선");
        put(63, "10차선");
        put(71, "첫번째 출구");
        put(72, "두번째 출구");
        put(73, "첫번째 오른쪽 길");
        put(74, "두번째 오른쪽 길");
        put(75, "첫번째 왼쪽 길");
        put(76, "두번째 왼쪽 길");
        put(101, "오른쪽 고속도로 입구");
        put(102, "왼쪽 고속도로 입구");
        put(103, "전방 고속도로 입구");
        put(104, "오른쪽 고속도로 출구");
        put(105, "왼쪽 고속도로 출구");
        put(106, "전방 고속도로 출구");
        put(111, "오른쪽 도시고속도로 입구");
        put(112, "왼쪽 도시고속도로 입구");
        put(113, "전방 도시고속도로 입구");
        put(114, "오른쪽 도시고속도로 출구");
        put(115, "왼쪽 도시고속도로 출구");
        put(116, "전방 도시고속도로 출구");
        put(117, "오른쪽 방향");
        put(118, "왼쪽 방향");
        put(119, "지하차도");
        put(120, "고가도로");
        put(121, "터널");
        put(122, "교량");
        put(123, "지하차도옆");
        put(124, "고가도로옆");
        put(130, "토끼굴 진입");
        put(131, "1시 방향");
        put(132, "2시 방향");
        put(133, "3시 방향");
        put(134, "4시 방향");
        put(135, "5시 방향");
        put(136, "6시 방향");
        put(137, "7시 방향");
        put(138, "8시 방향");
        put(139, "9시 방향");
        put(140, "10시 방향");
        put(141, "11시 방향");
        put(142, "12시 방향");
        put(150, "졸음쉼터");
        put(151, "휴게소");
        put(182, "왼쪽방향 도착안내");
        put(183, "오른쪽방향 도착안내");
        put(184, "경유지");
        put(185, "첫번째경유지");
        put(186, "두번째경유지");
        put(187, "세번째경유지");
        put(188, "네번째경유지");
        put(189, "다섯번째경유지");
        put(191, "제한속도");
        put(192, "사고다발");
        put(193, "급커브");
        put(194, "낙석주의");
        put(200, "출발지");
        put(201, "도착지");
        put(203, "목적지건너편");
        put(211, "횡단보도");
        put(212, "좌측 횡단보도");
        put(213, "우측 횡단보도");
        put(214, "8시 방향 횡단보도");
        put(215, "10시 방향 횡단보도");
        put(216, "2시 방향 횡단보도");
        put(217, "4시 방향 횡단보도");
        put(218, "엘리베이터");
        put(233, "직진 임시");
    }};
}
