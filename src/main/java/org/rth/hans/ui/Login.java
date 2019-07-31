package org.rth.hans.ui;

import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

import static com.opensymphony.xwork2.Action.*;

public class Login implements SessionAware {

    public static String UNAUTHORIZED = "unauthorized";


    // inputs
    public String username;
    public String password;


    private Map<String, Object> session;

    @Override
    public void setSession(final Map<String, Object> session) {
        this.session = session;
    }

    public String execute() throws Exception {

        if("toto".equals(password)) {
            session.put("user", new User(username));
            return SUCCESS;
        } else {
            return ERROR;
        }



    }



}
