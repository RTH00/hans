package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

import static com.opensymphony.xwork2.Action.SUCCESS;

public class Index implements SessionAware {

    private User user;

    private Map<String, Object> session;

    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }


    public String execute() throws Exception {

        /*
        user = (User)session.get("user");
        if(user != null) {
            return SUCCESS;
        } else {
            return Login.UNAUTHORIZED;
        }
        */

        return SUCCESS;
    }

}
