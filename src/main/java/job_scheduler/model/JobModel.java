package job_scheduler.model;

import job_scheduler.core.JobInfo;
import job_scheduler.util.SQLiteOperations;

public class JobModel {
    private final JobInfo info;
    private StatisticsModel stats;

    public JobModel( JobInfo info) {
        this.info = info;
    }

    public JobInfo getInfo() {
        return info;
    }

    public StatisticsModel getStats() {
        return stats;
    }

    public void initialize() {
        stats = SQLiteOperations.getStatistics(info.getJobData());
    }

}
