package com.huduck.application.myCar;

import android.content.Context;
import android.content.SharedPreferences;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class TruckInformation {
    private static final int M_TO_CM = 100;
    private static final int T_TO_KG = 1000;

    @Builder.Default @Getter @Setter
    private int truckWidth = 100;   // cm

    @Builder.Default @Getter @Setter
    private int truckHeight = 100;  // cm

    @Builder.Default @Getter @Setter
    private int truckLength = 200;  // cm

    @Builder.Default @Getter @Setter
    private int truckWeight = 0;    // kg

    @Builder.Default @Getter @Setter
    private int loadWeight = 500;   // kg

    @Builder.Default @Getter @Setter
    private int totalWeight = 500;  // kg

    public static TruckInformation getInstance(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("my_car", Context.MODE_PRIVATE);
        boolean saved = sharedPreferences.contains("saved");

        TruckInformation result;

        if(saved) {
            int width           = (int) (sharedPreferences.getFloat("width", 0)         * M_TO_CM);
            int height          = (int) (sharedPreferences.getFloat("height", 0)        * M_TO_CM);
            int length          = (int) (sharedPreferences.getFloat("length", 0)        * M_TO_CM);
            int truck_weight    = (int) (sharedPreferences.getFloat("truck_weight", 0)  * T_TO_KG);
            int load_weight     = (int) (sharedPreferences.getFloat("load_weight", 0)   * T_TO_KG);
            int total_weight    = (int) (sharedPreferences.getFloat("total_weight", 0)  * T_TO_KG);

            result = TruckInformation.builder()
                        .truckWeight(width)
                        .truckHeight(height)
                        .truckLength(length)
                        .truckWeight(truck_weight)
                        .loadWeight(load_weight)
                        .totalWeight(total_weight)
                        .build();
        }
        else
            result = TruckInformation.builder().build();

        return result;
    }
}
