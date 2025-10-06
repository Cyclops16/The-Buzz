package edu.lehigh.cse216.ant823.admin;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AppTest extends TestCase {
    private static Database db;

    public AppTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    protected void setUp() {
        if (db == null) {
            db = Database.getDatabase(Config.getDbUrl());
        }
        db.dropTable("messages");
        db.createTable("messages");
    }

    protected void tearDown() {
        // Do nothing here to keep db open between tests
    }

    public void testInsertRow() {
        int result = db.insertRow("messages", 1, 0, "JUnit Subject", "JUnit Body", null, 0, 0);
        assertEquals(1, result);
    }

    public void testSelectOne() {
        db.insertRow("messages", 1, 0, "Hello", "World", null, 0, 0);
        Object res = db.selectOne("messages", 1);
        assertNotNull(res);
        Database.MessageRow row = (Database.MessageRow) res;
        assertEquals("Hello", row.subject());
        assertEquals("World", row.body());
    }

    public void testSelectAll() {
        db.insertRow("messages", 1, 0, "Subject A", "Body A", null, 0, 0);
        db.insertRow("messages", 1, 0, "Subject B", "Body B", null, 0, 0);
        ArrayList<Object> allRows = db.selectAll("messages");
        assertNotNull(allRows);
        assertTrue(allRows.size() >= 2);
    }

    public void testDeleteRow() {
        db.insertRow("messages", 1, 0, "To be deleted", "Body", null, 0, 0);
        int result = db.deleteRow("messages", 1);
        assertEquals(1, result);
        Object row = db.selectOne("messages", 1);
        assertNull(row);
    }
}
