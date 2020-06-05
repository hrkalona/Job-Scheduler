package job_scheduler.core;

import job_scheduler.util.Util;

public class JobStatus {
    private final JobInfo job;
    private int status;
    private long retries;
    private long success;
    private long failure;

    public JobStatus(JobInfo job) {
        this.job = job;
        status = PipelineSettings.UNINITIALIZED;
        retries = 0;
        success = 0;
        failure = 0;
    }

    public JobStatus(JobStatus jobStat) {
        this.job = new JobInfo(jobStat.job);
        status = jobStat.status;
        retries = jobStat.retries;
        success = jobStat.success;
        failure = jobStat.failure;
    }

    public JobInfo getJobInfo() {
        return job;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getRetries() {
        return retries;
    }

    public void setRetries(long retries) {
        this.retries = retries;
    }

    public long getSuccess() {
        return success;
    }

    public void setSuccess(long success) {
        this.success = success;
    }

    public long getFailure() {
        return failure;
    }

    public void setFailure(long failure) {
        this.failure = failure;
    }

    public double getStability() {
        long fail = getFailure();
        long success = getSuccess();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public String getStabilityFormatted() {

        return Util.getPercentageFormatted(getStability());

    }
}
