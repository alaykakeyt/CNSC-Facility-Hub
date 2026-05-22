package com.example.cnscfacilityhubproject.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class RequestorNotificationAdapter extends RecyclerView.Adapter<RequestorNotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(DocumentSnapshot document);
    }

    public RequestorNotificationAdapter(Context context, List<DocumentSnapshot> notifications, OnNotificationClickListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_requestor_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        DocumentSnapshot doc = notifications.get(position);
        holder.bind(doc);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotification, cardIcon;
        ImageView ivStatusIcon;
        TextView tvTitle, tvMeta, tvDescription, tvTime, tvUnseenBadge;
        Chip chipStatus;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            cardIcon = itemView.findViewById(R.id.cardIcon);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMeta = itemView.findViewById(R.id.tvNotificationMeta);
            tvDescription = itemView.findViewById(R.id.tvNotificationDescription);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvUnseenBadge = itemView.findViewById(R.id.tvUnseenBadge);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }

        void bind(DocumentSnapshot doc) {
            String status = getDisplayStatus(doc);
            String purpose = getStringValue(doc, "purpose");
            String facility = getFinalFacility(doc);
            String startDate = getStringValue(doc, "startDateText");
            
            tvTitle.setText(!purpose.isEmpty() ? purpose : "Request Update");
            tvMeta.setText(facility + (!startDate.isEmpty() ? " • " + startDate : ""));
            tvDescription.setText(buildDescriptionText(doc, status));
            
            Timestamp ts = doc.getTimestamp("notificationUpdatedAt");
            if (ts == null) ts = doc.getTimestamp("updatedAt");
            if (ts != null) {
                tvTime.setText(DateUtils.getRelativeTimeSpanString(ts.toDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            } else {
                tvTime.setText("");
            }

            boolean unseen = RequestDataHelper.isRequestorNotificationUnseen(doc);
            tvUnseenBadge.setVisibility(unseen ? View.VISIBLE : View.GONE);
            cardNotification.setStrokeColor(unseen ? Color.parseColor("#970705") : Color.parseColor("#DDDDDD"));
            cardNotification.setStrokeWidth(unseen ? 3 : 2);

            chipStatus.setText(status);
            int mainColor = getStatusMainColor(status);
            chipStatus.setTextColor(mainColor);
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(getStatusLightColor(status)));
            cardIcon.setCardBackgroundColor(mainColor);
            ivStatusIcon.setImageResource(getStatusIcon(status));

            itemView.setOnClickListener(v -> listener.onNotificationClick(doc));
        }

        private String getDisplayStatus(DocumentSnapshot doc) {
            String status = getStringValue(doc, "status");
            String sacStatus = getStringValue(doc, "sacStatus");
            String itsoStatus = getStringValue(doc, "itsoStatus");
            String gsoStatus = getStringValue(doc, "gsoStatus");

            if ("Rejected".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(sacStatus) || "Rejected".equalsIgnoreCase(itsoStatus) || "Rejected".equalsIgnoreCase(gsoStatus)) {
                return "Returned";
            }
            if ("Returned".equalsIgnoreCase(status) || "Returned".equalsIgnoreCase(gsoStatus)) {
                return "Returned";
            }
            if ("Approved".equalsIgnoreCase(status) || "Approved".equalsIgnoreCase(gsoStatus)) {
                return "Approved";
            }
            return "Pending";
        }

        private String buildDescriptionText(DocumentSnapshot doc, String status) {
            String title = getStringValue(doc, "requestorNotificationTitle");
            String message = getStringValue(doc, "requestorNotificationMessage");

            if (!title.isEmpty() && !message.isEmpty()) return title + "\n" + message;
            if (!message.isEmpty()) return message;

            if ("Approved".equalsIgnoreCase(status)) return "Your booking request has been approved.";
            if ("Returned".equalsIgnoreCase(status)) return "Your booking request has been returned or rejected.";
            
            return "There is an update on your booking request.";
        }

        private String getFinalFacility(DocumentSnapshot doc) {
            String finalFacilityName = getStringValue(doc, "finalFacilityName");
            if (!finalFacilityName.isEmpty()) return finalFacilityName;
            String facility = getStringValue(doc, "facility");
            String otherFacility = getStringValue(doc, "otherFacility");
            if ("Others".equalsIgnoreCase(facility) && !otherFacility.isEmpty()) return otherFacility;
            return facility;
        }

        private int getStatusIcon(String status) {
            if ("Approved".equalsIgnoreCase(status)) return android.R.drawable.checkbox_on_background;
            if ("Returned".equalsIgnoreCase(status)) return android.R.drawable.ic_menu_revert;
            return android.R.drawable.ic_dialog_info;
        }

        private int getStatusMainColor(String status) {
            if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#2E7D32");
            if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#970705");
            return Color.parseColor("#313131");
        }

        private int getStatusLightColor(String status) {
            if ("Approved".equalsIgnoreCase(status)) return Color.parseColor("#E7F4E8");
            if ("Returned".equalsIgnoreCase(status)) return Color.parseColor("#F3D9D9");
            return Color.parseColor("#EEEEEE");
        }

        private String getStringValue(DocumentSnapshot doc, String field) {
            Object value = doc.get(field);
            return value == null ? "" : String.valueOf(value).trim();
        }
    }
}
