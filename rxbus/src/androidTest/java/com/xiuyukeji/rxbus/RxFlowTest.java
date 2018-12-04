package com.xiuyukeji.rxbus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class RxFlowTest {

    private int eventInteger;

    @Subscribe
    public void eventInteger(Integer num) {
        eventInteger = num;
    }

    @Before
    public void setUp() {
        RxBus.get().register(this);
    }

    @After
    public void teraDown() {
        RxBus.get().unregister(this);
    }

    @Test
    public void testEventInteger() {
        RxBus.get().post(1);
        assertEquals(1, eventInteger);
        RxBus.get().post(2);
        assertEquals(2, eventInteger);
    }

}
