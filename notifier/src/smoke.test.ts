// notifier/src/smoke.test.ts
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { mock } from "node:test";
import { run } from "./index.js";
import { DEFAULT_STATE } from "./state.js";

describe("smoke test: full notification flow", () => {
  it("detects 3 consecutive failures and sends alert", async () => {
    const savedStates: unknown[] = [];
    const sentMessages: string[] = [];

    const mockStorage = {
      loadState: mock.fn(async () => ({ ...DEFAULT_STATE })),
      saveState: mock.fn(async (state: unknown) => { savedStates.push(state); }),
    };

    const mockScaleway = {
      getRunsSince: mock.fn(async () => [
        { id: "r3", state: "failed", created_at: "2026-03-20T14:00:00Z", started_at: null, completed_at: null },
        { id: "r2", state: "failed", created_at: "2026-03-20T13:00:00Z", started_at: null, completed_at: null },
        { id: "r1", state: "failed", created_at: "2026-03-20T12:00:00Z", started_at: null, completed_at: null },
      ]),
    };

    const mockSendMessage = mock.fn(async (_t: string, _c: string, text: string) => {
      sentMessages.push(text);
    });

    const config = {
      telegram: { botToken: "t", chatId: "c" },
      scaleway: { secretKey: "k", accessKey: "a", projectId: "p", region: "nl-ams", bucketName: "b", jobId: "j" },
      scalewayConsoleLink: "https://console.scaleway.com/jobs/123",
    };

    await run({ config, storage: mockStorage as any, scaleway: mockScaleway as any, sendMessage: mockSendMessage });

    assert.equal(sentMessages.length, 1);
    assert.ok(sentMessages[0].includes("failed 3 times"));
    assert.equal((savedStates[0] as any).consecutiveFailures, 3);
    assert.equal((savedStates[0] as any).inFailedState, true);
  });

  it("detects recovery after failed state", async () => {
    const sentMessages: string[] = [];

    const mockStorage = {
      loadState: mock.fn(async () => ({
        ...DEFAULT_STATE,
        consecutiveFailures: 5,
        inFailedState: true,
        failedStateSince: "2026-03-20T09:00:00Z",
      })),
      saveState: mock.fn(async () => {}),
    };

    const mockScaleway = {
      getRunsSince: mock.fn(async () => [
        { id: "r1", state: "succeeded", created_at: "2026-03-20T14:00:00Z", started_at: null, completed_at: null },
      ]),
    };

    const mockSendMessage = mock.fn(async (_t: string, _c: string, text: string) => {
      sentMessages.push(text);
    });

    const config = {
      telegram: { botToken: "t", chatId: "c" },
      scaleway: { secretKey: "k", accessKey: "a", projectId: "p", region: "nl-ams", bucketName: "b", jobId: "j" },
      scalewayConsoleLink: "https://console.scaleway.com/jobs/123",
    };

    await run({ config, storage: mockStorage as any, scaleway: mockScaleway as any, sendMessage: mockSendMessage });

    assert.equal(sentMessages.length, 1);
    assert.ok(sentMessages[0].includes("recovered"));
  });
});
