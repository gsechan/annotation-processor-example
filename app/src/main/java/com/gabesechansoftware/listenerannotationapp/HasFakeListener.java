package com.gabesechansoftware.listenerannotationapp;

import java.util.LinkedList;
import java.util.List;

public interface HasFakeListener {
    Hidden hidden = new Hidden();

     default void addListener(String listener) {
         synchronized (hidden) {
             hidden.listeners.add(listener);
         }
     }

     class Hidden {
        private List<String> listeners = new LinkedList<>();
    }
}
