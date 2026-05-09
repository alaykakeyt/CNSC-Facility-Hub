package com.example.cnscfacilityhubproject.models;

public class RequestModel {
    private String documentId;
    private String purpose;
    private String facility;
    private String status;
    private String dateOfActivity;
    private String timeStartText;

    public RequestModel() {}

    public void setDocumentId(String id) {
        this.documentId = id;
    }

    // getters and setters
}
