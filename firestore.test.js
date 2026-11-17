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
const COLLECTOR_UID = "collector_789";
const ADMIN_UID = "admin_001";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("scrap_inventory").get());
  await assertFails(unauthDb.collection("pickups").get());
  await assertFails(unauthDb.collection("pricing_rules").get());
  await assertFails(
    unauthDb.collection("scrap_inventory").doc("inv_1").set({
      userId: ALICE_UID,
      materialId: "mat_copper_01",
      name: "Copper",
      category: "COPPER_BRASS",
      grade: "Standard A",
      currentStockKg: 50,
      minThresholdKg: 20,
      unitPricePkr: 2150,
      warehouseLocation: "Rawalpindi Hub",
      status: "IN_STOCK",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Users collection: prevents self-escalation, ghost fields, and cross-user PII reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  // Self-create as CUSTOMER succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Ali Raza",
      city: "Rawalpindi",
      zone: "Chaklala Scheme III",
      role: "CUSTOMER",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-escalation to ADMIN fails
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob Hacker",
      city: "Islamabad",
      role: "ADMIN",
      status: "ACTIVE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Role escalation via update fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(500),
    })
  );

  // Cross-user PII read fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("ScrapInventory: Kotlin payload create, partial update, query filter, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  // Create without @DocumentId "id" field succeeds
  await assertSucceeds(
    aliceDb.collection("scrap_inventory").doc("inv_copper_01").set({
      userId: ALICE_UID,
      materialId: "mat_copper_01",
      name: "Pure Copper Wire",
      urduName: "تانبا وائر",
      category: "COPPER_BRASS",
      grade: "Grade A",
      currentStockKg: 120.5,
      minThresholdKg: 50.0,
      unitPricePkr: 2150.0,
      warehouseLocation: "Rawalpindi Hub I-10",
      status: "IN_STOCK",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field ("id" or "isVerified")
  await assertFails(
    aliceDb.collection("scrap_inventory").doc("inv_bad").set({
      id: "inv_bad",
      userId: ALICE_UID,
      materialId: "mat_iron_01",
      name: "Iron",
      category: "IRON_STEEL",
      grade: "Standard A",
      currentStockKg: 10,
      minThresholdKg: 5,
      unitPricePkr: 145,
      warehouseLocation: "Hub",
      status: "IN_STOCK",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("scrap_inventory").doc("inv_future").set({
      userId: ALICE_UID,
      materialId: "mat_iron_01",
      name: "Iron",
      category: "IRON_STEEL",
      grade: "Standard A",
      currentStockKg: 10,
      minThresholdKg: 5,
      unitPricePkr: 145,
      warehouseLocation: "Hub",
      status: "IN_STOCK",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Partial update on stock succeeds
  await assertSucceeds(
    aliceDb.collection("scrap_inventory").doc("inv_copper_01").update({
      currentStockKg: 35.0,
      status: "LOW_STOCK",
      updatedAt: pastDate(1000),
    })
  );

  // Filtered query matching Kotlin repository succeeds; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("scrap_inventory").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("scrap_inventory").get());

  // Bob cannot read or mutate Alice's inventory
  await assertFails(bobDb.collection("scrap_inventory").doc("inv_copper_01").get());
  await assertFails(
    bobDb.collection("scrap_inventory").doc("inv_copper_01").update({
      currentStockKg: 0,
      updatedAt: pastDate(500),
    })
  );
});

test("Pickups workflow: customer create, collector weigh/verify, customer confirm, terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const collectorDb = testEnv.authenticatedContext(COLLECTOR_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("pickups").doc("PKP_9001").set({
      userId: ALICE_UID,
      customerName: "Ali Raza",
      customerPhone: "+92 300 1234567",
      city: "Islamabad",
      zone: "F-10 Markaz",
      address: "House 14, Street 8, F-10/3, Islamabad",
      itemsSummary: "Iron (40 kg), Copper (5 kg)",
      estimatedWeightKg: 45,
      estimatedTotalPkr: 16550,
      verifiedWeightKg: 0,
      verifiedTotalPkr: 0,
      status: "ASSIGNED",
      scheduledDate: "Today",
      timeSlot: "02:00 PM - 04:00 PM",
      paymentMethod: "RAAST",
      collectorId: COLLECTOR_UID,
      collectorName: "Rashid Khan",
      otpVerified: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unrelated Bob cannot read or update
  await assertFails(bobDb.collection("pickups").doc("PKP_9001").get());
  await assertSucceeds(
    aliceDb.collection("pickups").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    collectorDb.collection("pickups").where("collectorId", "==", COLLECTOR_UID).get()
  );

  // Assigned collector records verified weight + OTP
  await assertSucceeds(
    collectorDb.collection("pickups").doc("PKP_9001").update({
      status: "CONFIRMED",
      verifiedWeightKg: 47.5,
      verifiedTotalPkr: 17200,
      otpVerified: true,
      updatedAt: pastDate(2000),
    })
  );

  // Customer confirms payment & receipt
  await assertSucceeds(
    aliceDb.collection("pickups").doc("PKP_9001").update({
      status: "COMPLETED",
      digitalReceiptNumber: "RCPT-ISB-9001",
      customerRating: 5,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: neither customer nor collector can mutate COMPLETED pickup
  await assertFails(
    collectorDb.collection("pickups").doc("PKP_9001").update({
      verifiedTotalPkr: 999999,
      updatedAt: pastDate(500),
    })
  );
});

test("Pricing rules & Wallet ledger immutability & Marketplace bidding", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ active: true });
    await db.collection("pricing_rules").doc("pr_copper_isb").set({
      materialId: "mat_copper_01",
      materialName: "Pure Copper Wire",
      category: "COPPER_BRASS",
      grade: "A-Grade",
      city: "Islamabad",
      zone: "I-10 Industrial",
      marketRate: 2300,
      buyingMargin: 6,
      sellingMargin: 10,
      collectionCost: 15,
      sortingCost: 5,
      minPrice: 1900,
      maxPrice: 2400,
      active: true,
      updatedBy: ADMIN_UID,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("marketplace_lots").doc("LOT_101").set({
      userId: ADMIN_UID,
      lotCode: "LOT-101",
      title: "Sorted Millberry Copper",
      materialName: "Pure Copper Wire",
      category: "COPPER_BRASS",
      grade: "99% Pure",
      weightKg: 1000,
      pricePerKg: 2450,
      minimumBidPerKg: 2350,
      highestBidPerKg: 2360,
      bidsCount: 1,
      location: "I-10 Hub, Islamabad",
      warehouseName: "Islamabad Central Hub",
      status: "AVAILABLE",
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Active pricing rules list succeeds with filter; non-admin cannot mutate pricing rules
  await assertSucceeds(
    aliceDb.collection("pricing_rules").where("active", "==", true).get()
  );
  await assertFails(
    aliceDb.collection("pricing_rules").doc("pr_copper_isb").update({
      marketRate: 9999,
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("pricing_rules").doc("pr_copper_isb").update({
      marketRate: 2320,
      updatedBy: ADMIN_UID,
      updatedAt: pastDate(500),
    })
  );

  // Immutable wallet ledger
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wallet_ledger")
      .doc("tx_001")
      .set({
        userId: ALICE_UID,
        amount: 2500,
        type: "CREDIT",
        category: "PICKUP_PAYOUT",
        referenceId: "PKP_9001",
        idempotencyKey: "IDEMP_001",
        balanceAfter: 2500,
        description: "Payout for pickup PKP_9001",
        createdAt: pastDate(1000),
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("wallet_ledger")
      .doc("tx_001")
      .update({ amount: 500000 })
  );

  // Marketplace bid succeeds when strictly higher than highestBidPerKg
  await assertSucceeds(
    aliceDb.collection("marketplace_lots").doc("LOT_101").update({
      highestBidPerKg: 2390,
      highestBidderUid: ALICE_UID,
      bidsCount: 2,
      status: "UNDER_BID",
      updatedAt: pastDate(500),
    })
  );
  // Lower bid rejected
  await assertFails(
    aliceDb.collection("marketplace_lots").doc("LOT_101").update({
      highestBidPerKg: 2350,
      highestBidderUid: ALICE_UID,
      bidsCount: 3,
      status: "UNDER_BID",
      updatedAt: pastDate(400),
    })
  );
});

test("Admin email spoofing: unverified email matching admin fails; verified admin email succeeds", async () => {
  const spoofedDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "hasanaamir815@gmail.com",
      email_verified: false,
    })
    .firestore();

  const verifiedAdminDb = testEnv
    .authenticatedContext("real_admin_uid", {
      email: "hasanaamir815@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertFails(
    spoofedDb.collection("pricing_rules").doc("pr_spoof").set({
      materialId: "mat_iron_01",
      materialName: "Iron",
      category: "IRON_STEEL",
      grade: "A",
      city: "Rawalpindi",
      zone: "Saddar",
      marketRate: 160,
      buyingMargin: 5,
      sellingMargin: 8,
      collectionCost: 4,
      sortingCost: 2,
      minPrice: 120,
      maxPrice: 170,
      active: true,
      updatedBy: "spoof_uid",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    verifiedAdminDb.collection("pricing_rules").doc("pr_real").set({
      materialId: "mat_iron_01",
      materialName: "Iron",
      category: "IRON_STEEL",
      grade: "A",
      city: "Rawalpindi",
      zone: "Saddar",
      marketRate: 160,
      buyingMargin: 5,
      sellingMargin: 8,
      collectionCost: 4,
      sortingCost: 2,
      minPrice: 120,
      maxPrice: 170,
      active: true,
      updatedBy: "real_admin_uid",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});
