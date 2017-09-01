package com.xiuyukeji.rxbus;

import android.os.Looper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class RxBusBackgroundThreadTest extends AbstractRxBusTest {

    @Test
    public void testPostInCurrentThread() throws InterruptedException {
        eventBus.register(this);
        eventBus.post("Hello");
        waitForEventCount(1, 1000);

        assertEquals("Hello", lastEvent);
        assertNotEquals(Thread.currentThread(), lastThread);
    }

    @Test
    public void testPostFromMain() throws InterruptedException {
        eventBus.register(this);
        postInMainThread("Hello");
        waitForEventCount(1, 1000);
        assertEquals("Hello", lastEvent);
        assertFalse(lastThread.equals(Thread.currentThread()));
        assertFalse(lastThread.equals(Looper.getMainLooper().getThread()));
    }

    @Subscribe(mode = ThreadMode.SINGLE)
    public void onEventBackgroundThread(String event) {
        trackEvent(event);
    }

}
