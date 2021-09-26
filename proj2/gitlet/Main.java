package gitlet;
import gitlet.*;

import java.io.File;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Adam Woods
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if(!Utils.join(System.getProperty("user.dir"), ".gitlet").exists()
                && !args[0].equals("init")) {
            System.out.println("Not in an initialized Gitlet directory.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                checkArgs(args, 1);
                Repository.init();
                break;
            case "add":
                checkArgs(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                if(args.length == 1 || args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                checkArgs(args, 2);
                Repository.commit(args[1]);
                break;
            case "checkout":
                checkoutHelper(args);
                break;
            case "rm":
                checkArgs(args, 2);
                Repository.remove(args[1]);
                break;
            case "log":
                checkArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                checkArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                checkArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                checkArgs(args, 1);
                Repository.status();
                break;
            case "branch":
                checkArgs(args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                checkArgs(args, 2);
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                checkArgs(args, 2);
                Repository.reset(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
        }
    }

    public static void checkArgs(String[] args, int n) {
        if(args.length != n) {
            throw new RuntimeException("Incorrect number of arguments.");
        }
    }

    public static void checkoutHelper(String[] args) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            Repository.checkoutFile(args[2]);
        }
        else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            Repository.checkoutCommit(args[1], args[3]);
        }
        else if (args.length == 2) {
            Repository.checkoutBranch(args[1]);
        }
    }
}
