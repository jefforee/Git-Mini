package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

import static gitlet.Utils.join;

public class BFS {

    public static ArrayList<String> bfs(String commitSHA1) {
        ArrayList<String> returnCommits = new ArrayList<>();
        Queue<String> fringe = new PriorityQueue<>();
        fringe.add(commitSHA1);
        returnCommits.add(commitSHA1);
        while (!fringe.isEmpty()) {
            String v = fringe.remove();
            Commit currentCommit = Commit.fromFile(v);
            ArrayList<String> commitParents = new ArrayList<>();
            if (currentCommit.getParent() != null) {
                commitParents.add(currentCommit.getParent());
            }
            if (currentCommit.getParent2() != null) {
                commitParents.add(currentCommit.getParent2());
            }
            for (String w : commitParents) {
                fringe.add(w);
                returnCommits.add(w);
            }
        }
        return returnCommits;
    }

    public static ArrayList<String> remotebfs(String commitSHA1, String remoteName) {
        ArrayList<String> returnCommits = new ArrayList<>();
        Queue<String> fringe = new PriorityQueue<>();
        fringe.add(commitSHA1);
        returnCommits.add(commitSHA1);
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteCommits = join(remoteObjects, ".commits");
        while (!fringe.isEmpty()) {
            String v = fringe.remove();
            Commit currentCommit = Utils.readObject(join(remoteCommits, v), Commit.class);
            ArrayList<String> commitParents = new ArrayList<>();
            if (currentCommit.getParent() != null) {
                commitParents.add(currentCommit.getParent());
            }
            if (currentCommit.getParent2() != null) {
                commitParents.add(currentCommit.getParent2());
            }
            for (String w : commitParents) {
                fringe.add(w);
                returnCommits.add(w);
            }
        }
        return returnCommits;
    }
}
