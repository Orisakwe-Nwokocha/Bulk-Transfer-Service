package dev.orisha.bulk_transfer_service.services;

import dev.orisha.bulk_transfer_service.dto.TasksDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AsyncService {
    private final Queue<String> completedTasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger counter = new AtomicInteger(0);

    @Async
    public void performTask(String taskName) {
//        System.out.println(Thread.currentThread().getName() + " - Started task: " + taskName);
        try {
            counter.incrementAndGet();
            completedTasks.add(taskName);
            Thread.sleep(3000);
        } catch (Exception e) {
            log.info("Error while performing task {} {}", taskName, e.getMessage());
        }
        System.out.println(Thread.currentThread().getName() + " - Completed task: " + taskName);
    }

    public TasksDto getCompletedTasks() {
        log.info("Counter: {}", counter);
        return new TasksDto(completedTasks);
    }

    public void resetCompletedTasks() {
        counter.set(0);
        completedTasks.clear();
    }

}
