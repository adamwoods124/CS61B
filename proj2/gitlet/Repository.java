package gitlet;

import java.io.File;
import java.nio.file.Files;
import java.util.List;


import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  Establishes overall skeleton of the system, creating the file and directories where items are stored
 *  Uses helper methods from other classes to be the main driver of the system
 *
 *  @author Adam Woods
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static File COMMITS = join(GITLET_DIR, "commits");
    public static File BRANCHES = join(COMMITS, "branches");
    public static File MASTER = join(BRANCHES, "master");
    public static File BLOBS = join(GITLET_DIR, "blobs");
    public static File STAGE = join(GITLET_DIR, "stage");
    public static File ADD = join(STAGE, "add");
    public static File REMOVE = join(STAGE, "rm");
    public static String branch = "master";

    /* TODO: fill in the rest of this class. */

    /**
     * Initializes empty gitlet repository with an empty commit
     * Throws exception if a gitlet repo is already initialized
     * .gitlet
     *      COMMITS
     *          commit object files
     *          BRANCHES
     *              HEAD
     *      BLOBS
     *      STAGE
     *          ADD
     *          REMOVE
     */
    public static void init() {
        if(GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        makeRepositories();
        Commit initialCommit = new Commit();
        byte[] serializedInitialCommit = serialize(initialCommit);
        File commitFile = join(COMMITS, sha1(serializedInitialCommit));
        try {
            commitFile.createNewFile();
        } catch(Exception e) {
            System.out.println("Error creating commit file.");
        }
        writeObject(commitFile, initialCommit);
        try {
            MASTER.createNewFile();
        } catch(Exception e) {}
        writeContents(MASTER, sha1(serializedInitialCommit));
    }

    /**
     * Adds given file to staging area, and creates a blob that saves its contents if one does not already exist.
     * If file with given name already exists in the staging area, it is overwritten
     * @param name gives the name of the file to be added to staging area.
     */
    public static void add(String name) {
        /**
         * TODO Unstage if file is identical to version in previous commit
         */
        File cwdFile = join(CWD, name);
        if(!cwdFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String sha = sha1(readContents(cwdFile));
        if(alreadyCommitted(name, sha)) {
            return;
        }
        File addFile = join(ADD, name);
        if(!addFile.exists()) {
            try {
                addFile.createNewFile();
            } catch (Exception e){}
        }
        writeContents(addFile, sha);
        File blob = join(BLOBS, sha);
        if(blob.exists()) {
            return;
        }
        try {
            blob.createNewFile();
        } catch (Exception e) {
            System.out.println("Error creating blob file.");
        }
        //byte[] b = readContents(cwdFile);
        byte[] b = serialize(readContentsAsString(cwdFile));
        writeContents(blob, b);
    }

    public static void commit(String message) {
        if(ADD.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        File head = join(BRANCHES, branch);
        String headSha = readContentsAsString(head);
        Commit c = new Commit(message, headSha);
        String newHead = sha1(serialize(c));
        File f = join(COMMITS, newHead);
        writeObject(f, c);
        writeContents(head, newHead);
        for(File z : ADD.listFiles()) {
            if(!z.isDirectory()) {
                z.delete();
            }
        }
    }

    public static void checkoutFile(String fileName) {
        // TODO check if files can be serialized for storage
        File cur = join(CWD, fileName);
        Commit headCommit = getHead();
        if(!headCommit.map.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobName = headCommit.map.get(fileName);
        //byte[] blobContents = readContents(join(BLOBS, blobName));
        String blobContents = readObject(join(BLOBS, blobName), String.class);
        writeContents(cur, blobContents);
    }

    public static void checkoutCommit(String commit, String fileName) {

    }

    public static void checkoutBranch(String branch) {

    }

    public static void makeRepositories() {
        if(GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        BRANCHES.mkdir();
        BLOBS.mkdir();
        STAGE.mkdir();
        ADD.mkdir();
        REMOVE.mkdir();
    }

    public static boolean alreadyCommitted(String name, String sha) {
        Commit c = getHead();
        if(c.map.containsKey(name) && c.map.get(name).equals(sha)) {
            return true;
        }
        return false;
    }

    public static Commit getHead() {
        File head = join(BRANCHES, branch);
        String headSha = readContentsAsString(head);
        Commit c = readObject(join(COMMITS, headSha), Commit.class);
        return c;
    }
}
