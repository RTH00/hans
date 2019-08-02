package org.rth.hans.ui;

import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.interceptor.SessionAware;
import org.rth.hans.core.Hans;
import org.rth.hans.ui.util.PasswordUtils;

import java.time.Instant;
import java.util.Map;

public class Login extends ActionSupport implements SessionAware {

    // inputs
    public String username;
    public String password;


    private Map<String, Object> session;

    @Override
    public void setSession(final Map<String, Object> session) {
        this.session = session;
    }

    public String execute() throws Exception {

        // TODO remove
        if(Hans.database.getUserIdentification("admin") == null) {
            final User.Identification identification = PasswordUtils.generateHashing("admin");
            Hans.database.addUser(
                    "admin",
                    identification.getHashedPassword(),
                    identification.getSalt(),
                    Instant.now(),
                    User.Role.ADMIN.name()
            );
        }


        if(username != null && password != null) {
            final User.Identification identification = Hans.database.getUserIdentification(username);
            if(identification != null && PasswordUtils.verifyPassword(identification, password)) {
                session.put("user", new User(username));
                return SUCCESS;
            }
        }
        addActionError("Invalid username or password");
        return ERROR;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
