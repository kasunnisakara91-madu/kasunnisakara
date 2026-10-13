package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.CallSession
import com.example.data.model.ChatMessage
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.DirectoryProfile
import com.example.data.model.StatusUpdate
import com.example.data.model.UserProfile
import com.example.data.repository.CriminalRepository
import com.example.util.CriminalMediaAndNotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class CriminalAppViewModel(
    private val repository: CriminalRepository,
    val currentUserId: String
) : ViewModel() {

    val myProfileState: StateFlow<UiState<UserProfile?>> = repository.observeMyProfile()
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing profile", error)
            emit(UiState.Error(error.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository.observeMyConversations()
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing conversations", error)
            emit(UiState.Error(error.message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val directoryState: StateFlow<UiState<List<DirectoryProfile>>> = repository.observePublicDirectory()
        .map<List<DirectoryProfile>, UiState<List<DirectoryProfile>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing directory", error)
            emit(UiState.Error(error.message ?: "Failed to load user directory"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val contactsState: StateFlow<UiState<List<Contact>>> = repository.observeMyContacts()
        .map<List<Contact>, UiState<List<Contact>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing contacts", error)
            emit(UiState.Error(error.message ?: "Failed to load contacts"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val statusesState: StateFlow<UiState<List<StatusUpdate>>> = repository.observePublicStatuses()
        .map<List<StatusUpdate>, UiState<List<StatusUpdate>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing statuses", error)
            emit(UiState.Error(error.message ?: "Failed to load status updates"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val callsState: StateFlow<UiState<List<CallSession>>> = repository.observeMyCalls()
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing calls", error)
            emit(UiState.Error(error.message ?: "Failed to load call history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = _selectedConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(convId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing messages", error)
                        emit(UiState.Error(error.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _activeCallSessionId = MutableStateFlow<String?>(null)
    val activeCallSessionId: StateFlow<String?> = _activeCallSessionId.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var typingResetJob: Job? = null

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun selectConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun setActiveCallSessionId(callId: String?) {
        _activeCallSessionId.value = callId
    }

    fun saveUserProfile(
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
        blockedUserIds: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createOrUpdateProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                phoneNumber = phoneNumber,
                avatarColor = avatarColor,
                avatarBase64 = avatarBase64,
                privacyPhoto = privacyPhoto,
                privacyLastSeen = privacyLastSeen,
                privacyStatus = privacyStatus,
                notificationsSound = notificationsSound,
                notificationsVibrate = notificationsVibrate,
                themeMode = themeMode,
                blockedUserIds = blockedUserIds
            )
            _isSaving.value = false
            res.onSuccess {
                _bannerMessage.value = "Profile synchronized with CRIMINAL APP cloud"
                onSuccess()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun updatePresence(isOnline: Boolean) {
        viewModelScope.launch {
            val lastSeen = if (isOnline) "Online" else "Last seen recently"
            repository.setOnlineStatus(isOnline, lastSeen)
        }
    }

    fun addOrSaveContact(
        contactUid: String,
        username: String,
        displayName: String,
        phoneTag: String,
        bio: String,
        avatarColor: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.addOrUpdateContact(
                contactUid = contactUid,
                username = username,
                displayName = displayName,
                phoneTag = phoneTag,
                bio = bio,
                avatarColor = avatarColor
            )
            res.onSuccess {
                _bannerMessage.value = "$displayName added to contacts"
                onComplete()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to add contact"
            }
        }
    }

    fun toggleBlockUser(targetUid: String, currentProfile: UserProfile) {
        viewModelScope.launch {
            val isCurrentlyBlocked = targetUid in currentProfile.blockedUserIds
            val updatedBlocked = if (isCurrentlyBlocked) {
                currentProfile.blockedUserIds - targetUid
            } else {
                (currentProfile.blockedUserIds + targetUid).distinct().take(50)
            }
            repository.createOrUpdateProfile(
                username = currentProfile.username,
                displayName = currentProfile.displayName,
                bio = currentProfile.bio,
                phoneNumber = currentProfile.phoneNumber,
                avatarColor = currentProfile.avatarColor,
                avatarBase64 = currentProfile.avatarBase64,
                privacyPhoto = currentProfile.privacyPhoto,
                privacyLastSeen = currentProfile.privacyLastSeen,
                privacyStatus = currentProfile.privacyStatus,
                notificationsSound = currentProfile.notificationsSound,
                notificationsVibrate = currentProfile.notificationsVibrate,
                themeMode = currentProfile.themeMode,
                blockedUserIds = updatedBlocked
            )
            _bannerMessage.value = if (isCurrentlyBlocked) "User unblocked" else "User blocked"
        }
    }

    fun startDirectChat(
        myDisplayName: String,
        peerUid: String,
        peerDisplayName: String,
        peerAvatarColor: String,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createOrGetDirectConversation(
                myDisplayName = myDisplayName,
                peerUid = peerUid,
                peerDisplayName = peerDisplayName,
                peerAvatarColor = peerAvatarColor
            )
            _isSaving.value = false
            res.onSuccess { convId ->
                _selectedConversationId.value = convId
                onReady(convId)
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Could not open chat"
            }
        }
    }

    fun createGroupChat(
        groupTitle: String,
        groupDescription: String,
        avatarColor: String,
        memberUids: List<String>,
        participantNamesSummary: String,
        creatorDisplayName: String,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createGroupConversation(
                groupTitle = groupTitle,
                groupDescription = groupDescription,
                avatarColor = avatarColor,
                memberUids = memberUids,
                participantNamesSummary = participantNamesSummary,
                creatorDisplayName = creatorDisplayName
            )
            _isSaving.value = false
            res.onSuccess { convId ->
                _selectedConversationId.value = convId
                _bannerMessage.value = "Group \"$groupTitle\" created"
                onReady(convId)
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to create group"
            }
        }
    }

    fun sendMessage(
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
    ) {
        viewModelScope.launch {
            val res = repository.sendMessage(
                conversationId = conversationId,
                senderName = senderName,
                participantIds = participantIds,
                text = text,
                messageType = messageType,
                mediaBase64 = mediaBase64,
                mediaName = mediaName,
                mediaDurationSec = mediaDurationSec,
                replyToMessageId = replyToMessageId,
                replyToSenderName = replyToSenderName,
                replyToPreview = replyToPreview,
                isForwarded = isForwarded
            )
            res.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun editMessage(conversationId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            repository.editMessage(conversationId, messageId, newText)
                .onSuccess { _bannerMessage.value = "Message edited" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to edit message" }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageForEveryone(conversationId, messageId)
                .onSuccess { _bannerMessage.value = "Message deleted for everyone" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete message" }
        }
    }

    fun markMessageAsRead(conversationId: String, message: ChatMessage) {
        if (message.senderId == currentUserId || currentUserId in message.readByUids) return
        viewModelScope.launch {
            repository.markMessageRead(conversationId, message.id, message.readByUids)
        }
    }

    fun onUserTyping(conversationId: String, myDisplayName: String) {
        typingResetJob?.cancel()
        typingResetJob = viewModelScope.launch {
            repository.setTypingIndicator(conversationId, true, myDisplayName)
            delay(2500L)
            repository.setTypingIndicator(conversationId, false, myDisplayName)
        }
    }

    fun promoteToGroupAdmin(conversationId: String, currentAdmins: List<String>, targetUid: String) {
        viewModelScope.launch {
            repository.promoteGroupAdmin(conversationId, currentAdmins, targetUid)
                .onSuccess { _bannerMessage.value = "Member promoted to Group Admin" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update group admin" }
        }
    }

    fun postStatus(
        username: String,
        userDisplayName: String,
        userAvatarColor: String,
        mediaType: String,
        caption: String,
        backgroundColorHex: String,
        mediaBase64: String? = null,
        visibilityScope: String = "PUBLIC",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.postStatusUpdate(
                username = username,
                userDisplayName = userDisplayName,
                userAvatarColor = userAvatarColor,
                mediaType = mediaType,
                caption = caption,
                backgroundColorHex = backgroundColorHex,
                mediaBase64 = mediaBase64,
                visibilityScope = visibilityScope
            )
            _isSaving.value = false
            res.onSuccess {
                _bannerMessage.value = "Status published (expires in 24h)"
                onComplete()
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to post status"
            }
        }
    }

    fun markStatusViewed(status: StatusUpdate, viewerDisplayName: String) {
        viewModelScope.launch {
            repository.recordStatusView(status, viewerDisplayName)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
                .onSuccess { _bannerMessage.value = "Status deleted" }
        }
    }

    fun startCall(
        callerName: String,
        callerAvatarColor: String,
        receiverId: String,
        receiverName: String,
        receiverAvatarColor: String,
        callType: String,
        onCallCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val sdpOffer = CriminalMediaAndNotificationHelper.buildRealisticWebRtcSdpOffer(
                callType = callType,
                callerUid = currentUserId
            )
            val res = repository.startCallSession(
                callerName = callerName,
                callerAvatarColor = callerAvatarColor,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverAvatarColor = receiverAvatarColor,
                callType = callType,
                sdpOffer = sdpOffer
            )
            res.onSuccess { callId ->
                _activeCallSessionId.value = callId
                onCallCreated(callId)
            }.onFailure { err ->
                _bannerMessage.value = err.localizedMessage ?: "Failed to initiate call"
            }
        }
    }

    fun updateCallState(
        callId: String,
        status: String,
        sdpAnswer: String? = null,
        durationSeconds: Long? = null,
        isMuted: Boolean? = null,
        isCameraOn: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateCallSignalingState(
                callId = callId,
                status = status,
                sdpAnswer = sdpAnswer,
                durationSeconds = durationSeconds,
                isMuted = isMuted,
                isCameraOn = isCameraOn
            )
            if (status in listOf("ENDED", "DECLINED", "MISSED") && _activeCallSessionId.value == callId) {
                _activeCallSessionId.value = null
            }
        }
    }

    fun reportAbusiveUser(
        reportedUserId: String,
        reportedUsername: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            repository.submitAbuseReport(reportedUserId, reportedUsername, reason, details)
                .onSuccess { _bannerMessage.value = "Abuse report submitted to CRIMINAL APP Trust & Safety" }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to submit report" }
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.deleteMyAccountData()
                .onSuccess {
                    _isSaving.value = false
                    onDeleted()
                }
                .onFailure { err ->
                    _isSaving.value = false
                    _bannerMessage.value = err.localizedMessage ?: "Failed to delete account data"
                }
        }
    }

    companion object {
        private const val TAG = "CriminalAppVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L

        fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CriminalAppViewModel(CriminalRepository(db), currentUserId)
            }
        }
    }
}
