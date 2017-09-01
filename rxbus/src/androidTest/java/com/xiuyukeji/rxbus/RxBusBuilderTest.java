package com.xiuyukeji.rxbus;

import org.junit.Test;

public class RxBusBuilderTest extends AbstractRxBusTest {

    @Test
    public void testThrowSubscriberException() {
        eventBus.register(new SubscriberExceptionEventTracker());
        eventBus.register(new ThrowingSubscriber());
        eventBus.post("Foo");
    }

    @Test
    public void testDoNotSendSubscriberExceptionEvent() {
        eventBus.register(new SubscriberExceptionEventTracker());
        eventBus.register(new ThrowingSubscriber());
        eventBus.post("Foo");
        assertEventCount(1);
    }

    @Test
    public void testDoNotSendNoSubscriberEvent() {
        eventBus.register(new NoSubscriberEventTracker());
        eventBus.post("Foo");
        assertEventCount(1);
    }

    @Test
    public void testEventInheritance() {
        eventBus.register(new ThrowingSubscriber());
        eventBus.post("Foo");
    }

    public class SubscriberExceptionEventTracker {
        @Subscribe
        public void onEvent(String event) {
            trackEvent(event);
        }
    }

    public class NoSubscriberEventTracker {
        @Subscribe
        public void onEvent(String event) {
            trackEvent(event);
        }
    }

    public class ThrowingSubscriber {
        @Subscribe
        public void onEvent(Object event) {
            throw new RuntimeException();
        }
    }

}
