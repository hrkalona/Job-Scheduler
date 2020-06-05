package job_scheduler.model;

public class StatusModel {
    public static final int UNITIALIZED = -1;
    public static final int OK = 1;
    public static final int FAILED = 0;
    public static final int WARNING = 2;

    private int loadStatus;
    private String loadReason;

    public StatusModel()
    {
        loadStatus = UNITIALIZED;
    }

    public int getLoadStatus() {
        return loadStatus;
    }

    public void setLoadStatus(int loadStatus) {
        this.loadStatus = loadStatus;
    }

    public String getLoadReason() {
        return loadReason;
    }

    public void setLoadReason(String loadReason) {
        this.loadReason = loadReason;
    }

}
