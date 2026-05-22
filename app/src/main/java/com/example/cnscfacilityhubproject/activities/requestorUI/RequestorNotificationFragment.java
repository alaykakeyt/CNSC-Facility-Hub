package com.example.cnscfacilityhubproject.activities.requestorUI;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cnscfacilityhubproject.R;
import com.example.cnscfacilityhubproject.adapters.RequestorNotificationAdapter;
import com.example.cnscfacilityhubproject.utils.RequestDataHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestorNotificationFragment extends Fragment implements RequestorNotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private View layoutEmptyState;
    private TextView tvIncomingCount;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    private RequestorNotificationAdapter adapter;
    private final List<DocumentSnapshot> notificationList = new ArrayList<>();

    public RequestorNotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requestor_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvNotifications = view.findViewById(R.id.rvNotifications);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvIncomingCount = view.findViewById(R.id.tvIncomingCount);

        setupRecyclerView();
        listenForNotifications();
    }

    private void setupRecyclerView() {
        adapter = new RequestorNotificationAdapter(requireContext(), notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);
    }

    private void listenForNotifications() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        notificationListener = db.collection("requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("notificationForRequestor", true)
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        Toast.makeText(requireContext(), "Error loading notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();
                    int unseenCount = 0;

                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<DocumentSnapshot> docs = snapshot.getDocuments();
                        
                        // Manual sort by timestamp (Firestore doesn't allow inequality filter on one field and order by another easily without composite index)
                        // Actually, we are filtering by userId and notificationForRequestor (both equality).
                        // So we CAN use orderBy if we want.
                        
                        notificationList.addAll(docs);

                        Collections.sort(notificationList, (a, b) -> {
                            Timestamp t1 = getBestTimestamp(a);
                            Timestamp t2 = getBestTimestamp(b);
                            if (t1 == null || t2 == null) return 0;
                            return t2.compareTo(t1);
                        });

                        for (DocumentSnapshot doc : docs) {
                            if (RequestDataHelper.isRequestorNotificationUnseen(doc)) {
                                unseenCount++;
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateUi(unseenCount);
                });
    }

    private Timestamp getBestTimestamp(DocumentSnapshot doc) {
        Timestamp ts = doc.getTimestamp("notificationUpdatedAt");
        if (ts == null) ts = doc.getTimestamp("updatedAt");
        if (ts == null) ts = doc.getTimestamp("createdAt");
        return ts;
    }

    private void updateUi(int unseenCount) {
        if (notificationList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvIncomingCount.setText("0 booking notifications");
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvIncomingCount.setText(unseenCount + " unseen notification" + (unseenCount == 1 ? "" : "s"));
        }
    }

    @Override
    public void onNotificationClick(DocumentSnapshot doc) {
        String requestId = doc.getId();

        // Mark as seen
        doc.getReference().update(
                "requestorSeen", true,
                "requestorNotificationSeen", true,
                "requestorApprovedSeen", true,
                "notificationUpdatedAt", FieldValue.serverTimestamp(),
                "updatedAt", FieldValue.serverTimestamp()
        );

        // Open details
        RequestorRequestDetailsFragment fragment = RequestorRequestDetailsFragment.newInstance(requestId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
