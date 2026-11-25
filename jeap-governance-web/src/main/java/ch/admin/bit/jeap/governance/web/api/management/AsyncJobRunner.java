package ch.admin.bit.jeap.governance.web.api.management;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs the jobs of the management API in the background, on the executor Spring uses for {@code @Async}. Not an
 * {@link java.util.concurrent.Executor} bean on purpose: an executor bean makes Spring Boot back off from its
 * application task executor.
 */
@Component
public class AsyncJobRunner {

    @Async
    public void run(Runnable job) {
        job.run();
    }
}
