package com.example.cnscfacilityhubproject.activities.sacUI;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;

public class SACViewFactory {

    public static View createRequestCard(Context context, DocumentSnapshot doc, String status, View.OnClickListener listener) {
        MaterialCardView card = baseCard(context);
        LinearLayout container = baseContainer(context);

        container.addView(createHeader(context, doc, status));
        container.addView(createDescription(context, buildSummary(doc)));
        container.addView(createButton(context, "View Request", listener));

        card.addView(container);
        return card;
    }

    public static View createCompactRequestCard(Context context, DocumentSnapshot doc, String status, View.OnClickListener listener) {
        return createRequestCard(context, doc, status, listener);
    }

    public static View createNotificationCard(Context context, DocumentSnapshot doc, View.OnClickListener listener) {
        MaterialCardView card = baseCard(context);
        LinearLayout container = baseContainer(context);

        container.addView(createHeader(context, doc, "Incoming"));
        container.addView(createDescription(context, buildSummary(doc)));
        container.addView(createButton(context, "Review Request", listener));

        card.addView(container);
        return card;
    }

    private static MaterialCardView baseCard(Context context) {
        MaterialCardView card = new MaterialCardView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(context, 14));
        card.setLayoutParams(params);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(context, 26));
        card.setCardElevation(dp(context, 7));
        card.setStrokeWidth(dp(context, 1));
        card.setStrokeColor(Color.parseColor("#313131"));
        return card;
    }

    private static LinearLayout baseContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        return container;
    }

    private static View createHeader(Context context, DocumentSnapshot doc, String status) {
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        MaterialCardView iconCard = new MaterialCardView(context);
        iconCard.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)));
        iconCard.setRadius(dp(context, 16));
        iconCard.setCardElevation(0);
        iconCard.setCardBackgroundColor(getStatusMainColor(status));

        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        icon.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        icon.setImageResource(getStatusIcon(status));
        icon.setColorFilter(Color.WHITE);
        iconCard.addView(icon);

        LinearLayout titleLayout = new LinearLayout(context);
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(context, 12), 0, dp(context, 8), 0);
        titleLayout.setLayoutParams(titleParams);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(nonEmpty(getStringValue(doc, "purpose"), "Student Center Request"));
        tvTitle.setTextColor(Color.parseColor("#313131"));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvMeta = new TextView(context);
        tvMeta.setText(buildMetaText(doc));
        tvMeta.setTextColor(Color.parseColor("#313131"));
        tvMeta.setTextSize(12f);
        tvMeta.setAlpha(0.68f);

        titleLayout.addView(tvTitle);
        titleLayout.addView(tvMeta);

        Chip chip = new Chip(context);
        chip.setText(status);
        chip.setTextColor(getStatusMainColor(status));
        chip.setChipBackgroundColor(ColorStateList.valueOf(getStatusLightColor(status)));
        chip.setChipStrokeWidth(0);
        chip.setCheckable(false);
        chip.setClickable(false);

        headerRow.addView(iconCard);
        headerRow.addView(titleLayout);
        headerRow.addView(chip);

        return headerRow;
    }

    private static TextView createDescription(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#313131"));
        tv.setTextSize(14f);
        tv.setLineSpacing(3f, 1f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(context, 14), 0, 0);
        tv.setLayoutParams(params);
        return tv;
    }

    private static MaterialButton createButton(Context context, String label, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#313131")));
        button.setCornerRadius(dp(context, 16));
        button.setElevation(0);
        button.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 48)
        );
        params.setMargins(0, dp(context, 16), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private static String buildSummary(DocumentSnapshot doc) {
        String participants = getStringValue(doc, "participants");
        String number = getStringValue(doc, "numberOfParticipants");
        String amenities = getStringValue(doc, "otherAmenities");
        String technical = getTechnicalSummary(doc);

        StringBuilder builder = new StringBuilder();
        builder.append("Facility: ").append(nonEmpty(getFinalFacility(doc), "Student Center"));

        if (!participants.isEmpty()) builder.append("\nParticipants: ").append(participants);
        if (!number.isEmpty()) builder.append("\nNo. of Participants: ").append(number);
        if (!amenities.isEmpty()) builder.append("\nAmenities: ").append(amenities);
        if (!technical.isEmpty()) builder.append("\nTechnical: ").append(technical);

        return builder.toString();
    }

    private static String getTechnicalSummary(DocumentSnapshot doc) {
        StringBuilder builder = new StringBuilder();

        appendIfTrue(builder, doc, "soundSystemSetup", "Sound System");
        appendIfTrue(builder, doc, "microphones", "Microphones");
        appendIfTrue(builder, doc, "portableSpeaker", "Portable Speaker");
        appendIfTrue(builder, doc, "lights", "Lights");
        appendIfTrue(builder, doc, "livestreamingServices", "Livestreaming");
        appendIfTrue(builder, doc, "zoomHosting", "Zoom");
        appendIfTrue(builder, doc, "gmeetHosting", "GMeet");
        appendIfTrue(builder, doc, "webCamera", "Web Camera");
        appendIfTrue(builder, doc, "tripod", "Tripod");
        appendIfTrue(builder, doc, "multimediaProjector", "Projector");

        String connectors = getStringValue(doc, "connectors");
        if (!connectors.isEmpty()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append("Connectors: ").append(connectors);
        }

        return builder.toString();
    }

    private static void appendIfTrue(StringBuilder builder, DocumentSnapshot doc, String field, String label) {
        if (Boolean.TRUE.equals(doc.getBoolean(field))) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(label);
        }
    }

    private static String buildMetaText(DocumentSnapshot doc) {
        String facility = getFinalFacility(doc);
        String startDate = getStringValue(doc, "startDateText");
        String endDate = getStringValue(doc, "endDateText");
        String startTime = getStringValue(doc, "timeStartText");
        String endTime = getStringValue(doc, "timeEndText");

        StringBuilder builder = new StringBuilder();

        if (!facility.isEmpty()) builder.append(facility);

        if (!startDate.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");
            if (!endDate.isEmpty() && !startDate.equalsIgnoreCase(endDate)) builder.append(startDate).append(" - ").append(endDate);
            else builder.append(startDate);
        }

        if (!startTime.isEmpty()) {
            if (builder.length() > 0) builder.append(" • ");
            if (!endTime.isEmpty()) builder.append(startTime).append(" - ").append(endTime);
            else builder.append(startTime);
        }

        return builder.length() == 0 ? "No schedule details" : builder.toString();
    }

    private static String getFinalFacility(DocumentSnapshot doc) {
        String finalFacilityName = getStringValue(doc, "finalFacilityName");
        if (!finalFacilityName.isEmpty()) return finalFacilityName;

        String facility = getStringValue(doc, "facility");
        String otherFacility = getStringValue(doc, "otherFacility");

        if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) return otherFacility;
        return facility;
    }

    private static int getStatusIcon(String status) {
        if ("Forwarded".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) return android.R.drawable.checkbox_on_background;
        if ("Rejected".equalsIgnoreCase(status)) return android.R.drawable.ic_delete;
        if ("Incoming".equalsIgnoreCase(status)) return android.R.drawable.ic_menu_my_calendar;
        return android.R.drawable.ic_menu_recent_history;
    }

    private static int getStatusMainColor(String status) {
        if ("Forwarded".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) return Color.parseColor("#2E7D32");
        if ("Rejected".equalsIgnoreCase(status)) return Color.parseColor("#970705");
        if ("Incoming".equalsIgnoreCase(status)) return Color.parseColor("#970705");
        return Color.parseColor("#313131");
    }

    private static int getStatusLightColor(String status) {
        if ("Forwarded".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
        if ("Rejected".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
        if ("Incoming".equalsIgnoreCase(status)) return Color.parseColor("#F5E5E5");
        return Color.parseColor("#EEEEEE");
    }

    private static String getStringValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
