package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Jeffrey Huang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (correctOperands(1, args.length)) {
                    return;
                }
                Repository.init();
                break;
            case "add":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                String file = args[1];
                Repository.add(file);
                break;
            case "commit":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                if (args.length != 2) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                String message = args[1];
                if (message.equals("")) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                Repository.commit(message);
                break;
            case "log":
                if (correctOperands(1, args.length) || checkGitlet()) {
                    return;
                }
                Repository.log();
                break;
            case "checkout":
                if (checkGitlet()) {
                    return;
                }
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        System.out.println("Incorrect operands.");
                        break;
                    }
                    Repository.checkout1(args[2]);
                    break;
                }
                if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        break;
                    }
                    Repository.checkout2(args[1], args[3]);
                    break;
                }
                if (args.length == 2) {
                    Repository.checkout3(args[1]);
                    break;
                }
                break;
            case "rm":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.remove(args[1]);
                break;
            case "global-log":
                if (correctOperands(1, args.length) || checkGitlet()) {
                    return;
                }
                Repository.globalLog();
                break;
            case "find":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.find(args[1]);
                break;
            case "status":
                if (correctOperands(1, args.length) || checkGitlet()) {
                    return;
                }
                Repository.status();
                break;
            case "branch":
                if (args.length == 2 && args[1].contains(".")) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                if (checkGitlet()) {
                    return;
                }
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.reset(args[1]);
                break;
            case "merge":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.merge(args[1]);
                break;
            case "add-remote":
                if (correctOperands(3, args.length) || checkGitlet()) {
                    return;
                }
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                if (correctOperands(2, args.length) || checkGitlet()) {
                    return;
                }
                Repository.rmRemote(args[1]);
                break;
            case "fetch":
                if (correctOperands(3, args.length) || checkGitlet()) {
                    return;
                }
                Repository.fetch(args[1], args[2]);
                break;
            case "push":
                if (correctOperands(3, args.length) || checkGitlet()) {
                    return;
                }
                Repository.push(args[1], args[2]);
                break;
            case "pull":
                if (correctOperands(3, args.length) || checkGitlet()) {
                    return;
                }
                Repository.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    public static boolean checkGitlet() {
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return true;
        }
        return false;
    }

    public static boolean correctOperands(int expected, int given) {
        if (expected != given) {
            System.out.println("Incorrect operands.");
            return true;
        }
        return false;
    }
}
