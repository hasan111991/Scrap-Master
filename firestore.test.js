const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "cust_alice_01";
const OTHER_CUST_UID = "cust_bob_02";
const COLLECTOR_1_UID = "col_rashid_01";
const COLLECTOR_2_UID = "col_usman_02";
const ADMIN_UID = "admin_hasan_01";

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

async function seedAdminDoc(uid = ADMIN_UID) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(uid).set({
      uid,
      email: "admin@collectionhub.pk",
      role: "admin",
      createdAt: pastDate(),
    });
  });
}

async function seedApprovedCollector(uid = COLLECTOR_1_UID) {
  const now = pastDate();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("collectors").doc(uid).set({
      collectorId: uid,
      userId: uid,
      fullName: "Rashid Scrap Traders",
      shopOrBusinessName: "Rashid Kabari Center",
      phone: "+92 333 5551122",
      city: "Rawalpindi",
      serviceAreas: ["Chaklala Scheme III", "Saddar"],
      acceptedCategories: ["Iron", "Copper", "Cardboard"],
      vehicleType: "Loader Pickup",
      availabilityOnline: true,
      approvalStatus: "approved",
      radiusKm: 5,
      completedPickupsCount: 0,
      totalEarningsPkr: 0,
      commissionDuePkr: 0,
      createdAt: now,
      updatedAt: now,
    });
  });
}

test("Unauthenticated users: denied across all Collection Hub collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("bookings").get());
  await assertFails(unauthDb.collection("pickup_pool").get());
  await assertFails(unauthDb.collection("collectors").get());
  await assertFails(unauthDb.collection("scrap_prices").get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
});

test("Users collection: prevents self-escalation to admin & cross-user PII read", async () => {
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_CUST_UID).firestore();
  const now = pastDate();

  // Valid customer create
  await assertSucceeds(
    custDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Sana Tariq",
      city: "Rawalpindi",
      area: "Bahria Town Phase 7",
      role: "customer",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-escalation to admin on create MUST FAIL
  await assertFails(
    otherDb.collection("users").doc(OTHER_CUST_UID).set({
      userId: OTHER_CUST_UID,
      displayName: "Attacker",
      city: "Islamabad",
      role: "admin",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-escalation via update MUST FAIL
  await assertFails(
    custDb.collection("users").doc(CUSTOMER_UID).update({
      role: "admin",
      updatedAt: pastDate(500),
    })
  );

  // Cross-user read MUST FAIL
  await assertFails(otherDb.collection("users").doc(CUSTOMER_UID).get());
});

test("Collector registration: enforces pending_approval and blocks unapproved pool locking", async () => {
  const colDb = testEnv.authenticatedContext(COLLECTOR_1_UID).firestore();
  const now = pastDate();

  // Cannot self-register directly as approved
  await assertFails(
    colDb.collection("collectors").doc(COLLECTOR_1_UID).set({
      collectorId: COLLECTOR_1_UID,
      userId: COLLECTOR_1_UID,
      fullName: "Usman Kabari",
      shopOrBusinessName: "Usman Traders",
      phone: "+92 300 1234567",
      city: "Islamabad",
      serviceAreas: ["F-10 Markaz"],
      acceptedCategories: ["Iron", "Paper"],
      vehicleType: "Suzuki Pickup",
      availabilityOnline: true,
      approvalStatus: "approved",
      radiusKm: 5,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid pending_approval + offline registration succeeds
  await assertSucceeds(
    colDb.collection("collectors").doc(COLLECTOR_1_UID).set({
      collectorId: COLLECTOR_1_UID,
      userId: COLLECTOR_1_UID,
      fullName: "Usman Kabari",
      shopOrBusinessName: "Usman Traders",
      phone: "+92 300 1234567",
      city: "Islamabad",
      serviceAreas: ["F-10 Markaz"],
      acceptedCategories: ["Iron", "Paper"],
      vehicleType: "Suzuki Pickup",
      availabilityOnline: false,
      approvalStatus: "pending_approval",
      radiusKm: 5,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Pending collector cannot toggle availabilityOnline to true before admin approves
  await assertFails(
    colDb.collection("collectors").doc(COLLECTOR_1_UID).update({
      availabilityOnline: true,
      updatedAt: pastDate(500),
    })
  );

  // Pending collector cannot approve themselves
  await assertFails(
    colDb.collection("collectors").doc(COLLECTOR_1_UID).update({
      approvalStatus: "approved",
      updatedAt: pastDate(500),
    })
  );
});

test("Atomic booking creation, PII isolation, first-accept lock, and 10-step completion lifecycle", async () => {
  await seedApprovedCollector(COLLECTOR_1_UID);
  await seedApprovedCollector(COLLECTOR_2_UID);
  await seedAdminDoc(ADMIN_UID);

  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const col1Db = testEnv.authenticatedContext(COLLECTOR_1_UID).firestore();
  const col2Db = testEnv.authenticatedContext(COLLECTOR_2_UID).firestore();
  const bookingId = "bk_test_001";
  const created = pastDate(5000);

  // Customer creates booking + pickup_pool atomically in a batch
  const createBatch = custDb.batch();
  createBatch.set(custDb.collection("bookings").doc(bookingId), {
    customerId: CUSTOMER_UID,
    customerName: "Ayesha Khan",
    customerPhone: "+92 312 9876543",
    city: "Rawalpindi",
    area: "Chaklala Scheme III",
    fullAddress: "House 14-B, Street 6, Chaklala Scheme III, Rawalpindi",
    categories: ["Iron", "Cardboard"],
    itemsSummary: "Iron (20 kg), Cardboard (15 kg)",
    unitType: "KG",
    estimatedWeightKg: 35,
    estimatedMinPkr: 3200,
    estimatedMaxPkr: 4100,
    preferredDate: "Tomorrow",
    preferredTimeSlot: "02:00 PM - 04:00 PM",
    paymentMethod: "Cash",
    status: "searching",
    otpCode: "4821",
    otpVerified: false,
    verifiedWeightKg: 0,
    finalAmountPkr: 0,
    customerConfirmed: false,
    paymentConfirmedByCustomer: false,
    matchingRadiusKm: 5,
    createdAt: created,
    updatedAt: created,
  });
  createBatch.set(custDb.collection("pickup_pool").doc(bookingId), {
    bookingId,
    customerId: CUSTOMER_UID,
    city: "Rawalpindi",
    area: "Chaklala Scheme III",
    categories: ["Iron", "Cardboard"],
    itemsSummary: "Iron & Cardboard (~35 kg)",
    unitType: "KG",
    estimatedQuantityLabel: "35 KG",
    preferredDate: "Tomorrow",
    preferredTimeSlot: "02:00 PM - 04:00 PM",
    status: "searching",
    radiusKm: 5,
    createdAt: created,
    updatedAt: created,
  });
  await assertSucceeds(createBatch.commit());

  // PII check: Collector 1 can list open pickup_pool (no address/phone)
  await assertSucceeds(
    col1Db.collection("pickup_pool").where("status", "==", "searching").get()
  );

  // PII check: Collector 1 CANNOT read full private booking document BEFORE winning lock!
  await assertFails(col1Db.collection("bookings").doc(bookingId).get());

  // Collector 1 wins atomic accept transaction (updates pickup_pool + bookings together)
  const acceptTime = pastDate(2000);
  const acceptBatch1 = col1Db.batch();
  acceptBatch1.update(col1Db.collection("pickup_pool").doc(bookingId), {
    status: "locked",
    assignedCollectorId: COLLECTOR_1_UID,
    updatedAt: acceptTime,
  });
  acceptBatch1.update(col1Db.collection("bookings").doc(bookingId), {
    status: "assigned",
    assignedCollectorId: COLLECTOR_1_UID,
    assignedCollectorName: "Rashid Scrap Traders",
    assignedCollectorPhone: "+92 333 5551122",
    assignedAt: acceptTime,
    updatedAt: acceptTime,
  });
  await assertSucceeds(acceptBatch1.commit());

  // Race condition check: Collector 2 tries to accept the same booking -> MUST FAIL!
  const acceptBatch2 = col2Db.batch();
  acceptBatch2.update(col2Db.collection("pickup_pool").doc(bookingId), {
    status: "locked",
    assignedCollectorId: COLLECTOR_2_UID,
    updatedAt: pastDate(1000),
  });
  acceptBatch2.update(col2Db.collection("bookings").doc(bookingId), {
    status: "assigned",
    assignedCollectorId: COLLECTOR_2_UID,
    assignedCollectorName: "Usman Kabari",
    assignedCollectorPhone: "+92 300 0000000",
    assignedAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertFails(acceptBatch2.commit());

  // Now Collector 1 CAN read full booking (address & phone revealed only to winner)
  await assertSucceeds(col1Db.collection("bookings").doc(bookingId).get());
  // Collector 2 still CANNOT read Collector 1's assigned booking
  await assertFails(col2Db.collection("bookings").doc(bookingId).get());

  // Collector 1 progresses workflow: on_the_way -> arrived -> weight_price_confirmation
  await assertSucceeds(
    col1Db.collection("bookings").doc(bookingId).update({
      status: "on_the_way",
      updatedAt: pastDate(900),
    })
  );
  await assertSucceeds(
    col1Db.collection("bookings").doc(bookingId).update({
      status: "arrived",
      otpVerified: true,
      updatedAt: pastDate(800),
    })
  );
  await assertSucceeds(
    col1Db.collection("bookings").doc(bookingId).update({
      status: "weight_price_confirmation",
      verifiedWeightKg: 38.5,
      finalAmountPkr: 3850,
      collectorNotes: "Clean heavy iron and dry cartons",
      updatedAt: pastDate(700),
    })
  );

  // Customer confirms weight/price and payment received -> completed
  await assertSucceeds(
    custDb.collection("bookings").doc(bookingId).update({
      status: "completed",
      customerConfirmed: true,
      paymentConfirmedByCustomer: true,
      rating: 5,
      review: "Punctual and accurate digital scale",
      updatedAt: pastDate(500),
    })
  );

  // Terminal State Lock: Collector cannot alter finalAmountPkr after completed
  await assertFails(
    col1Db.collection("bookings").doc(bookingId).update({
      finalAmountPkr: 9999,
      updatedAt: pastDate(100),
    })
  );
});

test("Admin-only controls, email verification guard, and commission settlements", async () => {
  await seedAdminDoc(ADMIN_UID);
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const custDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const spoofedEmailAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "hasanaamir815@gmail.com",
      email_verified: false,
    })
    .firestore();
  const now = pastDate();

  // Customer & unverified admin email CANNOT write scrap_prices
  await assertFails(
    custDb.collection("scrap_prices").doc("price_iron").set({
      categoryId: "iron",
      categoryName: "Iron",
      unit: "KG",
      minEstimatedPkr: 130,
      maxEstimatedPkr: 155,
      indicative: true,
      city: "Rawalpindi",
      active: true,
      updatedBy: CUSTOMER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    spoofedEmailAdminDb.collection("scrap_prices").doc("price_iron").set({
      categoryId: "iron",
      categoryName: "Iron",
      unit: "KG",
      minEstimatedPkr: 130,
      maxEstimatedPkr: 155,
      indicative: true,
      city: "Rawalpindi",
      active: true,
      updatedBy: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified Admin CAN manage indicative prices
  await assertSucceeds(
    adminDb.collection("scrap_prices").doc("price_iron").set({
      categoryId: "iron",
      categoryName: "Iron",
      unit: "KG",
      minEstimatedPkr: 130,
      maxEstimatedPkr: 155,
      indicative: true,
      city: "Rawalpindi",
      active: true,
      updatedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Manual payment screenshot/note cannot be created as verified by non-admin
  await assertFails(
    custDb.collection("payments").doc("pay_1").set({
      bookingId: "bk_test_001",
      customerId: CUSTOMER_UID,
      collectorId: COLLECTOR_1_UID,
      method: "Easypaisa",
      amountPkr: 3850,
      submittedByUid: CUSTOMER_UID,
      referenceNumber: "EP-99182",
      proofNote: "Screenshot attached",
      customerConfirmed: true,
      verificationStatus: "verified",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid pending_verification manual payment succeeds
  await assertSucceeds(
    custDb.collection("payments").doc("pay_1").set({
      bookingId: "bk_test_001",
      customerId: CUSTOMER_UID,
      collectorId: COLLECTOR_1_UID,
      method: "Easypaisa",
      amountPkr: 3850,
      submittedByUid: CUSTOMER_UID,
      referenceNumber: "EP-99182",
      proofNote: "Collector paid via Easypaisa",
      customerConfirmed: true,
      verificationStatus: "pending_verification",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Commission settlement is strictly Admin-only and immutable
  await assertFails(
    custDb.collection("commission_settlements").doc("set_1").set({
      collectorId: COLLECTOR_1_UID,
      collectorName: "Rashid",
      periodLabel: "Week 1",
      totalAmountPkr: 385,
      commissionIds: ["comm_bk_test_001"],
      paymentMethod: "Bank Transfer",
      referenceNote: "TXN-1",
      recordedByAdminUid: CUSTOMER_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("commission_settlements").doc("set_1").set({
      collectorId: COLLECTOR_1_UID,
      collectorName: "Rashid",
      periodLabel: "Week 1",
      totalAmountPkr: 385,
      commissionIds: ["comm_bk_test_001"],
      paymentMethod: "Bank Transfer",
      referenceNote: "HBL Ref 88123",
      recordedByAdminUid: ADMIN_UID,
      createdAt: now,
    })
  );
});
