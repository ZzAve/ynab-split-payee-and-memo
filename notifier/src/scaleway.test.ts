import { describe, it, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { ScalewayJobsClient, type JobRun } from "./scaleway.js";

const server = setupServer();

describe("ScalewayJobsClient", () => {
  beforeEach(() => server.listen({ onUnhandledRequest: "error" }));
  afterEach(() => {
    server.resetHandlers();
    server.close();
  });

  it("fetches job runs sorted by created_at desc", async () => {
    const mockRuns: JobRun[] = [
      { id: "run-2", state: "succeeded", created_at: "2026-03-20T10:00:00Z", started_at: "2026-03-20T10:00:01Z", completed_at: "2026-03-20T10:00:30Z" },
      { id: "run-1", state: "failed", created_at: "2026-03-20T09:00:00Z", started_at: "2026-03-20T09:00:01Z", completed_at: "2026-03-20T09:00:30Z" },
    ];

    server.use(
      http.get("https://api.scaleway.com/serverless-jobs/v1alpha1/regions/nl-ams/job-runs", ({ request }) => {
        const url = new URL(request.url);
        assert.equal(url.searchParams.get("job_definition_id"), "job-123");
        assert.equal(url.searchParams.get("order_by"), "created_at_desc");
        assert.equal(request.headers.get("X-Auth-Token"), "secret-key");
        return HttpResponse.json({ job_runs: mockRuns, total_count: 2 });
      })
    );

    const client = new ScalewayJobsClient("secret-key", "nl-ams");
    const runs = await client.listJobRuns("job-123");
    assert.equal(runs.length, 2);
    assert.equal(runs[0].id, "run-2");
    assert.equal(runs[1].state, "failed");
  });

  it("filters runs since a given timestamp", async () => {
    const mockRuns: JobRun[] = [
      { id: "run-3", state: "succeeded", created_at: "2026-03-20T12:00:00Z", started_at: "2026-03-20T12:00:01Z", completed_at: "2026-03-20T12:00:30Z" },
      { id: "run-2", state: "failed", created_at: "2026-03-20T10:00:00Z", started_at: "2026-03-20T10:00:01Z", completed_at: "2026-03-20T10:00:30Z" },
      { id: "run-1", state: "succeeded", created_at: "2026-03-20T08:00:00Z", started_at: "2026-03-20T08:00:01Z", completed_at: "2026-03-20T08:00:30Z" },
    ];

    server.use(
      http.get("https://api.scaleway.com/serverless-jobs/v1alpha1/regions/nl-ams/job-runs", () => {
        return HttpResponse.json({ job_runs: mockRuns, total_count: 3 });
      })
    );

    const client = new ScalewayJobsClient("secret-key", "nl-ams");
    const runs = await client.getRunsSince("job-123", "2026-03-20T09:00:00Z");
    assert.equal(runs.length, 2);
    assert.equal(runs[0].id, "run-3");
    assert.equal(runs[1].id, "run-2");
  });
});
