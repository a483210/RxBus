package com.xiuyukeji.rxbus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class RxBusFallbackToReflectionTest extends AbstractRxBusTest {
    public class PrivateEvent {
    }

    public class PublicClass {
        @Subscribe
        public void onEvent(Object any) {
            trackEvent(any);
        }
    }

    public class PrivateClass {
        @Subscribe
        public void onEvent(Object any) {
            trackEvent(any);
        }
    }

    public class PublicWithPrivateSuperClass extends PrivateClass {
        @Subscribe
        public void onEvent(String any) {
            trackEvent(any);
        }
    }

    public class PublicClassWithPrivateEvent {
        @Subscribe
        public void onEvent(PrivateEvent any) {
            trackEvent(any);
        }
    }

    public class PublicClassWithPublicAndPrivateEvent {
        @Subscribe
        public void onEvent(String any) {
            trackEvent(any);
        }

        @Subscribe
        public void onEvent(PrivateEvent any) {
            trackEvent(any);
        }
    }

    public class PublicWithPrivateEventInSuperclass extends PublicClassWithPrivateEvent {
        @Subscribe
        public void onEvent(Object any) {
            trackEvent(any);
        }
    }

    public RxBusFallbackToReflectionTest() {
        super(true);
    }

    @Test
    public void testAnonymousSubscriberClass() {
        Object subscriber = new Object() {//该方法在运行时生效，无法在编译期间注解
            @Subscribe
            public void onEvent(String event) {
                trackEvent(event);
            }
        };
        eventBus.register(subscriber);

        eventBus.post("Hello");
        assertNotEquals("Hello", lastEvent);
        assertNotEquals(1, eventsReceived.size());
    }

    @Test
    public void testAnonymousSubscriberClassWithPublicSuperclass() {
        eventBus.register(new PublicClass());

        eventBus.post("Hello");
        assertEquals("Hello", lastEvent);
        assertEquals(1, eventsReceived.size());
    }

    @Test
    public void testAnonymousSubscriberClassWithPrivateSuperclass() {
        eventBus.register(new PublicWithPrivateSuperClass());
        eventBus.post("Hello");
        assertEquals("Hello", lastEvent);
        assertEquals(2, eventsReceived.size());
    }

    @Test
    public void testSubscriberClassWithPrivateEvent() {
        eventBus.register(new PublicClassWithPrivateEvent());
        PrivateEvent privateEvent = new PrivateEvent();
        eventBus.post(privateEvent);
        assertEquals(privateEvent, lastEvent);
        assertEquals(1, eventsReceived.size());
    }

    @Test
    public void testSubscriberClassWithPublicAndPrivateEvent() {
        eventBus.register(new PublicClassWithPublicAndPrivateEvent());

        eventBus.post("Hello");
        assertEquals("Hello", lastEvent);
        assertEquals(1, eventsReceived.size());

        PrivateEvent privateEvent = new PrivateEvent();
        eventBus.post(privateEvent);
        assertEquals(privateEvent, lastEvent);
        assertEquals(2, eventsReceived.size());
    }

    @Test
    public void testSubscriberExtendingClassWithPrivateEvent() {
        eventBus.register(new PublicWithPrivateEventInSuperclass());
        PrivateEvent privateEvent = new PrivateEvent();
        eventBus.post(privateEvent);
        assertEquals(privateEvent, lastEvent);
        assertEquals(2, eventsReceived.size());
    }

}
