package job_scheduler.core;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

public class PipelineSettings {
	public static final int UNINITIALIZED = 0;
	public static final int OK = 1;
	public static final int FAILED = 2;

	private Map<String, String> variablesMap;
	private final long creationTime;
	private long registeredJobs;
	private final String timeStamp;
	private int status;
	private long failedJobs;
	private long successfulJobs;
	private long id;
	
	public PipelineSettings() {
		variablesMap =  new TreeMap<>();
		creationTime = System.currentTimeMillis();
		registeredJobs = 0;
		timeStamp = LocalDateTime.now().toString();
		status = UNINITIALIZED;
		id = 0;
	}

	public PipelineSettings(PipelineSettings other) {

		variablesMap = new TreeMap<>();

		variablesMap.putAll(other.variablesMap);

		creationTime = other.creationTime;
		registeredJobs = other.registeredJobs;
		timeStamp = other.timeStamp;
		status = other.status;
		failedJobs = other.failedJobs;
		successfulJobs = other.successfulJobs;
		id = other.id;

	}

	public String getValueForVariable(String variable) {
		
		return variablesMap.get(variable);
		
	}

    public void removeVariable(String variable) {

        variablesMap.remove(variable);

    }
	
	public void setValueForVariable(String variable, String value) {
		
		if(!variablesMap.containsKey(variable)) {
			variablesMap.put(variable, value);
		}
		else {
			variablesMap.replace(variable, value);
		}
		
	}
	
	public long getCreationTime() {
		
		return creationTime;
		
	}
	
	public long getRegisteredJobs() {
		
		return registeredJobs;
		
	}
	
	public void setRegisteredJobs(long registeredJobs) {
		
		this.registeredJobs = registeredJobs;
		
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public Map<String, String> getVariablesMap() {
		return variablesMap;
	}

	public long getFailedJobs() {
		return failedJobs;
	}

	public void setFailedJobs(long failedJobs) {
		this.failedJobs = failedJobs;
	}

	public long getSuccessfulJobs() {
		return successfulJobs;
	}

	public void setSuccessfulJobs(long successfulJobs) {
		this.successfulJobs = successfulJobs;
	}

	public double getStability() {
		long fail = getFailedJobs();
		long success = getSuccessfulJobs();
		double total = fail + success;

		if(total == 0) {
			return -1;
		}

		return success / (total);
	}


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
