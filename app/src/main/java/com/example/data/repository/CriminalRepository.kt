package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AbuseReport
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.DirectoryProfile
import com.example.data.model.StatusUpdate
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CriminalRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return cleaned.ifBlank { UUID.randomUUID().toString().replace("-", "_") }.take(120)
    }

    // ---------------------------------------------------------------------------
    // 1. User Profile & Directory Operations
    // ---------------------------------------------------------------------------

    fun observeMyProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getMyProfile(): Result<UserProfile?> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createOrUpdateProfile(
        username: String,
        displayName: String,
        bio: String,
        phoneNumber: String,
        avatarColor: String,
        avatarBase64: String? = null,
        privacyPhoto: String = "EVERYONE",
        privacyLastSeen: String = "EVERYONE",
        privacyStatus: String = "EVERYONE",
        notificationsSound: Boolean = true,
        notificationsVibrate: Boolean = true,
        themeMode: String = "DARK",
        blockedUserIds: List<String> = emptyList()
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val dirDoc = db.collection("directory").document(uid)

        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "_").let {
            if (it.length < 3) "${it}_usr" else it.take(30)
        }
        val cleanDisplayName = displayName.trim().ifBlank { cleanUsername }.take(80)
        val cleanBio = bio.trim().ifBlank { "Hey there! I am using CRIMINAL APP." }.take(250)
        val cleanPhone = phoneNumber.trim().take(30)

        try {
            val existingSnap = userDoc.get().await()
            val profile = UserProfile(
                id = uid,
                userId = uid,
                username = cleanUsername,
                displayName = cleanDisplayName,
                bio = cleanBio,
                phoneNumber = cleanPhone,
                avatarColor = avatarColor,
                avatarBase64 = avatarBase64,
                isOnline = true,
                lastSeenText = "Online",
                privacyPhoto = privacyPhoto,
                privacyLastSeen = privacyLastSeen,
                privacyStatus = privacyStatus,
                notificationsSound = notificationsSound,
                notificationsVibrate = notificationsVibrate,
                themeMode = themeMode,
                blockedUserIds = blockedUserIds.take(50)
            )

            if (!existingSnap.exists()) {
                userDoc.set(profile.toCreateMap()).await()
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplayName,
                    "bio" to cleanBio,
                    "phoneNumber" to cleanPhone,
                    "avatarColor" to avatarColor,
                    "isOnline" to true,
                    "lastSeenText" to "Online",
                    "privacyPhoto" to privacyPhoto,
                    "privacyLastSeen" to privacyLastSeen,
                    "privacyStatus" to privacyStatus,
                    "notificationsSound" to notificationsSound,
                    "notificationsVibrate" to notificationsVibrate,
                    "themeMode" to themeMode,
                    "blockedUserIds" to blockedUserIds.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!avatarBase64.isNullOrBlank()) {
                    updateMap["avatarBase64"] = avatarBase64
                }
                userDoc.update(updateMap).await()
            }

            // Sync public directory entry using same existence state as userDoc
            val dirProfile = DirectoryProfile(
                id = uid,
                userId = uid,
                username = cleanUsername,
                displayName = cleanDisplayName,
                bio = if (privacyStatus == "NOBODY") "" else cleanBio,
                phoneTag = cleanPhone,
                avatarColor = avatarColor,
                avatarBase64 = if (privacyPhoto == "NOBODY") null else avatarBase64,
                isOnline = true,
                lastSeenText = if (privacyLastSeen == "NOBODY") "Hidden" else "Online",
                visibility = "PUBLIC"
            )
            if (!existingSnap.exists()) {
                dirDoc.set(dirProfile.toCreateMap()).await()
            } else {
                val dirUpdate = mutableMapOf<String, Any>(
                    "username" to dirProfile.username,
                    "displayName" to dirProfile.displayName,
                    "bio" to dirProfile.bio,
                    "phoneTag" to dirProfile.phoneTag,
                    "avatarColor" to dirProfile.avatarColor,
                    "isOnline" to dirProfile.isOnline,
                    "lastSeenText" to dirProfile.lastSeenText,
                    "visibility" to "PUBLIC",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!dirProfile.avatarBase64.isNullOrBlank()) {
                    dirUpdate["avatarBase64"] = dirProfile.avatarBase64
                }
                dirDoc.update(dirUpdate).await()
            }
            profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            throw e
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean, lastSeenText: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val dirDoc = db.collection("directory").document(uid)
        try {
            val snap = userDoc.get().await()
            if (snap.exists()) {
                userDoc.update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeenText" to lastSeenText.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            val dirSnap = dirDoc.get().await()
            if (dirSnap.exists()) {
                dirDoc.update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeenText" to lastSeenText.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            throw e
        }
    }

    suspend fun deleteMyAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val userDoc = db.collection("users").document(uid)
        val dirDoc = db.collection("directory").document(uid)
        try {
            val dirSnap = dirDoc.get().await()
            if (dirSnap.exists()) {
                dirDoc.delete().await()
            }
            val userSnap = userDoc.get().await()
            if (userSnap.exists()) {
                userDoc.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc.path)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // 2. Public Directory & Contacts Operations
    // ---------------------------------------------------------------------------

    fun observePublicDirectory(): Flow<List<DirectoryProfile>> = flow {
        requireUserId()
        val path = "directory"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DirectoryProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyContacts(): Flow<List<Contact>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/contacts"
        emitAll(
            db.collection("users").document(uid).collection("contacts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Contact::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addOrUpdateContact(
        contactUid: String,
        username: String,
        displayName: String,
        phoneTag: String,
        bio: String,
        avatarColor: String,
        isBlocked: Boolean = false,
        isFavorite: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val contactDocId = sanitizeId(contactUid)
        val docRef = db.collection("users").document(uid).collection("contacts").document(contactDocId)
        try {
            val contact = Contact(
                id = contactDocId,
                contactUid = contactDocId,
                username = username.trim().let { if (it.length < 3) "${it}_usr" else it.take(30) },
                displayName = displayName.trim().ifBlank { username }.take(80),
                phoneTag = phoneTag.trim().take(30),
                bio = bio.trim().take(250),
                avatarColor = avatarColor,
                isBlocked = isBlocked,
                isFavorite = isFavorite
            )
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(contact.toCreateMap()).await()
            } else {
                docRef.update(
                    mapOf(
                        "username" to contact.username,
                        "displayName" to contact.displayName,
                        "phoneTag" to contact.phoneTag,
                        "bio" to contact.bio,
                        "avatarColor" to contact.avatarColor,
                        "isBlocked" to contact.isBlocked,
                        "isFavorite" to contact.isFavorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            contactDocId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun setContactBlocked(contactId: String, blocked: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("contacts").document(sanitizeId(contactId))
        try {
            docRef.update(
                mapOf(
                    "isBlocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // 3. Conversations & Real-Time Messaging Operations
    // ---------------------------------------------------------------------------

    fun observeMyConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createOrGetDirectConversation(
        myDisplayName: String,
        peerUid: String,
        peerDisplayName: String,
        peerAvatarColor: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanPeerUid = sanitizeId(peerUid)
        val sortedIds = listOf(uid, cleanPeerUid).distinct().sorted()
        val convCollection = db.collection("conversations")
        try {
            val existingQuery = convCollection
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
            val existingDm = existingQuery.documents.firstOrNull { doc ->
                val isGroup = doc.getBoolean("isGroup") ?: false
                val participants = (doc.get("participantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                !isGroup && participants.contains(cleanPeerUid)
            }
            if (existingDm != null) {
                return@runCatching existingDm.id
            }

            val convId = "dm_${UUID.randomUUID().toString().replace("-", "").take(22)}"
            val docRef = convCollection.document(convId)
            val conv = Conversation(
                id = convId,
                creatorId = uid,
                isGroup = false,
                title = peerDisplayName.ifBlank { "Contact" }.take(100),
                description = "End-to-end style private channel",
                avatarColor = peerAvatarColor,
                participantIds = sortedIds,
                adminIds = listOf(uid),
                participantNames = "$myDisplayName, $peerDisplayName".take(1000),
                lastMessageText = "Encrypted conversation started",
                lastMessageSenderId = uid,
                lastMessageSenderName = myDisplayName.take(100),
                lastMessageType = "SYSTEM",
                lastMessageStatus = "DELIVERED",
                unreadCount = 0L,
                typingUserId = "",
                typingUserName = ""
            )
            docRef.set(conv.toCreateMap()).await()
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations")
            throw e
        }
    }

    suspend fun createGroupConversation(
        groupTitle: String,
        groupDescription: String,
        avatarColor: String,
        memberUids: List<String>,
        participantNamesSummary: String,
        creatorDisplayName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val convId = "grp_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("conversations").document(convId)
        val allParticipants = (listOf(uid) + memberUids.map { sanitizeId(it) }).distinct().take(50)
        try {
            val conv = Conversation(
                id = convId,
                creatorId = uid,
                isGroup = true,
                title = groupTitle.trim().ifBlank { "Criminal Squad" }.take(100),
                description = groupDescription.trim().ifBlank { "Group channel managed by $creatorDisplayName" }.take(300),
                avatarColor = avatarColor,
                participantIds = allParticipants,
                adminIds = listOf(uid),
                participantNames = participantNamesSummary.take(1000),
                lastMessageText = "Group \"$groupTitle\" created by $creatorDisplayName",
                lastMessageSenderId = uid,
                lastMessageSenderName = creatorDisplayName.take(100),
                lastMessageType = "SYSTEM",
                lastMessageStatus = "DELIVERED",
                unreadCount = 0L,
                typingUserId = "",
                typingUserName = ""
            )
            docRef.set(conv.toCreateMap()).await()
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val path = "conversations/$cleanConvId/messages"
        emitAll(
            db.collection("conversations").document(cleanConvId)
                .collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        conversationId: String,
        senderName: String,
        participantIds: List<String>,
        text: String,
        messageType: String = "TEXT",
        mediaBase64: String? = null,
        mediaName: String? = null,
        mediaDurationSec: Long? = null,
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToPreview: String? = null,
        isForwarded: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(24)}"
        val msgRef = db.collection("conversations").document(cleanConvId).collection("messages").document(msgId)
        val convRef = db.collection("conversations").document(cleanConvId)
        val validParticipants = (participantIds + uid).distinct().take(50)

        val message = ChatMessage(
            id = msgId,
            conversationId = cleanConvId,
            senderId = uid,
            senderName = senderName.ifBlank { "Member" }.take(100),
            participantIds = validParticipants,
            text = text.take(4000),
            messageType = messageType,
            deliveryStatus = "DELIVERED",
            isEdited = false,
            isDeleted = false,
            isForwarded = isForwarded,
            mediaBase64 = mediaBase64,
            mediaName = mediaName?.take(200),
            mediaDurationSec = mediaDurationSec,
            replyToMessageId = replyToMessageId?.take(128),
            replyToSenderName = replyToSenderName?.take(100),
            replyToPreview = replyToPreview?.take(300),
            readByUids = listOf(uid)
        )

        try {
            msgRef.set(message.toCreateMap()).await()
            val preview = when (messageType) {
                "IMAGE" -> "📷 Photo${if (text.isNotBlank()) ": $text" else ""}"
                "VIDEO" -> "🎥 Video${if (text.isNotBlank()) ": $text" else ""}"
                "VOICE" -> "🎤 Voice message (${mediaDurationSec ?: 0}s)"
                "DOCUMENT" -> "📄 ${mediaName ?: "Document"}"
                else -> text
            }.take(500)

            convRef.update(
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageSenderId" to uid,
                    "lastMessageSenderName" to senderName.ifBlank { "Member" }.take(100),
                    "lastMessageType" to messageType,
                    "lastMessageStatus" to "DELIVERED",
                    "typingUserId" to "",
                    "typingUserName" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val cleanMsgId = sanitizeId(messageId)
        val msgRef = db.collection("conversations").document(cleanConvId).collection("messages").document(cleanMsgId)
        try {
            msgRef.update(
                mapOf(
                    "text" to newText.trim().take(4000),
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun deleteMessageForEveryone(
        conversationId: String,
        messageId: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val cleanMsgId = sanitizeId(messageId)
        val msgRef = db.collection("conversations").document(cleanConvId).collection("messages").document(cleanMsgId)
        try {
            msgRef.update(
                mapOf(
                    "text" to "🚫 This message was deleted",
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun markMessageRead(
        conversationId: String,
        messageId: String,
        existingReadBy: List<String>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (uid in existingReadBy) return@runCatching
        val cleanConvId = sanitizeId(conversationId)
        val cleanMsgId = sanitizeId(messageId)
        val msgRef = db.collection("conversations").document(cleanConvId).collection("messages").document(cleanMsgId)
        try {
            val updatedReadBy = (existingReadBy + uid).distinct().take(50)
            msgRef.update(
                mapOf(
                    "deliveryStatus" to "READ",
                    "readByUids" to updatedReadBy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            throw e
        }
    }

    suspend fun setTypingIndicator(
        conversationId: String,
        isTyping: Boolean,
        userDisplayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val convRef = db.collection("conversations").document(cleanConvId)
        try {
            convRef.update(
                mapOf(
                    "typingUserId" to if (isTyping) uid else "",
                    "typingUserName" to if (isTyping) userDisplayName.take(100) else "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    suspend fun promoteGroupAdmin(
        conversationId: String,
        currentAdmins: List<String>,
        targetUid: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanConvId = sanitizeId(conversationId)
        val convRef = db.collection("conversations").document(cleanConvId)
        val newAdmins = (currentAdmins + sanitizeId(targetUid)).distinct().take(20)
        try {
            convRef.update(
                mapOf(
                    "adminIds" to newAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // 4. Status & Stories (24h Expiration) Operations
    // ---------------------------------------------------------------------------

    fun observePublicStatuses(): Flow<List<StatusUpdate>> = flow {
        requireUserId()
        val path = "statuses"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibilityScope", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    val now = System.currentTimeMillis()
                    snapshot.toObjects(
                        StatusUpdate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.expiresAtMillis > now }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun postStatusUpdate(
        username: String,
        userDisplayName: String,
        userAvatarColor: String,
        mediaType: String,
        caption: String,
        backgroundColorHex: String,
        mediaBase64: String? = null,
        visibilityScope: String = "PUBLIC"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val statusId = "sts_${UUID.randomUUID().toString().replace("-", "").take(22)}"
        val docRef = db.collection("statuses").document(statusId)
        val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        val status = StatusUpdate(
            id = statusId,
            userId = uid,
            username = username.trim().let { if (it.length < 3) "${it}_usr" else it.take(30) },
            userDisplayName = userDisplayName.trim().ifBlank { username }.take(80),
            userAvatarColor = userAvatarColor,
            mediaType = mediaType,
            caption = caption.trim().take(500),
            backgroundColorHex = backgroundColorHex,
            mediaBase64 = mediaBase64,
            visibilityScope = visibilityScope,
            expiresAtMillis = expiresAt,
            viewerUids = listOf(uid),
            viewerNames = listOf(userDisplayName.ifBlank { username }.take(80))
        )
        try {
            docRef.set(status.toCreateMap()).await()
            statusId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordStatusView(
        status: StatusUpdate,
        viewerDisplayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        if (uid in status.viewerUids) return@runCatching
        val docRef = db.collection("statuses").document(sanitizeId(status.id))
        try {
            val updatedUids = (status.viewerUids + uid).distinct().take(50)
            val updatedNames = (status.viewerNames + viewerDisplayName.take(80)).distinct().take(50)
            docRef.update(
                mapOf(
                    "viewerUids" to updatedUids,
                    "viewerNames" to updatedNames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("statuses").document(sanitizeId(statusId))
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // 5. Real-Time Voice & Video Call Signaling + Call History
    // ---------------------------------------------------------------------------

    fun observeMyCalls(): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        val path = "calls"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CallSession::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun startCallSession(
        callerName: String,
        callerAvatarColor: String,
        receiverId: String,
        receiverName: String,
        receiverAvatarColor: String,
        callType: String,
        sdpOffer: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(22)}"
        val docRef = db.collection("calls").document(callId)
        val cleanReceiverId = sanitizeId(receiverId)
        val callSession = CallSession(
            id = callId,
            callerId = uid,
            callerName = callerName.ifBlank { "Caller" }.take(80),
            callerAvatarColor = callerAvatarColor,
            receiverId = cleanReceiverId,
            receiverName = receiverName.ifBlank { "Contact" }.take(80),
            receiverAvatarColor = receiverAvatarColor,
            participantIds = listOf(uid, cleanReceiverId).distinct(),
            callType = callType,
            status = "RINGING",
            sdpOffer = sdpOffer.take(20000),
            sdpAnswer = "",
            iceCandidatesJson = "[]",
            durationSeconds = 0L,
            isMuted = false,
            isCameraOn = callType == "VIDEO"
        )
        try {
            docRef.set(callSession.toCreateMap()).await()
            callId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCallSignalingState(
        callId: String,
        status: String,
        sdpAnswer: String? = null,
        durationSeconds: Long? = null,
        isMuted: Boolean? = null,
        isCameraOn: Boolean? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("calls").document(sanitizeId(callId))
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (sdpAnswer != null) updates["sdpAnswer"] = sdpAnswer.take(20000)
        if (durationSeconds != null) updates["durationSeconds"] = durationSeconds
        if (isMuted != null) updates["isMuted"] = isMuted
        if (isCameraOn != null) updates["isCameraOn"] = isCameraOn
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------------------------
    // 6. Abuse Reporting
    // ---------------------------------------------------------------------------

    suspend fun submitAbuseReport(
        reportedUserId: String,
        reportedUsername: String,
        reason: String,
        details: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(22)}"
        val docRef = db.collection("reports").document(reportId)
        val report = AbuseReport(
            id = reportId,
            reporterId = uid,
            reportedUserId = sanitizeId(reportedUserId),
            reportedUsername = reportedUsername.ifBlank { "unknown" }.take(80),
            reason = reason.ifBlank { "Abusive content" }.take(100),
            details = details.take(500),
            status = "OPEN"
        )
        try {
            docRef.set(report.toCreateMap()).await()
            reportId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
