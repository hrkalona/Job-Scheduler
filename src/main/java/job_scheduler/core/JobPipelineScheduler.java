package job_scheduler.core;

import java.time.LocalDateTime;
import java.util.*;

import job_scheduler.util.Logger;
import job_scheduler.util.SQLiteOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobPipelineScheduler {
	public static final String DEFAULT_TAG = "";
	public static Map<String, Map<String,  PriorityQueue<JobInfo>>> pipelineMap;
	public static Map<String, PipelineSettings> pipelineSettings;
	public static Map<String, ArrayList<JobStatus>> servedJobsPerPipeline;
	private static final int CLEANUP_DELAY = 1800000;//every 30 minutes
	private static final int STALE_PIPELINE_TIMEOUT = 36000000; // 10 hours
	private static final int FINISHED_PIPELINE_TIMEOUT = 4 * 3600000; // 4 hours
	private static Timer timer;
	public static Integer mutex;
	public static long initializationTime;
	public static String initializationTimestamp;
	public static long sessionRegisteredPipelines;
	public static long sessionRegisteredJobs;
	public static long sessionRegisteredVariables;
	public static long sessionRegisteredTags;
	public static long sessionSucceedJobs;
	public static long sessionFailedJobs;
	public static long sessionFailedPipelines;
	public static long sessionSucceedPipelines;
	public static long sessionUndeterminedPipelines;
	public static long sessionCompletedPipelines;

	static {
		pipelineMap = new TreeMap<>();
		pipelineSettings = new TreeMap<>();
		servedJobsPerPipeline = new TreeMap<>();
		mutex = new Integer(0);
		Logger.initialize();
		SQLiteOperations.createDatabase();

		timer = new Timer();
		timer.schedule(new CleanUpTask(), CLEANUP_DELAY, CLEANUP_DELAY);
		Logger.logMessage("Setting the clean up task to be executed every " + CLEANUP_DELAY + " ms", Logger.INFO, JobPipelineScheduler.class.getName());

        initializationTime = System.currentTimeMillis();
		initializationTimestamp = LocalDateTime.now().toString();
	}
	
	@RequestMapping("/register_pipeline")
    public int registerPipeline(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="force", defaultValue="false")boolean force) {	
    	synchronized (mutex) {
	    	if(!pipelineMap.containsKey(pipeline_id)) {	    		
	    		Map<String,  PriorityQueue<JobInfo>> tagMap = new TreeMap<String,  PriorityQueue<JobInfo>>();
	    		pipelineMap.put(pipeline_id, tagMap);
				PipelineSettings settings = new PipelineSettings();
	    		pipelineSettings.put(pipeline_id, settings);
				servedJobsPerPipeline.put(pipeline_id, new ArrayList<>());

				long id = SQLiteOperations.updatePipelineStatistic("registered_pipelines");
				settings.setId(id);

				sessionRegisteredPipelines++;

				Logger.logMessage("Creating new pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
	    		return 1;
	    	}
	    	
	    	if(force) {
	    		unregisterPipeline(pipeline_id);
	    		Map<String,  PriorityQueue<JobInfo>> tagMap = new TreeMap<String,  PriorityQueue<JobInfo>>();
	    		pipelineMap.put(pipeline_id, tagMap);
				PipelineSettings settings = new PipelineSettings();
	    		pipelineSettings.put(pipeline_id, settings);
				servedJobsPerPipeline.put(pipeline_id, new ArrayList<>());

				long id = SQLiteOperations.updatePipelineStatistic("registered_pipelines");
				settings.setId(id);

				sessionRegisteredPipelines++;

				Logger.logMessage("Creating new pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
	    		return 1;
	    	}

			Logger.logMessage("Pipeline with id " + pipeline_id + " already exists", Logger.WARNING, JobPipelineScheduler.class.getName());
    		return -1;
    	}
    }
	
	@RequestMapping("/unregister_pipeline")
    public int unregisterPipeline(@RequestParam(value="pipeline_id") String pipeline_id) {	
    	synchronized (mutex) {
			return removePipeline(pipeline_id);
    	}
    }

	@RequestMapping("/unregister_all")
    public int unregisterAllPipelines() {	
    	synchronized (mutex) {
	    	if(pipelineMap.isEmpty()) {
				Logger.logMessage("No active pipelines exist", Logger.INFO, JobPipelineScheduler.class.getName());
	    		return 1;
	    	}

	    	ArrayList<String> pipelinesToBeRemoved = new ArrayList<>();

			for (Map.Entry<String, Map<String, PriorityQueue<JobInfo>>> entry : pipelineMap.entrySet()) {
				pipelinesToBeRemoved.add(entry.getKey());
			}

			for(String pipeline_id : pipelinesToBeRemoved) {
				unregisterPipeline(pipeline_id);
			}

	    	pipelineMap.clear();
	    	pipelineSettings.clear();
			servedJobsPerPipeline.clear();
			Logger.logMessage("Removing all pipelines", Logger.INFO, JobPipelineScheduler.class.getName());
			return 1;
    	}
    }
    
    @RequestMapping("/register_job")
    public int registerJob(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="job")String job, @RequestParam(value="priority", defaultValue="1")long priority, @RequestParam(value="tag", defaultValue=DEFAULT_TAG)String tag, @RequestParam(value="consistent", defaultValue="true")boolean consistent, @RequestParam(value="additional", defaultValue="")String additional, @RequestParam(value="use_time", defaultValue="true")boolean use_time, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override, @RequestParam(value="quantity", defaultValue="1")long quantity, @RequestParam(value="parameters", defaultValue="")String parameters) {
    	synchronized (mutex) {
	    	
	    	if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return -1;
	    	}
	    	
	    	Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);
	    	
	    	PriorityQueue<JobInfo> pipelineJobs = null;

	    	String finalTag = tag_override.isEmpty() ? tag : tag_override;

	    	if(!tagMap.containsKey(finalTag)) {
	    		pipelineJobs = new PriorityQueue<>();
	    		tagMap.put(finalTag, pipelineJobs);
				Logger.logMessage("Creating new tag '" + finalTag + "'", Logger.INFO, JobPipelineScheduler.class.getName());
				sessionRegisteredTags++;
	    	}
	    	else {
	    		pipelineJobs = tagMap.get(finalTag);
				Logger.logMessage("Tag '" + finalTag + "' already exists", Logger.INFO, JobPipelineScheduler.class.getName());
	    	}
	    	
	    	long customPriority = priority;
	    	
	    	if(use_time) {	    		
	    		long time = SQLiteOperations.getTimeForJob(job);
	    		
	    		if(time != -1) {
	    			customPriority = time;
					Logger.logMessage("Ignoring priority " + priority + " for job " + job, Logger.INFO, JobPipelineScheduler.class.getName());
					Logger.logMessage("Using average time " + customPriority + " as priority for job " + job, Logger.INFO, JobPipelineScheduler.class.getName());
	    		}
	    	}

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

	    	quantity = quantity <= 0 ? 1 : quantity;

			Map<String, String> params = parseParameters(parameters);

	    	for(long i = 0; i < quantity; i++) {
				Logger.logMessage("Queueing job " + job + " with priority " + customPriority + " and tag '" + finalTag + "' for pipeline " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

				JobInfo newJob = new JobInfo(job, additional, customPriority, finalTag, consistent, params);
				pipelineJobs.add(newJob);

				if(settings != null) {
					settings.setRegisteredJobs(settings.getRegisteredJobs() + 1);
				}

				sessionRegisteredJobs++;
			}

	    	return 1;
    	}
    }

	@RequestMapping("/unregister_job")
	public int unregisterJob(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="job")String job, @RequestParam(value="tag", defaultValue=DEFAULT_TAG)String tag, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override, @RequestParam(value="uuid", defaultValue="")String uuid) {
		synchronized (mutex) {

			if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);

			PriorityQueue<JobInfo> pipelineJobs = null;

			String finalTag = tag_override.isEmpty() ? tag : tag_override;

			if(!tagMap.containsKey(finalTag)) {
				Logger.logMessage("The tag '" + finalTag + "' does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}
			
			pipelineJobs = tagMap.get(finalTag);

			int oldSize = pipelineJobs.size();

			for(JobInfo currentJob : pipelineJobs) {
				if(!uuid.isEmpty() && currentJob.getUUID().equals(uuid)) {
					pipelineJobs.remove(currentJob);
					break;
				}
				else if(currentJob.getJobData().equals(job)) {
					pipelineJobs.remove(currentJob);
					break;
				}
			}

			if(oldSize == pipelineJobs.size()) {
				Logger.logMessage("Job " + job + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}
			
			PipelineSettings settings = pipelineSettings.get(pipeline_id);
	    	if(settings != null) {
		    	settings.setRegisteredJobs(settings.getRegisteredJobs() - 1);
	    	}

			Logger.logMessage("Removing the job " + job + " with tag '" + finalTag + "' from pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;

		}
	}

	@RequestMapping("/unregister_jobs")
	public int unregisterJobs(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="tag", defaultValue=DEFAULT_TAG)String tag, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override) {
		synchronized (mutex) {

			if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);

			PriorityQueue<JobInfo> pipelineJobs = null;

			String finalTag = tag_override.isEmpty() ? tag : tag_override;

			if(!tagMap.containsKey(finalTag)) {
				Logger.logMessage("The tag '" + finalTag + "' does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			pipelineJobs = tagMap.get(finalTag);

			ArrayList<String> job_names = new ArrayList<>();
			ArrayList<String> job_uuids = new ArrayList<>();

			for(JobInfo currentJob : pipelineJobs) {
				job_names.add(currentJob.getJobData());
				job_uuids.add(currentJob.getUUID());
			}

			for(int i = 0; i < job_names.size(); i++) {
				unregisterJob(pipeline_id, job_names.get(i), finalTag, DEFAULT_TAG, job_uuids.get(i));
			}

			Logger.logMessage("Removing all the jobs with tag '" + finalTag + "' from pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;

		}
	}

	@RequestMapping("/unregister_tag")
	public int unregisterTag(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="tag", defaultValue=DEFAULT_TAG)String tag, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override) {
		synchronized (mutex) {

			if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);

			String finalTag = tag_override.isEmpty() ? tag : tag_override;

			if(!tagMap.containsKey(finalTag)) {
				Logger.logMessage("The tag '" + finalTag + "' does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			PriorityQueue<JobInfo> pipelineJobs = tagMap.get(finalTag);
			long jobCount = pipelineJobs.size();
			pipelineJobs.clear();
			tagMap.remove(finalTag);
			
			PipelineSettings settings = pipelineSettings.get(pipeline_id);
	    	if(settings != null) {
		    	settings.setRegisteredJobs(settings.getRegisteredJobs() - jobCount);
	    	}
			
			Logger.logMessage("Removing the tag '" + finalTag + "' from pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;

		}
	}

	@RequestMapping("/unregister_tags")
	public int unregisterTags(@RequestParam(value="pipeline_id") String pipeline_id) {
		synchronized (mutex) {

			if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Map<String,  PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);
			ArrayList<String> tags = new ArrayList<>();

			for (Map.Entry<String, PriorityQueue<JobInfo>> entry : tagMap.entrySet()) {
				tags.add(entry.getKey());
			}

			for(String tag : tags) {
				unregisterTag(pipeline_id, tag, DEFAULT_TAG);
			}

			Logger.logMessage("Removing all the tags from pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;

		}
	}

	@RequestMapping("/register_tag")
	public int registerTag(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="tag", defaultValue=DEFAULT_TAG)String tag, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override) {
		synchronized (mutex) {

			if(!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);

			String finalTag = tag_override.isEmpty() ? tag : tag_override;

			if(tagMap.containsKey(finalTag)) {
				Logger.logMessage("The tag '" + finalTag + "' already exists", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}
			
			PriorityQueue<JobInfo> pipelineJobs = new PriorityQueue<>();
			tagMap.put(finalTag, pipelineJobs);
			Logger.logMessage("Creating new tag '" + finalTag + "'", Logger.INFO, JobPipelineScheduler.class.getName());
			sessionRegisteredTags++;

			return 1;

		}
	}

	@RequestMapping("/get_job_count")
    public long getJobCount(@RequestParam(value="pipeline_id") String pipeline_id) {
    	
    	synchronized (mutex) {

			return countJobs(pipeline_id);
    		
    	}
    	
    }

	@RequestMapping("/get_registered_job_count")
	public long getRegisteredJobCount(@RequestParam(value="pipeline_id") String pipeline_id) {

		synchronized (mutex) {

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			return settings.getRegisteredJobs();

		}

	}

    @RequestMapping("/get_available_jobs")
    public ArrayList<JobInfo> getAvailableJobs(@RequestParam(value="pipeline_id") String pipeline_id) {
    	
    	synchronized (mutex) {
    		
    		Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);
    		
    		if(tagMap == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return null;
	    	}
    		else if(tagMap.isEmpty()) {
    			return null;
    		}
    		
    		ArrayList<JobInfo> list = new ArrayList<>();
  
    		for (Map.Entry<String, PriorityQueue<JobInfo>> entry : tagMap.entrySet()) {
    			Iterator<JobInfo> it = entry.getValue().iterator();
    			while(it.hasNext()) {
    				list.add(it.next());
    			}
    		}
    		
    		return list;
    	}
    	
    }
    
    @RequestMapping("/set_variable")
    public int setVariable(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="variable_name") String variable_name, @RequestParam(value="variable_value") String variable_value) {
    	
    	synchronized (mutex) {
    		
    		PipelineSettings settings = pipelineSettings.get(pipeline_id);
    		
    		if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return -1;
	    	}

			Logger.logMessage("Setting variable \"" + variable_name + "\" with value of \"" + variable_value + "\" for pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
    		settings.setValueForVariable(variable_name, variable_value);

			sessionRegisteredVariables++;
    		
    		return 1;
    	}
    	
    }
    
    @RequestMapping("/get_variable")
    public String getVariable(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="variable_name") String variable_name) {
    	
    	synchronized (mutex) {
    		
    		PipelineSettings settings = pipelineSettings.get(pipeline_id);
    		
    		if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return "";
	    	}
    		
    		return settings.getValueForVariable(variable_name);
    	}
    	
    }

	@RequestMapping("/delete_variable")
	public int deleteVariable(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="variable_name") String variable_name) {

		synchronized (mutex) {

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			Logger.logMessage("Removing variable \"" + variable_name + "\" for pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
			settings.removeVariable(variable_name);

			return 1;
		}

	}

	@RequestMapping("/delete_variables")
	public int deleteVariables(@RequestParam(value="pipeline_id") String pipeline_id) {

		synchronized (mutex) {

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			ArrayList<String> variable_names = new ArrayList<>();

			for(Map.Entry<String, String> entrySet : settings.getVariablesMap().entrySet()) {
				variable_names.add(entrySet.getKey());
			}

			for(String variable_name : variable_names) {
				deleteVariable(pipeline_id, variable_name);
			}

			Logger.logMessage("Removing all variables for pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;
		}

	}

    @RequestMapping("/get_uptime")
    public long getUpTime(@RequestParam(value="pipeline_id") String pipeline_id) {
    	
    	synchronized (mutex) {
    		
    		PipelineSettings settings = pipelineSettings.get(pipeline_id);
    		
    		if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return -1;
	    	}
    		
    		return System.currentTimeMillis() - settings.getCreationTime();
    	}
    	
    }
    
    @RequestMapping("/get_job")
    public JobInfo getJob(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="tags", defaultValue=DEFAULT_TAG)String tags, @RequestParam(value="clean_up", defaultValue="false")boolean clean_up, @RequestParam(value="global_priority", defaultValue="false")boolean global_priority, @RequestParam(value="tag_override", defaultValue=DEFAULT_TAG)String tag_override) {
    	
    	ArrayList<String> tokens = parseTokens(tags, tag_override);
    	
    	synchronized (mutex) {
    		
    		Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);
    		
    		if(tagMap == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
	    		return new JobInfo();
	    	}
    		else if(tagMap.isEmpty()) {
    			if(clean_up) {
    				unregisterPipeline(pipeline_id); //clean-up
				}
    			return new JobInfo();
    		}
    		
    		JobInfo job = global_priority ? findJobWithMaxGlobalPriority(tokens, tagMap, pipeline_id, clean_up) : findJobWithMaxLocalPriority(tokens, tagMap, pipeline_id, clean_up);

    		if(clean_up && tagMap.isEmpty()) {
    			unregisterPipeline(pipeline_id); //clean-up
    		}

    		if(job == null) {
				Logger.logMessage("No available job was found for pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
	    		return new JobInfo();
    		}

			job.setServedTimeStamp(LocalDateTime.now().toString());
    		job.setStartTime(System.currentTimeMillis());

			ArrayList<JobStatus> servedJobs = servedJobsPerPipeline.get(pipeline_id);

    		if(servedJobs != null) {
				servedJobs.add(new JobStatus(job));
			}

    		return job;
    	}
    }
    
    @RequestMapping("/update_time")
    public int updateTime(@RequestParam(value="job_name") String job_name, @RequestParam(value="time") double time, @RequestParam(value="pipeline_id", defaultValue="")String pipeline_id, @RequestParam(value="job_uuid", defaultValue="")String job_uuid) {
    	
    	synchronized (mutex) {

			SQLiteOperations.updateAverageTime(job_name, time);

			if(!pipeline_id.isEmpty()) {
				PipelineSettings settings = pipelineSettings.get(pipeline_id);

				if(settings != null) {
					settings.setSuccessfulJobs(settings.getSuccessfulJobs() + 1);
				}

				ArrayList<JobStatus> servedJobs = servedJobsPerPipeline.get(pipeline_id);
				if(servedJobs != null && !job_uuid.isEmpty()) {
					JobStatus jobStatus = null;

					for(JobStatus curr : servedJobs) {
						if(curr.getJobInfo().getUUID().equals(job_uuid)) {
							jobStatus = curr;
							break;
						}
					}

					if(jobStatus != null && jobStatus.getStatus() == PipelineSettings.UNINITIALIZED) {
						jobStatus.setStatus(PipelineSettings.OK);
						jobStatus.getJobInfo().setEndTime(System.currentTimeMillis());
						jobStatus.setRetries(jobStatus.getRetries() + 1);
						jobStatus.setSuccess(jobStatus.getSuccess() + 1);
						jobStatus.getJobInfo().setFinalizedTimeStamp(LocalDateTime.now().toString());
						setStatus(pipeline_id, PipelineSettings.OK);
					}
				}
			}

			sessionSucceedJobs++;

    		return 1;

    	}
    	
    }
    
    @RequestMapping("/update_failure")
    public int updateFailedJob(@RequestParam(value="job_name") String job_name, @RequestParam(value="pipeline_id", defaultValue="")String pipeline_id, @RequestParam(value="job_uuid", defaultValue="")String job_uuid, @RequestParam(value="max_retries", defaultValue="1")long max_retries, @RequestParam(value="failed", defaultValue="false")boolean failed) {
    	
    	synchronized (mutex) {

			SQLiteOperations.updateFailure(job_name);

			if(!pipeline_id.isEmpty()) {
				PipelineSettings settings = pipelineSettings.get(pipeline_id);

				if(settings != null) {
					settings.setFailedJobs(settings.getFailedJobs() + 1);
				}
			}

			ArrayList<JobStatus> servedJobs = servedJobsPerPipeline.get(pipeline_id);
			if(servedJobs != null && !job_uuid.isEmpty()) {
				JobStatus jobStatus = null;

				for(JobStatus curr : servedJobs) {
					if(curr.getJobInfo().getUUID().equals(job_uuid)) {
						jobStatus = curr;
						break;
					}
				}

				if(jobStatus != null && jobStatus.getStatus() == PipelineSettings.UNINITIALIZED) {
					jobStatus.setRetries(jobStatus.getRetries() + 1);
					jobStatus.setFailure(jobStatus.getFailure() + 1);
					if (failed || jobStatus.getJobInfo().isConsistent() || jobStatus.getRetries() == max_retries) {
						jobStatus.setStatus(PipelineSettings.FAILED);
						jobStatus.getJobInfo().setEndTime(System.currentTimeMillis());
						jobStatus.getJobInfo().setFinalizedTimeStamp(LocalDateTime.now().toString());
						setStatus(pipeline_id, PipelineSettings.FAILED);
					}
				}
			}

			sessionFailedJobs++;
    		
    		return 1;

    	}
    	
    }

	@RequestMapping("/set_status")
	public int setStatus(@RequestParam(value="pipeline_id") String pipeline_id, @RequestParam(value="status") int status ) {

		synchronized (mutex) {

			if(status != PipelineSettings.FAILED && status != PipelineSettings.OK) {
				Logger.logMessage( "Unknown status provided for pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
				return -1;
			}

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			if(status == PipelineSettings.OK && settings.getStatus() == PipelineSettings.UNINITIALIZED) {
				settings.setStatus(PipelineSettings.OK);
			}
			else if(status == PipelineSettings.FAILED && (settings.getStatus() == PipelineSettings.UNINITIALIZED || settings.getStatus() == PipelineSettings.OK)) {
				settings.setStatus(PipelineSettings.FAILED);
				Logger.logMessage("Pipeline with id " + pipeline_id + " has failed", Logger.INFO, JobPipelineScheduler.class.getName());
			}

			return 1;

		}

	}

	@RequestMapping("/reset_status")
	public int resetStatus(@RequestParam(value="pipeline_id") String pipeline_id) {

		synchronized (mutex) {

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			settings.setStatus(PipelineSettings.UNINITIALIZED);

			return 1;

		}

	}

	@RequestMapping("/get_status")
	public int getStatus(@RequestParam(value="pipeline_id") String pipeline_id) {

		synchronized (mutex) {
			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			if(settings == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			return settings.getStatus();
		}

	}
    
    private ArrayList<String> parseTokens(String tags, String tag_override) {

    	ArrayList<String> tokens = new ArrayList<String>();

    	if(!tag_override.isEmpty()) {
			tokens.add(tag_override);
		}
    	
    	if(!tags.isEmpty()) {
    		StringTokenizer tokenizer = new StringTokenizer(tags, ",");
	    	while (tokenizer.hasMoreTokens()) {
	    		tokens.add(tokenizer.nextToken());   		 
	    	}
    	}
    	
    	tokens.add(DEFAULT_TAG);
    	
    	return tokens;
    	
    }

	private Map<String, String> parseParameters(String parameters) {

		Map<String, String> params = new TreeMap<>();

		StringTokenizer tokenizer = new StringTokenizer(parameters, ",");
		while (tokenizer.hasMoreTokens()) {

			StringTokenizer innerTokenizer = new StringTokenizer(tokenizer.nextToken(), ":");
			if(innerTokenizer.countTokens() == 2) {
				String key = innerTokenizer.nextToken();
				String value = innerTokenizer.nextToken();
				params.put(key, value);
			}
		}

		return params;

	}
    
    private JobInfo findJobWithMaxLocalPriority(ArrayList<String> tokens, Map<String, PriorityQueue<JobInfo>> tagMap, String pipeline_id, boolean clean_up) {

    	for(String token : tokens) {
			if(!tagMap.containsKey(token)) {
				continue;
			}
			
			PriorityQueue<JobInfo> pipelineJobs = tagMap.get(token);
				
			if(pipelineJobs == null || pipelineJobs.isEmpty()) {
				continue;
			}
				
			JobInfo job = pipelineJobs.poll();

			Logger.logMessage("Dequeuing job " + job.getJobData() + " with priority " + job.getPriority() + " and tag '" + job.getTag() + "' for pipeline " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
				
			PipelineSettings settings = pipelineSettings.get(pipeline_id);
				
			if(settings != null) {
				long availableJobs = getJobCount(pipeline_id);
				Logger.logMessage("Pipeline: " + pipeline_id + " Remaining jobs: " + availableJobs + "/" + settings.getRegisteredJobs() + " Completed: " + String.format("%6.2f", (((double)(settings.getRegisteredJobs() - availableJobs)) / settings.getRegisteredJobs()) * 100) + "%", Logger.INFO, JobPipelineScheduler.class.getName());
			}
				
			if(clean_up && pipelineJobs.isEmpty()) {
				tagMap.remove(token); //clean-up
				Logger.logMessage("Removing the tag '" + token + "'", Logger.INFO, JobPipelineScheduler.class.getName());
			}

			return job;
			
		}  
    	
    	return null;
    	
    }
    
    private JobInfo findJobWithMaxGlobalPriority(ArrayList<String> tokens, Map<String, PriorityQueue<JobInfo>> tagMap, String pipeline_id, boolean clean_up) {
    	
    	long priority = Long.MIN_VALUE;
		String matched_token = "";
		
		for(String token : tokens) {
			if(!tagMap.containsKey(token)) {
				continue;
			}
			
			PriorityQueue<JobInfo> pipelineJobs = tagMap.get(token);
			
			if(pipelineJobs != null && !pipelineJobs.isEmpty()) {
				JobInfo job = pipelineJobs.peek();
				
				if(job.getPriority() > priority) { //find the job with the highest priority of all the available tags
					priority = job.getPriority();
					matched_token = token;
				}
			}
		}   		

		if(priority != Integer.MIN_VALUE) {
			PriorityQueue<JobInfo> pipelineJobs = tagMap.get(matched_token);
			
			if(pipelineJobs == null || pipelineJobs.isEmpty()) {
				return null;
			}
			
			JobInfo job = pipelineJobs.poll();

			Logger.logMessage("Dequeuing job " + job.getJobData() + " with priority " + job.getPriority() + " and tag '" + job.getTag() + "' for pipeline " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());
			
			PipelineSettings settings = pipelineSettings.get(pipeline_id);
			
			if(settings != null) {
				long availableJobs = getJobCount(pipeline_id);
				Logger.logMessage("Pipeline: " + pipeline_id + " Remaining jobs: " + availableJobs + "/" + settings.getRegisteredJobs() + " Completed: " + String.format("%6.2f", (((double)(settings.getRegisteredJobs() - availableJobs)) / settings.getRegisteredJobs()) * 100) + "%", Logger.INFO, JobPipelineScheduler.class.getName());
			}
			
			if(clean_up && pipelineJobs.isEmpty()) {
				tagMap.remove(matched_token); //clean-up
				Logger.logMessage("Removing the tag '" + matched_token + "'", Logger.INFO, JobPipelineScheduler.class.getName());
			}
			
			return job;
		}
		
		return null;
		
    }
    
    public static void cleanUp() {
    	
    	synchronized (mutex) {

			Logger.logMessage("Performing scheduled pipeline clean-up", Logger.INFO, JobPipelineScheduler.class.getName());

			ArrayList<String> pipelinesToBeRemoved = new ArrayList<>();

    		for (Map.Entry<String, PipelineSettings> entrySet : pipelineSettings.entrySet()) {
    			String key = entrySet.getKey();
    			PipelineSettings settings = entrySet.getValue();
    			if(settings == null) {
    				continue;
    			}
    			
    			long upTime = System.currentTimeMillis() - settings.getCreationTime();

    			boolean added = false;
    			
    			if (upTime > STALE_PIPELINE_TIMEOUT) {
					Logger.logMessage("Pipeline with id " + key + " was found to be stale", Logger.INFO, JobPipelineScheduler.class.getName());
					pipelinesToBeRemoved.add(key);
					added = true;
    			}

    			if(!added) {
					long jobCount = countJobs(key);

					if (jobCount == 0 && upTime > FINISHED_PIPELINE_TIMEOUT) {
						Logger.logMessage("Pipeline with id " + key + " was found to be completed", Logger.INFO, JobPipelineScheduler.class.getName());
						pipelinesToBeRemoved.add(key);
					}
				}

    		}

    		for(String pipeline_id : pipelinesToBeRemoved) {
				removePipeline(pipeline_id);
			}
    		
    	}
    	
    }

	private static long countJobs(String pipeline_id) {

		synchronized (mutex) {

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);

			if (tagMap == null) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return 0;
			} else if (tagMap.isEmpty()) {
				return 0;
			}

			long count = 0;

			for (Map.Entry<String, PriorityQueue<JobInfo>> entry : tagMap.entrySet()) {
				count += entry.getValue().size();
			}

			return count;
		}

	}

	private static int removePipeline(String pipeline_id) {

		synchronized (mutex) {
			if (!pipelineMap.containsKey(pipeline_id)) {
				Logger.logMessage("The pipeline id " + pipeline_id + " does not exist", Logger.WARNING, JobPipelineScheduler.class.getName());
				return -1;
			}

			PipelineSettings settings = pipelineSettings.get(pipeline_id);

			long jobCount = countJobs(pipeline_id);

			//Not all jobs were executed so if the status was not set to failed, is should be set to undetermined
			if(jobCount > 0 && settings != null && settings.getStatus() == PipelineSettings.OK) {
				settings.setStatus(PipelineSettings.UNINITIALIZED);
			}

			Map<String, PriorityQueue<JobInfo>> tagMap = pipelineMap.get(pipeline_id);
			for (Map.Entry<String, PriorityQueue<JobInfo>> entry : tagMap.entrySet()) {
				entry.getValue().clear();
			}
			tagMap.clear();

			ArrayList<JobStatus> servedJobs = servedJobsPerPipeline.get(pipeline_id);

			if(servedJobs != null) {
				servedJobs.clear();
			}

			long count = SQLiteOperations.updatePipelineStatistic("completed_pipelines");
			sessionCompletedPipelines++;

			if(settings != null) {
				String type = "";

				switch (settings.getStatus())
				{
					case PipelineSettings.OK:
						type = "succeed_pipelines";
						sessionSucceedPipelines++;
						break;
					case PipelineSettings.FAILED:
						type = "failed_pipelines";
						sessionFailedPipelines++;
						break;
					case PipelineSettings.UNINITIALIZED:
					default:
						type = "undetermined_pipelines";
						sessionUndeterminedPipelines++;
						break;
				}

				SQLiteOperations.updatePipelineStatistic(type);
				SQLiteOperations.updatePipelineAverage("average_pipeline_jobs", settings.getRegisteredJobs(), count);
				SQLiteOperations.updatePipelineAverage("average_pipeline_completion_time", System.currentTimeMillis() - settings.getCreationTime(), count);

				double stability = settings.getStability();
				if(stability != -1) {
					SQLiteOperations.updatePipelineAverage("average_pipeline_jobs_stability", stability, count);
				}
			}

			pipelineMap.remove(pipeline_id);
			pipelineSettings.remove(pipeline_id);
			servedJobsPerPipeline.remove(pipeline_id);

			Logger.logMessage("Removing the pipeline with id " + pipeline_id, Logger.INFO, JobPipelineScheduler.class.getName());

			return 1;
		}

	}

}
