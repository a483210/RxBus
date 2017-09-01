package com.xiuyukeji.rxbus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RxBusSubscriberLegalTest extends AbstractRxBusTest {

    @Test
    public void testSubscriberLegal() {
        eventBus.register(this);
        eventBus.post("42");
        eventBus.unregister(this);
        assertEquals(1, eventCount.intValue());
    }

    // With build time verification, some of these tests are obsolete (and cause problems during build)
//    public void testSubscriberNotPublic() {
//        try {
//            eventBus.register(new NotPublic());
//            fail("Registration of ilegal subscriber successful");
//        } catch (EventBusException e) {
//            // Expected
//        }
//    }

//    public void testSubscriberStatic() {
//        try {
//            eventBus.register(new Static());
//            fail("Registration of ilegal subscriber successful");
//        } catch (EventBusException e) {
//            // Expected
//        }
//    }

    public void testSubscriberLegalAbstract() {
        eventBus.register(new AbstractImpl());

        eventBus.post("42");
        assertEquals(1, eventCount.intValue());
    }

    @Subscribe
    public void onEvent(String event) {
        trackEvent(event);
    }

//    public static class NotPublic {
//        @Subscribe
//        void onEvent(String event) {
//        }
//    }

    public static abstract class Abstract {
        public abstract void onEvent(String event);
    }

    public class AbstractImpl extends Abstract {

        @Override
        @Subscribe
        public void onEvent(String event) {
            trackEvent(event);
        }

    }

//    public static class Static {
//        @Subscribe
//        public static void onEvent(String event) {
//        }
//    }

}
