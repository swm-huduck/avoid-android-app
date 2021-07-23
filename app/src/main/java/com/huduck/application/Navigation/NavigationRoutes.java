package com.huduck.application.Navigation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;

@Getter
public class NavigationRoutes implements Serializable {
    private ArrayList<Integer> navigationSequence = new ArrayList<>();
    private HashMap<Integer, NavigationPoint> navigationPointHashMap = new HashMap<>();
    private HashMap<Integer, NavigationLineString> navigationLineStringHashMap = new HashMap<>();
}
