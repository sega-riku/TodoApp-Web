package com.sega.todoappweb.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskGroup {

    private LocalDate deadline;
    private String title;
    private List<Task> tasks;

    public TaskGroup(LocalDate deadline, String title) {
        this.deadline = deadline;
        this.title = title;
        this.tasks = new ArrayList<>();
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getTitle() {
        return title;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }
}