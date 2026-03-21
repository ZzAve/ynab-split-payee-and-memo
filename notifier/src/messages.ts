
function timeAgo(isoTimestamp: string, now: Date = new Date()): string {
  const diff = now.getTime() - new Date(isoTimestamp).getTime();
  const hours = Math.floor(diff / (1000 * 60 * 60));
  if (hours < 1) return "less than 1h ago";
  if (hours === 1) return "1h ago";
  return `${hours}h ago`;
}

function durationSince(isoTimestamp: string, now: Date = new Date()): string {
  const diff = now.getTime() - new Date(isoTimestamp).getTime();
  const hours = Math.floor(diff / (1000 * 60 * 60));
  if (hours < 1) return "less than 1 hour";
  if (hours === 1) return "1 hour";
  return `${hours} hours`;
}

export function formatConsecutiveFailures(
  count: number,
  lastSuccess: string | null,
  consoleLink: string
): string {
  const lastSuccessText = lastSuccess ? `Last success: ${timeAgo(lastSuccess)}.` : "No recent success.";
  return [
    `⚠️ <b>YNAB splitter has failed ${count} times in a row.</b>`,
    lastSuccessText,
    "",
    `→ <a href="${consoleLink}">Check logs</a>`,
    `→ Common fix: verify YNAB token hasn't expired`,
    `  https://app.ynab.com/settings/developer`,
  ].join("\n");
}

export function formatRecovery(
  failedSince: string,
  failedRunCount: number,
  now: string = new Date().toISOString()
): string {
  const duration = durationSince(failedSince, new Date(now));
  return [
    `✅ <b>YNAB splitter recovered.</b>`,
    `Was failing for ${duration} (${failedRunCount} runs).`,
    "",
    "No action needed.",
  ].join("\n");
}

export function formatDailyReminder(
  consecutiveFailures: number,
  lastSuccess: string | null,
  consoleLink: string
): string {
  const lastSuccessText = lastSuccess ? `Last success: ${timeAgo(lastSuccess)}.` : "No recent success.";
  return [
    `🔴 <b>YNAB splitter still failing.</b>`,
    `${consecutiveFailures} consecutive failures. ${lastSuccessText}`,
    "",
    `→ <a href="${consoleLink}">Check logs</a>`,
    `→ If token expired: regenerate at`,
    `  https://app.ynab.com/settings/developer`,
    `→ If API issue: check https://status.ynab.com`,
  ].join("\n");
}

export function formatWeeklyHeartbeat(
  succeeded: number,
  failed: number,
  consoleLink: string
): string {
  const total = succeeded + failed;
  const status = failed === 0 ? "healthy" : "had some failures";
  return [
    `📊 <b>Weekly status: YNAB splitter ${status}.</b>`,
    `${succeeded}/${total} runs succeeded this week.`,
    "",
    `→ <a href="${consoleLink}">Dashboard</a>`,
  ].join("\n");
}
