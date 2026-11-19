package org.patinanetwork.codebloom.utilities;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submitterContext = MDC.getCopyOfContextMap();

        return () -> {
            Map<String, String> workerContext = MDC.getCopyOfContextMap();
            setContext(submitterContext);

            try {
                runnable.run();
            } finally {
                setContext(workerContext);
            }
        };
    }

    private static void setContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
