package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;
import org.rth.hans.core.Hans;

import java.util.Map;

import static com.opensymphony.xwork2.Action.ERROR;
import static com.opensymphony.xwork2.Action.SUCCESS;

public class UpdateJob implements SessionAware {

    private Map<String, Object> session;

    private String jobName;

    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }

    public String updateIsActivated(final boolean newValue) throws Exception {
        if(jobName == null) {
            return ERROR;
        }
        // TODO check authorized
        if(Hans.database.updateJobIsActivated(jobName, newValue)) {
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    public String activate() throws Exception {
        return updateIsActivated(true);
    }

    public String desactivate() throws Exception {
        return updateIsActivated(false);
    }


    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }

}
