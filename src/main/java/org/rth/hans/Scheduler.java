package org.rth.hans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Scheduler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private static final String DEFAULT_NEW_CONFIGURATION_POLLING_TIME = "2 " + ChronoUnit.MINUTES.name();

    /*** For job configuration parsing ***/
    private final JobParser jobParser;
    private Instant lastJobParsing;

    private final Database database;

    private PriorityQueue<WaitingPartition> waitingPartitions;
    private LinkedList<AvailableExecution> waitingExecutions;

    /*** For running jobs ***/
    private final LinkedList<StartedExecution> runningJobs;

    public Scheduler(final JobParser jobParser,
                     final Database database) {
        super("Scheduler");
        this.jobParser = jobParser;
        this.lastJobParsing = Instant.ofEpochMilli(0L);
        this.database = database;
        runningJobs = new LinkedList<>();
    }

    public void buildIndexes() throws SQLException {
        logger.info("Rebuilding internal indexes - start");
        this.waitingPartitions = buildNextPartitionIndex();
        this.waitingExecutions = new LinkedList<>();
        while (addNewPartitionsInExecutionLogs());
        logger.info("Rebuilding internal indexes - Done");
    }


    @Override
    public void run() {
        runLoop(Long.MAX_VALUE);
    }

    public boolean runLoop(final long maxIterations) {
        try {
            logger.info("Loading initial configuration");
            if(!importNewJobGraph()) {
                logger.error("Can't load initial configuration");
                return false;
            }
            buildIndexes();

            for (long i = 0; i < maxIterations; i++) {
                final Instant startLoop = Instant.now();

                if (importNewJobGraph()) {
                    logger.info("Successfully import new jobs");
                    buildIndexes();
                }

                while (addNewPartitionsInExecutionLogs()) ;

                updateRunningExecutors();

                final int maxExecutors = Integer.parseInt(database.getConfiguration(JobConfiguration.MAX_PARALLELISM_KEY));
                while (addRunningExecutor(maxExecutors)) ;

                final long waitingTime = computeWaitingTime(
                        startLoop,
                        Duration.parseDuration(database.getConfiguration(JobConfiguration.SCHEDULER_POLLING_TIME_KEY))
                );
                if (waitingTime > 0) {
                    Thread.sleep(waitingTime);
                }
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return true;
    }

    // return millis
    private static long computeWaitingTime(final Instant start, final Duration duration) {
        final Instant end = duration.addToInstant(start);
        final long diff = end.toEpochMilli() - Instant.now().toEpochMilli();
        return diff;
    }

    private void addWaitingPartition(final PriorityQueue<WaitingPartition> waitingPartitions,
                                     final String jobName,
                                     final Instant partition,
                                     final Instant activationThreshold) throws SQLException {
        final Instant endPartition = database.getJob(jobName).getEndPartition();
        if(partition.compareTo(endPartition) < 0) {
            waitingPartitions.add(new WaitingPartition(
                    jobName,
                    partition,
                    activationThreshold));
        }
    }

    private PriorityQueue<WaitingPartition> buildNextPartitionIndex() throws SQLException {
        final PriorityQueue<WaitingPartition> waitingPartitions = new PriorityQueue<WaitingPartition>(WaitingPartition.comparator);
        for(final JobPartition partition: database.lastPartitionPerJob()) {
            final String jobName = partition.getJobName();
            final Instant startPartition = partition.getStartPartition();
            final Instant lastPartition = partition.getLastPartition();
            final Duration jobIncrement = database.getJob(jobName).getIncrement();
            if(lastPartition == null || startPartition.compareTo(lastPartition) > 0) {
                // has never run yet, or the new startPartition > lastPartition
                addWaitingPartition(waitingPartitions, jobName,
                        startPartition,
                        jobIncrement.addToInstant(startPartition)
                );
            } else {
                addWaitingPartition(waitingPartitions, jobName,
                        jobIncrement.addToInstant(lastPartition),
                        jobIncrement.addToInstant(lastPartition, 2L)
                );
            }
        }
        return waitingPartitions;
    }

    private static String coalesce(final String input1, final String input2) {
	return input1 == null ? input2 : input1;
    }

    private boolean importNewJobGraph() throws SQLException {
        final Instant now = Instant.now();
        final Instant nextParseInstant = Duration.parseDuration(
                coalesce(database.getConfiguration(JobConfiguration.NEW_CONFIGURATION_POLLING_TIME_KEY),
			 DEFAULT_NEW_CONFIGURATION_POLLING_TIME)
        ).addToInstant(lastJobParsing);
        if(now.compareTo(nextParseInstant) >= 0) {
            // try parsing
            final JobConfiguration jobConfiguration = jobParser.parseJobConfiguration();
            if(jobConfiguration != null) {
                // push into database
                database.pushConfiguration(jobConfiguration);
                // mark the file as imported
                jobParser.markAsImported();
                return true;
            }
            lastJobParsing = now;
        }
        return false;
    }

    private boolean addNewPartitionsInExecutionLogs() throws SQLException {
        final Instant now = Instant.now();

        final WaitingPartition next = waitingPartitions.peek();
        if(next != null && now.toEpochMilli() > next.getActivationThreshold().toEpochMilli()) {
            // remove it
            waitingPartitions.poll();
            // add in database
            database.addJobExecution(next.getJobName(), next.getPartition(), now);
            final Job job = database.getJob(next.getJobName());
            database.applyRetention(
                    next.getJobName(),
                    job.getRetention().minusToInstant(next.getPartition())
            );
            // add next waitingPartition
            addWaitingPartition(waitingPartitions, next.getJobName(),
                    next.getActivationThreshold(),
                    job.getIncrement().addToInstant(next.getActivationThreshold())
            );
            return true;
        } else {
            return false;
        }
    }

    // update counters at the same time
    private void updateRunningExecutors() throws SQLException {
        final Iterator<StartedExecution> it = runningJobs.iterator();

        while(it.hasNext()) {
            final StartedExecution execution = it.next();
            if(!execution.isAlive()) {
                final Instant endTime = Instant.now();
                final String jobName = execution.getJobName();
                it.remove();
                // update execution log
                final StartedExecution.Status status;
                final Instant nextScheduleTime;
                if(execution.isSuccessfull()) {
                    status = StartedExecution.Status.SUCCESS;
                    nextScheduleTime = null;
                } else {
                    switch (execution.getFailureBehavior()) {
                        case RETRY:
                            status = StartedExecution.Status.FAILURE;
                            nextScheduleTime = execution.getRetryDelay().addToInstant(endTime);
                            break;
                        case MARK_SUCCESS:
                            status = StartedExecution.Status.SUCCESS;
                            nextScheduleTime = null;
                            break;
                        default:
                            throw new Error("Should not reach here");
                    }
                }
                logger.info(jobName + " " + Utils.toSqliteFormat(execution.getPartition()) + " finished with status: " + status);
                // update counters
                database.decrementJobRunningInstances(jobName);
                database.updateJobEndExecution(jobName,
                        execution.getPartition(),
                        endTime,
                        nextScheduleTime,
                        status);
            }
        }
    }

    private boolean addRunningExecutor(final int maxExecutors) throws SQLException, IOException {
        if(runningJobs.size() >= maxExecutors) {
            // max already reached
            return false;
        }

        if(waitingExecutions.size() == 0) {
            waitingExecutions = database.nextAvailableExecutions(Instant.now());
        }

        final AvailableExecution availableExecution;
        if(waitingExecutions.size() == 0) {
            return false;
        } else {
            availableExecution = waitingExecutions.poll();
        }

        final String jobName = availableExecution.getJobName();
        final Instant partition = availableExecution.getPartition();

        database.updateJobStartExecution(jobName, partition, Instant.now());
        database.incrementJobRunningInstances(jobName);

        // prepare log paths
        final Job job = database.getJob(jobName);
        final File stdoutFile = new File(applyPartitionTemplating(job.getStdoutPath(), partition));
        final File stderrFile = new File(applyPartitionTemplating(job.getStderrPath(), partition));
        createParentFolders(stdoutFile);
        createParentFolders(stderrFile);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(stdoutFile));
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(stderrFile));
        processBuilder.command(applyPartitionTemplating(database.getJobCommands(availableExecution.getJobName()), partition));
        logger.info("Start execution of: " + availableExecution.getJobName() + " " + Utils.toSqliteFormat(partition));
        runningJobs.add(startExecution(job, partition, processBuilder, stderrFile));
        return true;
    }

    private static void createParentFolders(final File file) {
        new File(file.getAbsoluteFile().getParentFile().getAbsolutePath()).mkdirs();
    }

    private static StartedExecution startExecution(final Job job,
                                                   final Instant partition,
                                                   final ProcessBuilder processBuilder,
                                                   final File stderrFile) throws IOException {
        Process process;
        try {
            process = processBuilder.start();
        } catch (final IOException e) {
            try(final FileOutputStream fos = new FileOutputStream(stderrFile, true)) {
                Utils.writeToStream(e.getMessage(), fos);
            }
            process = null;
        }
        return new StartedExecution(job.getName(), partition, process, job.getFailureBehavior(), job.getRetryDelay());
    }


    private static String templateStartPartition = "\\[\\[partition\\]\\]";

    static ArrayList<String> applyPartitionTemplating(final ArrayList<String> commands, final Instant partition) {
        final ArrayList<String> newCommands = new ArrayList<>(commands.size());
        for(final String command: commands) {
            newCommands.add(applyPartitionTemplating(command, partition));
        }
        return newCommands;
    }

    static String applyPartitionTemplating(final String input, final Instant partition) {
        return input.replaceAll(templateStartPartition, Utils.toSqliteFormat(partition));
    }

}
