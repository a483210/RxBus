package com.xiuyukeji.rxbus;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public abstract class AbstractRxBusTest {
    /**
     * Activates long(er) running tests e.g. testing multi-threading more thoroughly.
     */
    protected static final boolean LONG_TESTS = false;

    protected RxBus eventBus;

    protected final AtomicInteger eventCount = new AtomicInteger();
    protected final List<Object> eventsReceived;

    protected volatile Object lastEvent;
    protected volatile Thread lastThread;

    private EventPostHandler mainPoster;

    public AbstractRxBusTest() {
        this(false);
    }

    public AbstractRxBusTest(boolean collectEventsReceived) {
        if (collectEventsReceived) {
            eventsReceived = new CopyOnWriteArrayList<Object>();
        } else {
            eventsReceived = null;
        }
    }

    @Before
    public void setUpBase() throws Exception {
        eventBus = RxBus.get();
        eventBus.addIndex(new SubscriberIndexImpl());
        eventBus.clearCaches();
        mainPoster = new EventPostHandler(Looper.getMainLooper());
        assertFalse(Looper.getMainLooper().getThread().equals(Thread.currentThread()));
    }

    protected void postInMainThread(Object event) {
        mainPoster.post(event);
    }

    protected void waitForEventCount(int expectedCount, int maxMillis) {
        for (int i = 0; i < maxMillis; i++) {
            int currentCount = eventCount.get();
            if (currentCount == expectedCount) {
                break;
            } else if (currentCount > expectedCount) {
                fail("Current count (" + currentCount + ") is already higher than expected count (" + expectedCount
                        + ")");
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        assertEquals(expectedCount, eventCount.get());
    }

    protected void trackEvent(Object event) {
        lastEvent = event;
        lastThread = Thread.currentThread();
        if (eventsReceived != null) {
            eventsReceived.add(event);
        }
        // Must the the last one because we wait for this
        eventCount.incrementAndGet();
    }

    @SuppressLint("HandlerLeak")
    class EventPostHandler extends Handler {
        public EventPostHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            eventBus.post(msg.obj);
        }

        void post(Object event) {
            sendMessage(obtainMessage(0, event));
        }

    }

    protected void assertEventCount(int expectedEventCount) {
        assertEquals(expectedEventCount, eventCount.intValue());
    }

    protected void countDownAndAwaitLatch(CountDownLatch latch, long seconds) {
        latch.countDown();
        awaitLatch(latch, seconds);
    }

    protected void awaitLatch(CountDownLatch latch, long seconds) {
        try {
            assertTrue(latch.await(seconds, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
