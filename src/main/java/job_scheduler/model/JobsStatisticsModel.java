package job_scheduler.model;

import job_scheduler.core.JobPipelineScheduler;
import job_scheduler.util.SQLiteOperations;
import job_scheduler.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class JobsStatisticsModel {
    private ArrayList<StatisticsModel> jobStats;
    public static final String SORT_BY_NAME = "name";
    public static final String SORT_BY_AVERAGE = "average";
    public static final String SORT_BY_SUCCESS = "success";
    public static final String SORT_BY_FAIL = "fail";
    public static final String SORT_BY_TOTAL = "total";
    public static final String SORT_BY_STABILITY = "stability";
    public static final String TYPE_ASCENDING = "asc";
    public static final String TYPE_DESCENDING = "desc";

    public JobsStatisticsModel() {
        jobStats = new ArrayList<>();
    }

    public ArrayList<StatisticsModel> getJobStats() {
        return jobStats;
    }

    public void initialize(String sort_by, String type) {

        synchronized (JobPipelineScheduler.mutex) {

            jobStats = SQLiteOperations.getAllStatistics();

            if (sort_by.equals(SORT_BY_NAME)) {
                Collections.sort(jobStats, new SortByNameAscending());
            } else if (sort_by.equals(SORT_BY_AVERAGE)) {
                Collections.sort(jobStats, new SortByAverageTimeAscending());
            } else if (sort_by.equals(SORT_BY_SUCCESS)) {
                Collections.sort(jobStats, new SortBySuccessCountAscending());
            } else if (sort_by.equals(SORT_BY_FAIL)) {
                Collections.sort(jobStats, new SortByFailCountAscending());
            } else if (sort_by.equals(SORT_BY_TOTAL)) {
                Collections.sort(jobStats, new SortByTotalCountAscending());
            } else if (sort_by.equals(SORT_BY_STABILITY)) {
                Collections.sort(jobStats, new SortByStabilityAscending());
            }

            if (type.equals(TYPE_DESCENDING)) {
                Collections.reverse(jobStats);
            }
        }

    }

    public String getAggregateAverageTime() {

        if(jobStats.isEmpty()) {
            return "N/A";
        }

        double average = 0;

        for(StatisticsModel job : jobStats) {
            average += job.getAverage_time();
        }

        return Util.calculateTime(average);

    }

    public long getAggregateSuccessCount() {

        long count = 0;

        for(StatisticsModel job : jobStats) {
            count += job.getSuccess_count();
        }

        return count;

    }

    public long getAggregateFailureCount() {

        long count = 0;

        for(StatisticsModel job : jobStats) {
            count += job.getFail_count();
        }

        return count;

    }

    public double getAggregateStability() {
        long fail = getAggregateFailureCount();
        long success = getAggregateSuccessCount();
        double total = fail + success;

        if(total == 0) {
            return -1;
        }

        return success / (total);
    }

    public String getAggregateStabilityFormatted() {

        return Util.getPercentageFormatted(getAggregateStability());

    }

    public long getAggregateTotalExecutions() {
        long count = 0;

        for(StatisticsModel job : jobStats) {
            count += job.getTotalExecutions();
        }

        return count;
    }

    class SortByNameAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            return a.getJobName().compareToIgnoreCase(b.getJobName());
        }
    }

    class SortByAverageTimeAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            if (a.getAverage_time() < b.getAverage_time()) return -1;
            if (a.getAverage_time() > b.getAverage_time()) return 1;
            return 0;
        }
    }

    class SortBySuccessCountAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            if (a.getSuccess_count() < b.getSuccess_count()) return -1;
            if (a.getSuccess_count() > b.getSuccess_count()) return 1;
            return 0;
        }
    }

    class SortByFailCountAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            if (a.getFail_count() < b.getFail_count()) return -1;
            if (a.getFail_count() > b.getFail_count()) return 1;
            return 0;
        }
    }

    class SortByTotalCountAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            if (a.getTotalExecutions() < b.getTotalExecutions()) return -1;
            if (a.getTotalExecutions() > b.getTotalExecutions()) return 1;
            return 0;
        }
    }

    class SortByStabilityAscending implements Comparator<StatisticsModel>
    {
        public int compare(StatisticsModel a, StatisticsModel b)
        {
            if (a.getStability() < b.getStability()) return -1;
            if (a.getStability() > b.getStability()) return 1;
            return 0;
        }
    }
}
