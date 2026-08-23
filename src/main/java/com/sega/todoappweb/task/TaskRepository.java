package com.sega.todoappweb.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sega.todoappweb.user.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);
    void deleteByUser(User user);

}