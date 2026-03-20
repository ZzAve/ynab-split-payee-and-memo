import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { evaluateRules, type RuleResult } from "./rules.js";
import { DEFAULT_STATE, type NotifierState } from "./state.js";
import type { JobRun } from "./scaleway.js";

function makeRun(state: string, createdAt: string): JobRun {
  return {
    id: `run-${createdAt}`,
    state,
    created_at: createdAt,
    started_at: createdAt,
    completed_at: createdAt,
  };
}

const NOW = "2026-03-20T15:00:00Z";

describe("evaluateRules", () => {
  it("returns no notification when all runs succeed and no state issues", () => {
    const runs = [makeRun("succeeded", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(DEFAULT_STATE, runs, NOW);
    assert.equal(result.notification, null);
    assert.equal(result.newState.consecutiveFailures, 0);
  });

  it("increments consecutive failures but no notification under 3", () => {
    const runs = [
      makeRun("failed", "2026-03-20T14:00:00Z"),
      makeRun("failed", "2026-03-20T13:00:00Z"),
    ];
    const result = evaluateRules(DEFAULT_STATE, runs, NOW);
    assert.equal(result.notification, null);
    assert.equal(result.newState.consecutiveFailures, 2);
    assert.equal(result.newState.inFailedState, false);
  });

  it("sends consecutive-failures notification at 3 failures", () => {
    const state: NotifierState = {
      ...DEFAULT_STATE,
      consecutiveFailures: 2,
      lastSuccessTimestamp: "2026-03-20T11:00:00Z",
    };
    const runs = [makeRun("failed", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(state, runs, NOW);
    assert.equal(result.notification, "consecutive-failures");
    assert.equal(result.newState.consecutiveFailures, 3);
    assert.equal(result.newState.inFailedState, true);
  });

  it("sends recovery notification after failed state resolves", () => {
    const state: NotifierState = {
      ...DEFAULT_STATE,
      consecutiveFailures: 5,
      inFailedState: true,
      failedStateSince: "2026-03-20T09:00:00Z",
    };
    const runs = [makeRun("succeeded", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(state, runs, NOW);
    assert.equal(result.notification, "recovery");
    assert.equal(result.newState.consecutiveFailures, 0);
    assert.equal(result.newState.inFailedState, false);
  });

  it("sends daily reminder once per day when in failed state", () => {
    const state: NotifierState = {
      ...DEFAULT_STATE,
      consecutiveFailures: 10,
      inFailedState: true,
      failedStateSince: "2026-03-19T20:00:00Z",
      lastDailyReminder: "2026-03-19T15:00:00Z",
    };
    const runs = [makeRun("failed", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(state, runs, NOW);
    assert.equal(result.notification, "daily-reminder");
  });

  it("does not send daily reminder if already sent today", () => {
    const state: NotifierState = {
      ...DEFAULT_STATE,
      consecutiveFailures: 10,
      inFailedState: true,
      failedStateSince: "2026-03-19T20:00:00Z",
      lastDailyReminder: "2026-03-20T11:00:00Z",
    };
    const runs = [makeRun("failed", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(state, runs, NOW);
    assert.equal(result.notification, null);
  });

  it("sends weekly heartbeat on Monday", () => {
    // 2026-03-16 is a Monday
    const mondayNow = "2026-03-16T07:00:00Z";
    const state: NotifierState = {
      ...DEFAULT_STATE,
      lastWeeklyHeartbeat: "2026-03-09T07:00:00Z",
    };
    const runs = [makeRun("succeeded", "2026-03-16T06:00:00Z")];
    const result = evaluateRules(state, runs, mondayNow);
    assert.equal(result.notification, "weekly-heartbeat");
  });

  it("does not send weekly heartbeat on non-Monday", () => {
    // 2026-03-20 is a Friday
    const state: NotifierState = {
      ...DEFAULT_STATE,
      lastWeeklyHeartbeat: "2026-03-16T07:00:00Z",
    };
    const runs = [makeRun("succeeded", "2026-03-20T14:00:00Z")];
    const result = evaluateRules(state, runs, NOW);
    assert.equal(result.notification, null);
  });

  it("handles empty runs array (no runs since last check)", () => {
    const result = evaluateRules(DEFAULT_STATE, [], NOW);
    assert.equal(result.notification, null);
  });

  it("processes runs in chronological order (oldest first)", () => {
    const state: NotifierState = { ...DEFAULT_STATE, consecutiveFailures: 2 };
    // Runs come newest first from API, but logic should process oldest first
    const runs = [
      makeRun("succeeded", "2026-03-20T14:00:00Z"),
      makeRun("failed", "2026-03-20T13:00:00Z"),
    ];
    const result = evaluateRules(state, runs, NOW);
    // After processing: fail → 3, then success → 0
    assert.equal(result.newState.consecutiveFailures, 0);
  });
});
