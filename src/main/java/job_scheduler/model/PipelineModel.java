package job_scheduler.model;

import job_scheduler.core.JobInfo;
import job_scheduler.core.JobStatus;
import job_scheduler.core.PipelineSettings;
import job_scheduler.util.Util;

import java.util.*;


public class PipelineModel {
    private Map<String, ArrayList<JobModel>> pipelineMap;
    private PipelineSettings pipelineSettings;
    private ArrayList<JobStatus> servedJobs;

    public PipelineModel() {
        pipelineMap = new TreeMap<>();
        servedJobs = new ArrayList<>();
    }

    public Map<String, ArrayList<JobModel>> getPipelineMap() {
        return pipelineMap;
    }

    public PipelineSettings getPipelineSettings() {
        return pipelineSettings;
    }

    public ArrayList<JobStatus> getServedJobs() {
        return servedJobs;
    }

    public long getServedJobsExecutions() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            count += curr.getRetries();
        }
        return count;
    }

    public long getServedJobsFailures() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            count += curr.getFailure();
        }
        return count;
    }

    public long getServedJobsSuccess() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            count += curr.getSuccess();
        }
        return count;
    }

    public double getServedJobsStability() {

        long fail = getServedJobsFailures();
        long success = getServedJobsSuccess();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);

    }


    public String getServedJobsStabilityFormatted() {

        return Util.getPercentageFormatted(getServedJobsStability());

    }

    public int getServedJobsStatus() {
        int status = PipelineSettings.UNINITIALIZED;
        for(JobStatus curr : servedJobs) {
            if(curr.getStatus() == PipelineSettings.OK && status == PipelineSettings.UNINITIALIZED) {
                status = PipelineSettings.OK;
            }
            else if(curr.getStatus() == PipelineSettings.FAILED && (status == PipelineSettings.UNINITIALIZED || status == PipelineSettings.OK)) {
                status = PipelineSettings.FAILED;
            }
        }
        return status;
    }

    public long getServedJobsStatusFailureCount() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            if(curr.getStatus() == PipelineSettings.FAILED) {
                count++;
            }
        }
        return count;
    }

    public long getServedJobsStatusSuccessCount() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            if(curr.getStatus() == PipelineSettings.OK) {
                count++;
            }
        }
        return count;
    }

    public long getServedJobsStatusUndeterminedCount() {
        long count = 0;
        for(JobStatus curr : servedJobs) {
            if(curr.getStatus() == PipelineSettings.UNINITIALIZED) {
                count++;
            }
        }
        return count;
    }

    public double getServedJobsStatusStability() {

        long fail = getServedJobsStatusFailureCount();
        long success = getServedJobsStatusSuccessCount();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);

    }


    public String getServedJobsStatusStabilityFormatted() {

        return Util.getPercentageFormatted(getServedJobsStatusStability());

    }

    private long getServedJobsExecutionTime() {
        long count = 0;
        boolean found = false;


        for(JobStatus curr : servedJobs) {
            long time = curr.getJobInfo().getExecutionTime();
            if(time != -1) {
                count += time;
                found = true;
            }
        }

        if(!found) {
            return -1;
        }

        return count;
    }

    public String getServedJobsExecutionTimeFormatted() {
        long count = getServedJobsExecutionTime();
        if(count == -1) {
            return "N/A";
        }
        return Util.calculateTime(count);
    }

    public void initialize(Map<String, PriorityQueue<JobInfo>> tagJobs, PipelineSettings pipelineSettings, ArrayList<JobStatus> servedJobs) {

        this.pipelineSettings = new PipelineSettings(pipelineSettings);

        for (Map.Entry<String, PriorityQueue<JobInfo>> entry : tagJobs.entrySet()) {
            ArrayList<JobModel> list = new ArrayList<>();

            Object[] jobsArray = entry.getValue().toArray();
            Arrays.sort(jobsArray);

            for(int i = 0; i < jobsArray.length; i++) {
                JobModel jobmodel = new JobModel(new JobInfo((JobInfo)jobsArray[i]));
                jobmodel.initialize();
                list.add(jobmodel);
            }

            pipelineMap.put(entry.getKey(), list);
        }

        for(JobStatus jobStat : servedJobs) {
            this.servedJobs.add(new JobStatus(jobStat));
        }

    }

    public long getAggregateRemainingJobCount() {

        long count = 0;

        for(Map.Entry<String, ArrayList<JobModel>> jobs : pipelineMap.entrySet()) {
            count += jobs.getValue().size();
        }

        return count;

    }

    public long getAggregateRegisteredJobCount() {


        return pipelineSettings.getRegisteredJobs();

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

    public long getSuccessfulJobCount() {

        return pipelineSettings.getSuccessfulJobs();

    }

    public long getFailedJobCount() {

        return pipelineSettings.getFailedJobs();

    }

    public double getStability() {

        return pipelineSettings.getStability();

    }

    public long getTotalJobCount() {

        return getSuccessfulJobCount() + getFailedJobCount();

    }

    public String getStabilityFormatted() {

        return Util.getPercentageFormatted(getStability());

    }

    public  String getFormattedUpTime()
    {
        long duration = System.currentTimeMillis() - pipelineSettings.getCreationTime();
        return Util.calculateTime(duration);
    }

}
