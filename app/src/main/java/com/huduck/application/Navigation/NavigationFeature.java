package com.huduck.application.Navigation;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
public class NavigationFeature {

    public NavigationFeature() {

    }

    @Getter
    @SuperBuilder
    public static class Geometry {
        protected String type;
    }

    @Getter
    @SuperBuilder
    public static class Properties {
        protected int index;
        protected String name;
        protected String description;
    }
}
