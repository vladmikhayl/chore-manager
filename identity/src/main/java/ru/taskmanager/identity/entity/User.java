package ru.taskmanager.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 30)
    private String login;

    @Column(name="password_hash", nullable=false)
    private String passwordHash;

    @Column(name="timezone_offset_hours", nullable=false)
    private int timezoneOffsetHours;

    @Column(name="daily_reminder_enabled", nullable=false)
    private boolean dailyReminderEnabled;

    @Column(name="daily_reminder_time")
    private LocalTime dailyReminderTime;
}
