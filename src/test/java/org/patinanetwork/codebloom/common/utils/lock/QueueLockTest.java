package org.patinanetwork.codebloom.common.utils.lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bucket4j.BlockingBucket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class QueueLockTest {
    private final BlockingBucket bucket = mock(BlockingBucket.class);
    private QueueLock queueLock;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        queueLock = new QueueLock(bucket, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void acquireShouldAppendToEndOfQueue() throws InterruptedException {
        CountDownLatch tickerCalledLatch = new CountDownLatch(1);
        CountDownLatch releaseTickerLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
                    tickerCalledLatch.countDown();
                    releaseTickerLatch.await();
                    return null;
                })
                .when(bucket)
                .consume(1);

        executor.submit(() -> {
            try {
                queueLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(tickerCalledLatch.await(2, TimeUnit.SECONDS));

        CountDownLatch done = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                queueLock.acquire();
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        releaseTickerLatch.countDown();

        assertTrue(done.await(2, TimeUnit.SECONDS));
        verify(bucket, times(2)).consume(1);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void acquireFastShouldAppendToStartOfQueue() throws InterruptedException {
        CountDownLatch tickerCallLatch = new CountDownLatch(1);
        CountDownLatch releaseTicker = new CountDownLatch(1);

        doAnswer(invocation -> {
                    tickerCallLatch.countDown();
                    releaseTicker.await();
                    return null;
                })
                .when(bucket)
                .consume(1);

        executor.submit(() -> {
            try {
                queueLock.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(tickerCallLatch.await(2, TimeUnit.SECONDS));

        List<String> orderedCalls = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch doneLatch = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                queueLock.acquire();
                orderedCalls.add("T2");
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        executor.submit(() -> {
            try {
                queueLock.acquireFast();
                orderedCalls.add("T3");
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(100);

        releaseTicker.countDown();

        assertTrue(doneLatch.await(2, TimeUnit.SECONDS));
        assertEquals("T3", orderedCalls.get(0));
        assertEquals("T2", orderedCalls.get(1));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void acquireShouldWorkNormally() throws InterruptedException {
        queueLock.acquire();
        verify(bucket, times(1)).consume(1);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void acquireShouldThrowInterruptedExceptionWhenInterrupted() throws InterruptedException {
        CountDownLatch ticketCalledLatch = new CountDownLatch(1);
        doAnswer(invocation -> {
                    ticketCalledLatch.countDown();
                    Thread.sleep(10000);
                    return null;
                })
                .when(bucket)
                .consume(1);

        CountDownLatch exceptionThrownLatch = new CountDownLatch(1);
        AtomicReference<Thread> interrupt = new AtomicReference<>();
        CountDownLatch readyLatch = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                queueLock.acquire();
            } catch (InterruptedException e) {
            }
        });

        assertTrue(ticketCalledLatch.await(2, TimeUnit.SECONDS));

        executor.submit(() -> {
            interrupt.set(Thread.currentThread());
            readyLatch.countDown();
            try {
                queueLock.acquire();
            } catch (InterruptedException e) {
                exceptionThrownLatch.countDown();
            }
        });

        assertTrue(readyLatch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);

        interrupt.get().interrupt();

        assertTrue(exceptionThrownLatch.await(2, TimeUnit.SECONDS));
    }
}
