package job_scheduler.model;

import job_scheduler.util.Util;

public class StatisticsModel {
    private double average_time;
    private long success_count;
    private long fail_count;
    private String jobName;

    public double getAverage_time() {
        return average_time * 1000; //convert to msec
    }

    public long getSuccess_count() {
        return success_count;
    }

    public long getFail_count() {
        return fail_count;
    }

    public void setAverage_time(double average_time) {
        this.average_time = average_time;
    }

    public void setSuccess_count(long success_count) {
        this.success_count = success_count;
    }

    public void setFail_count(long fail_count) {
        this.fail_count = fail_count;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public double getStability() {
        double sum = success_count + fail_count;

        if(sum == 0) {
            return -1;
        }
        return success_count / (sum);
    }

    public String getAverageTimeFormatted() {

        return Util.calculateTime(getAverage_time());

    }

    public String getStabilityFormatted() {

        return Util.getPercentageFormatted(getStability());

    }

    public long getTotalExecutions() {
        return success_count + fail_count;
    }
}
