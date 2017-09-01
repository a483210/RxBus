package com.xiuyukeji.rxbus;

import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RxBusMainThreadTest extends AbstractRxBusTest {

    private BackgroundPoster backgroundPoster;

    @Before
    public void setUp() throws Exception {
        backgroundPoster = new BackgroundPoster();
        backgroundPoster.start();
    }

    @After
    public void tearDown() throws Exception {
        backgroundPoster.shutdown();
        backgroundPoster.join();
    }

    @Test
    public void testPost() throws InterruptedException {
        eventBus.register(this);
        eventBus.post("Hello");
        waitForEventCount(1, 1000);

        assertEquals("Hello", lastEvent);
        assertEquals(Looper.getMainLooper().getThread(), lastThread);
    }

    @Test
    public void testPostInBackgroundThread() throws InterruptedException {
        eventBus.register(this);
        backgroundPoster.post("Hello");
        waitForEventCount(1, 1000);
        assertEquals("Hello", lastEvent);
        assertEquals(Looper.getMainLooper().getThread(), lastThread);
    }

    @Subscribe(mode = ThreadMode.MAIN)
    public void onEventMainThread(String event) {
        trackEvent(event);
    }

    class BackgroundPoster extends Thread {
        volatile boolean running = true;
        private final List<Object> eventQ = new ArrayList<Object>();
        private final List<Object> eventsDone = new ArrayList<Object>();

        public BackgroundPoster() {
            super("BackgroundPoster");
        }

        @Override
        public void run() {
            while (running) {
                Object event = pollEvent();
                if (event != null) {
                    eventBus.post(event);
                    synchronized (eventsDone) {
                        eventsDone.add(event);
                        eventsDone.notifyAll();
                    }
                }
            }
        }

        private synchronized Object pollEvent() {
            Object event = null;
            synchronized (eventQ) {
                if (eventQ.isEmpty()) {
                    try {
                        eventQ.wait(1000);
                    } catch (InterruptedException e) {
                    }
                }
                if(!eventQ.isEmpty()) {
                    event = eventQ.remove(0);
                }
            }
            return event;
        }

        void shutdown() {
            running = false;
            synchronized (eventQ) {
                eventQ.notifyAll();
            }
        }

        void post(Object event) {
            synchronized (eventQ) {
                eventQ.add(event);
                eventQ.notifyAll();
            }
            synchronized (eventsDone) {
                while (!eventsDone.remove(event)) {
                    try {
                        eventsDone.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

    }

}
