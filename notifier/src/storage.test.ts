// notifier/src/storage.test.ts
import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { mock } from "node:test";
import { StateStorage } from "./storage.js";
import { DEFAULT_STATE } from "./state.js";

describe("StateStorage", () => {
  it("returns default state when object does not exist", async () => {
    const mockS3 = {
      send: mock.fn(async () => {
        const error = new Error("NoSuchKey") as Error & { name: string };
        error.name = "NoSuchKey";
        throw error;
      }),
    };

    const storage = new StateStorage(mockS3 as any, "test-bucket");
    const state = await storage.loadState();
    assert.deepStrictEqual(state, DEFAULT_STATE);
  });

  it("parses state from S3 object body", async () => {
    const stateJson = JSON.stringify({
      ...DEFAULT_STATE,
      consecutiveFailures: 5,
      inFailedState: true,
    });

    const mockS3 = {
      send: mock.fn(async () => ({
        Body: { transformToString: async () => stateJson },
      })),
    };

    const storage = new StateStorage(mockS3 as any, "test-bucket");
    const state = await storage.loadState();
    assert.equal(state.consecutiveFailures, 5);
    assert.equal(state.inFailedState, true);
  });

  it("saves state as JSON to S3", async () => {
    const sendMock = mock.fn(async () => ({}));
    const mockS3 = { send: sendMock };

    const storage = new StateStorage(mockS3 as any, "test-bucket");
    const state = { ...DEFAULT_STATE, consecutiveFailures: 3 };
    await storage.saveState(state);

    assert.equal(sendMock.mock.calls.length, 1);
    const command = sendMock.mock.calls[0].arguments[0];
    assert.equal(command.input.Bucket, "test-bucket");
    assert.equal(command.input.Key, "state.json");
    assert.ok(command.input.Body.includes('"consecutiveFailures": 3'));
  });
});
