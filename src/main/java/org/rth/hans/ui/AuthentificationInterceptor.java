package org.rth.hans.ui;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.apache.struts2.interceptor.SessionAware;

import java.util.Map;

public class AuthentificationInterceptor implements Interceptor {

    public static String unauthorized = "unauthorized";

    @Override
    public String intercept(final ActionInvocation actionInvocation) throws Exception {
        final User user = (User)actionInvocation.getInvocationContext().getSession().get("user");
        if(user == null) {
            return unauthorized;
        } else {
            final String ret = actionInvocation.invoke();
            return ret;
        }
    }

    @Override
    public void init() {
        // nothing
    }

    @Override
    public void destroy() {
        // nothing
    }

}
