# Scaleway Serverless Jobs Deployment

**Date:** 2026-03-19
**Status:** Done — infrastructure deployed, cron job running in production (as of 2026-03-21)

## Problem

The app currently runs as a cron job on a laptop, meaning it only executes when the laptop is on. We need a reliable, always-on, low-maintenance way to run this hourly.

## Decision

Deploy to **Scaleway Serverless Jobs** — a managed service that runs a Docker container on a cron schedule and bills only for execution time. No server to maintain.

### Why Scaleway Serverless Jobs
- Zero infrastructure management (no OS, no Docker, no SSH)
- Pay-per-use pricing, likely within free tier (~21,600 vCPU-s/month vs 200,000 free)
- European provider (France), GDPR compliant
- Native cron trigger support
- Existing Docker image + CI pipeline require no changes

### Alternatives considered
- **Raspberry Pi 3** — rejected, ARMv7 (32-bit) incompatible with JDK 25
- **Raspberry Pi 4/5** — viable but requires hardware purchase + server maintenance
- **European VPS (Hetzner, Netcup, etc.)** — works but overkill for a single cron job; requires OS/Docker maintenance
- **Rewrite in Go/Rust** — would enable Pi 3 support but unjustified effort for a working app

## Architecture

```
GitHub Action (on push to main)
    → builds Docker image
    → pushes to Docker Hub (zzave/ynab-split-payee:latest)

Scaleway Serverless Job
    → cron trigger: "0 * * * *" (hourly, UTC)
    → pulls image from Docker Hub
    → runs container with env vars (YNAB_TOKEN, YNAB_BUDGET_ID, etc.)
    → container exits, resources freed
    → exit code visible in Scaleway dashboard
```

No code changes required.

## Setup Steps

1. ✅ **Create Scaleway account** — select Paris or Amsterdam region
2. ✅ **Create a Serverless Job** via console:
   - Image source: `docker.io/zzave/ynab-split-payee:latest`
   - Resources: 1 vCPU, 256-512MB RAM
   - Environment variables: `YNAB_TOKEN`, `YNAB_BUDGET_ID` (and optionally `YNAB_ACCOUNT_ID`, `YNAB_BUDGET_IDS`)
3. ✅ **Add cron trigger:** `0 * * * *` (hourly, UTC)
4. ✅ **Verify:** trigger a manual run, check logs in dashboard

## CI/CD Integration

No changes needed. The existing GitHub Action pushes `zzave/ynab-split-payee` to Docker Hub on push to main. Scaleway pulls `latest` on each job invocation.

**Note:** Verify whether Scaleway caches the image or picks up new `:latest` tags automatically. May need a re-deploy trigger.

## Monitoring (for now)

- Check Scaleway dashboard for job run history and exit codes
- Failed runs (non-zero exit) are visible in the job's run log

## Future Work

### Telegram Notifications
- Alert on first failure
- Daily digest if still failing
- Notification on recovery
- Optional weekly summary
- Connects to Scaleway job status (exit codes / webhook on failure)

### Other Infrastructure (separate from this setup)
- Grafana and/or Plex would need a VPS or Pi 4/5 — not suited for serverless
