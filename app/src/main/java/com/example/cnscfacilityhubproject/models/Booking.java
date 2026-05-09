package com.example.cnscfacilityhubproject.models;

public class Booking {
    private String title;
    private String facility;
    private String date;
    private String startTime;
    private String endTime;
    private String status;

    public Booking() {} // Required for Firebase

    public Booking(String title, String facility, String date,
                   String startTime, String endTime, String status) {
        this.title = title;
        this.facility = facility;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getTitle() { return title; }
    public String getFacility() { return facility; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
}