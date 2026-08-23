package com.sega.todoappweb.admin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminNotificationRepository
    extends JpaRepository<AdminNotification, Long> {

}