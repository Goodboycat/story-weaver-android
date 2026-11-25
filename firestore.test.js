const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const GM_UID = "gm_alice_123";
const PLAYER_UID = "player_bob_456";
const STRANGER_UID = "stranger_eve_789";

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

function baseStory(now) {
  return {
    title: "Curse of Ravenhold",
    description: "Gothic mystery campaign",
    systemName: "D&D 5e",
    joinCode: "RAV842",
    gmUserId: GM_UID,
    memberUids: [GM_UID],
    mapWidth: 12,
    mapHeight: 12,
    mapGridJson: "{}",
    mapTokensJson: "[]",
    guideProgressJson: "[]",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write stories or invites", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("stories").get());
  await assertFails(unauthDb.collection("story_invites").doc("RAV842").get());
  await assertFails(unauthDb.collection("stories").doc("story_1").set(baseStory(now)));
});

test("GM can create story and invite; player joins via invite and creates character; stranger is blocked", async () => {
  const gmDb = testEnv.authenticatedContext(GM_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  // GM creates story and invite
  await assertSucceeds(gmDb.collection("stories").doc("story_1").set(baseStory(created)));
  await assertSucceeds(
    gmDb.collection("story_invites").doc("RAV842").set({
      storyId: "story_1",
      title: "Curse of Ravenhold",
      systemName: "D&D 5e",
      gmUserId: GM_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Listing story_invites is always forbidden (no scraping)
  await assertFails(playerDb.collection("story_invites").get());

  // Player looks up exact invite code
  await assertSucceeds(playerDb.collection("story_invites").doc("RAV842").get());

  // Player joins story_1 by adding only own UID to memberUids
  await assertSucceeds(
    playerDb.collection("stories").doc("story_1").update({
      memberUids: [GM_UID, PLAYER_UID],
      updatedAt: updated,
    })
  );

  // Player cannot hijack GM fields or edit title
  await assertFails(
    playerDb.collection("stories").doc("story_1").update({
      title: "Hijacked Title",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Member query succeeds with array-contains; unfiltered query fails
  await assertSucceeds(
    playerDb.collection("stories").where("memberUids", "array-contains", PLAYER_UID).get()
  );
  await assertFails(playerDb.collection("stories").get());

  // Player moves token on map
  await assertSucceeds(
    playerDb.collection("stories").doc("story_1").update({
      mapTokensJson: '[{"id":"t1","x":2,"y":3}]',
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // GM can set session schedule; player cannot modify session schedule
  const nextWeek = Date.now() + 7 * 24 * 60 * 60 * 1000;
  await assertSucceeds(
    gmDb.collection("stories").doc("story_1").update({
      nextSessionAtMillis: nextWeek,
      nextSessionNote: "Session 4: Crypt of Mirrors",
      updatedAt: new Date(Date.now() - 150),
    })
  );
  await assertFails(
    playerDb.collection("stories").doc("story_1").update({
      nextSessionAtMillis: nextWeek + 1000,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Player creates own player character (NPC creation by player fails)
  await assertSucceeds(
    playerDb
      .collection("stories")
      .doc("story_1")
      .collection("characters")
      .doc("char_bob")
      .set({
        storyId: "story_1",
        gmUserId: GM_UID,
        ownerId: PLAYER_UID,
        memberUids: [GM_UID, PLAYER_UID],
        kind: "player",
        name: "Lyra Vance",
        avatarEmoji: "🗡️",
        roleTitle: "Rogue",
        level: 3,
        hpCurrent: 24,
        hpMax: 24,
        ac: 15,
        notes: "Shadowfell scout",
        inventoryJson: "[]",
        missionsJson: "[]",
        powersJson: "[]",
        equipmentJson: "{}",
        statsJson: "{}",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertFails(
    playerDb
      .collection("stories")
      .doc("story_1")
      .collection("characters")
      .doc("npc_illegal")
      .set({
        storyId: "story_1",
        gmUserId: GM_UID,
        ownerId: PLAYER_UID,
        memberUids: [GM_UID, PLAYER_UID],
        kind: "npc",
        name: "Illegal NPC",
        avatarEmoji: "🧙",
        roleTitle: "Mage",
        level: 5,
        hpCurrent: 30,
        hpMax: 30,
        ac: 12,
        notes: "",
        inventoryJson: "[]",
        missionsJson: "[]",
        powersJson: "[]",
        equipmentJson: "{}",
        statsJson: "{}",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Stranger cannot read story or characters
  await assertFails(strangerDb.collection("stories").doc("story_1").get());
  await assertFails(
    strangerDb
      .collection("stories")
      .doc("story_1")
      .collection("characters")
      .where("memberUids", "array-contains", PLAYER_UID)
      .get()
  );
});

test("Shadow fields, extra @DocumentId id key, and future timestamps are rejected", async () => {
  const gmDb = testEnv.authenticatedContext(GM_UID).firestore();
  const past = new Date(Date.now() - 2000);
  const future = new Date(Date.now() + 60000);

  await assertFails(
    gmDb.collection("stories").doc("story_bad_id").set({
      ...baseStory(past),
      id: "story_bad_id",
    })
  );

  await assertFails(
    gmDb.collection("stories").doc("story_future").set(baseStory(future))
  );

  await assertFails(
    gmDb.collection("stories").doc("story_ghost").set({
      ...baseStory(past),
      isAdmin: true,
    })
  );
});
