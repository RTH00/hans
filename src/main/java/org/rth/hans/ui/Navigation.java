package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

import static com.opensymphony.xwork2.Action.SUCCESS;

public class Navigation implements SessionAware {

    private Map<String, Object> session;

    private String page;

    @Override
    public void setSession(final Map<String, Object> session) {
        this.session = session;
    }

    public String execute() throws Exception {
        return SUCCESS;
    }

    public void setPage(final String page) {
        this.page = page;
    }

    public String getPage() {
        return page;
    }

    public String getJobsActive() {
        return activeStatus("jobs");
    }

    public String getDashboardActive() {
        return activeStatus("dashboard");
    }

    public String getPerformanceActive() {
        return activeStatus("performance");
    }

    private String activeStatus(final String targetPage) {
        if(targetPage.equals(page)) {
            return "active";
        } else {
            return "";
        }
    }

}
