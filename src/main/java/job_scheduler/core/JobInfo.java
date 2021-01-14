package job_scheduler.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import job_scheduler.util.Util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class JobInfo implements Comparable<JobInfo> {

    private final String jobData;
    private final String additionalData;
    private final long priority;
    private final String tag;
    private final boolean valid;
    private final String creationTimeStamp;
    private String servedTimeStamp;
    private String finalizedTimeStamp;
    private final boolean consistent;
    private final String uuid;
    private long startTime;
    private long endTime;
    private final Map<String, String> params;

    private String host;

    public JobInfo() {
    	jobData = "";
    	additionalData = "";
    	priority = -1;
    	tag = "";
    	valid = false;
    	creationTimeStamp = "";
        servedTimeStamp = "";
        finalizedTimeStamp = "";
    	consistent = false;
        uuid = "";
        params = new TreeMap<>();
        startTime = -1;
        endTime = -1;
        host = "";
    }
    
    public JobInfo(String jobData, String additionalData, long priority, String tag, boolean consistent, Map<String, String> params) {
        this.jobData = jobData;
        this.additionalData = additionalData;
        this.priority = priority;
        this.tag = tag;
        this.consistent = consistent;
        valid = true;
        creationTimeStamp = LocalDateTime.now().toString();
        servedTimeStamp = "";
        finalizedTimeStamp = "";
        startTime = -1;
        endTime = -1;
        uuid = UUID.randomUUID().toString();
        this.params = new TreeMap<>();
        this.params.putAll(params);
        host = "";
    }

    public JobInfo(JobInfo other) {
        jobData = other.jobData;
        additionalData = other.additionalData;
        priority = other.priority;
        tag = other.tag;
        consistent = other.consistent;
        valid = other.valid;
        creationTimeStamp = other.creationTimeStamp;
        servedTimeStamp = other.servedTimeStamp;
        finalizedTimeStamp = other.finalizedTimeStamp;
        startTime = other.startTime;
        endTime = other.endTime;
        uuid = other.uuid;
        params = new TreeMap<>();
        params.putAll(other.params);
        host = other.host;
    }

    public String getJobData() {
        return jobData;
    }
    
    public String getAdditionalData() {
    	return additionalData;
    }
    
    public long getPriority() {
    	return priority;
    }
    
    public String getTag() {
    	return tag;
    }
    
    public boolean isValid() {
    	return valid;
    }
    
    public String getCreationTimestamp() {
    	return creationTimeStamp;
    }

    public String getServedTimeStamp() {
        return servedTimeStamp;
    }

    public void setServedTimeStamp(String servedTimeStamp) {
        this.servedTimeStamp = servedTimeStamp;
    }

    public String getFinalizedTimeStamp() {
        return finalizedTimeStamp;
    }

    public void setFinalizedTimeStamp(String finalizedTimeStamp) {
        this.finalizedTimeStamp = finalizedTimeStamp;
    }

    @JsonIgnore
    public long getExecutionTime() {

        if(endTime == -1) {
            return -1;
        }
        else if(endTime < startTime) {
            return -1;
        }

        return endTime - startTime;
    }

    @JsonIgnore
    public String getExecutionTimeFormatted() {

        long time = getExecutionTime();
        if(time == -1) {
            return "N/A";
        }

        return Util.calculateTime(time);
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public boolean isConsistent() {
    	return consistent;
    }

    public String getUUID() {
        return uuid;
    }

    public Map<String, String> getParams() {
        return params;
    }

    @JsonIgnore
    public String getParamsFormatted() {

        String temp = "";
        for (Map.Entry<String, String> entrySet : params.entrySet()) {
            temp += entrySet.getKey() + " : " + entrySet.getValue() + "\n";
        }
        return temp;

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

	public int compareTo(JobInfo o) {
	    if(o.priority < priority) return -1;
		else if(o.priority > priority) return 1;		
		return 0;
	}
}
