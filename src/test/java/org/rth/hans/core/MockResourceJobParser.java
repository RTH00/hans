package org.rth.hans.core;

import org.rth.hans.core.JobConfiguration;
import org.rth.hans.core.JobParser;
import org.rth.hans.core.Utils;

public class MockResourceJobParser extends JobParser {

    private final String jobResourcePath;
    private boolean firstCallParseJobs;

    // TODO rename
    public MockResourceJobParser(final String jobResourcePath) {
        super(null);
        this.jobResourcePath = jobResourcePath;
        this.firstCallParseJobs = true;
    }

    @Override
    public JobConfiguration parseJobConfiguration() {
        if(jobResourcePath == null) {
            return null;
        }
        if(firstCallParseJobs) {
            firstCallParseJobs = false; // return the jobs only once
        } else {
            return null;
        }
        try {
            return parseJobConfiguration(Utils.readResource(jobResourcePath));
        } catch(final Exception e) {
            return null;
        }
    }
}
