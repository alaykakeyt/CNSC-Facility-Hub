package com.example.cnscfacilityhubproject.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.cnscfacilityhubproject.models.ProposalFileItem;

/**
 * Opens proposal files using HTTPS download URLs from Firebase Storage.
 */
public final class FileOpener {

    private FileOpener() {
    }

    public static void open(Context context, ProposalFileItem file) {
        if (file == null || file.getFileUrl().isEmpty()) {
            Toast.makeText(context, "File link is not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        openUrl(context, file.getFileUrl(), file.getFileName());
    }

    public static void openUrl(Context context, String url, String label) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    context,
                    "No app found to open " + (label != null ? label : "file") + ".",
                    Toast.LENGTH_SHORT
            ).show();
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open file.", Toast.LENGTH_SHORT).show();
        }
    }
}
