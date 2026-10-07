package org.patinanetwork.codebloom.common.utils.lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.bucket4j.BlockingBucket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    void acquireFastShouldAppendToStartOfQueue() throws Exception {
        var tickerStarted = new CountDownLatch(1);
        var releaseTicker = new CountDownLatch(1);
        var releaseNormal = new CountDownLatch(1);
        var normalSelected = new CountDownLatch(1);
        var calls = new AtomicInteger();

        doAnswer(invocation -> {
                    int call = calls.incrementAndGet();
                    if (call == 1) {
                        tickerStarted.countDown();
                        releaseTicker.await();
                    } else if (call == 3) {
                        normalSelected.countDown();
                        releaseNormal.await();
                    }
                    return null;
                })
                .when(bucket)
                .consume(1);

        try {
            var initial = executor.submit(() -> {
                queueLock.acquire();
                return null;
            });
            assertTrue(tickerStarted.await(2, TimeUnit.SECONDS));

            var normal = executor.submit(() -> {
                queueLock.acquire();
                return null;
            });
            awaitQueueSize(1);

            var fast = executor.submit(() -> {
                queueLock.acquireFast();
                return null;
            });
            awaitQueueSize(2);

            releaseTicker.countDown();
            fast.get(2, TimeUnit.SECONDS);
            assertTrue(normalSelected.await(2, TimeUnit.SECONDS));
            assertFalse(normal.isDone(), "Normal request must wait until the fast request is released");

            releaseNormal.countDown();
            normal.get(2, TimeUnit.SECONDS);
            initial.get(2, TimeUnit.SECONDS);
            verify(bucket, times(3)).consume(1);
        } finally {
            releaseTicker.countDown();
            releaseNormal.countDown();
        }
    }

    private void awaitQueueSize(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (queueLock.queue.size() != expected && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        assertEquals(expected, queueLock.queue.size(), "Requests did not enter the queue in time");
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
