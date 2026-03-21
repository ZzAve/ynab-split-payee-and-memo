// notifier/src/storage.ts
import { GetObjectCommand, PutObjectCommand, type S3Client } from "@aws-sdk/client-s3";
import { parseState, serializeState, DEFAULT_STATE, type NotifierState } from "./state.js";

const STATE_KEY = "state.json";

export class StateStorage {
  constructor(
    private readonly s3: S3Client,
    private readonly bucket: string
  ) {}

  async loadState(): Promise<NotifierState> {
    try {
      const response = await this.s3.send(
        new GetObjectCommand({ Bucket: this.bucket, Key: STATE_KEY })
      );
      const body = await response.Body!.transformToString();
      return parseState(body);
    } catch (error: unknown) {
      if (error instanceof Error && error.name === "NoSuchKey") {
        return { ...DEFAULT_STATE };
      }
      throw error;
    }
  }

  async saveState(state: NotifierState): Promise<void> {
    await this.s3.send(
      new PutObjectCommand({
        Bucket: this.bucket,
        Key: STATE_KEY,
        Body: serializeState(state),
        ContentType: "application/json",
      })
    );
  }
}
