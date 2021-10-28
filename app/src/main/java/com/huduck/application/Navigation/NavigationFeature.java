package com.huduck.application.Navigation;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

public interface NavigationFeature {
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

    public JSONObject toJson() throws JSONException;
}
