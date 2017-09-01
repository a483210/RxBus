package com.xiuyukeji.rxbus;

import android.os.Looper;

import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class RxBusMethodModifiersTest extends AbstractRxBusTest {

    @Test
    public void testRegisterForEventTypeAndPost() throws InterruptedException {
        eventBus.register(this);
        String event = "Hello";
        eventBus.post(event);
        waitForEventCount(4, 1000);
    }

    @Subscribe
    public void onEvent(String event) {
        trackEvent(event);
        assertNotSame(Looper.getMainLooper(), Looper.myLooper());
    }

    @Subscribe(mode = ThreadMode.MAIN)
    public void onEventMainThread(String event) {
        trackEvent(event);
        assertSame(Looper.getMainLooper(), Looper.myLooper());
    }

    @Subscribe(mode = ThreadMode.SINGLE)
    public void onEventBackgroundThread(String event) {
        trackEvent(event);
        assertNotSame(Looper.getMainLooper(), Looper.myLooper());
    }

    @Subscribe(mode = ThreadMode.ASYNC)
    public void onEventAsync(String event) {
        trackEvent(event);
        assertNotSame(Looper.getMainLooper(), Looper.myLooper());
    }

}
