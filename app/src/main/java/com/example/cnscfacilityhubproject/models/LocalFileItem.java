package com.example.cnscfacilityhubproject.models;

import android.net.Uri;

/** File selected on device before upload to Firebase Storage. */
public class LocalFileItem {

    private final Uri uri;
    private final String fileName;
    private final String mimeType;

    public LocalFileItem(Uri uri, String fileName, String mimeType) {
        this.uri = uri;
        this.fileName = fileName;
        this.mimeType = mimeType != null ? mimeType : "";
    }

    public Uri getUri() {
        return uri;
    }

    public String getFileName() {
        return fileName != null ? fileName : "file";
    }

    public String getMimeType() {
        return mimeType;
    }
}
