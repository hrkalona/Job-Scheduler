package job_scheduler.util;

import job_scheduler.core.JobPipelineScheduler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Logger {

    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;
    public static final String LOG_FILE = "log.ilf";
    private static PrintWriter out;

    public static void initialize() {
        out = null;
        try {
            FileWriter fw = new FileWriter(LOG_FILE, true);
            BufferedWriter bw = new BufferedWriter(fw);
            out = new PrintWriter(bw);

            logMessage("Initializing the log file", INFO, Logger.class.getName());

        } catch (IOException e) {
            logMessage("Failed to open the file " + LOG_FILE, ERROR, Logger.class.getName());
        }
    }

    public static void logMessage(String message, int type, String className) {
        System.out.println(message);

        if(out != null) {

            Calendar calendar = new GregorianCalendar();
            String dateTime = calendar.get(Calendar.YEAR) + "-" + String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" +String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "  |  " +
                    String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", calendar.get(Calendar.MINUTE)) + ":" + String.format("%02d", calendar.get(Calendar.SECOND)) + "." + String.format("%03d", calendar.get(Calendar.MILLISECOND));

            className = "class " + className;

            switch(type) {
                case INFO:
                    out.println("Info" + "  |  "  + dateTime  + "  |  "  + className  + "  |  " + Thread.currentThread().getId() + "  |  "  +  message);
                    out.flush();
                    break;
                case WARNING:
                    out.println("Warning High" + "  |  "  + dateTime  + "  |  "  + className  + "  |  " + Thread.currentThread().getId() + "  |  "  +  message);
                    out.flush();
                    break;
                case ERROR:
                    out.println("Error" + "  |  "  + dateTime  + "  |  "  + className  + "  |  " + Thread.currentThread().getId() + "  |  "  +  message);
                    out.flush();
                    break;
            }

        }
    }
}
