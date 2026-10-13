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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read trips", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("trips").get());
});

test("Authenticated user: can write their own profile, cannot write others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
    })
  );
});

test("Authenticated user: can create a trip where they are creator & member", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("trips").doc("trip_1").set({
      id: "trip_1",
      name: "Manali Trip",
      createdBy: ALICE_UID,
      memberIds: [ALICE_UID],
      createdAt: Date.now(),
    })
  );
});

test("Authenticated user: cannot read trip if not a member", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trips").doc("alice_trip").set({
      id: "alice_trip",
      name: "Alice's Secret Trip",
      createdBy: ALICE_UID,
      memberIds: [ALICE_UID],
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("trips").doc("alice_trip").get());
});

test("Trip members: can read and add expenses to their trip", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trips").doc("shared_trip").set({
      id: "shared_trip",
      name: "Shared Trip",
      createdBy: ALICE_UID,
      memberIds: [ALICE_UID, BOB_UID],
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("trips").doc("shared_trip").collection("expenses").doc("exp_1").set({
      id: "exp_1",
      tripId: "shared_trip",
      title: "Dinner",
      amountMinor: 150000,
      createdBy: BOB_UID,
    })
  );

  await assertSucceeds(
    bobDb.collection("trips").doc("shared_trip").collection("expenses").doc("exp_1").get()
  );
});

test("Non-members: cannot add expenses to a trip", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trips").doc("alice_only_trip").set({
      id: "alice_only_trip",
      name: "Alice Trip",
      createdBy: ALICE_UID,
      memberIds: [ALICE_UID],
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("trips").doc("alice_only_trip").collection("expenses").doc("exp_2").set({
      id: "exp_2",
      tripId: "alice_only_trip",
      title: "Unauthorized Expense",
      amountMinor: 50000,
      createdBy: BOB_UID,
    })
  );
});
