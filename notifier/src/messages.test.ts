import { describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  formatConsecutiveFailures,
  formatRecovery,
  formatDailyReminder,
  formatWeeklyHeartbeat,
} from "./messages.js";

const CONSOLE_LINK = "https://console.scaleway.com/jobs/123";

describe("message formatting", () => {
  it("formats consecutive failures message", () => {
    const msg = formatConsecutiveFailures(3, "2026-03-20T09:00:00Z", CONSOLE_LINK);
    assert.ok(msg.includes("failed 3 times in a row"));
    assert.ok(msg.includes(CONSOLE_LINK));
    assert.ok(msg.includes("ynab.com/settings/developer"));
  });

  it("formats recovery message", () => {
    const msg = formatRecovery("2026-03-20T06:00:00Z", 8, "2026-03-20T14:00:00Z");
    assert.ok(msg.includes("recovered"));
    assert.ok(msg.includes("8 runs"));
  });

  it("formats daily reminder", () => {
    const msg = formatDailyReminder(12, "2026-03-19T20:00:00Z", CONSOLE_LINK);
    assert.ok(msg.includes("still failing"));
    assert.ok(msg.includes("12 consecutive failures"));
    assert.ok(msg.includes(CONSOLE_LINK));
  });

  it("formats weekly heartbeat — all succeeded", () => {
    const msg = formatWeeklyHeartbeat(168, 0, CONSOLE_LINK);
    assert.ok(msg.includes("healthy"));
    assert.ok(msg.includes("168/168"));
  });

  it("formats weekly heartbeat — some failed", () => {
    const msg = formatWeeklyHeartbeat(160, 8, CONSOLE_LINK);
    assert.ok(msg.includes("160/168"));
  });
});
