package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

import static com.opensymphony.xwork2.Action.SUCCESS;

public class Logout implements SessionAware {

    private Map<String, Object> session;

    @Override
    public void setSession(final Map<String, Object> session) {
        this.session = session;
    }

    public String execute() throws Exception {

        session.clear();

        return SUCCESS;
    }


}
