// notifier/src/state.test.ts
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { parseState, serializeState, DEFAULT_STATE, type NotifierState } from "./state.js";

describe("state", () => {
  it("parses valid state JSON", () => {
    const json = JSON.stringify({
      lastCheckTimestamp: "2026-03-20T11:00:00Z",
      consecutiveFailures: 3,
      inFailedState: true,
      failedStateSince: "2026-03-20T08:00:00Z",
      lastSuccessTimestamp: "2026-03-20T07:00:00Z",
      lastWeeklyHeartbeat: "2026-03-17T07:00:00Z",
      lastDailyReminder: "2026-03-20T07:00:00Z",
    });
    const state = parseState(json);
    assert.equal(state.consecutiveFailures, 3);
    assert.equal(state.inFailedState, true);
    assert.equal(state.failedStateSince, "2026-03-20T08:00:00Z");
  });

  it("returns default state for empty/null input", () => {
    const state = parseState(null);
    assert.deepStrictEqual(state, DEFAULT_STATE);
  });

  it("serializes state to JSON", () => {
    const state: NotifierState = { ...DEFAULT_STATE, consecutiveFailures: 5 };
    const json = serializeState(state);
    const parsed = JSON.parse(json);
    assert.equal(parsed.consecutiveFailures, 5);
  });
});
