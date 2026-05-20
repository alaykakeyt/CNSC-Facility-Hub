package com.example.cnscfacilityhubproject.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/** Uploaded proposal / supporting file metadata stored in Firestore. */
public class ProposalFileItem {

    private String fileName;
    private String fileUrl;
    private String fileType;
    private String source;

    public ProposalFileItem() {
    }

    public ProposalFileItem(String fileName, String fileUrl, String fileType, String source) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.source = source;
    }

    public ProposalFileItem(String fileName, String fileUrl, String fileType) {
        this(fileName, fileUrl, fileType, "external_link");
    }

    public String getFileName() {
        return fileName != null ? fileName : "";
    }

    public String getFileUrl() {
        return fileUrl != null ? fileUrl : "";
    }

    public String getFileType() {
        return fileType != null ? fileType : "";
    }

    public String getSource() {
        return source != null ? source : "external_link";
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", getFileName());
        map.put("fileUrl", getFileUrl());
        map.put("fileType", getFileType());
        map.put("source", getSource());
        return map;
    }
}
