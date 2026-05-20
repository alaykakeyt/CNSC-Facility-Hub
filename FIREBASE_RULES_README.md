# Firebase Rules Deployment Guide

This directory contains Firebase security rules for CNSC Facility Hub:
- `firestore.rules` - Firestore database access control
- `storage.rules` - Cloud Storage file access control

## Prerequisites

1. **Firebase CLI installed**: https://firebase.google.com/docs/cli
   ```bash
   npm install -g firebase-tools
   ```

2. **Logged in to Firebase**:
   ```bash
   firebase login
   ```

3. **Project initialized**:
   ```bash
   firebase init
   ```

## Deployment Steps

### Deploy All Rules

```bash
firebase deploy
```

This will deploy both Firestore and Storage rules if configured in `firebase.json`.

### Deploy Only Firestore Rules

```bash
firebase deploy --only firestore:rules
```

### Deploy Only Storage Rules

```bash
firebase deploy --only storage
```

## Testing Rules (Local Emulator)

### Start Emulator Suite

```bash
firebase emulators:start
```

### Run Tests

```bash
firebase emulators:exec 'npm test'
```

## Firestore Rules Overview

The firestore.rules file implements role-based access control:

- **Requestor**: Can create/read/update own requests, cannot modify userType
- **GSO**: Can manage users, requests, and reports
- **ITSO**: Can approve technical requests and mark status
- **SAC**: Can approve student center requests
- **Staff roles**: Can read requests needed for their workflow

Key features:
- Users can only create requests with their own userId
- Requestors cannot change their userType
- Staff roles can update workflow/status fields
- GSO can delete or archive requests
- Test/dev collections are denied by default

## Storage Rules Overview

The storage.rules file implements file upload and access control:

- **Proposal Files** (proposal_files/{requestId}/{fileName}):
  - Users can upload files only for requests they own (max 10MB)
  - Staff can read proposal files
  - Users can delete their own files

- **Profile Pictures** (profile_pictures/{userId}/avatar.jpg):
  - Users can upload/update own profile pictures (max 5MB)
  - Any authenticated user can read profile pictures
  - Users can delete own profile pictures

- **Facility Documents** (facility_docs/{facilityId}/{fileName}):
  - GSO can manage facility documents
  - Staff and Requestors can read facility documents

- **Reports** (reports/{fileName}):
  - GSO only

## Security Best Practices

1. **Always test in Firebase Console first** before deploying to production
2. **Use the Emulator Suite** for local development and testing
3. **Monitor Firestore/Storage activity** in Firebase Console after deployment
4. **Set up Firestore backups** regularly
5. **Review rules monthly** for security updates
6. **Use field validators** when uploading data
7. **Implement rate limiting** for sensitive operations (Cloud Functions)

## Troubleshooting

### Permission Denied Errors

If users get "PERMISSION_DENIED" errors:

1. Check the user's `userType` field in `/users/{uid}`
2. Verify the Firestore rules are deployed (check Firebase Console → Firestore → Rules tab)
3. Use Firebase Console → Storage → Rules tab to test Storage paths
4. Check user authentication status in app

### Testing Rules in Firebase Console

1. Go to Firebase Console → Firestore → Rules tab
2. Click "Rules Playground"
3. Set auth context (UID, custom claims)
4. Query paths to test access

## Maintenance

After deploying these rules:

1. **Monitor logs** in Firebase Console for denied requests
2. **Adjust rules** if legitimate access is denied
3. **Add new collections** with appropriate access controls
4. **Test role changes** when adding new staff roles
5. **Document any customizations** made to rules

## Related Resources

- [Firestore Security Rules Documentation](https://firebase.google.com/docs/firestore/security/start)
- [Storage Security Rules Documentation](https://firebase.google.com/docs/storage/security)
- [Firebase Emulator Suite](https://firebase.google.com/docs/emulator-suite)
