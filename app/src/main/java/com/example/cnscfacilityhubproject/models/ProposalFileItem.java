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
    private Object uploadedAt;

    public ProposalFileItem() {
    }

    public ProposalFileItem(String fileName, String fileUrl, String fileType) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.uploadedAt = FieldValue.serverTimestamp();
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

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", getFileName());
        map.put("fileUrl", getFileUrl());
        map.put("fileType", getFileType());
        map.put("uploadedAt", uploadedAt != null ? uploadedAt : FieldValue.serverTimestamp());
        return map;
    }
}
