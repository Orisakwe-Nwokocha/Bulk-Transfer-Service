package dev.orisha.bulk_transfer_service.controller;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
public class TasksDto {

    private int completedTasksSize;
    private int failedTasksSize;
    private List<String> completedTasks;
    private List<String> failedTasks;

    public TasksDto(Collection<String> completedTasks) {
        this.completedTasks = new ArrayList<>(completedTasks);
        this.completedTasks.sort(Comparator.comparingInt(this::extractTaskNumber));
        this.completedTasksSize = this.completedTasks.size();
    }

    private int extractTaskNumber(String task) {
        return Integer.parseInt(task.replaceAll("\\D+", ""));
    }

}
