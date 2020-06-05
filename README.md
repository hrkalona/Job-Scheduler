# Job Scheduler
 A java spring implementation for registering some tasks/jobs and retrieving them from different workers.
 
 It supports registering a pipeline, which can hold different tags. A job/task can be added under a particular tag.
 Here is the tree-like structure:
 
 pipeline_name
  |_ tag1
  |    |_ job1
  |    |_ job2
  |    |_ job3
  |
  |_ tag2
       |_ job4
       |_ job5
       |_ job6
       
       
The jobs can have priorities assigned to them, so all jobs under one tag will be sorted by priority.

Here is a list of some of the basic API calls:
**register_pipeline** (Registers a pipeline) Parameters: pipeline_id
**unregister_pipeline** (Un-registers a pipeline) Parameters: pipeline_id
**unregister_all** (Un-registers all pipelines)
**register_job** (Registers a job) Parameteters: pipeline_id, job, priority, tag
**unregister_job** (Un-registers a job) Parameteters: pipeline_id, job, tag
**get_job** (Fetches the next prioritized available job) Parameters: pipeline_id, tags (a list of comma separated tags that the caller wants to process)
**get_available_jobs** (Fetches a list of all registered jobs in a pipeline) Parameters: pipeline_id
**set_variable** (Fetches a variable that is stored to a pipeline) Parameters: pipeline_id, variable_name

There are more available API calls for setting up the status of each job or pipeline.

A user interface is also provided for managing and displaying all pipelines and jobs, as well as viewing helpful statistics.
