package gitlet;
import gitlet.*;

import java.io.File;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Please enter a command.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                checkArgs("add", args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                if(args.length == 1) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                checkArgs("commit", args, 2);
                Repository.commit(args[1]);
                break;
            case "checkout":
                checkoutHelper(args);
                break;
            default:
                System.out.println("No command with that name.");
                System.out.println("Command: " + args);
                break;
        }
        return;
    }

    public static void checkArgs(String cmd, String[] args, int n) {
        if(args.length != n) {
            throw new RuntimeException("Incorrect number of arguments for command " + cmd);
        }
    }

    public static void checkoutHelper(String[] args) {
        if(args.length == 3) {
            Repository.checkoutFile(args[2]);
        }
        else if(args.length == 1) {
            Repository.checkoutCommit(args[1], args[3]);
        }
        else if(args.length == 2) {
            Repository.checkoutBranch(args[1]);
        } else {

        }
    }
}
