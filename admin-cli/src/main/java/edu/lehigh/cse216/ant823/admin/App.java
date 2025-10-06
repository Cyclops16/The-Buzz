package edu.lehigh.cse216.ant823.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class App {

    public static void main(String[] argv) {
        //simpleManualTests(argv);
        mainCliLoop(argv);
    }

    // public static void simpleManualTests(String[] argv) {
    //     String dbUrl = Config.getDbUrl();
    //     Database db = Database.getDatabase(dbUrl);        
    //     db.dropTable();
    //     db.createTable();
    //     db.insertRow("Test Title", "Test Description", 0);
    //     db.updateOne(1, "Updated Description");

    //     ArrayList<Database.RowData> list_rd = db.selectAll();
    //     System.out.println("Row data:");
    //     for (Database.RowData rd : list_rd)
    //         System.out.println(">\t" + rd);

    //     Database.RowData single_rd = db.selectOne(1);
    //     System.out.println("Single row: " + single_rd);

    //     db.deleteRow(1);

    //     if (db != null)
    //         db.disconnect();
    // } 

    public static void mainCliLoop(String[] argv) {
        String dbUrl = Config.getDbUrl();
        Database db = Database.getDatabase(dbUrl);

        if (db == null) {
            System.err.println("Unable to make database object, exiting.");
            System.exit(1);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String currentTable = "messages"; // Default starting table

        while (true) {
            char action = prompt(in);
            if (action == '?') {
                menu();
            } else if (action == 's') {
                String selected = tablePrompt(in);
                if (selected != null) {
                    currentTable = selected;
                    System.out.println("Switched to table: " + currentTable);
                }
            } else if (action == 'q') {
                break;
            } else if (action == 'T') {
                db.createTable(currentTable);
            } else if (action == 'D') {
                db.dropTable(currentTable);
            } else if (action == '1') {
                int id = getInt(in, "Enter the row ID");
                if (id == -1) continue;
                Object res = db.selectOne(currentTable, id);
                if (res != null) {
                    System.out.println("  --- Row Data ---");
                    switch (currentTable) {
                        case "users" -> {
                            Database.UserRow rd = (Database.UserRow) res;
                            System.out.println("  ID: " + rd.id());
                            System.out.println("  First Name: " + rd.firstName());
                            System.out.println("  Last Name: " + rd.lastName());
                            System.out.println("  Email: " + rd.email());
                            System.out.println("  Gender Identity: " + rd.genderIdentity());
                            System.out.println("  Sexual Orientation: " + rd.sexualOrientation());
                            System.out.println("  Note: " + rd.note());
                            System.out.println("  Is Valid: " + rd.isValid());
                        }
                        case "messages" -> {
                            Database.MessageRow rd = (Database.MessageRow) res;
                            System.out.println("  ID: " + rd.id());
                            System.out.println("  User ID: " + rd.userId());
                            System.out.println("  Subject: " + rd.subject());
                            System.out.println("  Body: " + rd.body());
                            System.out.println("  Likes: " + rd.likes());
                            System.out.println("  Dislikes: " + rd.dislikes());
                            System.out.println("  Is Valid: " + rd.isValid());
                        }
                        case "comments" -> {
                            Database.CommentRow rd = (Database.CommentRow) res;
                            System.out.println("  ID: " + rd.id());
                            System.out.println("  User ID: " + rd.userId());
                            System.out.println("  Message ID: " + rd.messageId());
                            System.out.println("  Comment: " + rd.comment());
                            System.out.println("  Is Valid: " + rd.isValid());
                        }
                        case "user_upvotes", "user_downvotes" -> {
                            Database.VoteRow rd = (Database.VoteRow) res;
                            System.out.println("  User ID: " + rd.userId());
                            System.out.println("  Message ID: " + rd.messageId());
                        }
                        default -> System.out.println("  Unknown table format.");
                    }
                }
            } else if (action == '*') {
                ArrayList<Object> res = db.selectAll(currentTable);
                if (res == null || res.isEmpty()) {
                    System.out.println("  No rows found in " + currentTable);
                    continue;
                }
                System.out.println("  --- All rows in table: " + currentTable + " ---");
                for (Object row : res) {
                    switch (currentTable) {
                        case "users" -> {
                            Database.UserRow rd = (Database.UserRow) row;
                            System.out.println("  [" + rd.id() + "] " + rd.firstName() + " " + rd.lastName());
                        }
                        case "messages" -> {
                            Database.MessageRow rd = (Database.MessageRow) row;
                            System.out.println("  [" + rd.id() + "] " + rd.subject());
                        }
                        case "comments" -> {
                            Database.CommentRow rd = (Database.CommentRow) row;
                            System.out.println("  [" + rd.id() + "] " + rd.comment());
                        }
                        case "user_upvotes", "user_downvotes" -> {
                            Database.VoteRow rd = (Database.VoteRow) row;
                            System.out.println("  [" + rd.userId() + "] user: " + rd.userId() + ", msg: " + rd.messageId());
                        }
                        default -> System.out.println("  Unknown row format.");
                    }
                    
                }
            } else if (action == '-') {
                int id = getInt(in, "Enter the row ID");
                if (id == -1) continue;
                int res = db.deleteRow(currentTable, id);
                if (res == -1) continue;
                System.out.println("  " + res + " rows deleted");
            } else if (action == '+') {
                switch (currentTable) {
                    case "users" -> {
                        String fName = getString(in, "Enter first name");
                        String lName = getString(in, "Enter last name");
                        String email = getString(in, "Enter email");
                        String gender = getString(in, "Enter gender identity");
                        String sexOrient = getString(in, "Enter sexual orientation");
                        String note = getString(in, "Enter note");

                        // Only insert if email is provided (or you can add more validation)
                        if (!email.isEmpty()) {
                            int res = db.insertRow("users", fName, lName, email, gender, sexOrient, note);
                            System.out.println(res + " user(s) added");
                        }
                    }
                    case "messages" -> {
                        int userId = getInt(in, "Enter user ID");
                        String subject = getString(in, "Enter message subject");
                        String body = getString(in, "Enter message body");
                        String attachmentUrl = getString(in, "Enter attachment URL (or leave blank)");
                        if (!subject.isEmpty() && !body.isEmpty()) {
                            int res = db.insertRow("messages", userId, 0, subject, body, attachmentUrl, 0, 0);
                            System.out.println(res + " message(s) added");
                        }
                    }
                    case "comments" -> {
                        int userId = getInt(in, "Enter user ID");
                        int msgId = getInt(in, "Enter message ID");
                        String commentBody = getString(in, "Enter comment body");
                        if (!commentBody.isEmpty()) {
                            int res = db.insertRow("comments", userId, msgId, null, commentBody, null, 0, 0);
                            System.out.println(res + " comment(s) added");
                        }
                    }
                    case "user_upvotes", "user_downvotes" -> {
                        int userId = getInt(in, "Enter user ID");
                        int msgId = getInt(in, "Enter message ID");
                        int res = db.insertRow(currentTable, userId, msgId, null, null, null, 0, 0);
                        System.out.println(res + " " + currentTable + " row(s) added");
                    }
                    default -> System.out.println("Unsupported table for insertion.");
                }
            } else if (action == '~') {
                int id = getInt(in, "Enter the row ID");
                if (id == -1) return;
                int res = -1;
                switch (currentTable) {
                    case "users" -> {
                        String email = getString(in, "Enter new email (or leave blank to skip)");
                        String gender = getString(in, "Enter new gender identity (or leave blank to skip)");
                        String sexualOrientation = getString(in, "Enter new sexual orientation (or leave blank to skip)");
                        String validStr = getString(in, "Is user valid? (true/false or leave blank to skip)");

                        // Always pass all fields in fixed order, use null or empty string if skipped
                        String[] userUpdateArgs = new String[] {
                            email.isEmpty() ? null : email,
                            gender.isEmpty() ? null : gender,
                            sexualOrientation.isEmpty() ? null : sexualOrientation,
                            validStr.isEmpty() ? null : validStr
                        };

                        res = db.updateOne("users", id, userUpdateArgs);
                        System.out.println(res + " user(s) updated");
                    }
                    case "messages" -> {
                        String subject = getString(in, "Enter new subject (or leave blank to skip)");
                        String body = getString(in, "Enter new body (or leave blank to skip)");
                        String validStr = getString(in, "Is message valid? (true/false or leave blank to skip)");

                        // Always pass all fields in fixed order, use null if skipped
                        String[] messageUpdateArgs = new String[] {
                            subject.isEmpty() ? null : subject,
                            body.isEmpty() ? null : body,
                            validStr.isEmpty() ? null : validStr
                        };

                        res = db.updateOne("messages", id, messageUpdateArgs);
                        System.out.println(res + " message(s) updated");
                    }
                    case "comments" -> {
                        String newComment = getString(in, "Enter new comment");
                        res = db.updateOne("comments", id, newComment);
                    }
                    default -> System.out.println("Unsupported table for update.");
                }
                if (res != -1) {
                    System.out.println("  " + res + " row(s) updated");
                }
            }else if (action == 'v') {
                db.createViews();
            }
            else if (action == 'j') {
                db.dropViews();
            }

            else if (action == 'u') {
                // 1) fetch attachments
                ArrayList<Database.AttachmentRow> atts = db.selectAttachmentsSortedByModified();
                if (atts.isEmpty()) {
                    System.out.println("  No attachments to show");
                    continue;
                }
                System.out.println("  --- Attachments (by last-modified) ---");
                for (Database.AttachmentRow a : atts) {
                    System.out.printf("  [%d] %s  (modified: %s)%n, UserID: %d%n",
                        a.id(), a.url(), a.lastModified(), a.userId());
                }

                // 2) ask if they want to clear one
                int toDel = getInt(in, "Enter attachment ID to delete (0 to cancel)");
                if (toDel > 0) {
                    int updated = db.clearAttachmentUrl(toDel);
                    System.out.println("  " + updated + " attachment(s) cleared");
                }
            }
        }

        db.disconnect();
    }


    static void menu() {
        System.out.println("Main Menu");
        System.out.println("  [s] Switch table");
        System.out.println("  [T] Create table");
        System.out.println("  [D] Drop table");
        System.out.println("  [1] Query a specific row");
        System.out.println("  [*] Query all rows");
        System.out.println("  [-] Delete a row");
        System.out.println("  [+] Insert a new row");
        System.out.println("  [~] Update a row");
        System.out.println("  [v] Create views");
        System.out.println("  [j] Drop views");
        System.out.println("  [u] List attachments");
        System.out.println("  [q] Quit");
        System.out.println("  [?] Help");
    }

    static String tablePrompt(BufferedReader in) {
        String[] tables = {"users", "messages", "comments", "user_upvotes", "user_downvotes"};
        System.out.println("Select table:");
        for (int i = 0; i < tables.length; i++) {
            System.out.println("  [" + i + "] " + tables[i]);
        }

        int choice = getInt(in, "Choose table number");
        if (choice < 0 || choice >= tables.length) {
            System.out.println("Invalid table choice.");
            return null;
        }
        return tables[choice];
    }

    static char prompt(BufferedReader in) {
        String actions = "sTD1*-+~q?vju";

        while (true) {
            System.out.print("[" + actions + "] :> ");
            String action;
            try {
                action = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            if (action.length() != 1)
                continue;
            if (actions.contains(action)) {
                return action.charAt(0);
            }
            System.out.println("Invalid Command");
        }
    }

    static String getString(BufferedReader in, String message) {
        String s;
        try {
            System.out.print(message + " :> ");
            s = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        return s;
    }

    static int getInt(BufferedReader in, String message) {
        int i = -1;
        try {
            System.out.print(message + " :> ");
            i = Integer.parseInt(in.readLine());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return i;
    }
}
