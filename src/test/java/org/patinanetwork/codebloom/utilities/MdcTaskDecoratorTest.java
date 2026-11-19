package org.patinanetwork.codebloom.utilities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

public class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();
    private final ExecutorService singleWorker = Executors.newSingleThreadExecutor();

    @AfterEach
    void teardown() {
        MDC.clear();
        singleWorker.shutdownNow();
    }

    @Test
    void taskSeesSubmitterRequestIdAndWorkerIsRestoredAfterwards() throws Exception {
        singleWorker.submit(() -> MDC.put("workerKey", "worker")).get();

        MDC.put(RequestIdFilter.MDC_KEY, "req-1");
        AtomicReference<Map<String, String>> seenByTask = new AtomicReference<>();
        Runnable capturing = decorator.decorate(() -> seenByTask.set(MDC.getCopyOfContextMap()));
        MDC.clear();

        singleWorker.submit(capturing).get();
        Map<String, String> workerAfter =
                singleWorker.submit(MDC::getCopyOfContextMap).get();

        assertEquals(Map.of(RequestIdFilter.MDC_KEY, "req-1"), seenByTask.get());
        assertEquals(Map.of("workerKey", "worker"), workerAfter);
    }

    @Test
    void taskSubmittedWithoutContextDoesNotInheritPreviousRequestId() throws Exception {
        MDC.put(RequestIdFilter.MDC_KEY, "req-1");
        singleWorker.submit(decorator.decorate(() -> {})).get();
        MDC.clear();

        AtomicReference<String> seenByTask = new AtomicReference<>("unset");
        singleWorker
                .submit(decorator.decorate(() -> seenByTask.set(MDC.get(RequestIdFilter.MDC_KEY))))
                .get();

        assertNull(seenByTask.get());
    }
}
