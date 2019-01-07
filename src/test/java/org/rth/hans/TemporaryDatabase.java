package org.rth.hans;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class TemporaryDatabase extends Database {

    private final File databaseFile;

    // for simpler testing
    public TemporaryDatabase(final String databaseRelativePath) throws SQLException, IOException {
        this(new File(databaseRelativePath));
    }

    public TemporaryDatabase(final File databaseFile) throws SQLException, IOException {
        super(databaseFile.getAbsolutePath());
        this.databaseFile = databaseFile;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if(!databaseFile.delete()) {
            throw new IOException("Can't delete: " + databaseFile.getAbsolutePath());
        }
    }
}
