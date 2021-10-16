package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  The commit tracks filenames to their contents.
 *
 *  @author Jeffrey Huang
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;

    /** The timestamp of the Commit. */
    private String timestamp;


    /** Parents are sha1codes of the parent commit. */
    private String parent;
    private String parent2;

    /** Sha1code of the commit. */
    private String sha1code;


    /** Holds filename -> blobSHA1code. */
    private HashMap<String, String> commitMap = new HashMap<String, String>();

    /** Commit constructor. */
    public Commit(String message, String timestamp, String parent) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent;
    }

    /** Merge commit constructor. */
    public Commit(String message, String timestamp, String parent1, String parent2) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent1;
        this.parent2 = parent2;
    }

    //@Source StackOverflow
    /** Returns the current timestamp. */
    public static String getDate() {
        Date date = new Date();
        TimeZone pstTimeZone = TimeZone.getTimeZone("America/Los_Angeles");
        DateFormat formatter = new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z");
        formatter.setTimeZone(pstTimeZone);
        String formattedDate = formatter.format(date);
        return formattedDate;

    }

    /** Makes a SHA1 code for the commit. */
    public static String getSHA1(byte[] item) {
        return sha1(item);
    }

    /** Returns sha1code of commit (makes this not naked). */
    public String getCommitSHA1code() {
        return sha1code;
    }

    /** Saves a commit to a file for future use. */
    public void saveCommmit() {
        this.sha1code = sha1(serialize(this));
        File newCommit = join(Repository.COMMIT_DIR, sha1code);
        if (!newCommit.exists()) {
            try {
                newCommit.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(newCommit, this);
    }

    /** Turns a file to a commit. */
    public static Commit fromFile(String sha1code) {
        return Utils.readObject(join(Repository.COMMIT_DIR, sha1code), Commit.class);
    }

    /** Returns the log message for this commit. */
    public void getLogMessage() {
        System.out.println("===");
        System.out.println("commit " + sha1code);
        if (parent2 != null) {
            System.out.println("Merge: " + parent.substring(0, 7) + " "
                    + parent2.substring(0, 7));
        }
        System.out.println("Date: " + timestamp);
        System.out.println(message + "\n");
    }

    /** Returns the commitMap. */
    public HashMap<String, String> getCommitMap() {
        return this.commitMap;
    }

    /** Returns the parent. */
    public String getParent() {
        return parent;
    }

    /** Returns the second parent. */
    public String getParent2() {
        return parent2;
    }

    /** Returns the commit message. */
    public String getCommitMessage() {
        return message;
    }

    /** Returns the sha1code of this commit. */
    public String getSha1code() {
        return sha1code;
    }

}
