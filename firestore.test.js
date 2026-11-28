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

test("Chat supports rich entity refs, Discord replies, character pins targeting owner, and member reactions", async () => {
  const gmDb = testEnv.authenticatedContext(GM_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  await assertSucceeds(
    gmDb.collection("stories").doc("story_chat").set({
      ...baseStory(ts),
      memberUids: [GM_UID, PLAYER_UID],
    })
  );

  // GM sends a rich power reference message + reply metadata
  await assertSucceeds(
    gmDb.collection("stories").doc("story_chat").collection("messages").doc("msg_1").set({
      storyId: "story_chat",
      gmUserId: GM_UID,
      memberUids: [GM_UID, PLAYER_UID],
      senderId: GM_UID,
      senderName: "Alice GM",
      senderRole: "GM",
      body: "Check out this spell for Lyra!",
      messageType: "ref",
      refType: "power",
      refId: "pow_1",
      refTitle: "Temper Item",
      refSubtitle: "Cost: 1 Action",
      refEmoji: "⚒️",
      refDescription: "Infuses an ally weapon with molten embers.",
      refTagsJson: '["Forge","Buff"]',
      pinned: true,
      pinnedBy: GM_UID,
      targetUserId: PLAYER_UID,
      targetCharacterId: "char_bob",
      targetCharacterName: "Lyra Vance",
      createdAt: ts,
    })
  );

  // Player replies to msg_1
  await assertSucceeds(
    playerDb.collection("stories").doc("story_chat").collection("messages").doc("msg_2").set({
      storyId: "story_chat",
      gmUserId: GM_UID,
      memberUids: [GM_UID, PLAYER_UID],
      senderId: PLAYER_UID,
      senderName: "Bob",
      senderRole: "Player",
      body: "Casting Temper Item now!",
      messageType: "chat",
      replyToMessageId: "msg_1",
      replyToSenderName: "Alice GM",
      replyToSnippet: "Check out this spell for Lyra!",
      createdAt: ts,
    })
  );

  // Member toggles emoji reaction on msg_1; editing body fails; stranger fails
  await assertSucceeds(
    playerDb.collection("stories").doc("story_chat").collection("messages").doc("msg_1").update({
      reactionsJson: '{"🔥":["player_bob_456"]}',
    })
  );
  await assertFails(
    playerDb.collection("stories").doc("story_chat").collection("messages").doc("msg_1").update({
      body: "Tampered body",
    })
  );
  await assertFails(
    strangerDb.collection("stories").doc("story_chat").collection("messages").doc("msg_1").update({
      reactionsJson: '{"💀":["stranger_eve_789"]}',
    })
  );
});

test("Unrestricted map grids (>30x30), saved_maps CRUD, location-map sync, and player token updates", async () => {
  const gmDb = testEnv.authenticatedContext(GM_UID).firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_UID).firestore();
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Large grid > 30x30 succeeds
  await assertSucceeds(
    gmDb.collection("stories").doc("story_large_map").set({
      ...baseStory(ts),
      memberUids: [GM_UID, PLAYER_UID],
      mapWidth: 150,
      mapHeight: 120,
      activeMapId: "map_citadel",
      activeMapName: "Iron Citadel",
      activeLocationId: "loc_citadel",
    })
  );

  // Grid smaller than 1x1 fails
  await assertFails(
    gmDb.collection("stories").doc("story_bad_grid").set({
      ...baseStory(ts),
      mapWidth: 0,
      mapHeight: 10,
    })
  );

  // GM creates linked location world_item with linkedMapId
  await assertSucceeds(
    gmDb.collection("stories").doc("story_large_map").collection("world_items").doc("loc_citadel").set({
      storyId: "story_large_map",
      gmUserId: GM_UID,
      ownerId: GM_UID,
      memberUids: [GM_UID, PLAYER_UID],
      category: "location",
      title: "Iron Citadel",
      subtitle: "Fortress",
      description: "Vast mountain keep.",
      iconName: "Place",
      iconEmoji: "🏰",
      sortOrder: 0,
      tagsJson: '["Fortress"]',
      linkedMapId: "map_citadel",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // GM creates saved_map with 150x120 dimensions linked to loc_citadel
  await assertSucceeds(
    gmDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").set({
      storyId: "story_large_map",
      gmUserId: GM_UID,
      ownerId: GM_UID,
      memberUids: [GM_UID, PLAYER_UID],
      name: "Iron Citadel",
      linkedLocationId: "loc_citadel",
      linkedLocationTitle: "Iron Citadel",
      width: 150,
      height: 120,
      gridJson: '{"120,100":{"terrain":"stone"}}',
      tokensJson: "[]",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Member can query saved_maps with memberUids filter; unfiltered list fails
  await assertSucceeds(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps")
      .where("memberUids", "array-contains", PLAYER_UID).get()
  );
  await assertFails(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps").get()
  );

  // Member can update tokensJson on saved_map, but cannot change width/gridJson/name
  await assertSucceeds(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").update({
      tokensJson: '[{"id":"p1","x":45,"y":60}]',
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").update({
      width: 200,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Player cannot create or delete saved_map; stranger cannot read
  await assertFails(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_player").set({
      storyId: "story_large_map",
      gmUserId: GM_UID,
      ownerId: PLAYER_UID,
      memberUids: [GM_UID, PLAYER_UID],
      name: "Illegal Map",
      width: 20,
      height: 20,
      gridJson: "{}",
      tokensJson: "[]",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    playerDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").delete()
  );
  await assertFails(
    strangerDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").get()
  );

  // GM can resize saved_map freely (e.g., 500x400) and delete
  await assertSucceeds(
    gmDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").update({
      width: 500,
      height: 400,
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertSucceeds(
    gmDb.collection("stories").doc("story_large_map").collection("saved_maps").doc("map_citadel").delete()
  );
});


