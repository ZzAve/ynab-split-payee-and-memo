// notifier/src/state.ts
export interface NotifierState {
  lastCheckTimestamp: string | null;
  consecutiveFailures: number;
  inFailedState: boolean;
  failedStateSince: string | null;
  lastSuccessTimestamp: string | null;
  lastWeeklyHeartbeat: string | null;
  lastDailyReminder: string | null;
}

export const DEFAULT_STATE: NotifierState = {
  lastCheckTimestamp: null,
  consecutiveFailures: 0,
  inFailedState: false,
  failedStateSince: null,
  lastSuccessTimestamp: null,
  lastWeeklyHeartbeat: null,
  lastDailyReminder: null,
};

export function parseState(json: string | null): NotifierState {
  if (!json) return { ...DEFAULT_STATE };
  return JSON.parse(json) as NotifierState;
}

export function serializeState(state: NotifierState): string {
  return JSON.stringify(state, null, 2);
}
