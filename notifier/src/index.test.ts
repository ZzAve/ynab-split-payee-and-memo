// notifier/src/index.test.ts
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { mock } from "node:test";
import { run } from "./index.js";
import { DEFAULT_STATE } from "./state.js";

describe("run (orchestrator)", () => {
  it("loads state, evaluates rules, sends notification, saves state", async () => {
    const mockStorage = {
      loadState: mock.fn(async () => ({
        ...DEFAULT_STATE,
        consecutiveFailures: 2,
        lastSuccessTimestamp: "2026-03-20T11:00:00Z",
      })),
      saveState: mock.fn(async () => {}),
    };

    const mockScaleway = {
      getRunsSince: mock.fn(async () => [
        { id: "run-1", state: "failed", created_at: "2026-03-20T14:00:00Z", started_at: null, completed_at: null },
      ]),
    };

    const sentMessages: string[] = [];
    const mockSendMessage = mock.fn(async (_token: string, _chatId: string, text: string) => {
      sentMessages.push(text);
    });

    const config = {
      telegram: { botToken: "token", chatId: "chat" },
      scaleway: { secretKey: "key", accessKey: "key", projectId: "proj", region: "nl-ams", bucketName: "bucket", jobId: "job-123" },
      scalewayConsoleLink: "https://console.scaleway.com/jobs/123",
    };

    await run({ config, storage: mockStorage as any, scaleway: mockScaleway as any, sendMessage: mockSendMessage });

    assert.equal(mockStorage.saveState.mock.calls.length, 1);
    assert.equal(sentMessages.length, 1);
    assert.ok(sentMessages[0].includes("failed 3 times"));
  });

  it("does not send message when no notification needed", async () => {
    const mockStorage = {
      loadState: mock.fn(async () => DEFAULT_STATE),
      saveState: mock.fn(async () => {}),
    };

    const mockScaleway = {
      getRunsSince: mock.fn(async () => [
        { id: "run-1", state: "succeeded", created_at: "2026-03-20T14:00:00Z", started_at: null, completed_at: null },
      ]),
    };

    const mockSendMessage = mock.fn(async () => {});

    const config = {
      telegram: { botToken: "token", chatId: "chat" },
      scaleway: { secretKey: "key", accessKey: "key", projectId: "proj", region: "nl-ams", bucketName: "bucket", jobId: "job-123" },
      scalewayConsoleLink: "https://console.scaleway.com/jobs/123",
    };

    await run({ config, storage: mockStorage as any, scaleway: mockScaleway as any, sendMessage: mockSendMessage });

    assert.equal(mockSendMessage.mock.calls.length, 0);
    assert.equal(mockStorage.saveState.mock.calls.length, 1);
  });
});
