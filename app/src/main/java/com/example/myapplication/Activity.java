package com.example.myapplication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activities")
public class Activity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private long dateTime; // timestamp in milliseconds
    private int reminderMinutesBefore;

    // Constructor, getters and setters
    public Activity(String title, String description, long dateTime, int reminderMinutesBefore) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.reminderMinutesBefore = reminderMinutesBefore;
    }
    // Getters and setters for all fields
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getDateTime() { return dateTime; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }
    public int getReminderMinutesBefore() { return reminderMinutesBefore; }
    public void setReminderMinutesBefore(int reminderMinutesBefore)
    { this.reminderMinutesBefore = reminderMinutesBefore; }
}