package com.sega.todoappweb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDateGroup {

    private LocalDate deadline;
    private List<TaskGroup> groups;

    public TaskDateGroup(LocalDate deadline) {
        this.deadline = deadline;
        this.groups = new ArrayList<>();
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public List<TaskGroup> getGroups() {
        return groups;
    }

    public void addGroup(TaskGroup group) {
        groups.add(group);
    }
}