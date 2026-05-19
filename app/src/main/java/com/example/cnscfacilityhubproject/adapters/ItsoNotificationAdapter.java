package com.example.cnscfacilityhubproject.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.databinding.ItemItsoNotificationBinding;
import com.example.cnscfacilityhubproject.utils.ItsoReminderHelper;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for ITSO notification cards - dynamically renders all incoming requests and reminders.
 * Replaces hardcoded 3-card limitation with flexible RecyclerView-based rendering.
 */
public class ItsoNotificationAdapter extends RecyclerView.Adapter<ItsoNotificationAdapter.NotificationViewHolder> {

    private final List<DocumentSnapshot> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onViewRequestClicked(String requestId);
    }

    public ItsoNotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<DocumentSnapshot> newNotifications) {
        this.notifications.clear();
        if (newNotifications != null) {
            this.notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemItsoNotificationBinding binding = ItemItsoNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        DocumentSnapshot doc = notifications.get(position);
        holder.bind(doc, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemItsoNotificationBinding binding;

        public NotificationViewHolder(ItemItsoNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DocumentSnapshot doc, OnNotificationClickListener listener) {
            String purpose = getSafeString(doc.getString("purpose"));
            boolean upcoming = ItsoReminderHelper.isUpcomingTechnicalEvent(doc);

            // Set title
            binding.tvNotificationTitle.setText(upcoming
                    ? "Tomorrow: " + (!purpose.isEmpty() ? purpose : "Technical Event")
                    : (!purpose.isEmpty() ? purpose : "Untitled Request"));

            // Set metadata (facilities and schedule)
            binding.tvNotificationMeta.setText(
                    RequestDataHelper.getFacilitiesDisplay(doc) + " • "
                            + RequestDataHelper.getScheduleDisplay(doc).replace("\n", " ")
            );

            // Set description
            binding.tvNotificationDesc.setText(upcoming
                    ? ItsoReminderHelper.buildReminderSummary(doc)
                    : buildTechnicalSummary(doc));

            // Set status chip
            binding.chipNotificationStatus.setText(upcoming ? "Upcoming" : "Incoming");
            binding.chipNotificationStatus.setTextColor(Color.parseColor(upcoming ? "#F57C00" : "#970705"));
            binding.chipNotificationStatus.setChipBackgroundColor(ColorStateList.valueOf(
                    Color.parseColor(upcoming ? "#FFF3E0" : "#F5E5E5")
            ));

            // Set icon background color
            binding.iconBackground.setCardBackgroundColor(
                    Color.parseColor(upcoming ? "#F57C00" : "#970705")
            );

            // Set click listener
            binding.btnViewRequest.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewRequestClicked(doc.getId());
                }
            });
        }

        private String buildTechnicalSummary(DocumentSnapshot doc) {
            StringBuilder sb = new StringBuilder();

            appendIfTrue(sb, doc.getBoolean("soundSystemSetup"), "Sound system");
            appendIfTrue(sb, doc.getBoolean("microphones"), "Microphones");
            appendIfTrue(sb, doc.getBoolean("portableSpeaker"), "Portable speaker");
            appendIfTrue(sb, doc.getBoolean("lights"), "Lights");
            appendIfTrue(sb, doc.getBoolean("livestreamingServices"), "Livestreaming");
            appendIfTrue(sb, doc.getBoolean("zoomHosting"), "Zoom hosting");
            appendIfTrue(sb, doc.getBoolean("gmeetHosting"), "GMeet hosting");
            appendIfTrue(sb, doc.getBoolean("webCamera"), "Web camera");
            appendIfTrue(sb, doc.getBoolean("tripod"), "Tripod");
            appendIfTrue(sb, doc.getBoolean("multimediaProjector"), "Projector");

            String connectors = getSafeString(doc.getString("connectors"));
            if (!connectors.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("Connectors: ").append(connectors);
            }

            return sb.length() == 0 ? "No technical requirements" : sb.toString();
        }

        private void appendIfTrue(StringBuilder sb, Boolean value, String label) {
            if (Boolean.TRUE.equals(value)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(label);
            }
        }

        private String getSafeString(String value) {
            return value != null ? value.trim() : "";
        }
    }
}
