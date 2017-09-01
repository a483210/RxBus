package com.xiuyukeji.rxbus;

import android.os.Handler;
import android.os.Looper;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class RxBusMainThreadRacingTest extends AbstractRxBusTest {

    private static final int ITERATIONS = LONG_TESTS ? 100000 : 1000;

    protected boolean unregistered;
    private CountDownLatch startLatch;
    private volatile RuntimeException failed;

    @Test
    public void testRacingThreads() throws InterruptedException {
        Runnable register = new Runnable() {
            @Override
            public void run() {
                eventBus.register(RxBusMainThreadRacingTest.this);
                unregistered = false;
            }
        };

        Runnable unregister = new Runnable() {
            @Override
            public void run() {
                eventBus.unregister(RxBusMainThreadRacingTest.this);
                unregistered = true;
            }
        };

        startLatch = new CountDownLatch(2);
        BackgroundPoster backgroundPoster = new BackgroundPoster();
        backgroundPoster.start();
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            Random random = new Random();
            countDownAndAwaitLatch(startLatch, 10);
            for (int i = 0; i < ITERATIONS; i++) {
                handler.post(register);
                Thread.sleep(0, random.nextInt(300)); // Sleep just some nanoseconds, timing is crucial here
                handler.post(unregister);
                if (failed != null) {
                    throw new RuntimeException("Failed in iteration " + i, failed);
                }
                // Don't let the queue grow to avoid out-of-memory scenarios
                waitForHandler(handler);
            }
        } finally {
            backgroundPoster.running = false;
            backgroundPoster.join();
        }
    }

    protected void waitForHandler(Handler handler) {
        final CountDownLatch doneLatch = new CountDownLatch(1);
        handler.post(new Runnable() {

            @Override
            public void run() {
                doneLatch.countDown();
            }
        });
        awaitLatch(doneLatch, 10);
    }

    @Subscribe(mode = ThreadMode.MAIN)
    public void onEventMainThread(String event) {
        trackEvent(event);
        if (unregistered) {
            failed = new RuntimeException("Main thread event delivered while unregistered on received event #"
                    + eventCount);
        }
    }

    class BackgroundPoster extends Thread {
        volatile boolean running = true;

        public BackgroundPoster() {
            super("BackgroundPoster");
        }

        @Override
        public void run() {
            countDownAndAwaitLatch(startLatch, 10);
            while (running) {
                eventBus.post("Posted in background");
                if (Math.random() > 0.9f) {
                    // Single cores would take very long without yielding
                    Thread.yield();
                }
            }
        }

    }

}
