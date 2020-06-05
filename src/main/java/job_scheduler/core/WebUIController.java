package job_scheduler.core;

import job_scheduler.model.JobsStatisticsModel;
import job_scheduler.model.PipelineStatisticsModel;
import job_scheduler.model.SchedulerModel;
import job_scheduler.model.StatusModel;
import job_scheduler.util.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Timer;


@Controller
public class WebUIController implements WebMvcConfigurer {
    private static final String MAIN_PAGE_HTML = "mainPage";
    private static final String MAIN_PAGE_URL = "dashboard";
    private static final String STATISTICS_PAGE_HTML = "statistics";
    private static final String STATISTICS_PAGE_URL = "statistics";
    private static final String REMOVE_PIPELINE_URL = "remove_pipeline";
    private static final String REMOVE_JOB_URL = "remove_job";
    private static final String REMOVE_JOBS_URL = "remove_jobs";
    private static final String REMOVE_TAG_URL = "remove_tag";
    private static final String REMOVE_TAGS_URL = "remove_tags";
    private static final String REMOVE_ALL_URL = "remove_all";
    private static final String ADD_PIPELINE_URL = "add_pipeline";
    private static final String REMOVE_VARIABLE_URL = "remove_variable";
    private static final String REMOVE_VARIABLES_URL = "remove_variables";
    private static final String ADD_VARIABLE_URL = "add_variable";
    private static final String ADD_TAG_URL = "add_tag";
    private static final String OPERATION_FAILURE_HTML = "operationFailure";
    private static final String ADD_JOB_URL = "add_job";
    private static final String SHUTDOWN_URL = "shutdown";
    private static final String SHUTDOWN_HTML = "shutdown";

    @GetMapping("/" + MAIN_PAGE_URL)
    public String dashboard(SchedulerModel schedulerModel, JobsStatisticsModel jobsStatisticsModel, PipelineStatisticsModel pipelineStatisticsModel, StatusModel statusModel) {

        try {
            synchronized (JobPipelineScheduler.mutex) {
                schedulerModel.initialize();
                jobsStatisticsModel.initialize("", "");
                pipelineStatisticsModel.initialize();
            }
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return MAIN_PAGE_HTML;

    }

    @GetMapping("/" + STATISTICS_PAGE_URL)
    public String statistics(JobsStatisticsModel jobsStatisticsModel, StatusModel statusModel, @RequestParam(value="sort_by", defaultValue=JobsStatisticsModel.SORT_BY_NAME)String sort_by, @RequestParam(value="type", defaultValue=JobsStatisticsModel.TYPE_ASCENDING)String type) {

        try {
            jobsStatisticsModel.initialize(sort_by, type);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return STATISTICS_PAGE_HTML;

    }

    @PostMapping("/" + REMOVE_PIPELINE_URL)
    public String removePipeline(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id) {

        try {
            jobPipelineScheduler.unregisterPipeline(pipeline_id);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + ADD_PIPELINE_URL)
    public String addPipeline(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id) {

        try {
            jobPipelineScheduler.registerPipeline(pipeline_id, false);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + ADD_TAG_URL)
    public String addTag(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam("tag") String tag) {

        try {
            jobPipelineScheduler.registerTag(pipeline_id, tag, JobPipelineScheduler.DEFAULT_TAG);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_JOB_URL)
    public String removeJob(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam(value="job")String job, @RequestParam(value="tag")String tag, @RequestParam(value="uuid")String uuid) {

        try {
            jobPipelineScheduler.unregisterJob(pipeline_id, job, tag, JobPipelineScheduler.DEFAULT_TAG, uuid);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_JOBS_URL)
    public String removeJobs(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam(value="tag")String tag) {

        try {
            jobPipelineScheduler.unregisterJobs(pipeline_id, tag, JobPipelineScheduler.DEFAULT_TAG);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_TAG_URL)
    public String removeTag(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam(value="tag")String tag) {

        try {
            jobPipelineScheduler.unregisterTag(pipeline_id, tag, JobPipelineScheduler.DEFAULT_TAG);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_TAGS_URL)
    public String removeTags(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id) {

        try {
            jobPipelineScheduler.unregisterTags(pipeline_id);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_VARIABLE_URL)
    public String removeVariable(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam(value="variable_name")String variable_name) {

        try {
            jobPipelineScheduler.deleteVariable(pipeline_id, variable_name);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_VARIABLES_URL)
    public String removeVariables(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id) {

        try {
            jobPipelineScheduler.deleteVariables(pipeline_id);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + ADD_VARIABLE_URL)
    public String addVariable(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam(value="variable_name")String variable_name, @RequestParam(value="variable_value")String variable_value) {

        try {
            jobPipelineScheduler.setVariable(pipeline_id, variable_name, variable_value);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + REMOVE_ALL_URL)
    public String removeAll(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel) {

        try {
            jobPipelineScheduler.unregisterAllPipelines();
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + ADD_JOB_URL)
    public String addJob(JobPipelineScheduler jobPipelineScheduler, StatusModel statusModel, @RequestParam("pipeline_id") String pipeline_id, @RequestParam("job") String job, @RequestParam("priority") long priority, @RequestParam("tag") String tag, @RequestParam(value="consistent")boolean consistent, @RequestParam(value="additional")String additional, @RequestParam(value="use_time")boolean use_time, @RequestParam(value="quantity")long quantity, @RequestParam(value="parameters")String parameters) {

        try {
            jobPipelineScheduler.registerJob(pipeline_id, job, priority, tag, consistent, additional, use_time, JobPipelineScheduler.DEFAULT_TAG, quantity, parameters);
        }
        catch(Exception ex)
        {
            statusModel.setLoadReason(ex.getMessage());
            return OPERATION_FAILURE_HTML;
        }

        return "redirect:" + MAIN_PAGE_URL;

    }

    @PostMapping("/" + SHUTDOWN_URL)
    public String shutdown(JobPipelineScheduler jobPipelineScheduler) {

        try {
            jobPipelineScheduler.unregisterAllPipelines();
        }
        catch(Exception ex)
        {
        }

        Logger.logMessage("Shutting down in 2 seconds", Logger.INFO, WebUIController.class.getName());
        Timer timer = new Timer();
        timer.schedule(new ShutdownTask(), 2000);

        return SHUTDOWN_HTML;

    }
}
