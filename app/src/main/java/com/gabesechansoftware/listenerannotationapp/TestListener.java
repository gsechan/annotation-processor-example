package com.gabesechansoftware.listenerannotationapp;

import com.gabesechansoftware.liblistenerannotation.Listener;

@Listener
public interface TestListener {
    void method1();
    void method2(String param1, int param2);
}
