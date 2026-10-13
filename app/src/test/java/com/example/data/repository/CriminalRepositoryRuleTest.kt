package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CriminalRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_criminal@test.com")
        val repo = CriminalRepository(firestore)

        val profileRes = withTimeout(5000L) {
            repo.createOrUpdateProfile(
                username = "alice_v",
                displayName = "Alice Vance",
                bio = "Secure channel ready",
                phoneNumber = "+1555019283",
                avatarColor = "#00E676"
            )
        }
        assertTrue("Profile creation should succeed: ${profileRes.exceptionOrNull()}", profileRes.isSuccess)

        val emitted = withTimeout(3000L) {
            repo.observeMyProfile().first { it != null && it.userId == aliceUid }
        }
        assertNotNull(emitted)
        assertEquals("Alice Vance", emitted?.displayName)
    }

    @Test
    fun directConversationAndMessage_participantsCanSendAndRead_outsiderDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_chat@test.com")
        val bobUid = signInTestUser("bob_chat@test.com")

        // Sign back in as Alice to start chat and send message
        signInTestUser("alice_chat@test.com")
        val repo = CriminalRepository(firestore)

        val convRes = withTimeout(5000L) {
            repo.createOrGetDirectConversation(
                myDisplayName = "Alice Vance",
                peerUid = bobUid,
                peerDisplayName = "Bob Sterling",
                peerAvatarColor = "#00B0FF"
            )
        }
        assertTrue("Conversation creation should succeed: ${convRes.exceptionOrNull()}", convRes.isSuccess)
        val convId = convRes.getOrThrow()

        val msgRes = withTimeout(5000L) {
            repo.sendMessage(
                conversationId = convId,
                senderName = "Alice Vance",
                participantIds = listOf(aliceUid, bobUid),
                text = "Hello from CRIMINAL APP"
            )
        }
        assertTrue("Message send should succeed: ${msgRes.exceptionOrNull()}", msgRes.isSuccess)
        val msgId = msgRes.getOrThrow()

        // Bob can observe the message
        signInTestUser("bob_chat@test.com")
        val bobMessages = withTimeout(3000L) {
            repo.observeMessages(convId).first { list -> list.any { it.id == msgId } }
        }
        assertTrue(bobMessages.any { it.text == "Hello from CRIMINAL APP" })

        // Eve (outsider) cannot read Alice & Bob's conversation document
        signInTestUser("eve_outsider@test.com")
        try {
            withTimeout(5000L) {
                firestore.collection("conversations").document(convId).get().await()
            }
            fail("Expected PERMISSION_DENIED for non-participant")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(3000L) {
                firestore.collection("directory")
                    .whereEqualTo("visibility", "PUBLIC")
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }
}
