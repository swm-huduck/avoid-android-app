package com.huduck.application.myCar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class TruckInformation {
    @Builder.Default @Getter @Setter
    private int truckWidth = 100;

    @Builder.Default @Getter @Setter
    private int truckHeight = 100;

    @Builder.Default @Getter @Setter
    private int truckLength = 200;

    @Builder.Default @Getter @Setter
    private int truckWeight = 0;

    @Builder.Default @Getter @Setter
    private int LoadWeight = 500;

    @Builder.Default @Getter @Setter
    private int totalWeight = 500;
}
