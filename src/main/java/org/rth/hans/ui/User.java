package org.rth.hans.ui;

import java.time.Instant;

public class User {

    public static class Identification {
        private final String hashedPassword;
        private final String salt;

        public Identification(final String hashedPassword, final String salt) {
            this.hashedPassword = hashedPassword;
            this.salt = salt;
        }

        public String getHashedPassword() {
            return hashedPassword;
        }

        public String getSalt() {
            return salt;
        }
    }

    public enum Role {
        VIEW, UPDATE, ADMIN
    }

    private final String userName;

    public User(final String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return userName;
    }
}
