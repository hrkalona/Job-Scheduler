package job_scheduler.model;

import job_scheduler.core.JobInfo;
import job_scheduler.core.JobPipelineScheduler;
import job_scheduler.util.Util;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class SchedulerModel {
    private Map<String, PipelineModel> schedulerModel;

    public SchedulerModel() {
        schedulerModel = new TreeMap<>();
    }

    public Map<String, PipelineModel> getSchedulerModel() {
        return schedulerModel;
    }

    public void initialize() {

        synchronized (JobPipelineScheduler.mutex) {

            for (Map.Entry<String, Map<String, PriorityQueue<JobInfo>>> entry : JobPipelineScheduler.pipelineMap.entrySet()) {

                PipelineModel model = new PipelineModel();
                model.initialize(entry.getValue(), JobPipelineScheduler.pipelineSettings.get(entry.getKey()), JobPipelineScheduler.servedJobsPerPipeline.get(entry.getKey()));
                schedulerModel.put(entry.getKey(), model);

            }

        }
    }

    public static String getFormattedUpTime()
    {
        long duration = System.currentTimeMillis() - JobPipelineScheduler.initializationTime;
        return Util.calculateTime(duration);
    }

    public long getAggregateRemainingJobCount() {

        long count = 0;

        for(Map.Entry<String, PipelineModel> pipeline : schedulerModel.entrySet()) {
            count += pipeline.getValue().getAggregateRemainingJobCount();
        }

        return count;

    }

    public long getAggregateRegisteredJobCount() {

        long count = 0;

        for(Map.Entry<String, PipelineModel> pipeline : schedulerModel.entrySet()) {
            count += pipeline.getValue().getAggregateRegisteredJobCount();
        }

        return count;

    }

    public double getCompletionFactor() {
        long registeredJobs = getAggregateRegisteredJobCount();

        if(registeredJobs == 0) {
            return -1;
        }

        return ((double)(registeredJobs - getAggregateRemainingJobCount())) / registeredJobs;
    }

    public String getCompletionFormatted() {

        return Util.getPercentageFormatted(getCompletionFactor());

    }

    public String getCompletionValue() {
        return Util.getPercentageValue(getCompletionFactor());
    }

    public long getAggregateSuccessfulJobCount() {

        long count = 0;

        for(Map.Entry<String, PipelineModel> pipeline : schedulerModel.entrySet()) {
            count += pipeline.getValue().getSuccessfulJobCount();
        }

        return count;

    }

    public long getAggregateFailedJobCount() {

        long count = 0;

        for(Map.Entry<String, PipelineModel> pipeline : schedulerModel.entrySet()) {
            count += pipeline.getValue().getFailedJobCount();
        }

        return count;

    }

    public long getAggregateTotalJobCount() {

        long count = getAggregateSuccessfulJobCount() + getAggregateFailedJobCount();

        return count;

    }

    public double getAggregateStability() {
        long fail = getAggregateFailedJobCount();
        long success = getAggregateSuccessfulJobCount();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public String getAggregateStabilityFormatted() {

        return Util.getPercentageFormatted(getAggregateStability());

    }

    public long getSessionRegisteredPipelines() {
        return JobPipelineScheduler.sessionRegisteredPipelines;
    }

    public long getSessionRegisteredJobs() {
        return JobPipelineScheduler.sessionRegisteredJobs;
    }

    public long getSessionRegisteredVariables() { return JobPipelineScheduler.sessionRegisteredVariables; }

    public long getSessionRegisteredTags() { return JobPipelineScheduler.sessionRegisteredTags; }

    public long getSessionSuccessfulJobs() { return JobPipelineScheduler.sessionSucceedJobs; }

    public long getSessionTotalJobs() { return getSessionSuccessfulJobs() + getSessionFailedJobs(); }

    public long getSessionFailedJobs() { return JobPipelineScheduler.sessionFailedJobs; }

    public long getSessionFailedPipelines() { return JobPipelineScheduler.sessionFailedPipelines; }
    public long getSessionSucceedPipelines() { return JobPipelineScheduler.sessionSucceedPipelines; }
    public long getSessionUndeterminedPipelines() { return JobPipelineScheduler.sessionUndeterminedPipelines; }
    public long getSessionCompletedPipelines() { return JobPipelineScheduler.sessionCompletedPipelines; }

    public double getSessionStability() {
        long fail = getSessionFailedJobs();
        long success = getSessionSuccessfulJobs();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public double getSessionPipelineStability() {
        long fail = getSessionFailedPipelines();
        long success = getSessionSucceedPipelines();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public String getSessionStabilityFormatted() {

        return Util.getPercentageFormatted(getSessionStability());

    }

    public String getSessionPipelineStabilityFormatted() {

        return Util.getPercentageFormatted(getSessionPipelineStability());

    }

    public String getInitializationTimestamp() {
        return JobPipelineScheduler.initializationTimestamp;
    }
}
