package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import static gitlet.Utils.join;

public class Remote implements Serializable {

    /** The remote file. */
    public static final File REMOTEFILE = join(Repository.GITLET_DIR, ".REMOTE");

    /** Hashmap that stores remote name -> remote address. */
    private HashMap<String, String> remoteMap = new HashMap<>();

    /** Saves the REMOTEFILE to a file for future use. */
    public void saveRemote() {
        if (!REMOTEFILE.exists()) {
            try {
                REMOTEFILE.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(REMOTEFILE, this);
    }

    /** Returns the remote. */
    public static Remote fromFile() {
        return Utils.readObject(REMOTEFILE, Remote.class);
    }

    /** Returns the remoteMap. */
    public HashMap<String, String> getRemoteMap() {
        return remoteMap;
    }

}
