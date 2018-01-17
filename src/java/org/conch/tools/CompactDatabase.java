/*
 * Copyright © 2017 sharder.org.
 * Copyright © 2014-2017 ichaoj.com.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with ichaoj.com,
 * no part of the COS software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package org.conch.tools;

import org.conch.Constants;
import org.conch.Conch;
import org.conch.util.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Compact and reorganize the NRS database.  The NRS application must not be
 * running.
 *
 * To run the database compact tool on Linux or Mac:
 *
 *   java -cp "classes:lib/*:conf" org.conch.tools.CompactDatabase
 *
 * To run the database compact tool on Windows:
 *
 *   java -cp "classes;lib/*;conf" -Dsharder.runtime.mode=desktop org.conch.tools.CompactDatabase
 */
public class CompactDatabase {

    /**
     * Compact the NRS database
     *
     * @param   args                Command line arguments
     */
    public static void main(String[] args) {
        //
        // Initialize Conch properties and logging
        //
        Logger.init();
        //
        // Compact the database
        //
        int exitCode = compactDatabase();
        //
        // Shutdown the logger and exit
        //
        Logger.shutdown();
        System.exit(exitCode);
    }

    /**
     * Compact the database
     */
    private static int compactDatabase() {
        int exitCode = 0;
        //
        // Get the database URL
        //
        String dbPrefix = Constants.isTestnet ? "sharder.testDb" : "sharder.db";
        String dbType = Conch.getStringProperty(dbPrefix + "Type");
        if (!"h2".equals(dbType)) {
            Logger.logErrorMessage("Database type must be 'h2'");
            return 1;
        }
        String dbUrl = Conch.getStringProperty(dbPrefix + "Url");
        if (dbUrl == null) {
            String dbPath = Conch.getDbDir(Conch.getStringProperty(dbPrefix + "Dir"));
            dbUrl = String.format("jdbc:%s:%s", dbType, dbPath);
        }
        String dbParams = Conch.getStringProperty(dbPrefix + "Params");
        dbUrl += ";" + dbParams;
        if (!dbUrl.contains("MV_STORE=")) {
            dbUrl += ";MV_STORE=FALSE";
        }
        String dbUsername = Conch.getStringProperty(dbPrefix + "Username", "sa");
        String dbPassword = Conch.getStringProperty(dbPrefix + "Password", "sa", true);
        //
        // Get the database path.  This is the third colon-separated operand and is
        // terminated by a semi-colon or by the end of the string.
        //
        int pos = dbUrl.indexOf(':');
        if (pos >= 0) {
            pos = dbUrl.indexOf(':', pos+1);
        }
        if (pos < 0) {
            Logger.logErrorMessage("Malformed database URL: " + dbUrl);
            return 1;
        }
        String dbDir;
        int startPos = pos + 1;
        int endPos = dbUrl.indexOf(';', startPos);
        if (endPos < 0) {
            dbDir = dbUrl.substring(startPos);
        } else {
            dbDir = dbUrl.substring(startPos, endPos);
        }
        //
        // Remove the optional 'file' operand
        //
        if (dbDir.startsWith("file:"))
            dbDir = dbDir.substring(5);
        //
        // Remove the database prefix from the end of the database path.  The path
        // separator can be either '/' or '\' (Windows will accept either separator
        // so we can't rely on the system property).
        //
        endPos = dbDir.lastIndexOf('\\');
        pos = dbDir.lastIndexOf('/');
        if (endPos >= 0) {
            if (pos >= 0) {
                endPos = Math.max(endPos, pos);
            }
        } else {
            endPos = pos;
        }
        if (endPos < 0) {
            Logger.logErrorMessage("Malformed database URL: " + dbUrl);
            return 1;
        }
        dbDir = dbDir.substring(0, endPos);
        Logger.logInfoMessage("Database directory is '" + dbDir + '"');
        //
        // Create our files
        //
        int phase = 0;
        File sqlFile = new File(dbDir, "backup.sql.gz");
        File dbFile = new File(dbDir, "sharder.h2.db");
        if (!dbFile.exists()) {
            dbFile = new File(dbDir, "sharder.mv.db");
            if (!dbFile.exists()) {
                Logger.logErrorMessage("Sharder database not found");
                return 1;
            }
        }
        File oldFile = new File(dbFile.getPath() + ".bak");
        try {
            //
            // Create the SQL script
            //
            Logger.logInfoMessage("Creating the SQL script");
            if (sqlFile.exists()) {
                if (!sqlFile.delete()) {
                    throw new IOException(String.format("Unable to delete '%s'", sqlFile.getPath()));
                }
            }
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                    Statement s = conn.createStatement()) {
                s.execute("SCRIPT TO '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
            }
            //
            // Create the new database
            //
            Logger.logInfoMessage("Creating the new database");
            if (!dbFile.renameTo(oldFile)) {
                throw new IOException(String.format("Unable to rename '%s' to '%s'",
                                                    dbFile.getPath(), oldFile.getPath()));
            }
            phase = 1;
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                    Statement s = conn.createStatement()) {
                s.execute("RUNSCRIPT FROM '" + sqlFile.getPath() + "' COMPRESSION GZIP CHARSET 'UTF-8'");
                s.execute("ANALYZE");
            }
            //
            // New database has been created
            //
            phase = 2;
            Logger.logInfoMessage("Database successfully compacted");
        } catch (Throwable exc) {
            Logger.logErrorMessage("Unable to compact the database", exc);
            exitCode = 1;
        } finally {
            switch (phase) {
                case 0:
                    //
                    // We failed while creating the SQL file
                    //
                    if (sqlFile.exists()) {
                        if (!sqlFile.delete()) {
                            Logger.logErrorMessage(String.format("Unable to delete '%s'", sqlFile.getPath()));
                        }
                    }
                    break;
                case 1:
                    //
                    // We failed while creating the new database
                    //
                    File newFile = new File(dbDir, "sharder.h2.db");
                    if (newFile.exists()) {
                        if (!newFile.delete()) {
                            Logger.logErrorMessage(String.format("Unable to delete '%s'", newFile.getPath()));
                        }
                    } else {
                        newFile = new File(dbDir, "sharder.mv.db");
                        if (newFile.exists()) {
                            if (!newFile.delete()) {
                                Logger.logErrorMessage(String.format("Unable to delete '%s'", newFile.getPath()));
                            }
                        }
                    }
                    if (!oldFile.renameTo(dbFile)) {
                        Logger.logErrorMessage(String.format("Unable to rename '%s' to '%s'",
                                                             oldFile.getPath(), dbFile.getPath()));
                    }
                    break;
                case 2:
                    //
                    // New database created
                    //
                    if (!sqlFile.delete()) {
                        Logger.logErrorMessage(String.format("Unable to delete '%s'", sqlFile.getPath()));
                    }
                    if (!oldFile.delete()) {
                        Logger.logErrorMessage(String.format("Unable to delete '%s'", oldFile.getPath()));
                    }
                    break;
            }
        }
        return exitCode;
    }
}