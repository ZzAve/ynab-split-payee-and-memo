// notifier/src/config.ts
export interface Config {
  telegram: {
    botToken: string;
    chatId: string;
  };
  scaleway: {
    secretKey: string;
    accessKey: string;
    projectId: string;
    region: string;
    bucketName: string;
    jobId: string;
  };
  scalewayConsoleLink: string;
}

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export function loadConfig(): Config {
  return {
    telegram: {
      botToken: requireEnv("TELEGRAM_BOT_TOKEN"),
      chatId: requireEnv("TELEGRAM_CHAT_ID"),
    },
    scaleway: {
      secretKey: requireEnv("SCW_SECRET_KEY"),
      accessKey: requireEnv("SCW_ACCESS_KEY"),
      projectId: requireEnv("SCW_PROJECT_ID"),
      region: requireEnv("SCW_REGION"),
      bucketName: requireEnv("SCW_BUCKET_NAME"),
      jobId: requireEnv("SCW_JOB_ID"),
    },
    scalewayConsoleLink: requireEnv("SCALEWAY_CONSOLE_LINK"),
  };
}
