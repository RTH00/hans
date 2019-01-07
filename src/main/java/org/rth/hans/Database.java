package org.rth.hans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Database implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private final Connection connection;


    private final PreparedStatement addJobExecution;
    private final PreparedStatement getAllExecutions;
    private final PreparedStatement applyRetention;
    private final PreparedStatement updateJobStartExecution;
    private final PreparedStatement updateJobEndExecution;
    private final PreparedStatement resetRunningExecution;
    private final PreparedStatement nextAvailableExecution;

    private final PreparedStatement lastPartitionPerJob;

    private final PreparedStatement addConfiguration;
    private final PreparedStatement getConfiguration;

    /*** job & dependencies ***/
    private final PreparedStatement addJob;
    private final PreparedStatement addJobDependency;
    private final PreparedStatement addJobCommand;
    private final PreparedStatement[] cleanJobTables;
    private final PreparedStatement getJob;
    private final PreparedStatement incrementJobRunningInstances;
    private final PreparedStatement decrementJobRunningInstances;
    private final PreparedStatement resetJobRunningInstances;
    private final PreparedStatement getJobCommands;

    public Database(final String databasePath) throws SQLException, IOException {
        this.connection = connect(databasePath);
        init(connection);

        addJobExecution = connection.prepareStatement(Utils.readResource("sql/addJobExecution.sql"));
        getAllExecutions = connection.prepareStatement(Utils.readResource("sql/getAllExecutions.sql"));
        applyRetention = connection.prepareStatement(Utils.readResource("sql/applyRetention.sql"));
        updateJobStartExecution = connection.prepareStatement(Utils.readResource("sql/updateJobStartExecution.sql"));
        updateJobEndExecution = connection.prepareStatement(Utils.readResource("sql/updateJobEndExecution.sql"));
        resetRunningExecution = connection.prepareStatement(Utils.readResource("sql/resetRunningExecution.sql"));
        nextAvailableExecution = connection.prepareStatement(Utils.readResource("sql/nextAvailableExecution.sql"));

        addConfiguration = connection.prepareStatement(Utils.readResource("sql/addConfiguration.sql"));
        getConfiguration = connection.prepareStatement(Utils.readResource("sql/getConfiguration.sql"));
        /*** jobs ***/
        addJob = connection.prepareStatement(Utils.readResource("sql/addJob.sql"));
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

        lastPartitionPerJob = connection.prepareStatement(Utils.readResource("sql/lastPartitionPerJob.sql"));
        getJob = connection.prepareStatement(Utils.readResource("sql/getJob.sql"));
        incrementJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/incrementJobRunningInstances.sql"));
        decrementJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/decrementJobRunningInstances.sql"));
        resetJobRunningInstances = connection.prepareStatement(Utils.readResource("sql/resetJobRunningInstances.sql"));
        getJobCommands = connection.prepareStatement(Utils.readResource("sql/getJobCommands.sql"));
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
        connection.createStatement().executeUpdate(Utils.readResource("sql/initExecutionTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/initJobsTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/initJobDependenciesTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/initJobCommandsTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/initJobRunningInstancesTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/initConfigurationTable.sql"));
        connection.createStatement().executeUpdate(Utils.readResource("sql/createIndexExecutions.sql"));
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
    public ArrayList<JobPartition> lastPartitionPerJob() throws SQLException {
        synchronized (lastPartitionPerJob) {
            try(final ResultSet rs = lastPartitionPerJob.executeQuery()) {
                final ArrayList<JobPartition> ret = new ArrayList<>();
                while(rs.next()) {
                    ret.add(new JobPartition(
                            readNullableStringField(rs, 1),
                            Utils.parseSqliteFormat(readNullableStringField(rs, 2)),
                            Utils.parseSqliteFormat(readNullableStringField(rs, 3))
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

    /* use for tests */
    public Execution[] getAllExecutions() throws SQLException {
        final ArrayList<Execution> executions = new ArrayList<>();
        synchronized (getAllExecutions) {
            try (final ResultSet rs = getAllExecutions.executeQuery()) {
                while(rs.next()) {
                    executions.add(new Execution(
                            rs.getString(1),
                            readNullableInstantField(rs, 2),
                            StartedExecution.Status.valueOf(rs.getString(3)),
                            readNullableInstantField(rs, 4),
                            readNullableInstantField(rs, 5),
                            readNullableInstantField(rs, 6)
                    ));
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
    private void addJob(final Job job) throws SQLException {
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
            addJob.executeUpdate();
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
            addJob(job);
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
                            rs.getString(10)
                    );
                } else {
                    return null;
                }
            }
        }
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

}
