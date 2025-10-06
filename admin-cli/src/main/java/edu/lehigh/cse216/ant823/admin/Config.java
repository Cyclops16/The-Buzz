package edu.lehigh.cse216.ant823.admin;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    public static String getDbUrl() {
        Dotenv dotenv = Dotenv.configure()
                              .ignoreIfMissing()
                              .load();
        String url = dotenv.get("DB_URI");
        if (url == null) {
            System.err.println("DATABASE_URI not found in .env file");
        }
        return url;
    }
}
