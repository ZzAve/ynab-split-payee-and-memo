export interface JobRun {
  id: string;
  state: string;
  created_at: string;
  started_at: string | null;
  completed_at: string | null;
}

interface ListJobRunsResponse {
  job_runs: JobRun[];
  total_count: number;
}

export class ScalewayJobsClient {
  private readonly baseUrl: string;
  private readonly secretKey: string;

  constructor(secretKey: string, region: string) {
    this.secretKey = secretKey;
    this.baseUrl = `https://api.scaleway.com/serverless-jobs/v1alpha1/regions/${region}`;
  }

  async listJobRuns(jobDefinitionId: string): Promise<JobRun[]> {
    const url = new URL(`${this.baseUrl}/job-runs`);
    url.searchParams.set("job_definition_id", jobDefinitionId);
    url.searchParams.set("order_by", "created_at_desc");

    const response = await fetch(url.toString(), {
      headers: { "X-Auth-Token": this.secretKey },
    });

    if (!response.ok) {
      throw new Error(`Scaleway API error: ${response.status} ${response.statusText}`);
    }

    const data = (await response.json()) as ListJobRunsResponse;
    return data.job_runs;
  }

  async getRunsSince(jobDefinitionId: string, since: string): Promise<JobRun[]> {
    const allRuns = await this.listJobRuns(jobDefinitionId);
    const sinceDate = new Date(since);
    return allRuns.filter((run) => new Date(run.created_at) > sinceDate);
  }
}
