package job_scheduler.model;

import job_scheduler.util.SQLiteOperations;
import job_scheduler.util.Util;

public class PipelineStatisticsModel {
    private long succeed_pipelines;
    private long failed_pipelines;
    private long undetermined_pipelines;
    private long registered_pipelines;
    private long completed_pipelines;
    private double average_pipeline_completion_time;
    private double average_pipeline_jobs;
    private double average_pipeline_jobs_stability;

    public PipelineStatisticsModel() {
        average_pipeline_jobs_stability = -1;
    }

    public void initialize() {
        SQLiteOperations.getPipelineStatistics(this);
    }

    public long getSucceed_pipelines() {
        return succeed_pipelines;
    }

    public void setSucceed_pipelines(long succeed_pipelines) {
        this.succeed_pipelines = succeed_pipelines;
    }

    public long getFailed_pipelines() {
        return failed_pipelines;
    }

    public void setFailed_pipelines(long failed_pipelines) {
        this.failed_pipelines = failed_pipelines;
    }

    public long getUndetermined_pipelines() {
        return undetermined_pipelines;
    }

    public void setUndetermined_pipelines(long undetermined_pipelines) {
        this.undetermined_pipelines = undetermined_pipelines;
    }

    public long getRegistered_pipelines() {
        return registered_pipelines;
    }

    public void setRegistered_pipelines(long registered_pipelines) {
        this.registered_pipelines = registered_pipelines;
    }

    public long getCompleted_pipelines() {
        return completed_pipelines;
    }

    public void setCompleted_pipelines(long completed_pipelines) {
        this.completed_pipelines = completed_pipelines;
    }

    public double getAverage_pipeline_completion_time() {
        return average_pipeline_completion_time;
    }

    public void setAverage_pipeline_completion_time(double average_pipeline_completion_time) {
        this.average_pipeline_completion_time = average_pipeline_completion_time;
    }

    public String getFormattedAverageCompletionTime()
    {
        return Util.calculateTime(getAverage_pipeline_completion_time());
    }

    public double getPipelineStability() {
        long fail = getFailed_pipelines();
        long success = getSucceed_pipelines();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public String getPipelineStabilityFormatted() {

        return Util.getPercentageFormatted(getPipelineStability());

    }

    public String getAveragePipelineJobStabilityFormatted() {

        return Util.getPercentageFormatted(getAverage_pipeline_jobs_stability());

    }

    public double getAverage_pipeline_jobs() {
        return average_pipeline_jobs;
    }

    public String getAverage_pipeline_jobsFormatted() {
        return String.format("%.2f", getAverage_pipeline_jobs());
    }

    public void setAverage_pipeline_jobs(double average_pipeline_jobs) {
        this.average_pipeline_jobs = average_pipeline_jobs;
    }

    public double getAverage_pipeline_jobs_stability() {
        return average_pipeline_jobs_stability;
    }

    public void setAverage_pipeline_jobs_stability(double average_pipeline_jobs_stability) {
        this.average_pipeline_jobs_stability = average_pipeline_jobs_stability;
    }
}
