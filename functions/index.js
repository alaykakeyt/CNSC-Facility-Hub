/**
 * Cloud Functions for CNSC Facility Hub.
 *
 * This backend implementation handles automatic push notifications triggered by Firestore changes
 * and scheduled tasks.
 *
 * DEPLOYMENT INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Login: firebase login
 * 3. Deploy: firebase deploy --only functions
 *
 * NOTE: Cloud Functions deployment may require the Firebase Blaze (pay-as-you-go) plan.
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

// Initialize Firebase Admin SDK
initializeApp();

const db = getFirestore();

// --- PHASE 2: Helper Functions ---

/**
 * Gets active FCM tokens for a specific user.
 * @param {string} userId The user's UID.
 * @return {Promise<string[]>} List of active FCM tokens.
 */
async function getActiveTokensForUser(userId) {
    if (!userId) return [];
    const snapshot = await db
        .collection("users")
        .doc(userId)
        .collection("fcmTokens")
        .where("active", "==", true)
        .get();

    return snapshot.docs.map(doc => doc.data().token).filter(t => !!t);
}

/**
 * Gets active FCM tokens for all users with a specific role.
 * @param {string} role The user role (GSO, ITSO, SAC).
 * @return {Promise<string[]>} Combined list of active FCM tokens.
 */
async function getActiveTokensForRole(role) {
    if (!role) return [];

    // Normalize role case for comparison
    const normalizedRole = role.toLowerCase();

    const usersSnapshot = await db
        .collection("users")
        .where("userType", ">=", "") // Ensure userType exists
        .get();

    const roleUsers = usersSnapshot.docs.filter(doc => {
        const type = doc.data().userType;
        return type && type.toLowerCase() === normalizedRole;
    });

    const tokenPromises = roleUsers.map(doc => getActiveTokensForUser(doc.id));
    const tokenArrays = await Promise.all(tokenPromises);

    // Flatten and remove duplicates
    return [...new Set(tokenArrays.flat())];
}

/**
 * Sends a notification to a list of tokens.
 * @param {string[]} tokens List of FCM tokens.
 * @param {string} title Notification title.
 * @param {string} body Notification body.
 * @param {object} data Custom data payload.
 * @param {string} channelId Android notification channel ID.
 */
async function sendToTokens(tokens, title, body, data, channelId) {
    if (!tokens || tokens.length === 0) {
        logger.info("No tokens provided, skipping notification");
        return;
    }

    // FCM multicast limit is 500 tokens per call
    const message = {
        tokens: tokens.slice(0, 500),
        notification: {
            title: title,
            body: body
        },
        data: {
            requestId: String(data.requestId || ""),
            targetRole: String(data.targetRole || ""),
            targetScreen: String(data.targetScreen || ""),
            notificationType: String(data.notificationType || "")
        },
        android: {
            priority: "high",
            notification: {
                channelId: channelId || "request_updates",
                clickAction: "FLUTTER_NOTIFICATION_CLICK" // Common for many frameworks, but included for reliability
            }
        }
    };

    try {
        const response = await getMessaging().sendEachForMulticast(message);
        logger.info(`Multicast response: ${response.successCount} success, ${response.failureCount} failure`);

        // Optional: Clean up invalid tokens
        if (response.failureCount > 0) {
            // Logic to mark tokens inactive in Firestore could be added here
        }
    } catch (error) {
        logger.error("Error sending multicast notification:", error);
    }
}

/**
 * Sends a notification to a specific user.
 */
async function sendToUser(userId, title, body, data, channelId) {
    const tokens = await getActiveTokensForUser(userId);
    await sendToTokens(tokens, title, body, data, channelId);
}

/**
 * Sends a notification to all users of a specific role.
 */
async function sendToRole(role, title, body, data, channelId) {
    const tokens = await getActiveTokensForRole(role);
    await sendToTokens(tokens, title, body, data, channelId);
}

/**
 * Creates a key for duplicate prevention.
 */
function createEventKey(requestId, recipientType, recipientValue, eventType, extra = "") {
    return `${requestId}_${recipientType}_${recipientValue}_${eventType}_${extra}`.replace(/[^a-zA-Z0-9_]/g, "_");
}

/**
 * Checks if a notification was already sent.
 */
async function wasAlreadySent(eventKey) {
    const doc = await db.collection("sentNotifications").doc(eventKey).get();
    return doc.exists;
}

/**
 * Marks a notification as sent.
 */
async function markSent(eventKey, payload) {
    await db.collection("sentNotifications").doc(eventKey).set({
        ...payload,
        createdAt: FieldValue.serverTimestamp()
    });
}

// --- PHASE 4: Request Created Trigger ---

exports.onRequestCreated = onDocumentCreated("requests/{requestId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const requestData = snapshot.data();
    const requestId = event.params.requestId;

    let targetRole = "";
    let title = "";
    let body = "";

    // Determine target office/role based on app logic
    if (requestData.sendToSAC === true || requestData.notificationTarget === "SAC" || requestData.workflowStage === "SAC_REVIEW") {
        targetRole = "SAC";
        title = "New Student Center Request";
        body = "A new Student Center request requires your review.";
    } else if (requestData.sendToITSO === true || requestData.notificationTarget === "ITSO" || requestData.workflowStage === "ITSO_REVIEW") {
        targetRole = "ITSO";
        title = "New Technical Support Request";
        body = "A request needs ITSO technical review.";
    } else if (requestData.sendToGSO === true || requestData.notificationTarget === "GSO" || requestData.workflowStage === "GSO_REVIEW") {
        targetRole = "GSO";
        title = "New Request for Final Review";
        body = "A request has been submitted for GSO review.";
    }

    if (targetRole) {
        const eventKey = createEventKey(requestId, "role", targetRole, "new_assignment");

        if (await wasAlreadySent(eventKey)) {
            logger.info(`Notification ${eventKey} already sent, skipping`);
            return;
        }

        const data = {
            requestId,
            targetRole,
            targetScreen: "requests",
            notificationType: "new_assignment"
        };

        await sendToRole(targetRole, title, body, data, "staff_assignments");
        await markSent(eventKey, { requestId, targetRole, title, body, eventType: "new_assignment" });
    }
});

// --- PHASE 5: Request Updated Trigger ---

exports.onRequestUpdated = onDocumentUpdated("requests/{requestId}", async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    const requestId = event.params.requestId;

    // A. Notify requestor when requestor notification is triggered
    if (after.notificationForRequestor === true && after.requestorNotificationSeen !== true) {
        const shouldNotify = !before.notificationForRequestor ||
                             before.requestorNotificationType !== after.requestorNotificationType ||
                             before.status !== after.status ||
                             before.requestorNotificationMessage !== after.requestorNotificationMessage;

        if (shouldNotify) {
            const title = after.requestorNotificationTitle || inferTitleFromStatus(after.status);
            const body = after.requestorNotificationMessage || "Your request status has been updated.";
            const notificationType = after.requestorNotificationType || "request_update";

            const eventKey = createEventKey(requestId, "user", after.userId, "requestor_update", notificationType + "_" + (after.status || ""));

            if (!(await wasAlreadySent(eventKey))) {
                await sendToUser(after.userId, title, body, {
                    requestId,
                    targetRole: "Requestor",
                    targetScreen: "notifications",
                    notificationType
                }, "request_updates");

                await markSent(eventKey, { requestId, userId: after.userId, title, body, eventType: "requestor_update" });
            }
        }
    }

    // B, C, D: Notify Staff when forwarded
    const targetRoles = ["SAC", "ITSO", "GSO"];
    for (const role of targetRoles) {
        const fieldName = `sendTo${role}`;
        const isNewlyForwarded = (after[fieldName] === true && before[fieldName] !== true) ||
                                 (after.notificationTarget === role && before.notificationTarget !== role) ||
                                 (after.workflowStage === `${role}_REVIEW` && before.workflowStage !== `${role}_REVIEW`);

        if (isNewlyForwarded) {
            const title = role === "ITSO" ? "Technical Support Review Needed" :
                          role === "GSO" ? "Request Ready for GSO Review" : "Request Sent to SAC";
            const body = role === "ITSO" ? "A request requires ITSO review." :
                          role === "GSO" ? "A request is ready for final GSO review." : "A request requires SAC review.";

            const eventKey = createEventKey(requestId, "role", role, "forwarded_update", after.workflowStage || "");

            if (!(await wasAlreadySent(eventKey))) {
                await sendToRole(role, title, body, {
                    requestId,
                    targetRole: role,
                    targetScreen: "requests",
                    notificationType: "assignment_forwarded"
                }, "staff_assignments");

                await markSent(eventKey, { requestId, targetRole: role, title, body, eventType: "assignment_forwarded" });
            }
        }
    }
});

function inferTitleFromStatus(status) {
    if (!status) return "Request Update";
    const s = status.toLowerCase();
    if (s.includes("approved") && s.includes("available")) return "Technical Support Available";
    if (s.includes("approved")) return "Request Approved";
    if (s.includes("returned")) return "Request Returned";
    if (s.includes("rejected")) return "Request Rejected";
    if (s.includes("completed")) return "Appointment Completed";
    return "Request Update";
}

// --- PHASE 7: Scheduled ITSO Reminder ---

exports.scheduledITSOEventReminder = onSchedule({
    schedule: "0 7 * * *",
    timeZone: "Asia/Manila"
}, async (event) => {
    // Schedule: Every day at 7:00 AM Asia/Manila

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowDateKey = formatDate(tomorrow); // Expected format MMMM dd, yyyy

    logger.info(`Running scheduled ITSO reminder for ${tomorrowDateKey}`);

    // Query approved requests with technical support
    const snapshot = await db.collection("requests")
        .where("status", "in", ["Approved", "Booked", "Approved - Available"])
        .get();

    const technicalRequests = snapshot.docs.filter(doc => {
        const d = doc.data();
        return d.technicalNeeded === true || d.needsITSO === true;
    });

    for (const doc of technicalRequests) {
        const data = doc.data();
        const requestId = doc.id;

        const isScheduledTomorrow = checkIsScheduledForDate(data, tomorrowDateKey);

        if (isScheduledTomorrow) {
            const eventKey = createEventKey(requestId, "role", "ITSO", "itso_reminder", tomorrowDateKey);

            if (!(await wasAlreadySent(eventKey))) {
                const purpose = data.purpose || "Event";
                const facility = data.finalFacilityName || data.facility || "requested facility";
                const title = "Upcoming Technical Support Tomorrow";
                const body = `${purpose} is scheduled tomorrow at ${facility}.`;

                await sendToRole("ITSO", title, body, {
                    requestId,
                    targetRole: "ITSO",
                    targetScreen: "requests",
                    notificationType: "itso_tomorrow_reminder"
                }, "event_reminders");

                await markSent(eventKey, { requestId, targetRole: "ITSO", title, body, eventType: "itso_reminder", date: tomorrowDateKey });
            }
        }
    }
});

function formatDate(date) {
    // Returns MMMM dd, yyyy (e.g. May 22, 2026) to match Android side
    const months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    return `${months[date.getMonth()]} ${String(date.getDate()).padStart(2, '0')}, ${date.getFullYear()}`;
}

function checkIsScheduledForDate(data, dateKey) {
    // Check scheduleDays[]
    if (Array.isArray(data.scheduleDays)) {
        return data.scheduleDays.some(day => day.dateText === dateKey);
    }
    // Fallback to legacy fields
    return data.startDateText === dateKey || data.endDateText === dateKey;
}

// --- PHASE 8: Future Clearance Placeholders ---
// TODO: Implement clearance notifications when feature is ready:
// - clearance_required
// - clearance_submitted
// - clearance_approved
// - clearance_rejected
