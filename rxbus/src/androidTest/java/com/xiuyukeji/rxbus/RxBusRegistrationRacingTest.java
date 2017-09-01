package com.xiuyukeji.rxbus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;

public class RxBusRegistrationRacingTest extends AbstractRxBusTest {

    // On a Nexus 5, bad synchronization always failed on the first iteration or went well completely.
    // So a high number of iterations do not guarantee a better probability of finding bugs.
    private static final int ITERATIONS = LONG_TESTS ? 1000 : 10;
    private static final int THREAD_COUNT = 16;

    volatile CountDownLatch startLatch;
    volatile CountDownLatch registeredLatch;
    volatile CountDownLatch canUnregisterLatch;
    volatile CountDownLatch unregisteredLatch;
    
    final Executor threadPool = Executors.newCachedThreadPool();

    @Test
    public void testRacingRegistrations() throws InterruptedException {
        for (int i = 0; i < ITERATIONS; i++) {
            startLatch = new CountDownLatch(THREAD_COUNT);
            registeredLatch = new CountDownLatch(THREAD_COUNT);
            canUnregisterLatch = new CountDownLatch(1);
            unregisteredLatch = new CountDownLatch(THREAD_COUNT);
            
            List<SubscriberThread> threads = startThreads();
            registeredLatch.await();
            eventBus.post("42");
            canUnregisterLatch.countDown();
            for (int t = 0; t < THREAD_COUNT; t++) {
                int eventCount = threads.get(t).eventCount;
                if (eventCount != 1) {
                    fail("Failed in iteration " + i + ": thread #" + t + " has event count of " + eventCount);
                }
            }
            // Wait for threads to be done
            unregisteredLatch.await();
        }
    }

    private List<SubscriberThread> startThreads() {
        List<SubscriberThread> threads = new ArrayList<SubscriberThread>(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            SubscriberThread thread = new SubscriberThread();
            threadPool.execute(thread);
            threads.add(thread);
        }
        return threads;
    }

    public class SubscriberThread implements Runnable {
        volatile int eventCount;

        @Override
        public void run() {
            countDownAndAwaitLatch(startLatch, 10);
            eventBus.register(this);
            registeredLatch.countDown();
            try {
                canUnregisterLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            eventBus.unregister(this);
            unregisteredLatch.countDown();
        }

        @Subscribe
        public void onEvent(String event) {
            eventCount++;
        }

    }

}
