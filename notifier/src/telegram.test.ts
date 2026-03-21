import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { sendTelegramMessage } from "./telegram.js";

const server = setupServer();

describe("sendTelegramMessage", () => {
  beforeEach(() => server.listen({ onUnhandledRequest: "error" }));
  afterEach(() => {
    server.resetHandlers();
    server.close();
  });

  it("sends a message via Telegram Bot API", async () => {
    let capturedBody: Record<string, unknown> | null = null;

    server.use(
      http.post("https://api.telegram.org/bottest-token/sendMessage", async ({ request }) => {
        capturedBody = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ ok: true, result: { message_id: 1 } });
      })
    );

    await sendTelegramMessage("test-token", "chat-123", "Hello <b>World</b>");

    assert.ok(capturedBody);
    assert.equal(capturedBody!.chat_id, "chat-123");
    assert.equal(capturedBody!.text, "Hello <b>World</b>");
    assert.equal(capturedBody!.parse_mode, "HTML");
  });

  it("throws on API error", async () => {
    server.use(
      http.post("https://api.telegram.org/bottest-token/sendMessage", () => {
        return HttpResponse.json({ ok: false, description: "Bad Request" }, { status: 400 });
      })
    );

    await assert.rejects(
      () => sendTelegramMessage("test-token", "chat-123", "Hello"),
      /Telegram API error/
    );
  });
});
