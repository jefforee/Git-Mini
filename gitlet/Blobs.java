package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static gitlet.Utils.*;

/**The saved contents of files. Since Gitlet saves many versions of files,
 * a single file might correspond to multiple blobs: each being tracked in
 * a different commit.*/

public class Blobs implements Serializable {
    private String blobSHA1code;
    private String fileSHA1code;

    private static File fileDir = join(Repository.OBJECT_DIR, ".fileContents");

    /** Constructor for a blob that makes the variables of contents and blobSHA1code. */
    public Blobs(String fileName) {
        File fileLook = join(Repository.CWD, fileName);
        byte[] contents = readContents(fileLook);
        fileSHA1code = sha1(contents);
        File newFile = join(fileDir, fileSHA1code);
        if (!newFile.exists()) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeContents(newFile, readContentsAsString(fileLook));

    }

    /** Get blobSHA1code. */
    public String getBlobSHA1code() {
        return blobSHA1code;
    }

    /** Get blobSHA1code. */
    public String getFileSHA1code() {
        return fileSHA1code;
    }

    /** Returns file dir. */
    public static File getFileDir() {
        return fileDir;
    }


    /** Saves a blob to a file for future use. */
    public void saveBlob() {
        this.blobSHA1code = sha1(serialize(this));
        File newBlob = join(Repository.BLOB_DIR, blobSHA1code);
        if (!newBlob.exists()) {
            try {
                newBlob.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(newBlob, this);
    }

    /** Turns file to Blobs. */
    public static Blobs fromFile(String sha1Code) {
        return Utils.readObject(join(Repository.BLOB_DIR, sha1Code), Blobs.class);
    }

    /** Helps checkout #1 rewrite the file in current directory. */
    public void checkoutRewrite(String filename) {
        File rewrittenFile = join(Repository.CWD, filename);
        if (!rewrittenFile.exists()) {
            try {
                rewrittenFile.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        File contents = Utils.join(fileDir, fileSHA1code);
        Utils.writeContents(rewrittenFile, readContentsAsString(contents));
    }

}
