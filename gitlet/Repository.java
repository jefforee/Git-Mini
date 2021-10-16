package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author Jeffrey Huang
 */
public class Repository implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The Object directory that holds blobs, trees, and commits.
     */
    public static final File OBJECT_DIR = join(GITLET_DIR, ".objects");

    /**
     * A commit folder holding all commits.
     **/
    public static final File COMMIT_DIR = join(OBJECT_DIR, ".commits");

    /**
     * A blob folder holding all blobs.
     **/
    public static final File BLOB_DIR = join(OBJECT_DIR, ".blobs");

    /**
     * Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit
     * (just like that, with no punctuation). It will have a single branch:
     * master, which initially points to this initial commit, and master will
     * be the current branch. The timestamp for this initial commit will be
     * 00:00:00 UTC, Thursday, 1 January 1970 in whatever format you choose
     * for dates. Since the initial commit in all repositories created
     * by Gitlet will have exactly the same content, it follows that all
     * repositories will automatically share this commit (they will all have
     * the same UID) and all commits in all repositories will trace back to it.
     */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists "
                    + "in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        OBJECT_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOB_DIR.mkdir();
        Blobs.getFileDir().mkdir();
        StagingArea sa = new StagingArea();
        sa.saveStagingArea();
        Commit initialCommit = new Commit("initial commit",
                "Wed Dec 31 16:00:00 1969 -0800", null);
        Trees tree = new Trees();
        initialCommit.saveCommmit();
        tree.setMaster(initialCommit.getCommitSHA1code());
        tree.setHead(initialCommit.getCommitSHA1code());
        tree.setHeadName("master");
        tree.getBranchHolder().put("master", tree.getMaster());
        tree.getAllCommitsMap().put(initialCommit.getCommitSHA1code(), initialCommit);
        tree.saveTree();
        Remote remote = new Remote();
        remote.saveRemote();
    }

    /**
     * Adds a copy of the file as it currently exists to the staging area
     * (see the description of the commit command). For this reason, adding
     * a file is also called staging the file for addition. Staging an
     * already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere in .gitlet.
     * If the current working version of the file is identical to the version
     * in the current commit, do not stage it to be added, and remove it from
     * the staging area if it is already there (as can happen when a file is
     * changed, added, and then changed back to it’s original version). The file
     * will no longer be staged for removal (see gitlet rm), if it was at the
     * time of the command.
     */
    public static void add(String fileName) {
        File tempFile = join(CWD, fileName);
        if (!tempFile.exists()) { //Checks if file exists already
            System.out.println("File does not exist");
            return;
        }
        StagingArea sa = StagingArea.fromFile();
        if (!Blobs.getFileDir().exists()) {
            Blobs.getFileDir().mkdir();
        }
        Blobs newBlob = new Blobs(fileName);
        if (sa.getAdditionMap().containsKey(fileName)) {
            String newSHA1code = newBlob.getFileSHA1code();
            String blobSHA1code = sa.getAdditionMap().get(fileName);
            Blobs oldBlob = Blobs.fromFile(blobSHA1code);
            String oldSHA1code = oldBlob.getFileSHA1code();
            if (newSHA1code.equals(oldSHA1code)) {
                return;
            }
        }
        if (sa.getDeletionMap().containsKey(fileName)) {
            sa.getDeletionMap().remove(fileName);
            sa.saveStagingArea();
        }
        newBlob.saveBlob();
        Trees tree = Trees.fromFile();
        Commit currentCommit = Commit.fromFile(tree.getHead());
        HashMap<String, String> commitMap = currentCommit.getCommitMap();
        if (commitMap.get(fileName) != null
                && commitMap.get(fileName).equals(newBlob.getBlobSHA1code())) {
            return;
        }
        if (!sa.getDeletionMap().containsKey(fileName)) {
            sa.addToAddition(fileName, newBlob.getBlobSHA1code());
        }
        sa.saveStagingArea();
    }

    /**
     * Saves a snapshot of tracked files in the current commit and
     * staging area so they can be restored at a later time, creating
     * a new commit. The commit is said to be tracking the saved files.
     * By default, each commit’s snapshot of files will be exactly the
     * same as its parent commit’s snapshot of files; it will keep versions
     * of files exactly as they are, and not update them. A commit will only
     * update the contents of files it is tracking that have been staged
     * for addition at the time of commit, in which case the commit will
     * now include the version of the file that was staged instead of the
     * version it got from its parent. A commit will save and start tracking
     * any files that were staged for addition but weren’t tracked by its
     * parent. Finally, files tracked in the current commit may be untracked
     * in the new commit as a result being staged for removal by the rm command
     * (below).
     */
    public static void commit(String message) {
        StagingArea sa = StagingArea.fromFile();
        if (sa.getAdditionMap().isEmpty() && sa.getDeletionMap().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        String timestamp = Commit.getDate();
        Trees tree = Trees.fromFile();
        String master = tree.getHead();
        Commit newCommit = new Commit(message, timestamp, master);
        HashMap<String, String> additionMap = sa.getAdditionMap();
        HashMap<String, String> commitMap = newCommit.getCommitMap();
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        for (String key : currentCommit.getCommitMap().keySet()) {
            commitMap.put(key, currentCommit.getCommitMap().get(key));
        }
        for (Map.Entry<String, String> set : additionMap.entrySet()) {
            String key = set.getKey();
            commitMap.put(key, set.getValue());
        }
        HashMap<String, String> deletionMap = sa.getDeletionMap();
        for (Map.Entry<String, String> set : deletionMap.entrySet()) {
            String key = set.getKey();
            commitMap.remove(key);
        }
        sa.clear();
        sa.saveStagingArea();
        newCommit.saveCommmit();
        String sha1code = newCommit.getCommitSHA1code();
        tree.getAllCommitsMap().put(sha1code, newCommit);
        if (tree.getHeadName().contains("master")) {
            tree.setMaster(sha1code);
        }
        tree.setHead(sha1code);
        tree.getBranchHolder().put(tree.getHeadName(), tree.getHead());
        tree.saveTree();
    }

    /**
     * Commit for merges.
     */
    public static void mergeCommit(String message, String parent2) {
        StagingArea sa = StagingArea.fromFile();
        if (sa.getAdditionMap().isEmpty() && sa.getDeletionMap().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        String timestamp = Commit.getDate();
        Trees tree = Trees.fromFile();
        String master = tree.getHead();
        String parent2sha1 = tree.getBranchHolder().get(parent2);
        Commit newCommit = new Commit(message, timestamp, master, parent2sha1);
        HashMap<String, String> additionMap = sa.getAdditionMap();
        HashMap<String, String> commitMap = newCommit.getCommitMap();
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        for (String key : currentCommit.getCommitMap().keySet()) {
            commitMap.put(key, currentCommit.getCommitMap().get(key));
        }
        for (Map.Entry<String, String> set : additionMap.entrySet()) {
            String key = set.getKey();
            commitMap.put(key, set.getValue());
        }
        HashMap<String, String> deletionMap = sa.getDeletionMap();
        for (Map.Entry<String, String> set : deletionMap.entrySet()) {
            String key = set.getKey();
            commitMap.remove(key);
        }
        sa.clear();
        sa.saveStagingArea();
        newCommit.saveCommmit();
        String sha1code = newCommit.getCommitSHA1code();
        tree.getAllCommitsMap().put(sha1code, newCommit);
        if (tree.getHeadName().contains("master")) {
            tree.setMaster(sha1code);
        }
        tree.setHead(sha1code);
        tree.getBranchHolder().put(tree.getHeadName(), tree.getHead());
        tree.saveTree();
    }

    /**
     * Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file
     * from the working directory if the user has not already done so (do not
     * remove it unless it is tracked in the current commit).
     */
    public static void remove(String fileName) {
        StagingArea sa = StagingArea.fromFile();
        Trees tree = Trees.fromFile();
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        if (!(sa.getAdditionMap().containsKey(fileName))
                && !(currentCommit.getCommitMap().containsKey(fileName))) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (sa.getAdditionMap().containsKey(fileName)) {
            sa.getAdditionMap().remove(fileName);
        }
        if (currentCommit.getCommitMap().containsKey(fileName)) {
            String blobSHA1code = currentCommit.getCommitMap().get(fileName);
            Blobs oldBlob = Blobs.fromFile(blobSHA1code);
            sa.addToDeletion(fileName, oldBlob.getBlobSHA1code());
            File tempFile = join(CWD, fileName);
            restrictedDelete(tempFile);
        }
        sa.saveStagingArea();
    }

    /**
     * Starting at the current head commit, display information about each
     * commit backwards along the commit tree until the initial commit,
     * following the first parent commit links, ignoring any second parents
     * found in merge commits. (In regular Git, this is what you get with git
     * log --first-parent). This set of commit nodes is called the commit’s
     * history. For every node in this history, the information it should
     * display is the commit id, the time the commit was made, and the commit
     * message.
     */
    public static void log() {
        Trees tree = Trees.fromFile();
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        currentCommit.getLogMessage();
        String parent = currentCommit.getParent();
        while (parent != null) {
            Commit parentCommit = tree.getCommit(parent);
            parentCommit.getLogMessage();
            parent = parentCommit.getParent();
        }
    }

    /**
     * Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     */
    public static void globalLog() {
        List<String> allCommits = plainFilenamesIn(COMMIT_DIR);
        for (String commit : allCommits) {
            Commit currentCommit = Commit.fromFile(commit);
            currentCommit.getLogMessage();
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids out
     * on separate lines. The commit message is a single operand; to indicate
     * a multiword message, put the operand in quotation marks, as for the commit
     * command below.
     */
    public static void find(String commitMessage) {
        boolean exists = false;
        List<String> allCommits = plainFilenamesIn(COMMIT_DIR);
        for (String commit : allCommits) {
            Commit currentCommit = Commit.fromFile(commit);
            if ((currentCommit.getCommitMessage()).equals(commitMessage)) {
                exists = true;
                System.out.println(currentCommit.getSha1code());
            }
        }
        if (!exists) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * Displays what branches currently exist, and marks the current
     * branch with a *. Also displays what files have been staged
     * for addition or removal.
     */
    public static void status() {
        System.out.println("=== Branches ===");
        Trees tree = Trees.fromFile();
        Set<String> branchHolderSet = tree.getBranchHolder().keySet();
        List<String> branchHolderList = new ArrayList<>(branchHolderSet);
        Collections.sort(branchHolderList);
        for (String branches : branchHolderList) {
            if (branches.contains(tree.getHeadName())) {
                System.out.println("*" + branches);
            } else {
                System.out.println(branches);
            }
        }
        StagingArea sa = StagingArea.fromFile();
        System.out.println("\n" + "=== Staged Files ===");
        HashMap<String, String> additionMap = sa.getAdditionMap();
        Set<String> addSet = additionMap.keySet();
        List<String> tempAddList = new ArrayList<>(addSet);
        Collections.sort(tempAddList);
        for (String file : tempAddList) {
            System.out.println(file);
        }
        System.out.println("\n" + "=== Removed Files ===");
        HashMap<String, String> deletionMap = sa.getDeletionMap();
        Set<String> deleteSet = deletionMap.keySet();
        List<String> tempDelList = new ArrayList<>(deleteSet);
        Collections.sort(tempDelList);
        for (String file : tempDelList) {
            System.out.println(file);
        }
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        HashMap<String, String> commitMap = currentCommit.getCommitMap();
        List<String> allFiles = plainFilenamesIn(CWD);
        Collections.sort(allFiles);
        System.out.println("\n" + "=== Modifications Not Staged For Commit ===");
        for (String files : allFiles) {
            if (!(files.contains(".txt"))) {
                continue;
            }
            File fileLook = join(Repository.CWD, files);
            byte[] contents = readContents(fileLook);
            String fileSHA1code = sha1(contents);
            if (commitMap.containsKey(files) && !additionMap.containsKey(files)
                    && !deletionMap.containsKey(files)) {
                Blobs tempBlob = Blobs.fromFile(commitMap.get(files));
                String fileSha1 = tempBlob.getFileSHA1code();
                if (!fileSHA1code.equals(fileSha1)) {
                    System.out.println(files + " (modified)");
                }
            }
            if (additionMap.containsKey(files)) {
                Blobs tempBlob = Blobs.fromFile(additionMap.get(files));
                String fileSha1 = tempBlob.getFileSHA1code();
                if (!(fileSHA1code.equals(fileSha1))) {
                    System.out.println(files + " (modified)");
                }
            }
            if (additionMap.containsKey(files) && !fileLook.exists()) {
                System.out.println(files + " (deleted)");
            }
        }
        Set<String> keySet = commitMap.keySet();
        for (String files : keySet) {
            File fileLook = join(Repository.CWD, files);
            if (!fileLook.exists() && !sa.getDeletionMap().containsKey(files)) {
                System.out.println(files + " (deleted)");
            }
        }
        System.out.println("\n" + "=== Untracked Files ===");
        for (String files : allFiles) {
            if (!(additionMap.containsKey(files)) && !(commitMap.containsKey(files))
                    && files.contains(".txt")) {
                System.out.println(files);
            }
        }
    }

    /**
     * Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version of
     * the file that’s already there if there is one. The new version of
     * the file is not staged.
     */
    public static void checkout1(String filename) {
        Trees tree = Trees.fromFile();
        String currentCommitID = tree.getHead();
        Commit currentCommit = Commit.fromFile(currentCommitID);
        HashMap<String, String> commitMap = currentCommit.getCommitMap();
        if (!commitMap.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobSHA1code = commitMap.get(filename);
        Blobs currentBlob = Blobs.fromFile(blobSHA1code);
        currentBlob.checkoutRewrite(filename);
    }

    /**
     * Takes the version of the file as it exists in the commit with the
     * given id, and puts it in the working directory, overwriting the version
     * of the file that’s already there if there is one. The new version of the
     * file is not staged.
     */
    public static void checkout2(String commitID, String filename) {
        Trees tree = Trees.fromFile();
        String realID = abbreviated(commitID);
        HashMap<String, Commit> allCommits = tree.getAllCommitsMap();
        if (!allCommits.containsKey(realID) || realID.equals("false")) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit currentCommit = Commit.fromFile(realID);
        HashMap<String, String> commitMap = currentCommit.getCommitMap();
        if (!commitMap.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobSHA1code = commitMap.get(filename);
        Blobs currentBlob = Blobs.fromFile(blobSHA1code);
        currentBlob.checkoutRewrite(filename);
    }

    /**
     * Takes all files in the commit at the head of the given branch, and
     * puts them in the working directory, overwriting the versions of the
     * files that are already there if they exist. Also, at the end of this
     * command, the given branch will now be considered the current branch
     * (HEAD). Any files that are tracked in the current branch but are not
     * present in the checked-out branch are deleted. The staging area is
     * cleared, unless the checked-out branch is the current branch.
     */
    public static void checkout3(String branchName) {
        Trees tree = Trees.fromFile();
        if (!(tree.getBranchHolder().containsKey(branchName))) {
            System.out.println("No such branch exists.");
            return;
        }

        if (branchName.equals(tree.getHeadName())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        List<String> allFiles = plainFilenamesIn(CWD);
        Commit headCommit = Commit.fromFile(tree.getHead());
        HashMap<String, String> headCommitMap = headCommit.getCommitMap();
        String commitSha1 = tree.getBranchHolder().get(branchName);
        tree.setHead(commitSha1);
        tree.setHeadName(branchName);
        Commit currentCommit = Commit.fromFile(commitSha1);
        HashMap<String, String> commitMap = currentCommit.getCommitMap();
        for (String files : allFiles) {
            if (!(headCommitMap.containsKey(files)) && files.contains(".txt")
                    && commitMap.containsKey(files)) {
                System.out.println("There is an untracked file in the way; delete it,"
                        + " or add and commit it first.");
                return;
            }
        }
        for (String files : headCommitMap.keySet()) {
            if (!commitMap.containsKey(files) && files.contains(".txt")) {
                remove(files);
            }
        }
        for (String filename : commitMap.keySet()) {
            if (filename.contains(".txt")) {
                String blobSHA1code = commitMap.get(filename);
                Blobs currentBlob = Blobs.fromFile(blobSHA1code);
                currentBlob.checkoutRewrite(filename);
            }
        }
        StagingArea sa = StagingArea.fromFile();
        sa.clear();
        sa.saveStagingArea();
        tree.saveTree();
    }

    /**
     * Creates a new branch with the given name, and points it at the current
     * head commit. A branch is nothing more than a name for a reference
     * (a SHA-1 identifier) to a commit node. This command does NOT immediately
     * switch to the newly created branch (just as in real Git). Before you ever
     * call branch, your code should be running with a default branch
     * called master.
     */
    public static void branch(String branchName) {
        Trees tree = Trees.fromFile();
        if (tree.getBranchHolder().containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        tree.getBranchHolder().put(branchName, tree.getHead());
        tree.saveTree();
    }

    /**
     * Deletes the branch with the given name. This only means to delete the
     * pointer associated with the branch; it does not mean to delete all commits
     * that were created under the branch, or anything like that.
     */
    public static void removeBranch(String branchName) {
        Trees tree = Trees.fromFile();
        if (!(tree.getBranchHolder().containsKey(branchName))) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if ((branchName).equals(tree.getHeadName())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        tree.getBranchHolder().remove(branchName);
        tree.saveTree();
    }

    /**
     * Checks out all the files tracked by the given commit. Removes tracked files
     * that are not present in that commit. Also moves the current branch’s head to
     * that commit node. See the intro for an example of what happens to the head
     * pointer after using reset. The [commit id] may be abbreviated as for checkout.
     * The staging area is cleared. The command is essentially checkout of an arbitrary
     * commit that also changes the current branch head.
     */
    public static void reset(String commitID) {
        String realID = abbreviated(commitID);
        if (realID.contains("false")) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit currentCommit = Commit.fromFile(realID);
        HashMap<String, String> currentCommitMap = currentCommit.getCommitMap();
        List<String> allFiles = plainFilenamesIn(CWD);
        Collections.sort(allFiles);
        Trees tree = Trees.fromFile();
        Commit headCommit = Commit.fromFile(tree.getHead());
        HashMap<String, String> headCommitMap = headCommit.getCommitMap();
        for (String files : allFiles) {
            if (!(headCommitMap.containsKey(files)) && files.contains(".txt")
                    && currentCommitMap.containsKey(files)) {
                System.out.println("There is an untracked file in the way; delete it,"
                        + " or add and commit it first.");
                return;
            }
        }
        for (String files : currentCommitMap.keySet()) {
            checkout2(realID, files);
        }
        for (String files : allFiles) {
            if (files.contains(".txt")) {
                if (!(currentCommitMap.containsKey(files))
                        && (headCommitMap.containsKey(files))) {
                    remove(files);
                }
            }
        }
        tree.setHead(realID);
        tree.getBranchHolder().put(tree.getHeadName(), tree.getHead());
        tree.saveTree();
        StagingArea sa = StagingArea.fromFile();
        sa.clear();
        sa.saveStagingArea();
    }

    /**
     * Helper function that finds commit ID based on abbreviations.
     */
    private static String abbreviated(String commitID) {
        Trees tree = Trees.fromFile();
        for (String commits : tree.getAllCommitsMap().keySet()) {
            if (commits.contains(commitID)) {
                return commits;
            }
        }
        return "false";
    }

    /**
     * Checks merge conditions.
     */
    private static boolean mergeConditions(String branchName) {
        Trees tree = Trees.fromFile();
        StagingArea sa = StagingArea.fromFile();
        if (!sa.getAdditionMap().isEmpty() || !sa.getDeletionMap().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!tree.getBranchHolder().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (tree.getHeadName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }

    /**
     * Checks merge conditions.
     */
    private static boolean mergeConditions2(String branchName) {
        Trees tree = Trees.fromFile();
        Commit givenBranch = Commit.fromFile(tree.getBranchHolder().get(branchName));
        Commit currentBranch = Commit.fromFile(tree.getBranchHolder().get(tree.getHeadName()));
        Commit splitPoint = getSplitPoint(givenBranch, currentBranch);
        HashMap<String, String> givenBranchMap = givenBranch.getCommitMap();
        HashMap<String, String> currentBranchMap = currentBranch.getCommitMap();
        List<String> allFiles = plainFilenamesIn(CWD);
        for (String files : allFiles) {
            if (!(currentBranchMap.containsKey(files)) && files.contains(".txt")
                    && givenBranchMap.containsKey(files)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return true;
            }
        }
        if (splitPoint.getSha1code().equals(givenBranch.getSha1code())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return true;
        }
        if (splitPoint.getSha1code().equals(currentBranch.getSha1code())) {
            checkout3(branchName);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    /**
     * Merges files from the given branch into the current branch.
     */
    public static void merge(String branchName) {
        Trees tree = Trees.fromFile();
        StagingArea sa = StagingArea.fromFile();
        if (mergeConditions(branchName)) {
            return;
        }
        Commit givenBranch = Commit.fromFile(tree.getBranchHolder().get(branchName));
        Commit currentBranch = Commit.fromFile(tree.getBranchHolder().get(tree.getHeadName()));
        Commit splitPoint = getSplitPoint(givenBranch, currentBranch);
        HashMap<String, String> givenBranchMap = givenBranch.getCommitMap();
        HashMap<String, String> currentBranchMap = currentBranch.getCommitMap();
        HashMap<String, String> splitPointMap = splitPoint.getCommitMap();
        if (mergeConditions2(branchName)) {
            return;
        }
        boolean conflict = false;
        for (String files : givenBranchMap.keySet()) {
            String givenCommitSha1 = givenBranchMap.get(files);
            String currentCommitSha1 = currentBranchMap.get(files);
            String splitCommitSha1 = splitPointMap.get(files);
            if (givenCommitSha1 == null) {
                givenCommitSha1 = "";
            }
            if (currentCommitSha1 == null) {
                currentCommitSha1 = "";
            }
            if (splitCommitSha1 == null) {
                splitCommitSha1 = "";
            }
            if (!(givenCommitSha1).equals(splitCommitSha1)) {
                if (splitCommitSha1.equals("") && currentCommitSha1.equals("")
                        && !givenCommitSha1.equals("")) { // Condition 5
                    checkout2(givenBranch.getCommitSHA1code(), files);
                    add(files);
                    sa.addToAddition(files, givenCommitSha1);
                    continue;
                } else if (splitCommitSha1.equals(currentCommitSha1)
                        && !givenCommitSha1.equals("")) { // Condition 1
                    checkout2(givenBranch.getCommitSHA1code(), files);
                    add(files);
                    sa.addToAddition(files, givenCommitSha1);
                    continue;
                } else if (givenCommitSha1.equals(currentCommitSha1)) { // Condition 3
                    continue;
                } else if (!givenCommitSha1.equals(currentCommitSha1)) { // Condition 8
                    String sha1Blob = mergeConflict(givenBranch, currentBranch, files);
                    sa.addToAddition(files, sha1Blob);
                    conflict = true;
                    continue;
                }
            } else if (currentCommitSha1.equals("")) { // Condition 7 (and 4)
                continue;
            }
        }
        sa.saveStagingArea();
        if (mergeLittle(branchName) || conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        StagingArea saNew = StagingArea.fromFile();
        saNew.saveStagingArea();
        mergeCommit("Merged " + branchName + " into " + tree.getHeadName() + ".", branchName);
    }

    /**
     * Merge conditions part2.
     */
    public static boolean mergeLittle(String branchName) {
        Trees tree = Trees.fromFile();
        StagingArea sa = StagingArea.fromFile();
        Commit givenBranch = Commit.fromFile(tree.getBranchHolder().get(branchName));
        Commit currentBranch = Commit.fromFile(tree.getBranchHolder().get(tree.getHeadName()));
        Commit splitPoint = getSplitPoint(givenBranch, currentBranch);
        HashMap<String, String> givenBranchMap = givenBranch.getCommitMap();
        HashMap<String, String> currentBranchMap = currentBranch.getCommitMap();
        HashMap<String, String> splitPointMap = splitPoint.getCommitMap();
        boolean conflict = false;
        for (String files : splitPointMap.keySet()) {
            String givenCommitSha1 = givenBranchMap.get(files);
            String currentCommitSha1 = currentBranchMap.get(files);
            String splitCommitSha1 = splitPointMap.get(files);
            if (givenCommitSha1 == null) {
                givenCommitSha1 = "";
            }
            if (currentCommitSha1 == null) {
                currentCommitSha1 = "";
            }
            if (splitCommitSha1 == null) {
                splitCommitSha1 = "";
            }
            if (!splitCommitSha1.equals(currentCommitSha1)
                    && splitCommitSha1.equals(givenCommitSha1)) { // Condition 2
                continue;
            }
            if (splitCommitSha1.equals(currentCommitSha1)
                    && givenCommitSha1.equals("")) { //Condition 6
                remove(files);
                sa.addToDeletion(files, givenCommitSha1);
                continue;
            }
            if (splitCommitSha1.equals(givenCommitSha1)
                    && currentCommitSha1.equals("")) { // Condition 7
                continue;
            }
            if (givenCommitSha1.equals(currentCommitSha1)) { // Condition 3
                continue;
            }
            if (!givenCommitSha1.equals(currentCommitSha1)) { // Condition 8 (del file)
                if (!givenCommitSha1.equals(splitCommitSha1)
                        || !currentCommitSha1.equals(splitCommitSha1)) {
                    if (givenCommitSha1.equals("") || currentCommitSha1.equals("")) {
                        String sha1Blob = mergeConflict(givenBranch, currentBranch, files);
                        sa.addToAddition(files, sha1Blob);
                        conflict = true;
                        continue;
                    }
                }
            }
        }
        sa.saveStagingArea();
        return conflict;
    }

    /**
     * Finds split point.
     */
    private static Commit getSplitPoint(Commit commit1, Commit commit2) {
        ArrayList<String> bfsCommit1 = BFS.bfs(commit1.getCommitSHA1code());
        ArrayList<String> bfsCommit2 = BFS.bfs(commit2.getCommitSHA1code());
        for (String commits : bfsCommit2) {
            if (bfsCommit1.contains(commits)) {
                return Commit.fromFile(commits);
            }
        }
        return null;
    }

    /**
     * Conflict in merge and returns string of sha1code.
     */
    private static String mergeConflict(Commit commit1, Commit commit2, String filename) {
        String readContents1;
        String readContents2;
        String blob1Sha1code = commit1.getCommitMap().get(filename);
        String blob2Sha1code = commit2.getCommitMap().get(filename);
        if (blob1Sha1code != null) {
            Blobs blob1 = Blobs.fromFile(blob1Sha1code);
            String file1Sha1 = blob1.getFileSHA1code();
            File contents1 = Utils.join(Blobs.getFileDir(), file1Sha1);
            readContents1 = readContentsAsString(contents1);
        } else {
            readContents1 = "";
        }
        if (blob2Sha1code != null) {
            Blobs blob2 = Blobs.fromFile(blob2Sha1code);
            String file2Sha1 = blob2.getFileSHA1code();
            File contents2 = Utils.join(Blobs.getFileDir(), file2Sha1);
            readContents2 = readContentsAsString(contents2);

        } else {
            readContents2 = "";
        }
        File tempFile = join(CWD, filename);
        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeContents(tempFile, "<<<<<<< HEAD\n" + readContents2
                + "=======\n" + readContents1 + ">>>>>>>\n");
        StagingArea sa = StagingArea.fromFile();
        Blobs newBlob = new Blobs(filename);
        newBlob.saveBlob();
        sa.getAdditionMap().put(filename, newBlob.getBlobSHA1code());
        sa.saveStagingArea();
        return newBlob.getBlobSHA1code();
    }

    public static void addRemote(String remoteName, String remotePath) {
        Remote remote = Remote.fromFile();
        if (remote.getRemoteMap().containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        String fs = File.separator;
        String newRemotePath = "";
        for (int i = 0; i < remotePath.length(); i++) {
            if (remotePath.substring(i, i + 1).equals("/")) {
                newRemotePath += fs;
            } else {
                newRemotePath += remotePath.substring(i, i + 1);
            }
        }
        remote.getRemoteMap().put(remoteName, newRemotePath);
        remote.saveRemote();
    }

    public static void rmRemote(String remoteName) {
        Remote remote = Remote.fromFile();
        if (!remote.getRemoteMap().containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remote.getRemoteMap().remove(remoteName);
        remote.saveRemote();
    }

    public static void push(String remoteName, String remoteBranchName) {
        Remote remote = Remote.fromFile();
        File directory = new File(remote.getRemoteMap().get(remoteName));


        if (!directory.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        if (!inHistory(remoteName, remoteBranchName)) {
            System.out.println("Please pull down remote changes before pushing.");
            return;
        }
        Trees tree = Trees.fromFile();
        Commit currentCommit = Commit.fromFile(tree.getHead());
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        Trees remoteTree = Utils.readObject(join(remoteObjects, "tree"), Trees.class);
        String remoteCommitID = remoteTree.getBranchHolder().get(remoteBranchName);
        File remoteCommits = join(remoteObjects, ".commits");
        Commit remoteCommit = Utils.readObject(join(remoteCommits, remoteCommitID), Commit.class);
        remoteTree.setHead(currentCommit.getCommitSHA1code());
        remoteTree.saveRemoteTree(join(remoteObjects, "tree"));
        while (!currentCommit.getSha1code().equals(remoteCommit.getSha1code())) {
            pushCommits(currentCommit.getCommitSHA1code(), remoteName);
            for (String filename : currentCommit.getCommitMap().keySet()) {
                String blobs = currentCommit.getCommitMap().get(filename);
                pushBlobs(blobs, remoteName);
                Blobs newBlobs = Utils.readObject(join(BLOB_DIR, blobs), Blobs.class);
                pushContents(newBlobs.getFileSHA1code(), remoteName);
            }
            currentCommit = Utils.readObject(join(COMMIT_DIR,
                    currentCommit.getParent()), Commit.class);
        }
    }

    public static void pushCommits(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteCommits = join(remoteObjects, ".commits");
        Commit commit = Commit.fromFile(filename);
        File addCommit = join(remoteCommits, filename);
        if (!addCommit.exists()) {
            try {
                addCommit.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(addCommit, commit);
        Trees remoteTree = Utils.readObject(join(remoteObjects, "tree"), Trees.class);
        remoteTree.getAllCommitsMap().put(filename, commit);
        remoteTree.saveRemoteTree(join(remoteObjects, "tree"));
    }

    public static void pushBlobs(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteBlobs = join(remoteObjects, ".blobs");
        Blobs blobs = Blobs.fromFile(filename);
        File addBlob = join(remoteBlobs, filename);
        if (!addBlob.exists()) {
            try {
                addBlob.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(addBlob, blobs);
    }

    public static void pushContents(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteContents = join(remoteObjects, ".fileContents");
        File addContents = join(remoteContents, filename);
        File contents = join(Blobs.getFileDir(), filename);
        if (!addContents.exists()) {
            try {
                addContents.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeContents(addContents, readContentsAsString(contents));
    }

    private static boolean inHistory(String remoteName, String remoteBranchName) {
        Remote remote = Remote.fromFile();
        Trees tree = Trees.fromFile();
        String commitID = tree.getHead();
        Commit myCommit = Commit.fromFile(commitID);
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        Trees remoteTree = Utils.readObject(join(remoteObjects, "tree"), Trees.class);
        String remoteCommitID = remoteTree.getBranchHolder().get(remoteBranchName);
        File remoteCommits = join(remoteObjects, ".commits");
        Commit remoteCommit = Utils.readObject(join(remoteCommits, remoteCommitID), Commit.class);
        ArrayList<String> bfsCommit1 = BFS.bfs(myCommit.getCommitSHA1code());
        if (bfsCommit1.contains(remoteCommit.getCommitSHA1code())) {
            return true;
        }
        return false;
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        if (fetchFailure(remoteName, remoteBranchName)) {
            return;
        }
        File remoteCommits = join(remoteObjects, ".commits");
        Trees remoteTree = Utils.readObject(join(remoteObjects, "tree"), Trees.class);
        String remCurrCommitSha1 = remoteTree.getBranchHolder().get(remoteBranchName);
        Trees tree = Trees.fromFile();
        tree.getBranchHolder().put(remoteName + "/" + remoteBranchName,
                remCurrCommitSha1);
        tree.saveTree();
        Commit remCurrCommit = Utils.readObject(join(remoteCommits,
                remCurrCommitSha1), Commit.class);
        File remoteBlobs = join(remoteObjects, ".blobs");
        while (remCurrCommit != null) {
            fetchCommits(remCurrCommit.getCommitSHA1code(), remoteName);
            for (String filename : remCurrCommit.getCommitMap().keySet()) {
                String blobs = remCurrCommit.getCommitMap().get(filename);
                fetchBlobs(blobs, remoteName);
                Blobs newBlobs = Utils.readObject(join(remoteBlobs, blobs), Blobs.class);
                fetchContents(newBlobs.getFileSHA1code(), remoteName);
            }
            if (remCurrCommit.getParent() != null) {
                remCurrCommit = Utils.readObject(join(remoteCommits,
                        remCurrCommit.getParent()), Commit.class);
            } else {
                remCurrCommit = null;
            }
        }
    }

    public static void fetchCommits(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteCommits = join(remoteObjects, ".commits");
        Commit newCommit = Utils.readObject(join(remoteCommits, filename), Commit.class);
        File addCommit = join(COMMIT_DIR, filename);
        if (!addCommit.exists()) {
            try {
                addCommit.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(addCommit, newCommit);
        Trees tree = Trees.fromFile();
        tree.getAllCommitsMap().put(filename, newCommit);
        tree.saveTree();
    }

    public static void fetchBlobs(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteBlobs = join(remoteObjects, ".blobs");
        Blobs newBlobs = Utils.readObject(join(remoteBlobs, filename), Blobs.class);
        File addBlob = join(BLOB_DIR, filename);
        if (!addBlob.exists()) {
            try {
                addBlob.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeObject(addBlob, newBlobs);
    }

    public static void fetchContents(String filename, String remoteName) {
        Remote remote = Remote.fromFile();
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        File remoteContents = join(remoteObjects, ".fileContents");
        File newContents = join(remoteContents, filename);
        File addContents = join(join(Repository.OBJECT_DIR, ".fileContents"),
                filename);
        if (!addContents.exists()) {
            try {
                addContents.createNewFile();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        Utils.writeContents(addContents, readContents(newContents));
    }

    public static boolean fetchFailure(String remoteName, String remoteBranchName) {
        Remote remote = Remote.fromFile();
        File directory = new File(remote.getRemoteMap().get(remoteName));
        if (!directory.exists()) {
            System.out.println("Remote directory not found.");
            return true;
        }
        File remoteRepository = new File(remote.getRemoteMap().get(remoteName));
        File remoteObjects = join(remoteRepository, ".objects");
        Trees remoteTree = Utils.readObject(join(remoteObjects, "tree"), Trees.class);
        if (!remoteTree.getBranchHolder().containsKey(remoteBranchName)) {
            System.out.println("That remote does not have that branch.");
            return true;
        }
        return false;
    }

    public static void pull(String remoteName, String remoteBranchName) {
        if (fetchFailure(remoteName, remoteBranchName)) {
            return;
        }
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName);
    }
}
