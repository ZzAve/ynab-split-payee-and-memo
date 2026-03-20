// notifier/src/index.ts
import { S3Client } from "@aws-sdk/client-s3";
import { loadConfig, type Config } from "./config.js";
import { ScalewayJobsClient } from "./scaleway.js";
import { sendTelegramMessage } from "./telegram.js";
import { StateStorage } from "./storage.js";
import { evaluateRules } from "./rules.js";
import {
  formatConsecutiveFailures,
  formatRecovery,
  formatDailyReminder,
  formatWeeklyHeartbeat,
} from "./messages.js";

interface Dependencies {
  config: Config;
  storage: StateStorage;
  scaleway: ScalewayJobsClient;
  sendMessage: typeof sendTelegramMessage;
}

export async function run(deps: Dependencies): Promise<void> {
  const { config, storage, scaleway, sendMessage } = deps;

  // 1. Load state
  const state = await storage.loadState();
  console.log("Loaded state:", JSON.stringify(state));

  // 2. Fetch runs since last check
  const since = state.lastCheckTimestamp ?? new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
  const runs = await scaleway.getRunsSince(config.scaleway.jobId, since);
  console.log(`Found ${runs.length} runs since ${since}`);

  // 3. Evaluate rules
  const now = new Date().toISOString();
  const result = evaluateRules(state, runs, now);
  console.log("Rule result:", result.notification ?? "no notification");

  // 4. Send notification if needed
  if (result.notification) {
    const message = formatMessage(result, config);
    await sendMessage(config.telegram.botToken, config.telegram.chatId, message);
    console.log(`Sent ${result.notification} notification`);
  }

  // 5. Save updated state
  await storage.saveState(result.newState);
  console.log("State saved");
}

function formatMessage(
  result: ReturnType<typeof evaluateRules>,
  config: Config
): string {
  switch (result.notification) {
    case "consecutive-failures":
      return formatConsecutiveFailures(
        result.newState.consecutiveFailures,
        result.newState.lastSuccessTimestamp,
        config.scalewayConsoleLink
      );
    case "recovery":
      return formatRecovery(
        result.newState.failedStateSince ?? new Date().toISOString(),
        result.context.failedRunCount ?? 0
      );
    case "daily-reminder":
      return formatDailyReminder(
        result.newState.consecutiveFailures,
        result.newState.lastSuccessTimestamp,
        config.scalewayConsoleLink
      );
    case "weekly-heartbeat":
      return formatWeeklyHeartbeat(
        result.context.succeededThisWeek ?? 0,
        result.context.failedThisWeek ?? 0,
        config.scalewayConsoleLink
      );
    default:
      return "";
  }
}

// Entry point — only runs when executed directly
const isMainModule = process.argv[1] && import.meta.url.endsWith(process.argv[1].replace(/\\/g, "/"));
if (isMainModule || process.argv[1]?.endsWith("dist/index.js")) {
  const config = loadConfig();

  const s3 = new S3Client({
    region: config.scaleway.region,
    endpoint: `https://s3.${config.scaleway.region}.scw.cloud`,
    credentials: {
      accessKeyId: config.scaleway.accessKey,
      secretAccessKey: config.scaleway.secretKey,
    },
  });

  const storage = new StateStorage(s3, config.scaleway.bucketName);
  const scaleway = new ScalewayJobsClient(config.scaleway.secretKey, config.scaleway.region);

  run({ config, storage, scaleway, sendMessage: sendTelegramMessage })
    .then(() => {
      console.log("Done");
      process.exit(0);
    })
    .catch((error) => {
      console.error("Fatal error:", error);
      process.exit(1);
    });
}
