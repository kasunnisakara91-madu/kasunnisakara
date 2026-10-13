package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "Hey there! I am using CRIMINAL APP.",
    val phoneNumber: String = "",
    val avatarColor: String = "#00E676",
    val avatarBase64: String? = null,
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val privacyPhoto: String = "EVERYONE",
    val privacyLastSeen: String = "EVERYONE",
    val privacyStatus: String = "EVERYONE",
    val notificationsSound: Boolean = true,
    val notificationsVibrate: Boolean = true,
    val themeMode: String = "DARK",
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "username" to username,
            "displayName" to displayName,
            "bio" to bio,
            "phoneNumber" to phoneNumber,
            "avatarColor" to avatarColor,
            "isOnline" to isOnline,
            "lastSeenText" to lastSeenText,
            "privacyPhoto" to privacyPhoto,
            "privacyLastSeen" to privacyLastSeen,
            "privacyStatus" to privacyStatus,
            "notificationsSound" to notificationsSound,
            "notificationsVibrate" to notificationsVibrate,
            "themeMode" to themeMode,
            "blockedUserIds" to blockedUserIds.take(50),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarBase64.isNullOrBlank()) {
            map["avatarBase64"] = avatarBase64
        }
        return map
    }
}

@IgnoreExtraProperties
data class DirectoryProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val phoneTag: String = "",
    val avatarColor: String = "#00E676",
    val avatarBase64: String? = null,
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "username" to username,
            "displayName" to displayName,
            "bio" to bio,
            "phoneTag" to phoneTag,
            "avatarColor" to avatarColor,
            "isOnline" to isOnline,
            "lastSeenText" to lastSeenText,
            "visibility" to "PUBLIC",
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarBase64.isNullOrBlank()) {
            map["avatarBase64"] = avatarBase64
        }
        return map
    }
}

@IgnoreExtraProperties
data class Contact(
    @DocumentId val id: String = "",
    val contactUid: String = "",
    val username: String = "",
    val displayName: String = "",
    val phoneTag: String = "",
    val bio: String = "",
    val avatarColor: String = "#00B0FF",
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "contactUid" to contactUid,
        "username" to username,
        "displayName" to displayName,
        "phoneTag" to phoneTag,
        "bio" to bio,
        "avatarColor" to avatarColor,
        "isBlocked" to isBlocked,
        "isFavorite" to isFavorite,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val isGroup: Boolean = false,
    val title: String = "",
    val description: String = "",
    val avatarColor: String = "#00E676",
    val groupIconBase64: String? = null,
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val participantNames: String = "",
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageSenderName: String = "",
    val lastMessageType: String = "TEXT",
    val lastMessageStatus: String = "SENT",
    val unreadCount: Long = 0L,
    val typingUserId: String = "",
    val typingUserName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "creatorId" to creatorId,
            "isGroup" to isGroup,
            "title" to title,
            "description" to description,
            "avatarColor" to avatarColor,
            "participantIds" to participantIds.take(50),
            "adminIds" to adminIds.take(20),
            "participantNames" to participantNames,
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageSenderName" to lastMessageSenderName,
            "lastMessageType" to lastMessageType,
            "lastMessageStatus" to lastMessageStatus,
            "unreadCount" to unreadCount,
            "typingUserId" to typingUserId,
            "typingUserName" to typingUserName,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!groupIconBase64.isNullOrBlank()) {
            map["groupIconBase64"] = groupIconBase64
        }
        return map
    }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val messageType: String = "TEXT", // TEXT, IMAGE, VIDEO, DOCUMENT, VOICE
    val deliveryStatus: String = "SENT", // SENT, DELIVERED, READ
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isForwarded: Boolean = false,
    val mediaBase64: String? = null,
    val mediaName: String? = null,
    val mediaDurationSec: Long? = null,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val readByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "participantIds" to participantIds.take(50),
            "text" to text,
            "messageType" to messageType,
            "deliveryStatus" to deliveryStatus,
            "isEdited" to isEdited,
            "isDeleted" to isDeleted,
            "isForwarded" to isForwarded,
            "readByUids" to readByUids.take(50),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!mediaBase64.isNullOrBlank()) map["mediaBase64"] = mediaBase64
        if (!mediaName.isNullOrBlank()) map["mediaName"] = mediaName
        if (mediaDurationSec != null) map["mediaDurationSec"] = mediaDurationSec
        if (!replyToMessageId.isNullOrBlank()) map["replyToMessageId"] = replyToMessageId
        if (!replyToSenderName.isNullOrBlank()) map["replyToSenderName"] = replyToSenderName
        if (!replyToPreview.isNullOrBlank()) map["replyToPreview"] = replyToPreview
        return map
    }
}

@IgnoreExtraProperties
data class StatusUpdate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userDisplayName: String = "",
    val userAvatarColor: String = "#00E676",
    val mediaType: String = "TEXT", // TEXT, IMAGE, VIDEO
    val caption: String = "",
    val backgroundColorHex: String = "#0B0F19",
    val mediaBase64: String? = null,
    val visibilityScope: String = "PUBLIC", // PUBLIC, CONTACTS, PRIVATE
    val expiresAtMillis: Long = 0L,
    val viewerUids: List<String> = emptyList(),
    val viewerNames: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "username" to username,
            "userDisplayName" to userDisplayName,
            "userAvatarColor" to userAvatarColor,
            "mediaType" to mediaType,
            "caption" to caption,
            "backgroundColorHex" to backgroundColorHex,
            "visibilityScope" to visibilityScope,
            "expiresAtMillis" to expiresAtMillis,
            "viewerUids" to viewerUids.take(50),
            "viewerNames" to viewerNames.take(50),
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!mediaBase64.isNullOrBlank()) {
            map["mediaBase64"] = mediaBase64
        }
        return map
    }
}

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatarColor: String = "#00E676",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverAvatarColor: String = "#00B0FF",
    val participantIds: List<String> = emptyList(),
    val callType: String = "VOICE", // VOICE, VIDEO
    val status: String = "RINGING", // RINGING, CONNECTED, ENDED, MISSED, DECLINED
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidatesJson: String = "[]",
    val durationSeconds: Long = 0L,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "callerId" to callerId,
        "callerName" to callerName,
        "callerAvatarColor" to callerAvatarColor,
        "receiverId" to receiverId,
        "receiverName" to receiverName,
        "receiverAvatarColor" to receiverAvatarColor,
        "participantIds" to participantIds.take(10),
        "callType" to callType,
        "status" to status,
        "sdpOffer" to sdpOffer,
        "sdpAnswer" to sdpAnswer,
        "iceCandidatesJson" to iceCandidatesJson,
        "durationSeconds" to durationSeconds,
        "isMuted" to isMuted,
        "isCameraOn" to isCameraOn,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class AbuseReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reportedUsername: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "reporterId" to reporterId,
        "reportedUserId" to reportedUserId,
        "reportedUsername" to reportedUsername,
        "reason" to reason,
        "details" to details,
        "status" to status,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
