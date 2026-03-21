import type { NotifierState } from "./state.js";
import type { JobRun } from "./scaleway.js";

export type NotificationType =
  | "consecutive-failures"
  | "recovery"
  | "daily-reminder"
  | "weekly-heartbeat";

export interface RuleResult {
  notification: NotificationType | null;
  newState: NotifierState;
  context: {
    failedRunCount?: number;
    failedStateSince?: string | null;
    succeededThisWeek?: number;
    failedThisWeek?: number;
  };
}

export function evaluateRules(
  state: NotifierState,
  runs: JobRun[],
  now: string = new Date().toISOString()
): RuleResult {
  // Process runs in chronological order (oldest first)
  const sortedRuns = [...runs].sort(
    (a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime()
  );

  const newState: NotifierState = { ...state, lastCheckTimestamp: now };
  let notification: NotificationType | null = null;
  // Capture pre-recovery values for the recovery message
  let preRecoveryFailedRunCount = state.consecutiveFailures;
  let preRecoveryFailedStateSince = state.failedStateSince;

  for (const run of sortedRuns) {
    if (run.state === "succeeded") {
      if (newState.inFailedState) {
        preRecoveryFailedRunCount = newState.consecutiveFailures;
        preRecoveryFailedStateSince = newState.failedStateSince;
        notification = "recovery";
      }
      newState.consecutiveFailures = 0;
      newState.inFailedState = false;
      newState.failedStateSince = null;
      newState.lastSuccessTimestamp = run.created_at;
    } else if (run.state === "failed") {
      newState.consecutiveFailures++;
      if (newState.consecutiveFailures >= 3 && !newState.inFailedState) {
        newState.inFailedState = true;
        newState.failedStateSince = newState.failedStateSince ?? run.created_at;
        notification = "consecutive-failures";
      }
    }
  }

  // Daily reminder: in failed state, no recovery/new-failure notification, not sent today
  if (
    newState.inFailedState &&
    notification === null &&
    shouldSendDailyReminder(newState.lastDailyReminder, now)
  ) {
    notification = "daily-reminder";
    newState.lastDailyReminder = now;
  }

  // Weekly heartbeat: Monday, not in failed state, no other notification
  if (notification === null && isMonday(now) && shouldSendWeeklyHeartbeat(newState.lastWeeklyHeartbeat, now)) {
    notification = "weekly-heartbeat";
    newState.lastWeeklyHeartbeat = now;
  }

  const oneWeekAgo = new Date(new Date(now).getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
  const weekRuns = sortedRuns.filter((r) => r.created_at > oneWeekAgo);
  const succeededThisWeek = weekRuns.filter((r) => r.state === "succeeded").length;
  const failedThisWeek = weekRuns.filter((r) => r.state === "failed").length;

  return {
    notification,
    newState,
    context: { failedRunCount: preRecoveryFailedRunCount, failedStateSince: preRecoveryFailedStateSince, succeededThisWeek, failedThisWeek },
  };
}

function shouldSendDailyReminder(lastReminder: string | null, now: string): boolean {
  if (!lastReminder) return true;
  const lastDate = new Date(lastReminder).toISOString().slice(0, 10);
  const nowDate = new Date(now).toISOString().slice(0, 10);
  return lastDate !== nowDate;
}

function isMonday(isoTimestamp: string): boolean {
  return new Date(isoTimestamp).getUTCDay() === 1;
}

function shouldSendWeeklyHeartbeat(lastHeartbeat: string | null, now: string): boolean {
  if (!lastHeartbeat) return true;
  const diff = new Date(now).getTime() - new Date(lastHeartbeat).getTime();
  return diff > 6 * 24 * 60 * 60 * 1000;
}
