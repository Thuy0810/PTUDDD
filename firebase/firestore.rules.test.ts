// ============================================================
// Firebase Firestore Security Rules — Test Suite
//
// Run with: firebase emulators:exec --only firestore "jest firestore.test.js"
// Hoặc: npx firebase firestore:delete --project demo-test
//
// Môi trường: Firebase Emulator Suite
// ============================================================

const { firestoreTest, firebase } = require("@firebase/rules-unit-testing");
const { assertFails, assertSucceeds } = firebase;

const PROJECT_ID = "expense-manager-test";
let testEnv;

beforeEach(async () => {
  testEnv = await firestoreTest.initTestEnvironment({
    projectId: PROJECT_ID,
  });
});

afterEach(() => {
  testEnv.cleanup();
});

function authedContext(uid) {
  return testEnv.authenticatedContext(uid, { uid: uid });
}
function unauthedContext() {
  return testEnv.unauthenticatedContext();
}

// ============================================================
// Helper: tạo valid transaction data
// ============================================================
function validTransaction(overrides = {}) {
  return {
    amount: 100000,
    type: "expense",
    date: new Date(),
    walletId: "wallet1",
    categoryId: "cat1",
    note: "Test",
    ...overrides,
  };
}
function validWallet(overrides = {}) {
  return {
    name: "Test Wallet",
    currentBalance: 1000000,
    type: "cash",
    ...overrides,
  };
}
function validBudget(overrides = {}) {
  return {
    scope: "monthly",
    limitAmount: 5000000,
    month: "2026-06",
    ...overrides,
  };
}
function validRecurring(overrides = {}) {
  return {
    type: "expense",
    amount: 50000,
    cycleType: "monthly",
    dayOfMonth: 15,
    enabled: true,
    walletId: "wallet1",
    categoryId: "cat1",
    note: "Recurring rent",
    ...overrides,
  };
}
function validGoal(overrides = {}) {
  return {
    title: "Mua xe",
    targetAmount: 50000000,
    savedAmount: 0,
    completed: false,
    walletId: "wallet1",
    deadline: new Date(Date.now() + 30 * 86400000),
    ...overrides,
  };
}
function validChallenge(overrides = {}) {
  return {
    title: "Tiết kiệm 30 ngày",
    targetSavings: 3000000,
    totalDays: 30,
    completedDays: 0,
    active: true,
    startDate: new Date(),
    ...overrides,
  };
}

// ============================================================
// TEST GROUP 1: Authentication & Authorization
// ============================================================
describe("1. Authentication & Authorization", () => {
  test("1.1 — Authenticated user CAN read own data", async () => {
    const db = authedContext("user1").firestore();
    await assertSucceeds(db.collection("users").doc("user1").get());
    await assertSucceeds(
      db.collection("users").doc("user1").collection("transactions").get()
    );
  });

  test("1.2 — Authenticated user CANNOT read other user's data", async () => {
    // Tạo data của user2
    await testEnv
      .withUserId("user2")
      .firestore()
      .collection("users")
      .doc("user2")
      .set({ email: "user2@test.com" });

    // user1 thử đọc user2
    const db = authedContext("user1").firestore();
    await assertFails(db.collection("users").doc("user2").get());
    await assertFails(
      db.collection("users").doc("user2").collection("transactions").get()
    );
  });

  test("1.3 — Unauthenticated user CANNOT read any data", async () => {
    const db = unauthedContext().firestore();
    await assertFails(db.collection("users").doc("anyone").get());
    await assertFails(
      db.collection("users").doc("anyone").collection("transactions").get()
    );
  });

  test("1.4 — Authenticated user CANNOT write to other user's UID", async () => {
    const db = authedContext("user1").firestore();
    await assertFails(
      db.collection("users").doc("user2").collection("transactions").add({})
    );
  });
});

// ============================================================
// TEST GROUP 2: Transactions — Amount Validation
// ============================================================
describe("2. Transactions — Amount Validation", () => {
  test("2.1 — Transaction with amount > 0 is ALLOWED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertSucceeds(col.add(validTransaction({ amount: 1 })));
    await assertSucceeds(
      col.add(validTransaction({ amount: 500000000 }))
    );
  });

  test("2.2 — Transaction with amount = 0 is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(col.add(validTransaction({ amount: 0 })));
  });

  test("2.3 — Transaction with negative amount is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(col.add(validTransaction({ amount: -50000 })));
    await assertFails(col.add(validTransaction({ amount: -1 })));
  });

  test("2.4 — Transaction with decimal amount is REJECTED (must be integer)", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(col.add(validTransaction({ amount: 50000.5 })));
    await assertFails(col.add(validTransaction({ amount: 100.1 })));
  });
});

// ============================================================
// TEST GROUP 3: Transactions — Type Validation
// ============================================================
describe("3. Transactions — Type Validation", () => {
  const db = authedContext("user1").firestore();
  const col = db
    .collection("users")
    .doc("user1")
    .collection("transactions");

  test("3.1 — type='income' is ALLOWED", async () => {
    await assertSucceeds(col.add(validTransaction({ type: "income" })));
  });

  test("3.2 — type='expense' is ALLOWED", async () => {
    await assertSucceeds(col.add(validTransaction({ type: "expense" })));
  });

  test("3.3 — type='transfer' is ALLOWED", async () => {
    await assertSucceeds(
      col.add(
        validTransaction({
          type: "transfer",
          fromWalletId: "wallet1",
          toWalletId: "wallet2",
        })
      )
    );
  });

  test("3.4 — type='hacker' is REJECTED", async () => {
    await assertFails(col.add(validTransaction({ type: "hacker" })));
  });

  test("3.5 — type='donation' is REJECTED", async () => {
    await assertFails(col.add(validTransaction({ type: "donation" })));
  });

  test("3.6 — type missing is REJECTED", async () => {
    const docWithoutType = { amount: 50000, date: new Date() };
    await assertFails(col.add(docWithoutType));
  });
});

// ============================================================
// TEST GROUP 4: Transactions — Transfer Validation
// ============================================================
describe("4. Transactions — Transfer Validation", () => {
  test("4.1 — Transfer with fromWalletId == toWalletId is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(
      col.add(
        validTransaction({
          type: "transfer",
          fromWalletId: "wallet1",
          toWalletId: "wallet1", // same!
        })
      )
    );
  });

  test("4.2 — Transfer without fromWalletId is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(
      col.add(
        validTransaction({
          type: "transfer",
          toWalletId: "wallet2",
        })
      )
    );
  });

  test("4.3 — Transfer without toWalletId is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(
      col.add(
        validTransaction({
          type: "transfer",
          fromWalletId: "wallet1",
        })
      )
    );
  });

  test("4.4 — Income/expense transfer CAN have walletId without fromWalletId/toWalletId", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertSucceeds(
      col.add(validTransaction({ type: "income", walletId: "wallet1" }))
    );
    await assertSucceeds(
      col.add(validTransaction({ type: "expense", walletId: "wallet1" }))
    );
  });
});

// ============================================================
// TEST GROUP 5: Transactions — Date Validation
// ============================================================
describe("5. Transactions — Date Validation", () => {
  test("5.1 — Transaction with valid timestamp date is ALLOWED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertSucceeds(col.add(validTransaction({ date: new Date() })));
  });

  test("5.2 — Transaction without date is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    const { date, ...noDate } = validTransaction();
    await assertFails(col.add(noDate));
  });

  test("5.3 — Transaction with date as string is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("transactions");
    await assertFails(
      col.add(validTransaction({ date: "2026-06-14T10:00:00Z" }))
    );
  });
});

// ============================================================
// TEST GROUP 6: Wallets — Balance Validation
// ============================================================
describe("6. Wallets — Balance Validation", () => {
  test("6.1 — Create wallet with valid positive balance is ALLOWED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("wallets");
    await assertSucceeds(col.add(validWallet({ currentBalance: 1000000 })));
    await assertSucceeds(col.add(validWallet({ currentBalance: 0 })));
  });

  test("6.2 — Create wallet with negative balance is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("wallets");
    await assertFails(col.add(validWallet({ currentBalance: -100 })));
  });

  test("6.3 — Create wallet without currentBalance is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("wallets");
    const { currentBalance, ...noBalance } = validWallet();
    await assertFails(col.add(noBalance));
  });

  test("6.4 — Update wallet balance to negative is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const ref = db
      .collection("users")
      .doc("user1")
      .collection("wallets")
      .doc("wallet1");
    await testEnv.withUserId("user1").firestore().doc(ref.path).set(validWallet());
    await assertFails(ref.update({ currentBalance: -50000 }));
  });

  test("6.5 — Wallet name max length 100 chars is ENFORCED", async () => {
    const db = authedContext("user1").firestore();
    const col = db.collection("users").doc("user1").collection("wallets");
    const longName = "A".repeat(101);
    await assertFails(col.add(validWallet({ name: longName })));
    await assertSucceeds(col.add(validWallet({ name: "A".repeat(100) })));
  });
});

// ============================================================
// TEST GROUP 7: Budgets — Limit Validation
// ============================================================
describe("7. Budgets — Limit Validation", () => {
  const db = authedContext("user1").firestore();
  const col = db.collection("users").doc("user1").collection("budgets");

  test("7.1 — Budget with limitAmount > 0 is ALLOWED", async () => {
    await assertSucceeds(col.add(validBudget({ limitAmount: 1 })));
    await assertSucceeds(col.add(validBudget({ limitAmount: 5000000 })));
  });

  test("7.2 — Budget with limitAmount = 0 is REJECTED", async () => {
    await assertFails(col.add(validBudget({ limitAmount: 0 })));
  });

  test("7.3 — Budget with negative limitAmount is REJECTED", async () => {
    await assertFails(col.add(validBudget({ limitAmount: -1000 })));
  });

  test("7.4 — Budget scope must be 'monthly' or 'category'", async () => {
    await assertSucceeds(col.add(validBudget({ scope: "monthly" })));
    await assertSucceeds(
      col.add(validBudget({ scope: "category", categoryId: "cat1" }))
    );
    await assertFails(col.add(validBudget({ scope: "weekly" })));
    await assertFails(col.add(validBudget({ scope: "invalid" })));
  });

  test("7.5 — Category budget MUST have categoryId", async () => {
    await assertFails(
      col.add(validBudget({ scope: "category" /* no categoryId */ }))
    );
    await assertSucceeds(
      col.add(validBudget({ scope: "category", categoryId: "cat1" }))
    );
  });

  test("7.6 — Monthly budget MUST NOT have categoryId", async () => {
    await assertFails(
      col.add(
        validBudget({ scope: "monthly", categoryId: "cat1" /* not allowed */ })
      )
    );
  });

  test("7.7 — Month format yyyy-MM is ENFORCED", async () => {
    await assertSucceeds(col.add(validBudget({ month: "2026-06" })));
    await assertSucceeds(col.add(validBudget({ month: "2025-01" })));
    await assertFails(col.add(validBudget({ month: "06-2026" }))); // wrong order
    await assertFails(col.add(validBudget({ month: "2026/06" }))); // wrong separator
    await assertFails(col.add(validBudget({ month: "2026-6" }))); // single digit
  });
});

// ============================================================
// TEST GROUP 8: Recurring — Field Validation
// ============================================================
describe("8. Recurring — Field Validation", () => {
  const db = authedContext("user1").firestore();
  const col = db.collection("users").doc("user1").collection("recurring");

  test("8.1 — Recurring with valid type (income/expense) is ALLOWED", async () => {
    await assertSucceeds(col.add(validRecurring({ type: "income" })));
    await assertSucceeds(col.add(validRecurring({ type: "expense" })));
  });

  test("8.2 — Recurring with invalid type is REJECTED", async () => {
    await assertFails(col.add(validRecurring({ type: "transfer" })));
    await assertFails(col.add(validRecurring({ type: "invalid" })));
  });

  test("8.3 — Recurring with amount <= 0 is REJECTED", async () => {
    await assertFails(col.add(validRecurring({ amount: 0 })));
    await assertFails(col.add(validRecurring({ amount: -1000 })));
  });

  test("8.4 — Recurring cycleType must be daily/weekly/monthly/yearly", async () => {
    await assertSucceeds(col.add(validRecurring({ cycleType: "daily" })));
    await assertSucceeds(col.add(validRecurring({ cycleType: "weekly" })));
    await assertSucceeds(col.add(validRecurring({ cycleType: "monthly" })));
    await assertSucceeds(col.add(validRecurring({ cycleType: "yearly" })));
    await assertFails(col.add(validRecurring({ cycleType: "hourly" })));
    await assertFails(col.add(validRecurring({ cycleType: "biweekly" })));
  });

  test("8.5 — Recurring dayOfMonth must be 1-31", async () => {
    await assertSucceeds(col.add(validRecurring({ dayOfMonth: 1 })));
    await assertSucceeds(col.add(validRecurring({ dayOfMonth: 31 })));
    await assertFails(col.add(validRecurring({ dayOfMonth: 0 })));
    await assertFails(col.add(validRecurring({ dayOfMonth: 32 })));
  });

  test("8.6 — Recurring dayOfWeek must be 1-7", async () => {
    await assertSucceeds(col.add(validRecurring({ cycleType: "weekly", dayOfWeek: 1 })));
    await assertSucceeds(col.add(validRecurring({ cycleType: "weekly", dayOfWeek: 7 })));
    await assertFails(col.add(validRecurring({ cycleType: "weekly", dayOfWeek: 0 })));
    await assertFails(col.add(validRecurring({ cycleType: "weekly", dayOfWeek: 8 })));
  });

  test("8.7 — Recurring monthOfYear must be 1-12", async () => {
    await assertSucceeds(col.add(validRecurring({ cycleType: "yearly", monthOfYear: 1 })));
    await assertSucceeds(col.add(validRecurring({ cycleType: "yearly", monthOfYear: 12 })));
    await assertFails(col.add(validRecurring({ cycleType: "yearly", monthOfYear: 0 })));
    await assertFails(col.add(validRecurring({ cycleType: "yearly", monthOfYear: 13 })));
  });

  test("8.8 — Recurring enabled must be bool if present", async () => {
    await assertSucceeds(col.add(validRecurring({ enabled: true })));
    await assertSucceeds(col.add(validRecurring({ enabled: false })));
    await assertFails(col.add(validRecurring({ enabled: "yes" })));
  });

  test("8.9 — Recurring dateStart must be timestamp if present", async () => {
    await assertSucceeds(col.add(validRecurring({ dateStart: new Date() })));
    await assertFails(col.add(validRecurring({ dateStart: "2026-01-01" })));
  });

  test("8.10 — Recurring dateEnd must be timestamp if present", async () => {
    await assertSucceeds(col.add(validRecurring({ dateEnd: new Date() })));
    await assertFails(col.add(validRecurring({ dateEnd: "2026-12-31" })));
  });
});

// ============================================================
// TEST GROUP 9: Savings Goals — Amount Validation
// ============================================================
describe("9. Savings Goals — Amount Validation", () => {
  const db = authedContext("user1").firestore();
  const col = db.collection("users").doc("user1").collection("savings_goals");

  test("9.1 — Goal with targetAmount > 0 is ALLOWED", async () => {
    await assertSucceeds(col.add(validGoal({ targetAmount: 1 })));
    await assertSucceeds(col.add(validGoal({ targetAmount: 50000000 })));
  });

  test("9.2 — Goal with targetAmount <= 0 is REJECTED", async () => {
    await assertFails(col.add(validGoal({ targetAmount: 0 })));
    await assertFails(col.add(validGoal({ targetAmount: -1000 })));
  });

  test("9.3 — Goal savedAmount must be >= 0", async () => {
    await assertSucceeds(col.add(validGoal({ savedAmount: 0 })));
    await assertSucceeds(col.add(validGoal({ savedAmount: 1000000 })));
    await assertFails(col.add(validGoal({ savedAmount: -1 })));
  });

  test("9.4 — Goal savedAmount must not exceed targetAmount", async () => {
    await assertFails(
      col.add(validGoal({ targetAmount: 10000000, savedAmount: 10000001 }))
    );
    await assertSucceeds(
      col.add(validGoal({ targetAmount: 10000000, savedAmount: 10000000 }))
    );
  });

  test("9.5 — Goal title is required and max 200 chars", async () => {
    await assertFails(col.add(validGoal({ title: "" })));
    await assertFails(col.add(validGoal({ title: "A".repeat(201) })));
    await assertSucceeds(col.add(validGoal({ title: "A".repeat(200) })));
  });

  test("9.6 — Goal completed must be bool if present", async () => {
    await assertSucceeds(col.add(validGoal({ completed: true })));
    await assertSucceeds(col.add(validGoal({ completed: false })));
    await assertFails(col.add(validGoal({ completed: "yes" })));
  });

  test("9.7 — Goal deadline must be timestamp if present", async () => {
    await assertSucceeds(
      col.add(validGoal({ deadline: new Date() }))
    );
    await assertFails(col.add(validGoal({ deadline: "2026-12-31" })));
  });
});

// ============================================================
// TEST GROUP 10: Savings Challenges — Amount Validation
// ============================================================
describe("10. Savings Challenges — Amount Validation", () => {
  const db = authedContext("user1").firestore();
  const col = db
    .collection("users")
    .doc("user1")
    .collection("savings_challenges");

  test("10.1 — Challenge with targetSavings > 0 is ALLOWED", async () => {
    await assertSucceeds(col.add(validChallenge({ targetSavings: 1 })));
    await assertSucceeds(
      col.add(validChallenge({ targetSavings: 3000000 }))
    );
  });

  test("10.2 — Challenge with targetSavings <= 0 is REJECTED", async () => {
    await assertFails(col.add(validChallenge({ targetSavings: 0 })));
    await assertFails(col.add(validChallenge({ targetSavings: -1000 })));
  });

  test("10.3 — Challenge title max 200 chars", async () => {
    await assertFails(col.add(validChallenge({ title: "A".repeat(201) })));
    await assertSucceeds(
      col.add(validChallenge({ title: "A".repeat(200) }))
    );
  });

  test("10.4 — Challenge active must be bool if present", async () => {
    await assertSucceeds(col.add(validChallenge({ active: true })));
    await assertSucceeds(col.add(validChallenge({ active: false })));
    await assertFails(col.add(validChallenge({ active: 1 })));
  });

  test("10.5 — Challenge startDate must be timestamp if present", async () => {
    await assertSucceeds(
      col.add(validChallenge({ startDate: new Date() }))
    );
    await assertFails(col.add(validChallenge({ startDate: "2026-01-01" })));
  });
});

// ============================================================
// TEST GROUP 11: Budget — Update Validation
// ============================================================
describe("11. Budget — Update Validation", () => {
  test("11.1 — Update budget with valid data is ALLOWED", async () => {
    const db = authedContext("user1").firestore();
    const ref = db
      .collection("users")
      .doc("user1")
      .collection("budgets")
      .doc("budget1");
    await testEnv.withUserId("user1").firestore().doc(ref.path).set(validBudget());
    await assertSucceeds(ref.update({ limitAmount: 6000000 }));
    await assertSucceeds(ref.update({ scope: "category", categoryId: "cat2" }));
  });

  test("11.2 — Update budget with limitAmount = 0 is REJECTED", async () => {
    const db = authedContext("user1").firestore();
    const ref = db
      .collection("users")
      .doc("user1")
      .collection("budgets")
      .doc("budget1");
    await testEnv.withUserId("user1").firestore().doc(ref.path).set(validBudget());
    await assertFails(ref.update({ limitAmount: 0 }));
  });
});

// ============================================================
// TEST GROUP 12: Concurrent Write Simulation
// ============================================================
describe("12. Concurrent Write Simulation", () => {
  test("12.1 — Multiple writes to same doc by same user: last write wins (Firestore behavior)", async () => {
    const db = authedContext("user1").firestore();
    const ref = db
      .collection("users")
      .doc("user1")
      .collection("wallets")
      .doc("wallet1");

    // Set initial
    await testEnv.withUserId("user1").firestore().doc(ref.path).set(validWallet());

    // Two concurrent updates — both may succeed at app level
    // At rules level, each update is independent
    await assertSucceeds(ref.update({ currentBalance: 2000000 }));
    await assertSucceeds(ref.update({ name: "Updated Name" }));
  });
});
