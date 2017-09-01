package com.xiuyukeji.rxbus;

import android.app.Activity;
import android.os.SystemClock;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.core.deps.guava.eventbus.EventBus;
import android.support.test.rule.UiThreadTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RxBusBasicTest {

    public static class WithIndex extends RxBusBasicTest {
        @Test
        public void dummy() {
        }

    }

    @Rule
    public final UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    protected RxBus eventBus;
    private String lastStringEvent;
    private int countStringEvent;
    private int countIntEvent;
    private int lastIntEvent;
    private int countMyEventExtended;
    private int countMyEvent;
    private int countMyEvent2;

    @Before
    public void setUp() throws Exception {
        eventBus = RxBus.get();
        eventBus.clearCaches();
    }

    @Test
    @UiThreadTest
    public void testRegisterAndPost() {
        // Use an activity to test real life performance
        TestActivity testActivity = new TestActivity();
        String event = "Hello";

        long start = System.currentTimeMillis();
        eventBus.register(testActivity);
        long time = System.currentTimeMillis() - start;
        Log.d(RxBus.TAG, "Registered in " + time + "ms");

        eventBus.post(event);

        assertEquals(event, testActivity.lastStringEvent);
    }

    @Test
    public void testPostWithoutSubscriber() {
        eventBus.post("Hello");
    }

    @Test
    public void testUnregisterWithoutRegister() {
        // Results in a warning without throwing
        eventBus.unregister(this);
    }

    // This will throw "out of memory" if subscribers are leaked
    @Test
    public void testUnregisterNotLeaking() {
        int heapMBytes = (int) (Runtime.getRuntime().maxMemory() / (1024L * 1024L));
        for (int i = 0; i < heapMBytes * 2; i++) {
            RxBusBasicTest subscriber = new RxBusBasicTest() {
                byte[] expensiveObject = new byte[1024 * 1024];
            };
            eventBus.register(subscriber);
            eventBus.unregister(subscriber);
            Log.d("Test", "Iteration " + i + " / max heap: " + heapMBytes);
        }
    }

    @Test
    public void testRegisterTwice() {
        eventBus.register(this);
        eventBus.register(this);
    }

    @Test
    public void testIsRegistered() {
        assertFalse(eventBus.isRegistered(this));
        eventBus.register(this);
        assertTrue(eventBus.isRegistered(this));
        eventBus.unregister(this);
        assertFalse(eventBus.isRegistered(this));
    }

    @Test
    public void testPostWithTwoSubscriber() {
        RxBusBasicTest test2 = new RxBusBasicTest();
        eventBus.register(this);
        eventBus.register(test2);
        String event = "Hello";
        eventBus.post(event);
        assertEquals(event, lastStringEvent);
        assertEquals(event, test2.lastStringEvent);
    }

    @Test
    public void testPostMultipleTimes() {
        eventBus.register(this);
        MyEvent event = new MyEvent();
        int count = 1000;
        long start = System.currentTimeMillis();
        // Debug.startMethodTracing("testPostMultipleTimes" + count);
        for (int i = 0; i < count; i++) {
            eventBus.post(event);
        }
        // Debug.stopMethodTracing();
        long time = System.currentTimeMillis() - start;
        Log.d(RxBus.TAG, "Posted " + count + " events in " + time + "ms");
        assertEquals(count, countMyEvent);
    }

    @Test
    public void testMultipleSubscribeMethodsForEvent() {
        eventBus.register(this);
        MyEvent event = new MyEvent();
        eventBus.post(event);
        assertEquals(1, countMyEvent);
        assertEquals(1, countMyEvent2);
    }

    @Test
    public void testPostAfterUnregister() {
        eventBus.register(this);
        eventBus.unregister(this);
        eventBus.post("Hello");
        assertNull(lastStringEvent);
    }

    @Test
    public void testRegisterAndPostTwoTypes() {
        eventBus.register(this);
        eventBus.post(42);
        eventBus.post("Hello");
        assertEquals(1, countIntEvent);
        assertEquals(1, countStringEvent);
        assertEquals(42, lastIntEvent);
        assertEquals("Hello", lastStringEvent);
    }

    @Test
    public void testRegisterUnregisterAndPostTwoTypes() {
        eventBus.register(this);
        eventBus.unregister(this);
        eventBus.post(42);
        eventBus.post("Hello");
        assertEquals(0, countIntEvent);
        assertEquals(0, lastIntEvent);
        assertEquals(0, countStringEvent);
    }

    @Test
    public void testPostOnDifferentEventBus() {
        eventBus.register(this);
        new EventBus().post("Hello");
        assertEquals(0, countStringEvent);
    }

    @Test
    public void testPostInEventHandler() {
        RepostInteger reposter = new RepostInteger();
        eventBus.register(reposter);
        eventBus.register(this);
        eventBus.post(1);
        assertEquals(10, countIntEvent);
        assertEquals(10, lastIntEvent);
        assertEquals(10, reposter.countEvent);
        assertEquals(10, reposter.lastEvent);
    }

    @Test
    public void testHasSubscriberForEvent() throws InterruptedException {
        assertFalse(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));

        eventBus.register(this);
        assertTrue(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));

        eventBus.unregister(this);

        waitRecycler();

        assertFalse(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));
    }

    @Test
    public void testHasSubscriberForEventSuperclass() {
        assertFalse(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));

        Object subscriber = new ObjectSubscriber();
        eventBus.register(subscriber);
        assertTrue(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));

        eventBus.unregister(subscriber);

        waitRecycler();

        assertFalse(eventBus.hasSubscriberForTag(EventType.DEFAULT_TAG));
    }

    @Test
    public void testHasSubscriberForEventImplementedInterface() {
        assertFalse(eventBus.hasSubscriberForTag(1));

        Object subscriber = new CharSequenceSubscriber();
        eventBus.register(subscriber);
        assertTrue(eventBus.hasSubscriberForTag(1));

        eventBus.unregister(subscriber);

        waitRecycler();

        assertFalse(eventBus.hasSubscriberForTag(1));
    }

    private void waitRecycler() {
        SystemClock.sleep(500);//这里必须等待，因为是延迟回收
    }

    @Subscribe
    public void onEvent(String event) {
        lastStringEvent = event;
        countStringEvent++;
    }

    @Subscribe
    public void onEvent(Integer event) {
        lastIntEvent = event;
        countIntEvent++;
    }

    @Subscribe
    public void onEvent(MyEvent event) {
        countMyEvent++;
    }

    @Subscribe
    public void onEvent2(MyEvent event) {
        countMyEvent2++;
    }

    @Subscribe
    public void onEvent(MyEventExtended event) {
        countMyEventExtended++;
    }

    public static class TestActivity extends Activity {
        public String lastStringEvent;

        @Subscribe
        public void onEvent(String event) {
            lastStringEvent = event;
        }
    }

    public static class CharSequenceSubscriber {
        @Subscribe(tag = 1)
        public void onEvent(CharSequence event) {
        }
    }

    public static class ObjectSubscriber {
        @Subscribe
        public void onEvent(Object event) {
        }
    }

    public class MyEvent {
    }

    public class MyEventExtended extends MyEvent {
    }

    public class RepostInteger {
        public int lastEvent;
        public int countEvent;

        @Subscribe
        public void onEvent(Integer event) {
            lastEvent = event;
            countEvent++;
            assertEquals(countEvent, event.intValue());

            if (event < 10) {
                int countIntEventBefore = countEvent;
                eventBus.post(event + 1);
                // All our post calls will just enqueue the event, so check count is unchanged
                assertEquals(countIntEventBefore, countIntEventBefore);
            }
        }
    }

}
