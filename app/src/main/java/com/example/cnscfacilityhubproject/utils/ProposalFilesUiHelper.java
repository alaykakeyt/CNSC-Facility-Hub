package com.example.cnscfacilityhubproject.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.models.ProposalFileItem;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Builds a list of buttons to open uploaded proposal files on detail screens.
 */
public final class ProposalFilesUiHelper {

    private ProposalFilesUiHelper() {
    }

    public static void bindFiles(
            Context context,
            LinearLayout container,
            TextView emptyLabel,
            MaterialButton legacyButton,
            List<ProposalFileItem> files
    ) {
        if (container == null) {
            return;
        }

        container.removeAllViews();

        if (files == null || files.isEmpty()) {
            container.setVisibility(View.GONE);
            if (emptyLabel != null) {
                emptyLabel.setVisibility(View.VISIBLE);
                emptyLabel.setText("No proposal files uploaded.");
            }
            if (legacyButton != null) {
                legacyButton.setVisibility(View.GONE);
            }
            return;
        }

        container.setVisibility(View.VISIBLE);
        if (emptyLabel != null) {
            emptyLabel.setVisibility(View.GONE);
        }
        if (legacyButton != null) {
            legacyButton.setVisibility(View.GONE);
        }

        for (int i = 0; i < files.size(); i++) {
            ProposalFileItem file = files.get(i);
            MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            if (i > 0) {
                ((LinearLayout.LayoutParams) button.getLayoutParams()).topMargin = dp(context, 8);
            }
            button.setAllCaps(false);
            button.setText("Open: " + file.getFileName());
            button.setOnClickListener(v -> FileOpener.open(context, file));
            container.addView(button);
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
