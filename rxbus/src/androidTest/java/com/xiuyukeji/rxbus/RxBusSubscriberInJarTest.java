package com.xiuyukeji.rxbus;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RxBusSubscriberInJarTest extends TestCase {
    protected RxBus eventBus = RxBus.get();

    @Test
    public void testSubscriberInJar() {
        SubscriberInJar subscriber = new SubscriberInJar();
        eventBus.register(subscriber);
        eventBus.post("Hi Jar");
        eventBus.post(42);
        Assert.assertEquals(1, subscriber.getCollectedStrings().size());
        Assert.assertEquals("Hi Jar", subscriber.getCollectedStrings().get(0));
    }

    public static class SubscriberInJar {
        List<String> collectedStrings = new ArrayList();

        public SubscriberInJar() {
        }

        @Subscribe
        public void collectString(String string) {
            this.collectedStrings.add(string);
        }

        public List<String> getCollectedStrings() {
            return this.collectedStrings;
        }
    }

}
