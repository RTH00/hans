package org.rth.hans.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.rth.hans.ui.User;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class Database implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(Database.class);

    private final Connection connection;


    private final PreparedStatement addJobExecution;
    private final PreparedStatement getAllExecutions;
    private final PreparedStatement getExecutionsBetween;
    private final PreparedStatement applyRetention;
    private final PreparedStatement updateJobStartExecution;
    private final PreparedStatement updateJobEndExecution;
    private final PreparedStatement resetRunningExecution;
    private final PreparedStatement nextAvailableExecution;

    private final PreparedStatement jobStartPartition;

    private final PreparedStatement addConfiguration;
    private final PreparedStatement getConfiguration;

    /*** job & dependencies ***/
    private final PreparedStatement addJob;
    private final PreparedStatement updateJobIsActivated;
    private final PreparedStatement addJobDependency;
    private final PreparedStatement addJobCommand;
    private final PreparedStatement[] cleanJobTables;
    private final PreparedStatement getJob;
    private final PreparedStatement getAllJobs;
    private final PreparedStatement incrementJobRunningInstances;
    private final PreparedStatement decrementJobRunningInstances;
    private final PreparedStatement resetJobRunningInstances;
    private final PreparedStatement getJobCommands;

    /*** users ***/
    private final PreparedStatement getUserIdentification;
    private final PreparedStatement addUser;

    public Database(final String databasePath) throws SQLException, IOException {
        this.connection = connect(databasePath);
        init(connection);

        addJobExecution = connection.prepareStatement(Utils.readResource("sql/addJobExecution.sql"));
        getAllExecutions = connection.prepareStatement(Utils.readResource("sql/getAllExecutions.sql"));
        getExecutionsBetween = connection.prepareStatement(Utils.readResource("sql/getExecutionsBetween.sql"));
        applyRetention = connection.prepareStatement(Utils.readResource("sql/applyRetention.sql"));
        updateJobStartExecution = connection.prepareStatement(Utils.readResource("sql/updateJobStartExecution.sql"));
        updateJobEndExecution = connection.prepareStatement(Utils.readResource("sql/updateJobEndExecution.sql"));
        resetRunningExecution = connection.prepareStatement(Utils.readResource("sql/resetRunningExecution.sql"));
        nextAvailableExecution = connection.prepareStatement(Utils.readResource("sql/nextAvailableExecution.sql"));

        addConfiguration = connection.prepareStatement(Utils.readResource("sql/addConfiguration.sql"));
        getConfiguration = connection.prepareStatement(Utils.readResource("sql/getConfiguration.sql"));
        /*** jobs ***/
        addJob = connection.prepareStatement(Utils.readResource("sql/addJob.sql"));
        updateJobIsActivated = connection.prepareStatement(Utils.readResource("sql/updateJobIsActivated.sql"));
        addJobDependency = connection.prepareStatement(Utils.readResource("sql/addJobDependency.sql"));
        addJobCommand = connection.prepareStatement(Utils.readResource("sql/addJobCommand.sql"));
        cleanJobTables = Arrays.stream(new String[]{
                "DELETE FROM jobs;",
                "DELETE FROM job_dependencies;",
                "DELETE FROM job_commands",
                "DELETE FROM configuration"
        }).map(query -> {
            try {
                return connection.prepareStatement(query);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).toArray(PreparedStatement[]::new);

        jobStartPartition = connection.prepareStatement(Utils.readResource("sql/jobStartPartition.sql"));
        getJob = connection.prepareStatement(Utils.readResource("sql/getJob.sql"));
        getAllJobs = connection.prepareStatement(Utils.readResource("sql/getAllJobs.sql"));
        incrementJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/incrementJobRunningInstances.sql"));
        decrementJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/decrementJobRunningInstances.sql"));
        resetJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/resetJobRunningInstances.sql"));
        getJobCommands = connection.prepareStatement(Utils.readResource("sql/getJobCommands.sql"));
        /*** users ***/
        getUserIdentification = connection.prepareStatement(Utils.readResource("sql/users/getUserIdentification.sql"));
        addUser = connection.prepareStatement(Utils.readResource("sql/users/addUser.sql"));
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    public static Connection connect(final String databasePath) throws SQLException {
        final Connection connection;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            logger.info("Connection to database opened");
            return connection;
        } catch (SQLException e) {
            logger.warn(e.getMessage(), e);
            throw e;
        }
    }

    private static void init(final Connection connection) throws SQLException, IOException {
        for(final String path : new String[]{
                "sql/initExecutionTable.sql",
                "sql/initJobsTable.sql",
                "sql/initJobDependenciesTable.sql",
                "sql/initJobCommandsTable.sql",
                "sql/initJobRunningInstancesTable.sql",
                "sql/initConfigurationTable.sql",
                "sql/createIndexExecutions.sql",
                "sql/users/initUsersTable.sql"
        }) {
            connection.createStatement().executeUpdate(Utils.readResource(path));
        }
    }

    /*** utils ***/

    private static String readNullableStringField(final ResultSet rs, final int fieldIndex) throws SQLException {
        final String ret = rs.getString(fieldIndex);
        if(rs.wasNull()) {
            return null;
        } else {
            return ret;
        }
    }

    private static long readLongField(final ResultSet rs, final int fieldIndex) throws SQLException {
        final long ret = rs.getLong(fieldIndex);
        if(rs.wasNull()) {
            throw new IllegalArgumentException("max_parallelism should not be null");
        } else {
            return ret;
        }
    }

    private static Duration readDurationField(final ResultSet rs, final int fieldIndex) throws SQLException {
        return Duration.parseDuration(readNullableStringField(rs, fieldIndex));
    }


    private static Instant readNullableInstantField(final ResultSet rs, final int fieldIndex) throws SQLException {
        return Utils.parseSqliteFormat(readNullableStringField(rs, fieldIndex));
    }

    /*** partitions ***/
    public ArrayList<JobStartPartition> jobStartPartition() throws SQLException {
        synchronized (jobStartPartition) {
            try(final ResultSet rs = jobStartPartition.executeQuery()) {
                final ArrayList<JobStartPartition> ret = new ArrayList<>();
                while(rs.next()) {
                    ret.add(new JobStartPartition(
                            readNullableStringField(rs, 1),
                            Utils.parseSqliteFormat(readNullableStringField(rs, 2))
                    ));
                }
                return ret;
            }
        }
    }

    /*** configuration ***/
    public void addConfiguration(final String key, final String value) throws SQLException {
        synchronized (addConfiguration) {
            addConfiguration.setString(1, key);
            addConfiguration.setString(2, value);
            addConfiguration.executeUpdate();
        }
    }

    public String getConfiguration(final String key) throws SQLException {
        synchronized (getConfiguration) {
            getConfiguration.setString(1, key);
            try(final ResultSet rs = getConfiguration.executeQuery()) {
                if(rs.next()) {
                    return readNullableStringField(rs, 1);
                } else {
                    return null;
                }
            }
        }
    }

    /*** executions table ***/
    public void applyRetention(final String jobName,
                               final Instant minPartition) throws SQLException {
        synchronized (applyRetention) {
            applyRetention.setString(1, jobName);
            applyRetention.setString(2, Utils.toSqliteFormat(minPartition));
            applyRetention.executeUpdate();
        }
    }

    public void resetRunningExecution() throws SQLException {
        resetRunningExecution.executeUpdate();
    }

    private static Execution parseExecution(final ResultSet rs) throws SQLException {
        return new Execution(
                rs.getString(1),
                readNullableInstantField(rs, 2),
                StartedExecution.Status.valueOf(rs.getString(3)),
                readNullableInstantField(rs, 4),
                readNullableInstantField(rs, 5),
                readNullableInstantField(rs, 6)
        );
    }

    // TODO add tests
    // TODO add indexes
    public ArrayList<Execution> getExecutionsBetween(final String jobName, final Instant partitionStart, final Instant partitionEnd) throws SQLException {
        final ArrayList<Execution> executions = new ArrayList<>();
        getExecutionsBetween.setString(1, jobName);
        getExecutionsBetween.setString(2, Utils.toSqliteFormat(partitionStart));
        getExecutionsBetween.setString(3, Utils.toSqliteFormat(partitionEnd));
        synchronized (getExecutionsBetween) {
            try (final ResultSet rs = getExecutionsBetween.executeQuery()) {
                while(rs.next()) {
                    executions.add(parseExecution(rs));
                }
            }
        }
        return executions;
    }

    /* use for tests */
    public Execution[] getAllExecutions() throws SQLException {
        final ArrayList<Execution> executions = new ArrayList<>();
        synchronized (getAllExecutions) {
            try (final ResultSet rs = getAllExecutions.executeQuery()) {
                while(rs.next()) {
                    executions.add(parseExecution(rs));
                }
            }
        }
        return executions.toArray(new Execution[0]);
    }

    public void addJobExecution(final String jobName,
                                final Instant partition,
                                final Instant nextScheduleTime) throws SQLException {
        synchronized (addJobExecution) {
            addJobExecution.setString(1, jobName);
            addJobExecution.setString(2, Utils.toSqliteFormat(partition));
            addJobExecution.setString(3, Utils.toSqliteFormat(nextScheduleTime));
            addJobExecution.executeUpdate();
        }
    }

    public boolean updateJobEndExecution(final String jobName,
                                         final Instant partition,
                                         final Instant endTime,
                                         final Instant nextScheduleTime,
                                         final StartedExecution.Status status) throws SQLException {
        synchronized (updateJobEndExecution) {
            updateJobEndExecution.setString(1, Utils.toSqliteFormat(endTime));
            updateJobEndExecution.setString(2, Utils.toSqliteFormat(nextScheduleTime));
            updateJobEndExecution.setString(3, status.toString());
            updateJobEndExecution.setString(4, jobName);
            updateJobEndExecution.setString(5, Utils.toSqliteFormat(partition));
            return updateJobEndExecution.executeUpdate() == 1;
        }
    }

    public boolean updateJobStartExecution(final String jobName,
                                           final Instant partition,
                                           final Instant startTime) throws SQLException {
        synchronized (updateJobStartExecution) {
            updateJobStartExecution.setString(1, Utils.toSqliteFormat(startTime));
            updateJobStartExecution.setString(2, jobName);
            updateJobStartExecution.setString(3, Utils.toSqliteFormat(partition));
            return updateJobStartExecution.executeUpdate() == 1;
        }
    }

    public LinkedList<AvailableExecution> nextAvailableExecutions(final Instant now) throws SQLException {
        synchronized (nextAvailableExecution) {
            nextAvailableExecution.setString(1, Utils.toSqliteFormat(now));
            try(final ResultSet rs = nextAvailableExecution.executeQuery()) {
                final LinkedList<AvailableExecution> ret = new LinkedList<>();
                if(rs.next()) {
                    ret.add(new AvailableExecution(rs.getString(1), Utils.parseSqliteFormat(rs.getString(2))));
                }
                return ret;
            }
        }
    }

    /*** jobs & job_* tables ***/
    private void addJob(final Job job, final boolean overrideIsActivated) throws SQLException {
        synchronized (addJob) {
            addJob.setString(1, job.getName());
            addJob.setString(2, Utils.toSqliteFormat(job.getStartPartition()));
            addJob.setString(3, Utils.toSqliteFormat(job.getEndPartition()));
            addJob.setString(4, job.getIncrement().toSQLiteFormat());
            addJob.setLong(5, job.getMaxParallelism());
            addJob.setString(6, job.getFailureBehavior().name());
            addJob.setString(7, job.getRetryDelay().toSQLiteFormat());
            addJob.setString(8, job.getRetention().toSQLiteFormat());
            addJob.setString(9, job.getStdoutPath());
            addJob.setString(10, job.getStderrPath());
            addJob.setBoolean(11, overrideIsActivated);
            addJob.executeUpdate();
        }
    }

    public boolean updateJobIsActivated(final String jobName, final boolean newValue) throws SQLException {
        synchronized (updateJobIsActivated) {
            updateJobIsActivated.setBoolean(1, newValue);
            updateJobIsActivated.setString(2, jobName);
            return updateJobIsActivated.executeUpdate() == 1;
        }
    }

    private void addJobCommand(final String jobName,
                               final long ordering,
                               final String command) throws SQLException {
        synchronized (addJobCommand) {
            addJobCommand.setString(1, jobName);
            addJobCommand.setLong(2, ordering);
            addJobCommand.setString(3, command);
            addJobCommand.executeUpdate();
        }
    }

    private void addJobDependency(final String jobName,
                                  final String dependencyJobName,
                                  final Duration shift) throws SQLException {
        synchronized (addJobDependency) {
            addJobDependency.setString(1, jobName);
            addJobDependency.setString(2, dependencyJobName);
            addJobDependency.setString(3, shift.toSQLiteFormat());
            addJobDependency.executeUpdate();
        }
    }

    // push new job configuration 'atomically'
    public void pushConfiguration(final JobConfiguration jobConfiguration) throws SQLException {
        // push first in import tables
        // then clean & copy in the main tables
        connection.prepareStatement("BEGIN TRANSACTION;").execute();
        final HashMap<String, Job> oldJobs = Utils.toHashMap(getAllJobs(), Job::getName);
        Arrays.stream(cleanJobTables).forEach(preparedStatement -> {
            try {
                preparedStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        // push conf
        addConfiguration(JobConfiguration.MAX_PARALLELISM_KEY, Long.toString(jobConfiguration.getMaxParallelism()));
        addConfiguration(JobConfiguration.NEW_CONFIGURATION_POLLING_TIME_KEY, jobConfiguration.getNewConfigurationPollingTime().toSQLiteFormat());
        addConfiguration(JobConfiguration.SCHEDULER_POLLING_TIME_KEY, jobConfiguration.getSchedulerPollingTime().toSQLiteFormat());
        // push jobs
        for(final Job job: jobConfiguration.getJobs()) {
            final boolean oldJobIsActivated;
            final Job oldJob = oldJobs.get(job.getName());
            if(oldJob == null) {
                oldJobIsActivated = Scheduler.INITIAL_JOB_IS_ACTIVATED;
            } else {
                oldJobIsActivated = oldJob.getIsActivated();
            }
            addJob(job, oldJobIsActivated);
            final String[] commands = job.getCommands();
            for(int i = 0; i < commands.length; i++) {
                addJobCommand(job.getName(), i, commands[i]);
            }
            for(final Dependency dependency: job.getDependencies()) {
                addJobDependency(job.getName(), dependency.getJobName(), dependency.getShift());
            }
        }
        // commit all changes
        connection.prepareStatement("COMMIT;").execute();
    }

    // doesn't return commands/dependencies
    public Job getJob(final String jobName) throws SQLException {
        synchronized (getJob) {
            getJob.setString(1, jobName);
            try(final ResultSet rs = getJob.executeQuery()) {
                if(rs.next()) {
                    return parseJob(rs);
                } else {
                    return null;
                }
            }
        }
    }

    public synchronized ArrayList<Job> getAllJobs() throws SQLException {
        synchronized (getAllJobs) {
            try(final ResultSet rs = getAllJobs.executeQuery()) {
                final ArrayList<Job> jobs = new ArrayList<Job>();
                while(rs.next()) {
                    jobs.add(parseJob(rs));
                }
                return jobs;
            }
        }
    }

    // returns null commands & dependencies
    public static Job parseJob(final ResultSet rs) throws SQLException {
        return new Job(
                rs.getString(1),
                readNullableInstantField(rs, 2),
                readNullableInstantField(rs, 3),
                readDurationField(rs, 4),
                null, // commands
                null, // dependencies
                readLongField(rs, 5),
                Job.FailureBehavior.valueOf(rs.getString(6)),
                readDurationField(rs, 7),
                readDurationField(rs, 8),
                rs.getString(9),
                rs.getString(10),
                rs.getBoolean(11)
        );
    }

    public boolean incrementJobRunningInstances(final String jobName) throws SQLException {
        synchronized (incrementJobRunningInstances) {
            incrementJobRunningInstances.setString(1, jobName);
            incrementJobRunningInstances.setString(2, jobName);
            return incrementJobRunningInstances.executeUpdate() == 1;
        }
    }

    public boolean decrementJobRunningInstances(final String jobName) throws SQLException {
        synchronized (decrementJobRunningInstances) {
            decrementJobRunningInstances.setString(1, jobName);
            return decrementJobRunningInstances.executeUpdate() == 1;
        }
    }

    public void resetJobRunningInstances() throws SQLException {
        resetJobRunningInstances.executeUpdate();
    }

    public ArrayList<String> getJobCommands(final String jobName) throws SQLException {
        synchronized (getJobCommands) {
            getJobCommands.setString(1, jobName);
            try(final ResultSet rs = getJobCommands.executeQuery()) {
                final ArrayList<String> ret = new ArrayList<>();
                while(rs.next()) {
                    ret.add(rs.getString(1));
                }
                return ret;
            }
        }
    }

    public User.Identification getUserIdentification(final String userName) throws SQLException {
        synchronized (getUserIdentification) {
            getUserIdentification.setString(1, userName);
            try (final ResultSet rs = getUserIdentification.executeQuery()) {
                if (rs.next()) {
                    return new User.Identification(
                            rs.getString(1),
                            rs.getString(2)
                    );
                } else {
                    return null;
                }
            }
        }
    }

    public boolean addUser(final String userName,
                           final String hashedPassword,
                           final String salt,
                           final Instant creationDate,
                           final String role) throws SQLException {
        synchronized (addUser) {
            addUser.setString(1, userName);
            addUser.setString(2, hashedPassword);
            addUser.setString(3, salt);
            addUser.setString(4, Utils.toSqliteFormat(creationDate));
            addUser.setString(5, role);
            return addUser.executeUpdate() == 1;
        }
    }

}
