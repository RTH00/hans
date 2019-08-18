package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;
import org.rth.hans.core.*;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

import static com.opensymphony.xwork2.Action.SUCCESS;

public class Jobs implements SessionAware {

    private Map<String, Object> session;

    // inputs
    private String jobName;
    private long limit = 24L;
    private long offset = 0L;

    // outputs
    private ArrayList<Job> jobs;
    private Job selectedJob; // without commands/dependencies
    private ArrayList<Dependency> dependencies;
    private ArrayList<String> commands;

    private ArrayList<ExecutionInfo> executionInfos;

    @Override
    public void setSession(final Map<String, Object> session) {
        this.session = session;
    }

    private static class ExecutionInfo {

        private final String jobName;
        private final Instant partition;
        private final StartedExecution.Status status;
        private final Instant startTime;
        private final Instant endTime;

        // TODO print missing dependencies and isActivated when the task is initialised

        public static final DescPartitionComparator descPartitionComparator = new DescPartitionComparator();

        private ExecutionInfo(final Execution execution, final Instant now) {
            this.jobName = execution.getJobName();
            this.partition = execution.getPartition();
            this.status = execution.getStatus();
            this.startTime = execution.getStartTime();
            this.endTime = execution.getEndTime();
        }

        private static class DescPartitionComparator implements Comparator<ExecutionInfo> {
            @Override
            public int compare(final ExecutionInfo executionInfo1, final ExecutionInfo executionInfo2) {
                return -executionInfo1.partition.compareTo(executionInfo2.partition);
            }
        }

        public String getJobName() {
            return jobName;
        }
        public String getPartition() {
            return Utils.toSqliteFormat(partition);
        }
        public String getStatus() {
            return status.toString();
        }
        public String getStartTime() {
            if(startTime == null) {
                return "";
            } else {
                return Utils.toSqliteFormat(startTime);
            }
        }
        public String getEndTime() {
            if(endTime == null) {
                return "";
            } else {
                return Utils.toSqliteFormat(endTime);
            }
        }
        public String getRunningTime() { // in seconds
            if(startTime == null) {
                return null;
            } else {
                final Instant currentEndTime = Utils.coalesce(endTime, Instant::now);
                return Long.toString(currentEndTime.getEpochSecond() - startTime.getEpochSecond());
            }
        }

    }

    public String execute() throws Exception {

        jobs = Hans.database.getAllJobs();
        jobs.sort(Job.jobNameCaseInsensitiveComparator);
        selectedJob = Hans.database.getJob(jobName);

        if(selectedJob != null) {
            // TODO update start instant
            final Instant now = Instant.now();
            executionInfos = Utils.map(
                    Hans.database.getExecutionsBetween(jobName, limit, offset),
                    exe -> new ExecutionInfo(exe, now)
            );
            executionInfos.sort(ExecutionInfo.descPartitionComparator);
            fetchDependencies(jobName);
            fetchCommands(jobName);
        }

        return SUCCESS;

    }

    public void fetchDependencies(final String jobName) throws SQLException {
        dependencies = Hans.database.getJobDependencies(jobName);
    }

    public void fetchCommands(final String jobName) throws SQLException {
        commands = Hans.database.getJobCommands(jobName);
    }


    public ArrayList<Job> getJobs() {
        return jobs;
    }

    public Job getSelectedJob() {
        return selectedJob;
    }

    public void setJobName(final String selectedJobName) {
        this.jobName = selectedJobName;
    }

    public String getJobName() {
        return jobName;
    }

    public ArrayList<ExecutionInfo> getExecutionInfos() {
        return executionInfos;
    }

    public ArrayList<Dependency> getDependencies() {
        return dependencies;
    }

    public ArrayList<String> getCommands() {
        return commands;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    public long getPreviousOffset() {
        return Math.max(0L, offset - limit);
    }

    public long getNextOffset() {
        return offset + limit;
    }

    public boolean getHashMoreExecutions() {
        return executionInfos != null && limit == executionInfos.size();
    }

}
