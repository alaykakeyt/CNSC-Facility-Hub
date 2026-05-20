package com.example.cnscfacilityhubproject.models;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/** One scheduled day with its own time range. */
public class ScheduleDayItem {

    private String dateText;
    private Timestamp date;
    private String startTimeText;
    private String endTimeText;

    public ScheduleDayItem() {
    }

    public ScheduleDayItem(String dateText, Timestamp date, String startTimeText, String endTimeText) {
        this.dateText = dateText;
        this.date = date;
        this.startTimeText = startTimeText;
        this.endTimeText = endTimeText;
    }

    public String getDateText() {
        return dateText != null ? dateText : "";
    }

    public Timestamp getDate() {
        return date;
    }

    public String getStartTimeText() {
        return startTimeText != null ? startTimeText : "";
    }

    public String getEndTimeText() {
        return endTimeText != null ? endTimeText : "";
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("dateText", getDateText());
        if (date != null) {
            map.put("date", date);
        }
        map.put("startTimeText", getStartTimeText());
        map.put("endTimeText", getEndTimeText());
        return map;
    }
}
