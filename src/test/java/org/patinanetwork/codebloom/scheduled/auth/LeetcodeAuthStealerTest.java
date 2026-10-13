package org.patinanetwork.codebloom.scheduled.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.patinanetwork.codebloom.common.db.models.auth.Auth;
import org.patinanetwork.codebloom.common.db.repos.auth.AuthRepository;
import org.patinanetwork.codebloom.common.env.Env;
import org.patinanetwork.codebloom.common.lag.FakeLag;
import org.patinanetwork.codebloom.common.redis.RedisClient;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.playwright.PlaywrightClient;
import org.slf4j.LoggerFactory;

public class LeetcodeAuthStealerTest {
    private LeetcodeAuthStealer leetcodeAuthStealer;

    private RedisClient redisClient;
    private AuthRepository authRepository;
    private Reporter reporter;
    private Env env;
    private PlaywrightClient playwrightClient;

    private ListAppender<ILoggingEvent> logWatcher;

    public LeetcodeAuthStealerTest() {
        redisClient = mock(RedisClient.class);
        authRepository = mock(AuthRepository.class);
        reporter = mock(Reporter.class);
        env = mock(Env.class);
        playwrightClient = mock(PlaywrightClient.class);
    }

    @BeforeEach
    void setup() {
        leetcodeAuthStealer =
                spy(new LeetcodeAuthStealer(redisClient, authRepository, reporter, env, playwrightClient));

        logWatcher = new ListAppender<>();
        logWatcher.start();
        ((Logger) LoggerFactory.getLogger(leetcodeAuthStealer.getClass())).addAppender(logWatcher);

        when(env.isCi()).thenReturn(false);
        playwrightClientResolvesSlowly(Auth.builder().build());
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(leetcodeAuthStealer.getClass())).detachAndStopAllAppenders();
    }

    private void playwrightClientResolvesSlowly(Auth authToReturn) {
        when(playwrightClient.getLeetcodeCookie(any(), any())).thenAnswer(invocation -> {
            FakeLag.sleep(1000);
            return Optional.ofNullable(authToReturn);
        });
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies multiple threads can acquire read locks concurrently across the same thread pool - Auth Repository")
    void testReadLockConcurrentAccessSameThreadPoolAuthRepo() throws InterruptedException {
        Auth mockAuth = Auth.builder()
                .token("test-token")
                .csrf(Optional.of("test-csrf"))
                .createdAt(StandardizedOffsetDateTime.now())
                .build();
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.of(mockAuth));

        leetcodeAuthStealer.stealAuthCookie();

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);
        AtomicInteger concurrentReads = new AtomicInteger(0);
        AtomicInteger maxConcurrentReads = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        verify(leetcodeAuthStealer, never()).stealCookieImpl();

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(maxConcurrentReads.get() > 1, "Multiple read locks should be acquired concurrently");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies multiple threads can acquire read locks concurrently across the same thread pool - New cookie fetched")
    void testReadLockConcurrentAccessSameThreadPoolNewCookieFetched() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());
        doReturn("cookie").when(leetcodeAuthStealer).stealCookieImpl();
        doReturn("cookie").when(leetcodeAuthStealer).getCookie();

        leetcodeAuthStealer.stealAuthCookie();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(5);
        AtomicInteger concurrentReads = new AtomicInteger(0);
        AtomicInteger maxConcurrentReads = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(maxConcurrentReads.get() > 1, "Multiple read locks should be acquired concurrently");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies multiple threads can acquire read locks concurrently across different thread pools - Auth Repository")
    void testReadLockConcurrentAccessDifferentThreadPoolsAuthRepository() throws InterruptedException {
        Auth mockAuth = Auth.builder()
                .token("test-token")
                .csrf(Optional.of("test-csrf"))
                .createdAt(StandardizedOffsetDateTime.now())
                .build();
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.of(mockAuth));

        leetcodeAuthStealer.stealAuthCookie();

        ExecutorService pool1 = Executors.newFixedThreadPool(3);
        ExecutorService pool2 = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(6);
        AtomicInteger concurrentReads = new AtomicInteger(0);
        AtomicInteger maxConcurrentReads = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            pool1.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < 3; i++) {
            pool2.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        pool1.shutdown();

        assertTrue(
                maxConcurrentReads.get() > 1,
                "Multiple read locks should be acquired concurrently across different thread pools");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies multiple threads can acquire read locks concurrently across different thread pools - New cookie fetched")
    void testReadLockConcurrentAccessDifferentThreadPoolsFetchedNewCookie() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());
        doReturn("string").when(leetcodeAuthStealer).stealCookieImpl();
        doReturn("string").when(leetcodeAuthStealer).getCookie();

        leetcodeAuthStealer.stealAuthCookie();

        ExecutorService pool1 = Executors.newFixedThreadPool(3);
        ExecutorService pool2 = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(6);
        AtomicInteger concurrentReads = new AtomicInteger(0);
        AtomicInteger maxConcurrentReads = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            pool1.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < 3; i++) {
            pool2.submit(() -> {
                try {
                    startLatch.await();
                    int current = concurrentReads.incrementAndGet();
                    maxConcurrentReads.updateAndGet(max -> Math.max(max, current));

                    String cookie = leetcodeAuthStealer.getCookie();
                    assertNotNull(cookie);
                    assertEquals("test-token", cookie);

                    Thread.sleep(100);
                    concurrentReads.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        pool1.shutdown();

        assertTrue(
                maxConcurrentReads.get() > 1,
                "Multiple read locks should be acquired concurrently across different thread pools");
    }

    @Test
    @Timeout(15)
    @DisplayName("Verifies that only one thread can try to steal cookie at a time across the same thread pool")
    void testWriteLockExclusiveAccessSameThreadPool() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        AtomicBoolean isOneWriteBlockingOtherWrites = new AtomicBoolean(false);

        pool.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.stealAuthCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.writeLock().tryLock()) {
                        isOneWriteBlockingOtherWrites.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(
                isOneWriteBlockingOtherWrites.get(),
                "Write locks should be exclusive within the same thread pool - only one at a time");
    }

    @Test
    @Timeout(15)
    @DisplayName("Verifies that only one thread can try to steal cookie at a time across different thread pools")
    void testWriteLockExclusiveAccessDifferentThreadPools() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());

        ExecutorService pool1 = Executors.newFixedThreadPool(2);
        ExecutorService pool2 = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        AtomicBoolean isOneWriteBlockingOtherWrites = new AtomicBoolean(false);

        pool1.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.stealAuthCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            pool2.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.writeLock().tryLock()) {
                        isOneWriteBlockingOtherWrites.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        pool1.shutdown();
        pool2.shutdown();

        assertTrue(
                isOneWriteBlockingOtherWrites.get(),
                "Write locks should be exclusive across different thread pools - only one at a time");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies that no read operations can happen when stealing authentication cookie in the same thread pool")
    void testReadWriteLockInteractionSameThreadPool() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        AtomicBoolean readBlockedByWrite = new AtomicBoolean(false);

        pool.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.stealAuthCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.readLock().tryLock()) {
                        readBlockedByWrite.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(8, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(readBlockedByWrite.get(), "Read operations should be blocked when write lock is held");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies that no read operations can happen when stealing authentication cookie in different thread pools")
    void testReadWriteLockInteractionDifferentThreadPools() throws InterruptedException {
        when(authRepository.getMostRecentAuth()).thenReturn(Optional.empty());

        ExecutorService writePool = Executors.newFixedThreadPool(1);
        ExecutorService readPool = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        AtomicBoolean readBlockedByWrite = new AtomicBoolean(false);

        writePool.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.stealAuthCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            readPool.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.readLock().tryLock()) {
                        readBlockedByWrite.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(8, TimeUnit.SECONDS));
        writePool.shutdown();
        readPool.shutdown();

        assertTrue(
                readBlockedByWrite.get(),
                "Read operations from different thread pool should wait for write lock to be released");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies that no read operations can happen when reloading authentication cookie in the same thread pool")
    void testReloadCookieReadWriteLockInteractionSameThreadPool() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        AtomicBoolean readBlockedByWrite = new AtomicBoolean(false);

        pool.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.reloadCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.readLock().tryLock()) {
                        readBlockedByWrite.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(8, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(readBlockedByWrite.get(), "Read operations should be blocked when write lock is held");
    }

    @Test
    @Timeout(10)
    @DisplayName(
            "Verifies that no read operations can happen when reloading authentication cookie in different thread pools")
    void testReloadCookieReadWriteLockInteractionDifferentThreadPools() throws InterruptedException {
        ExecutorService writePool = Executors.newFixedThreadPool(1);
        ExecutorService readPool = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        AtomicBoolean readBlockedByWrite = new AtomicBoolean(false);

        writePool.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100);
                leetcodeAuthStealer.reloadCookie();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        for (int i = 0; i < 3; i++) {
            readPool.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(200);

                    if (!leetcodeAuthStealer.LOCK.readLock().tryLock()) {
                        readBlockedByWrite.set(true);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(8, TimeUnit.SECONDS));
        writePool.shutdown();
        readPool.shutdown();

        assertTrue(
                readBlockedByWrite.get(),
                "Read operations from different thread pool should wait for write lock to be released");
    }

    @Test
    @Timeout(10)
    @DisplayName("reloadCookie - If one thread is stealing cookie, other thread will bounce")
    void testReloadCookieIfOneThreadIsStealingCookieOtherThreadWillBounce() throws InterruptedException {
        ExecutorService writePool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        writePool.execute(() -> {
            leetcodeAuthStealer.reloadCookie();
        });

        AtomicReference<Optional<String>> ref = new AtomicReference<>();
        writePool.execute(() -> {
            try {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ref.set(leetcodeAuthStealer.reloadCookie().join());
            } finally {
                latch.countDown();
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(ref.get().isEmpty());
    }

    @Test
    @Timeout(10)
    @DisplayName("stealAuthCookie - If one thread is stealing cookie, other thread will bounce")
    void testStealAuthCookieIfOneThreadIsStealingCookieOtherThreadWillBounce() throws InterruptedException {
        ExecutorService writePool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        writePool.execute(() -> {
            leetcodeAuthStealer.stealAuthCookie();
        });

        writePool.execute(() -> {
            try {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                leetcodeAuthStealer.stealAuthCookie();
            } finally {
                latch.countDown();
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        assertTrue(logWatcher.list.stream()
                .anyMatch(log -> log.getLevel().equals(Level.INFO)
                        && log.getFormattedMessage().contains("Lock failed to be acquired, bouncing...")));
    }
}
