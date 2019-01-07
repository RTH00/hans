package org.rth.hans;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {

    static final String sqLiteFormatterPattern = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter SQLiteFormatter = DateTimeFormatter.ofPattern(sqLiteFormatterPattern).withZone(ZoneId.of("UTC"));

    public static Instant parseSqliteFormat(final String input) {
        if(input == null) {
            return null;
        } else {
            return Instant.from(SQLiteFormatter.parse(input));
        }
    }

    public static String toSqliteFormat(final Instant instant) {
        if(instant == null) {
            return null;
        } else {
            return SQLiteFormatter.format(instant);
        }
    }

    public static String readStream(final InputStream is) throws IOException {
        try(final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final byte[] b = new byte[1024];
            while(true) {
                final int index = is.read(b);
                if(index <= 0) {
                    return new String(os.toByteArray(), "UTF-8");
                }
                os.write(b, 0, index);
            }
        }
    }

    public static String readFile(final File file) throws IOException  {
        try(final FileInputStream is = new FileInputStream(file)) {
            return readStream(is);
        }
    }

    public static String readResource(final String path) throws IOException  {
        try(final InputStream is = Utils.class.getClassLoader().getResourceAsStream(path)) {
            return readStream(is);
        }
    }

    public static void writeToStream(final String input, final OutputStream os) throws IOException {
        os.write(input.getBytes(Charset.forName("UTF-8")));
    }
}
