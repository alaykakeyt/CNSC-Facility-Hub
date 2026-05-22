# Push Notifications Backend Plan - CNSC Facility Hub

## Overview
This document outlines the Cloud Functions backend implementation required to enable automatic push notifications for the CNSC Facility Hub Android app. The Android app can now **receive** push notifications (PART 1 ✅). The backend handles **sending** push notifications automatically based on Firestore events and scheduled tasks (PART 2).

---

## Backend Implementation Summary

### Security & Architecture
- ✅ **Secure Sending**: Notifications are sent via the Firebase Admin SDK in a trusted backend environment. FCM server keys are NOT stored in the Android app.
- ✅ **Firebase Functions v2**: Uses the latest trigger styles for improved performance and scalability.
- ✅ **Multicast Support**: Uses `sendEachForMulticast` to efficiently notify multiple staff members.
- ✅ **Duplicate Prevention**: Implements an idempotency layer using a `sentNotifications` collection to prevent spamming users if triggers fire multiple times.

---

## Cloud Functions Implemented

### 1. New Request Trigger (`onRequestCreated`)
**Trigger**: A new document is created in `requests/{requestId}`.
**Logic**:
- Identifies the target office (SAC, ITSO, or GSO) based on fields like `sendToSAC`, `notificationTarget`, or `workflowStage`.
- Notifies all staff members of that role with active FCM tokens.
- **Channel**: `staff_assignments`

### 2. Request Update Trigger (`onRequestUpdated`)
**Trigger**: A document in `requests/{requestId}` is updated.
**Logic**:
- **Requestor Notifications**: Notifies the requestor when `notificationForRequestor` is true and a meaningful change occurs (status, message, etc.).
- **Staff Forwarding**: Notifies staff roles when a request is newly forwarded to them (e.g., SAC → ITSO → GSO).
- **Channel**: `request_updates` or `staff_assignments`

### 3. Scheduled ITSO Reminder (`scheduledITSOEventReminder`)
**Trigger**: Daily at 07:00 AM.
**Logic**:
- Scans for approved/booked technical requests scheduled for the following day.
- Checks both `scheduleDays[]` and legacy date fields.
- Sends a reminder to all ITSO staff.
- **Channel**: `event_reminders`

---

## Firestore Schema & Fields Used

### User Documents (`users/{uid}`)
- `userType`: Used to filter recipients by role (Requestor, GSO, ITSO, SAC).
- `fcmTokens`: Subcollection containing `token`, `active`, and `platform`.

### Request Documents (`requests/{requestId}`)
- Status & Workflow: `status`, `workflowStage`, `notificationTarget`.
- Forwarding: `sendToSAC`, `sendToITSO`, `sendToGSO`.
- Schedule: `scheduleDays`, `startDateText`, `endDateText`.
- Requestor Info: `userId`, `purpose`, `finalFacilityName`.
- Notif Metadata: `notificationForRequestor`, `requestorNotificationTitle`, `requestorNotificationMessage`.

### sentNotifications Collection
- Document ID: Unique event key derived from `requestId`, recipient, and event type.
- Ensures each notification is sent exactly once.

---

## Deployment & Setup

### Prerequisites
1. **Firebase CLI**: `npm install -g firebase-tools`
2. **Billing**: Cloud Functions v2 requires the **Firebase Blaze (pay-as-you-go)** plan.
3. **Initialization**:
   ```bash
   firebase init functions
   ```

### Deployment Commands
```bash
cd functions
npm install
firebase deploy --only functions
```

### Local Testing
```bash
firebase emulators:start --only functions,firestore
```

---

## Future Enhancements
- [ ] **Clearance Notifications**: Placeholders added for when clearance features are implemented.
- [ ] **Token Cleanup**: Logic to automatically remove invalid/expired tokens upon failed delivery.
- [ ] **In-App Notification Center**: Synchronization with the push notification history.

**Status**: Backend code READY for deployment 🔄
