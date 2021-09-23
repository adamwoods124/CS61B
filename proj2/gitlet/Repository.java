package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  Establishes overall skeleton of the system, creating the files
 *  and directories where items are stored
 *  Uses helper methods from other classes to be the main driver of the system
 *
 *  @author Adam Woods
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** Create initial file structure */
    private static File COMMITS = join(GITLET_DIR, "commits");
    private static File BRANCHES = join(COMMITS, "branches");
    private static File MASTER = join(BRANCHES, "master");
    private static File BLOBS = join(GITLET_DIR, "blobs");
    private static File STAGE = join(GITLET_DIR, "stage");
    private static File ADD = join(STAGE, "add");
    private static File REMOVE = join(STAGE, "rm");
    private static String branch = "master";

    /**
     * Create initial file structure
     * .gitlet
     *      blobs
     *          hashed blob files
     *      commits
     *          folders named by first 6 digits of sha
     *              actual sha files
     *      stage
     *          add
     *          rm
     */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists " +
                    "in the current directory.");
            return;
        }
        makeRepositories();


         // Create initial commit and serialize it for storage
        Commit initialCommit = new Commit();
        byte[] serializedInitialCommit = serialize(initialCommit);

        // Create folder with first 2 digits of commit sha as well as file that will hold full sha
        File commitDir = join(COMMITS, sha1(serializedInitialCommit).substring(0, 2));
        commitDir.mkdir();
        File commitFile = join(commitDir, sha1(serializedInitialCommit));
        try {
            commitFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating commit file.");
        }
        // Saving the commit persistently
        writeObject(commitFile, initialCommit);
        try {
            MASTER.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating master file.");
        }
        writeContents(MASTER, sha1(serializedInitialCommit));
    }

    /**
     * Adds given file to staging area, and creates a blob
     * that saves its contents if one does not already exist.
     * If file with given name already exists in the staging area, it is overwritten
     * @param name gives the name of the file to be added to staging area.
     */
    public static void add(String name) {
        // Saves file that will be added and errors if it does not exist
        File cwdFile = join(CWD, name);
        if (!cwdFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        // Clear file from staging area if it was staged for removal
        File checkStaging = join(REMOVE, name);
        if(checkStaging.exists()) {
            checkStaging.delete();
        }
        /** Sha the contents of the file to be added
         * return if it is already saved in the current head commit
         */
        String sha = sha1(readContents(cwdFile));
        if (alreadyCommitted(name, sha)) {
            return;
        }
        // Create file in the add directory if it needs to be added to staging area
        File addFile = join(ADD, name);
        if (!addFile.exists()) {
            try {
                addFile.createNewFile();
            } catch (Exception e){
                System.out.println("Cannot create file.");
            }
        }

        // Write the sha of contents (pointer to the blob where contents are saved) to the file in the add directory
        writeContents(addFile, sha);

        // Get blob contents and write them to file in the add directory
        File blob = join(BLOBS, sha);
        if (blob.exists()) {
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
        // Stop if staging area is empty
        if (ADD.listFiles().length == 0 && REMOVE.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // Get sha of the current head commit, and create a new commit that is a clone of it
        File head = join(BRANCHES, branch);
        String headSha = readContentsAsString(head);
        Commit c = new Commit(message, headSha);

        // Change head pointer to new commit
        String newHead = sha1(serialize(c));
        File shortDir = join(COMMITS, newHead.substring(0, 2));
        if(!shortDir.exists()) {
            shortDir.mkdir();
        }
        File f = join(shortDir, newHead);
        try {
            f.createNewFile();
        } catch(Exception e) {}
        // Save commit persistently
        writeObject(f, c);
        writeContents(head, newHead);

        // Clear staging area
        for(File z : ADD.listFiles()) {
            z.delete();
        }

        for(File z : REMOVE.listFiles()) {
            c.getMap().remove(z.getName());
            z.delete();
        }
    }

    public static void remove(String fileName) {
        File f = join(ADD, fileName);
        Commit c = getHead();
        if(!f.exists() && !c.getMap().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if(f.exists()) {
            f.delete();
        }

        if(c.getMap().containsKey(fileName)) {
            File m = join(REMOVE, fileName);
            try {
                m.createNewFile();
            } catch (Exception e) {}
            writeContents(m, readContentsAsString(join(CWD, fileName)));
            join(CWD, fileName).delete();
        }
    }

    public static void log() {
        Commit c = getHead();
        String sha = readContentsAsString(join(BRANCHES, branch));
        while(c.getParents() != null) {
            if (c.getParents().size() == 1) {
                System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n", sha, c.getDate(), c.getMessage());
            } else {
                System.out.printf("===\ncommit %1$s\nMerge: %2$s %3$s\nDate: %4$s\n%5$s\n\n", sha, c.getDate(), c.getParents().get(0).substring(0, 7), c.getParents().get(1).substring(0, 7), c.getMessage());
            }
            c = getCommit(c.getParents().get(0));
            sha = c.getSha();
        }
        System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n", sha, c.getDate(), c.getMessage());
    }

    public static void globalLog() {
        for(File n : COMMITS.listFiles()) {
            if(!n.getName().equals("branches")) {
                for(File f : n.listFiles()) {
                    Commit c = readObject(f, Commit.class);
                    if(c.getParents() == null || c.getParents().size() == 1) {
                        System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n", c.getSha(), c.getDate(), c.getMessage());
                    } else {
                        System.out.printf("===\ncommit %1$s\nMerge: %2$s %3$s\nDate: %4$s\n%5$s\n\n", c.getSha(), c.getDate(), c.getParents().get(0).substring(0, 7), c.getParents().get(1).substring(0, 7), c.getMessage());
                    }
                }
            }
        }
    }

    public static void find(String message) {
        for(File n : COMMITS.listFiles()) {
            if(!n.getName().equals("branches")) {
                for(File f : n.listFiles()) {
                    Commit c = readObject(f, Commit.class);
                    if(c.getMessage().equals(message)) {
                        System.out.println(c.getSha());
                    }
                }
            }
        }
    }

    public static void status() {
        System.out.println("=== Branches ===");
        for(String s : Objects.requireNonNull(plainFilenamesIn(BRANCHES))) {
            if(branch.equals(s)) {
                System.out.print("*");
            }
            System.out.println(s);
        }
        System.out.println("\n=== Staged Files ===");
        for(String s : Objects.requireNonNull(plainFilenamesIn(ADD))) {
            System.out.println(s);
        }
        System.out.println("\n=== Removed Files ===");
        for(String s : Objects.requireNonNull(plainFilenamesIn(REMOVE))) {
            System.out.println(s);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===");
    }

    public static void checkoutFile(String fileName) {
        File cur = join(CWD, fileName);
        Commit headCommit = getHead();
        if(!headCommit.getMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobName = headCommit.getMap().get(fileName);
        //byte[] blobContents = readContents(join(BLOBS, blobName));
        String blobContents = readObject(join(BLOBS, blobName), String.class);
        writeContents(cur, blobContents);
    }

    public static void checkoutCommit(String commit, String fileName) {
        File cf = join(COMMITS, commit.substring(0, 2));
        if(!cf.exists()) {
            throw new RuntimeException("No commit with that ID exists");
        }
        File cwdFile = join(CWD, fileName);
        if(!cwdFile.exists()) {
            try {
                cwdFile.createNewFile();
            } catch (Exception e) {
                throw new RuntimeException("Error creating file.");
            }
        }
        Commit c = getCommit(commit);
        if(!c.getMap().containsKey(fileName)) {
            throw new RuntimeException("File does not exist in that commit.");
        }
        String blobName = c.getMap().get(fileName);
        String blobContents = readObject(join(BLOBS, blobName), String.class);
        writeContents(cwdFile, blobContents);
    }

    public static void checkoutBranch(String newBranch) {
        if(!join(BRANCHES, newBranch).exists()) {
            throw new IllegalArgumentException("No such branch exists.");
        }
        if(newBranch.equals(branch)) {
            throw new IllegalArgumentException("No need to checkout the current branch.");
        }
        String branchHead = readContentsAsString(join(BRANCHES, newBranch));
        Commit c = getCommit(branchHead);
        for(File f : CWD.listFiles()) {
            if(!c.getMap().containsKey(f.getName())) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        for(File f : CWD.listFiles()) {
           f.delete();
        }
        for(File f : ADD.listFiles()) {
            f.delete();
        }
        for(String s : c.getMap().keySet()) {
            File f = join(CWD, s);
            if(!f.exists()) {
                try {
                    f.createNewFile();
                } catch(Exception e) {
                    System.out.println("Error creating file.");
                }
            }
            String blobName = c.getMap().get(s);
            String blobContents = readObject(join(BLOBS, blobName), String.class);
            writeContents(f, blobContents);
            branch = newBranch;
        }
    }

    public static void branch(String name) {
        File f = join(BRANCHES, name);
        if (f.exists()) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        try {
            f.createNewFile();
        } catch (Exception e) {}
        writeContents(f, getHead().getSha());
    }

    public static void removeBranch(String name) {
        File f = join(BRANCHES, name);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (name.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        f.delete();
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
        if(c.getMap().containsKey(name) && c.getMap().get(name).equals(sha)) {
            return true;
        }
        return false;
    }

    public static Commit getHead() {
        File head = join(BRANCHES, branch);
        String headSha = readContentsAsString(head);
        Commit c = readObject(join(COMMITS, headSha.substring(0, 2), headSha), Commit.class);
        return c;
    }

    public static Commit getCommit(String sha) {
        if(sha.length() < 40) {
            File dir = join(COMMITS, sha.substring(0, 2));
            if (dir.listFiles().length == 1) {
                for (File f : dir.listFiles()) {
                    return readObject(f, Commit.class);
                }
            } else {
                for(File f : dir.listFiles()) {
                    if(f.getName().startsWith(sha)) {
                        return readObject(f, Commit.class);
                    }
                }
                throw new RuntimeException("No commit with that ID.");
            }
        }
        return readObject(join(COMMITS, sha.substring(0, 2), sha), Commit.class);
    }

}
