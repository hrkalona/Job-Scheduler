package job_scheduler.core;

import java.util.TimerTask;

public class ShutdownTask extends TimerTask {

    public ShutdownTask() {
        super();
    }

    @Override
    public void run() {
        System.exit(0);
    }

}
