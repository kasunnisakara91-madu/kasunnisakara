const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowTs() {
  return new Date(Date.now() - 1000);
}

test("1. Unauthenticated user cannot read or write users, conversations, statuses, or calls", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("conversations").get());
  await assertFails(unauthDb.collection("statuses").get());
  await assertFails(unauthDb.collection("calls").get());
});

test("2. UserProfile: owner can create and read own profile; non-owner is rejected (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const validProfile = {
    userId: ALICE_UID,
    username: "alice_criminal",
    displayName: "Alice Vance",
    bio: "Encrypted comms online",
    phoneNumber: "+15550192831",
    avatarColor: "#00E676",
    isOnline: true,
    lastSeenText: "Online",
    privacyPhoto: "EVERYONE",
    privacyLastSeen: "CONTACTS",
    privacyStatus: "EVERYONE",
    notificationsSound: true,
    notificationsVibrate: true,
    themeMode: "DARK",
    blockedUserIds: [],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  // Bob cannot read Alice's private user profile containing phoneNumber/settings
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("3. DirectoryProfile: public directory supports aligned list queries and rejects spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("directory").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_criminal",
      displayName: "Alice Vance",
      bio: "Available",
      phoneTag: "+15550192",
      avatarColor: "#00E676",
      isOnline: true,
      lastSeenText: "Online",
      visibility: "PUBLIC",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob can query public directory with aligned filter
  await assertSucceeds(
    bobDb.collection("directory").where("visibility", "==", "PUBLIC").get()
  );

  // Bob cannot overwrite Alice's directory entry
  await assertFails(
    bobDb.collection("directory").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: nowTs(),
    })
  );
});

test("4. Conversations & Messages: participants can create, query, and message; non-participants rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  const convId = "conv_alice_bob";
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      creatorId: ALICE_UID,
      isGroup: false,
      title: "Bob Sterling",
      description: "Direct encrypted chat",
      avatarColor: "#00B0FF",
      participantIds: [ALICE_UID, BOB_UID],
      adminIds: [ALICE_UID],
      participantNames: "Alice Vance, Bob Sterling",
      lastMessageText: "Hey Bob!",
      lastMessageSenderId: ALICE_UID,
      lastMessageSenderName: "Alice Vance",
      lastMessageType: "TEXT",
      lastMessageStatus: "SENT",
      unreadCount: 1,
      typingUserId: "",
      typingUserName: "",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob can query his conversations using array-contains
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );

  // Eve cannot read the conversation
  await assertFails(eveDb.collection("conversations").doc(convId).get());

  // Alice sends a message
  const msgId = "msg_001";
  await assertSucceeds(
    aliceDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc(msgId)
      .set({
        conversationId: convId,
        senderId: ALICE_UID,
        senderName: "Alice Vance",
        participantIds: [ALICE_UID, BOB_UID],
        text: "Secure payload delivered.",
        messageType: "TEXT",
        deliveryStatus: "SENT",
        isEdited: false,
        isDeleted: false,
        isForwarded: false,
        readByUids: [ALICE_UID],
        createdAt: nowTs(),
        updatedAt: nowTs(),
      })
  );

  // Bob can list messages when filtering by participantIds
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Bob can mark message as READ
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc(msgId)
      .update({
        deliveryStatus: "READ",
        readByUids: [ALICE_UID, BOB_UID],
        updatedAt: nowTs(),
      })
  );

  // Bob cannot edit Alice's message text
  await assertFails(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc(msgId)
      .update({
        text: "Tampered text",
        isEdited: true,
        updatedAt: nowTs(),
      })
  );

  // Eve cannot read messages
  await assertFails(
    eveDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc(msgId)
      .get()
  );
});

test("5. StatusUpdate & CallSession: valid creation, query alignment, and participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("statuses").doc("status_1").set({
      userId: ALICE_UID,
      username: "alice_criminal",
      userDisplayName: "Alice Vance",
      userAvatarColor: "#00E676",
      mediaType: "TEXT",
      caption: "Building CRIMINAL APP by Madusanka",
      backgroundColorHex: "#0B0F19",
      visibilityScope: "PUBLIC",
      expiresAtMillis: Date.now() + 86400000,
      viewerUids: [ALICE_UID],
      viewerNames: ["Alice Vance"],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("statuses").where("visibilityScope", "==", "PUBLIC").get()
  );

  await assertSucceeds(
    aliceDb.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice Vance",
      callerAvatarColor: "#00E676",
      receiverId: BOB_UID,
      receiverName: "Bob Sterling",
      receiverAvatarColor: "#00B0FF",
      participantIds: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      sdpOffer: "v=0\r\no=- 12345 2 IN IP4 127.0.0.1...",
      sdpAnswer: "",
      iceCandidatesJson: "[]",
      durationSeconds: 0,
      isMuted: false,
      isCameraOn: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("calls").where("participantIds", "array-contains", BOB_UID).get()
  );

  await assertFails(eveDb.collection("calls").doc("call_1").get());
});
