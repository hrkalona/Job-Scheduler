package job_scheduler.core;

import java.util.TimerTask;

public class CleanUpTask extends TimerTask {

	public CleanUpTask() {
		super();
	}
	
	@Override
    public void run() {
		JobPipelineScheduler.cleanUp();
    }
}
