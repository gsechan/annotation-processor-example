package com.gabesechansoftware.listenerannotationapp;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addListener() {
        HasAListener hasAListener = new HasAListener();
        TestListener listener = mock(TestListener.class);
        hasAListener.addTestListener(listener);

        hasAListener.callTestListenermethod1();
        verify(listener, times(1)).method1();
        verify(listener, times(0)).method2(anyString(), anyInt());

        hasAListener.callTestListenermethod2("hello", 1);
        verify(listener, times(1)).method1();
        verify(listener, times(1)).method2("hello",1);

    }
}