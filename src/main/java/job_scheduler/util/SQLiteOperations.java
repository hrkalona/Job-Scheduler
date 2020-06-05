package job_scheduler.util;

import job_scheduler.model.PipelineStatisticsModel;
import job_scheduler.model.StatisticsModel;

import java.sql.*;
import java.util.ArrayList;

public class SQLiteOperations {
    private static final String DB_NAME = "scheduler.db";
    private static final int MAX_AVERAGE_SAMPLES = 20;

    public static void createDatabase() {

        // SQL statements for creating tables
        String sql = "CREATE TABLE IF NOT EXISTS jobs (\n"
                + "	id integer PRIMARY KEY AUTOINCREMENT,\n"
                + "	name text NOT NULL,\n"
                + "	average_time real,\n"
                + "	success_count integer,\n"
                + "	failure_count integer\n"
                + ");";

        String sql2 = "CREATE TABLE IF NOT EXISTS info (\n"
                + "	key text PRIMARY KEY,\n"
                + "	value text\n"
                + ");";
        try {
            Connection conn = connect();
            Statement stmt = conn.createStatement();
            // create a new table
            stmt.execute(sql);
            stmt.execute(sql2);
            conn.close();
        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }
    }

    public static Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:" + DB_NAME;
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }
        return conn;
    }

    public static void updateFailure(String name) {

        String sql = "SELECT name, failure_count "
                + "FROM jobs WHERE name = ?";

        //DONT CHANGE THIS LOG, its parsed from LogAnalyzer
        Logger.logMessage("Job " + name + " has failed", Logger.INFO, SQLiteOperations.class.getName());

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            // set the value
            pstmt.setString(1, name);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) { //update
                long failure_count = rs.getLong("failure_count");

                sql = "UPDATE jobs SET failure_count = ? "
                        + "WHERE name = ?";

                failure_count++;

                PreparedStatement pstmt2 = conn.prepareStatement(sql);

                pstmt2.setDouble(1, failure_count);
                pstmt2.setString(2, name);

                pstmt2.executeUpdate();

                Logger.logMessage("Updating job " + name, Logger.INFO, SQLiteOperations.class.getName());
            }
            else { //insert
                sql = "INSERT INTO jobs(name,average_time,success_count,failure_count) VALUES(?,?,?,?)";

                PreparedStatement pstmt2 = conn.prepareStatement(sql);
                pstmt2.setString(1, name);
                pstmt2.setDouble(2, 0);
                pstmt2.setLong(3, 0);
                pstmt2.setLong(4, 1);
                pstmt2.executeUpdate();

                Logger.logMessage("Inserted new row for job " + name, Logger.INFO, SQLiteOperations.class.getName());
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }
    }

    public static long getTimeForJob(String name) {

        double average_time = -1;

        String sql = "SELECT name, average_time "
                + "FROM jobs WHERE name = ?";

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) { //update
                average_time = rs.getDouble("average_time");
                Logger.logMessage("Getting average time for job " + name, Logger.INFO, SQLiteOperations.class.getName());
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }

        return (long)average_time;
    }

    public static void updateAverageTime(String name, double currentTime) {

        String sql = "SELECT name, average_time, success_count "
                + "FROM jobs WHERE name = ?";

        //DONT CHANGE THIS LOG, its parsed from LogAnalyzer
        Logger.logMessage("Job " + name + " has succeeded with execution time of " + currentTime, Logger.INFO, SQLiteOperations.class.getName());

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setString(1, name);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) { //update

                long success_count = rs.getLong("success_count");
                double average_time = rs.getDouble("average_time");

                sql = "UPDATE jobs SET average_time = ? , "
                        + "success_count = ? "
                        + "WHERE name = ?";

                success_count++;

                if(success_count == 1) {
                    average_time = currentTime;
                }
                else {
                    double samples = success_count < MAX_AVERAGE_SAMPLES ? success_count : MAX_AVERAGE_SAMPLES;
                    average_time = average_time * (samples - 1) / samples + currentTime * (1 / samples);
                }

                PreparedStatement pstmt2 = conn.prepareStatement(sql);

                pstmt2.setDouble(1, average_time);
                pstmt2.setLong(2, success_count);
                pstmt2.setString(3, name);

                pstmt2.executeUpdate();

                Logger.logMessage("Updating job " + name, Logger.INFO, SQLiteOperations.class.getName());
            }
            else { //insert
                sql = "INSERT INTO jobs(name,average_time,success_count,failure_count) VALUES(?,?,?,?)";

                PreparedStatement pstmt2 = conn.prepareStatement(sql);
                pstmt2.setString(1, name);
                pstmt2.setDouble(2, currentTime);
                pstmt2.setLong(3, 1);
                pstmt2.setLong(4, 0);
                pstmt2.executeUpdate();

                Logger.logMessage("Inserted new row for job " + name, Logger.INFO, SQLiteOperations.class.getName());
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }
    }

    public static StatisticsModel getStatistics(String jobName) {

        String sql = "SELECT average_time, success_count, failure_count "
                + "FROM jobs WHERE name = ?";

        StatisticsModel stats = new StatisticsModel();

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setString(1, jobName);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) {
                stats.setAverage_time(rs.getDouble("average_time"));
                stats.setSuccess_count(rs.getLong("success_count"));
                stats.setFail_count(rs.getLong("failure_count"));
                stats.setJobName(jobName);
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }

        return stats;

    }

    public static ArrayList<StatisticsModel> getAllStatistics() {

        String sql = "SELECT name, average_time, success_count, failure_count "
                + "FROM jobs";

        ArrayList<StatisticsModel> allStats = new ArrayList<>();

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery();

            while(rs.next()) {
                StatisticsModel stats = new StatisticsModel();
                stats.setAverage_time(rs.getDouble("average_time"));
                stats.setSuccess_count(rs.getLong("success_count"));
                stats.setFail_count(rs.getLong("failure_count"));
                stats.setJobName(rs.getString("name"));
                allStats.add(stats);
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }

        return allStats;

    }

    public static long updatePipelineStatistic(String type) {

        String sql = "SELECT value "
                + "FROM info WHERE key = ?";

        long count = 0;

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            // set the value
            pstmt.setString(1, type);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) { //update
                count = Long.parseLong(rs.getString("value"));

                sql = "UPDATE info SET value = ? "
                        + "WHERE key = ?";

                count++;

                PreparedStatement pstmt2 = conn.prepareStatement(sql);

                pstmt2.setString(1, "" + count);
                pstmt2.setString(2, type);

                pstmt2.executeUpdate();
            }
            else { //insert
                sql = "INSERT INTO info(key,value) VALUES(?,?)";

                count = 1;

                PreparedStatement pstmt2 = conn.prepareStatement(sql);
                pstmt2.setString(1, type);
                pstmt2.setString(2, "" + count);
                pstmt2.executeUpdate();
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }

        return count;
    }

    public static void updatePipelineAverage(String type, double value, long count) {

        String sql = "SELECT value "
                + "FROM info WHERE key = ?";

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            // set the value
            pstmt.setString(1, type);

            ResultSet rs  = pstmt.executeQuery();

            if(rs.next()) { //update
                double db_value = Double.parseDouble(rs.getString("value"));

                sql = "UPDATE info SET value = ? "
                        + "WHERE key = ?";

                if(count == 1) {
                    db_value = value;
                }
                else {
                    double samples = count < MAX_AVERAGE_SAMPLES ? count : MAX_AVERAGE_SAMPLES;
                    db_value = db_value * (samples - 1) / samples + value * (1 / samples);
                }

                PreparedStatement pstmt2 = conn.prepareStatement(sql);

                pstmt2.setString(1, "" + db_value);
                pstmt2.setString(2, type);

                pstmt2.executeUpdate();
            }
            else { //insert
                sql = "INSERT INTO info(key,value) VALUES(?,?)";

                PreparedStatement pstmt2 = conn.prepareStatement(sql);
                pstmt2.setString(1, type);
                pstmt2.setString(2, "" + value);
                pstmt2.executeUpdate();
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }
    }

    public static PipelineStatisticsModel getPipelineStatistics(PipelineStatisticsModel obj) {

        String sql = "SELECT key, value "
                + "FROM info";

        try {

            Connection conn = connect();
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            ResultSet rs  = pstmt.executeQuery();

            while(rs.next()) {
                if(rs.getString("key").equals("succeed_pipelines")) {
                    obj.setSucceed_pipelines(Long.parseLong(rs.getString("value")));
                }
                else if(rs.getString("key").equals("failed_pipelines")) {
                    obj.setFailed_pipelines(Long.parseLong(rs.getString("value")));
                }
                else if(rs.getString("key").equals("undetermined_pipelines")) {
                    obj.setUndetermined_pipelines(Long.parseLong(rs.getString("value")));
                }
                else if(rs.getString("key").equals("registered_pipelines")) {
                    obj.setRegistered_pipelines(Long.parseLong(rs.getString("value")));
                }
                else if(rs.getString("key").equals("completed_pipelines")) {
                    obj.setCompleted_pipelines(Long.parseLong(rs.getString("value")));
                }
                else if(rs.getString("key").equals("average_pipeline_completion_time")) {
                    obj.setAverage_pipeline_completion_time(Double.parseDouble(rs.getString("value")));
                }
                else if(rs.getString("key").equals("average_pipeline_jobs")) {
                    obj.setAverage_pipeline_jobs(Double.parseDouble(rs.getString("value")));
                }
                else if(rs.getString("key").equals("average_pipeline_jobs_stability")) {
                    obj.setAverage_pipeline_jobs_stability(Double.parseDouble(rs.getString("value")));
                }
            }

            conn.close();

        } catch (SQLException e) {
            Logger.logMessage(e.getMessage(), Logger.ERROR, SQLiteOperations.class.getName());
        }

        return obj;

    }
 }
