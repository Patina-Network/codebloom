package org.patinanetwork.codebloom.utilities.exception;

import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.env.Env;
import org.patinanetwork.codebloom.common.reporter.Reporter;
import org.patinanetwork.codebloom.common.reporter.report.Report;
import org.patinanetwork.codebloom.common.reporter.report.location.Location;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

/** Add {@link Reporter} to Spring Boot `@Scheduled` methods. */
@Configuration
@Slf4j
public class ScheduledTaskExceptionHandler {

    private final Reporter errorReporter;
    private final Env env;

    public ScheduledTaskExceptionHandler(final Reporter errorReporter, final Env env) {
        this.errorReporter = errorReporter;
        this.env = env;
    }

    /**
     * * Default Spring Boot task scheduler:
     * https://github.com/spring-projects/spring-framework/blob/main/spring-context/src/main/java/org/springframework/scheduling/config/ScheduledTaskRegistrar.java#L420-L421
     */
    @Bean
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true);

        scheduler.setErrorHandler(throwable -> {
            errorReporter.error(
                    "taskScheduler",
                    Report.builder()
                            .environments(env.getActiveProfiles())
                            .location(Location.BACKEND)
                            .data(Reporter.throwableToString(throwable))
                            .build());

            log.error(throwable.getMessage(), throwable);
        });

        return scheduler;
    }
}
