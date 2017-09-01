package com.xiuyukeji.rxbus;

import org.junit.Test;

public class RxBusGenericsTest extends AbstractRxBusTest {
    public static class GenericEvent<T> {
        T value;
    }

    public class GenericEventSubscriber<T> {
        @Subscribe
        public void onGenericEvent(GenericEvent<T> event) {
            trackEvent(event);
        }
    }

    public class FullGenericEventSubscriber<T> {
        @Subscribe
        public void onGenericEvent(T event) {
            trackEvent(event);
        }
    }

    public class GenericNumberEventSubscriber<T extends Number> {
        @Subscribe
        public void onGenericEvent(T event) {
            trackEvent(event);
        }
    }

    public class GenericFloatEventSubscriber extends GenericNumberEventSubscriber<Float> {
    }

    @Test
    public void testGenericEventAndSubscriber() {
        GenericEventSubscriber<IntTestEvent> genericSubscriber = new GenericEventSubscriber<IntTestEvent>();
        eventBus.register(genericSubscriber);
        eventBus.post(new GenericEvent<Integer>());
        assertEventCount(1);
    }

    @Test
    public void testGenericEventAndSubscriber_TypeErasure() {
        FullGenericEventSubscriber<IntTestEvent> genericSubscriber = new FullGenericEventSubscriber<IntTestEvent>();
        eventBus.register(genericSubscriber);
        eventBus.post(new IntTestEvent(42));
        eventBus.post("Type erasure!");
        assertEventCount(2);
    }

    @Test
    public void testGenericEventAndSubscriber_BaseType() {
        GenericNumberEventSubscriber<Float> genericSubscriber = new GenericNumberEventSubscriber<>();
        eventBus.register(genericSubscriber);
        eventBus.post(new Float(42));
        eventBus.post(new Double(23));
        assertEventCount(2);
        eventBus.post("Not the same base type");
        assertEventCount(2);
    }

    @Test
    public void testGenericEventAndSubscriber_Subclass() {
        GenericFloatEventSubscriber genericSubscriber = new GenericFloatEventSubscriber();
        eventBus.register(genericSubscriber);
        eventBus.post(new Float(42));
        eventBus.post(new Double(77));
        assertEventCount(2);
        eventBus.post("Not the same base type");
        assertEventCount(2);
    }
}
