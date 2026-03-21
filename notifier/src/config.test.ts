// notifier/src/config.test.ts
import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import { loadConfig } from "./config.js";

describe("loadConfig", () => {
  const REQUIRED_VARS = {
    TELEGRAM_BOT_TOKEN: "test-bot-token",
    TELEGRAM_CHAT_ID: "123456",
    SCALEWAY_CONSOLE_LINK: "https://console.scaleway.com/jobs/123",
    SCW_SECRET_KEY: "scw-secret",
    SCW_ACCESS_KEY: "scw-access",
    SCW_PROJECT_ID: "project-123",
    SCW_REGION: "nl-ams",
    SCW_BUCKET_NAME: "ynab-notifier",
    SCW_JOB_ID: "job-def-123",
  };

  let savedEnv: Record<string, string | undefined>;

  beforeEach(() => {
    savedEnv = { ...process.env };
    for (const [key, value] of Object.entries(REQUIRED_VARS)) {
      process.env[key] = value;
    }
  });

  afterEach(() => {
    process.env = savedEnv;
  });

  it("loads all config from environment variables", () => {
    const config = loadConfig();
    assert.equal(config.telegram.botToken, "test-bot-token");
    assert.equal(config.telegram.chatId, "123456");
    assert.equal(config.scaleway.secretKey, "scw-secret");
    assert.equal(config.scaleway.accessKey, "scw-access");
    assert.equal(config.scaleway.region, "nl-ams");
    assert.equal(config.scaleway.bucketName, "ynab-notifier");
    assert.equal(config.scaleway.jobId, "job-def-123");
    assert.equal(config.scalewayConsoleLink, "https://console.scaleway.com/jobs/123");
  });

  it("throws when a required variable is missing", () => {
    delete process.env.TELEGRAM_BOT_TOKEN;
    assert.throws(() => loadConfig(), /TELEGRAM_BOT_TOKEN/);
  });
});
