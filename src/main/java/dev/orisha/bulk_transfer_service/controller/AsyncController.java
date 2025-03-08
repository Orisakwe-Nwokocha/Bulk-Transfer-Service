package dev.orisha.bulk_transfer_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@RestController
public class AsyncController {

    private static final Logger log = LoggerFactory.getLogger(AsyncController.class);
    private final AsyncService asyncService;
    private final ThreadPoolTaskExecutor executor;
    private final List<String> failedTasks = new ArrayList<>();

    public AsyncController(AsyncService asyncService, @Qualifier("taskExecutor") Executor executor) {
        this.asyncService = asyncService;
        this.executor = (ThreadPoolTaskExecutor) executor;
    }

    @GetMapping("/start")
    public String startAsyncTasks(@RequestParam(defaultValue = "100") int num) {
        log.info("REST request to start async {} tasks", num);

        int counter = 1;
        try {
            while (counter <= num) {
                asyncService.performTask("Task-" + counter);
                counter++;
            }
        } catch (RejectedExecutionException exception) {
            log.error("{} tasks rejected. Error: {}", (num - counter) + 1, exception.getMessage());

            while (counter <= num) {
                failedTasks.add("Task-" + counter);
                counter++;
            }
        }

        return "Request received";
    }

    @GetMapping
    public Object getCompletedTasks() throws InterruptedException {
        checkThreadPoolStatus();

        TasksDto completedTasks = asyncService.getCompletedTasks();
        completedTasks.setFailedTasks(failedTasks);
        completedTasks.setFailedTasksSize(failedTasks.size());

        log.info("Completed tasks: {}", completedTasks);
        return completedTasks;
    }

    @GetMapping("/reset")
    public String resetCompletedTasks() {
        asyncService.resetCompletedTasks();
        failedTasks.clear();
        return "Reset completed tasks!";
    }

    public void checkThreadPoolStatus() throws InterruptedException {
        System.out.println("Active Threads: " + executor.getActiveCount());
        System.out.println("Pool Size: " + executor.getPoolSize());
        System.out.println("Max Pool Size: " + executor.getMaxPoolSize());
        int queueSize = executor.getQueueSize();
        System.out.println("Queue Size: " + queueSize);

        if (queueSize != 0) {
            log.info("The number of active threads is {} and pending tasks is {}", executor.getActiveCount(), queueSize);
            Thread.sleep(7000);
            checkThreadPoolStatus();
        }
    }

}
