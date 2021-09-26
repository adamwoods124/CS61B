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
    private static File BranchMapFile = join(COMMITS, "branchmap");
    private static File BLOBS = join(GITLET_DIR, "blobs");
    private static File STAGE = join(GITLET_DIR, "stage");
    private static File ADD = join(STAGE, "add");
    private static File REMOVE = join(STAGE, "rm");
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
            System.out.println("A Gitlet version-control system already exists "
                   + "in the current directory.");
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
            BranchMapFile.createNewFile();
            commitFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating commit file.");
        }
        // Saving the commit persistently
        writeObject(commitFile, initialCommit);
        BranchMap bm = new BranchMap(sha1(serializedInitialCommit));
        writeObject(BranchMapFile, bm);
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
        if (checkStaging.exists()) {
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
            } catch (IOException e) {
                System.out.println("Cannot create file.");
            }
        }

        /** Write the sha of contents (pointer to the blob where contents are saved)
         * to the file in the add directory
         */
        writeContents(addFile, sha);

        // Get blob contents and write them to file in the add directory
        File blob = join(BLOBS, sha);
        if (blob.exists()) {
            return;
        }
        try {
            blob.createNewFile();
        } catch (IOException e) {
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
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        // Get sha of the current head commit, and create a new commit that is a clone of it
        String headSha = bm.headSha();
        Commit c = new Commit(message, headSha);

        // Change head pointer to new commit
        String newHead = sha1(serialize(c));
        File shortDir = join(COMMITS, newHead.substring(0, 2));
        if (!shortDir.exists()) {
            shortDir.mkdir();
        }
        File f = join(shortDir, newHead);
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating file.");
        }
        // Save commit persistently
        writeObject(f, c);
        bm.newCommit(c.getSha());
        writeObject(BranchMapFile, bm);
        // Clear staging area
        for (File z : ADD.listFiles()) {
            z.delete();
        }

        for (File z : REMOVE.listFiles()) {
            c.getMap().remove(z.getName());
            z.delete();
        }
    }

    public static void remove(String fileName) {
        File f = join(ADD, fileName);
        Commit c = getHead();
        if (!f.exists() && !c.getMap().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (f.exists()) {
            f.delete();
        }
        if (c.getMap().containsKey(fileName)) {
            File m = join(REMOVE, fileName);
            if (m.isDirectory()) {
                return;
            }
            try {
                m.createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating file.");
            }
            join(CWD, fileName).delete();
        }
    }

    public static void log() {
        Commit c = getHead();
        String sha = c.getSha();
        while (c.getParents() != null) {
            if (c.getParents().size() == 1) {
                System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n", sha,
                        c.getDate(), c.getMessage());
            } else {
                System.out.printf("===\ncommit %1$s\nMerge: %2$s %3$s\nDate: %4$s\n%5$s\n\n",
                        sha, c.getDate(), c.getParents().get(0).substring(0, 7),
                        c.getParents().get(1).substring(0, 7), c.getMessage());
            }
            c = getCommit(c.getParents().get(0));
            assert c != null;
            sha = c.getSha();
        }
        System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n",
                sha, c.getDate(), c.getMessage());
    }

    public static void globalLog() {
        for (File n : COMMITS.listFiles()) {
            if (!n.getName().equals("branches")) {
                for (File f : n.listFiles()) {
                    Commit c = readObject(f, Commit.class);
                    if (c.getParents() == null || c.getParents().size() == 1) {
                        System.out.printf("===\ncommit %1$s\nDate: %2$s\n%3$s\n\n",
                                c.getSha(), c.getDate(), c.getMessage());
                    } else {
                        System.out.printf("===\ncommit %1$s\nMerge: %2$s %3$s\n" +
                                        "Date: %4$s\n%5$s\n\n",
                                c.getSha(), c.getDate(), c.getParents().get(0).substring(0, 7),
                                c.getParents().get(1).substring(0, 7), c.getMessage());
                    }
                }
            }
        }
    }

    public static void find(String message) {
        boolean found = false;
        for (File n : Objects.requireNonNull(COMMITS.listFiles())) {
            if (!n.getName().equals("branches") && n.list() != null) {
                for (File f : Objects.requireNonNull(n.listFiles())) {
                    Commit c = readObject(f, Commit.class);
                    if (c.getMessage().equals(message)) {
                        System.out.println(c.getSha());
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        System.out.println("=== Branches ===");
        for (String s : bm.getBranchToHead().keySet()) {
            if (bm.getCurBranch().equals(s)) {
                System.out.print("*");
            }
            System.out.println(s);
        }
        System.out.println("\n=== Staged Files ===");
        for (String s : Objects.requireNonNull(plainFilenamesIn(ADD))) {
            System.out.println(s);
        }
        System.out.println("\n=== Removed Files ===");
        for (String s : Objects.requireNonNull(plainFilenamesIn(REMOVE))) {
            System.out.println(s);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===\n");
    }

    public static void checkoutFile(String fileName) {
        File cur = join(CWD, fileName);
        Commit headCommit = getHead();
        if (!headCommit.getMap().containsKey(fileName)) {
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
        if (!cf.exists()) {
            System.out.println("No commit with that ID exists.");
            return;
        }
        File cwdFile = join(CWD, fileName);
        if (!cwdFile.exists()) {
            try {
                cwdFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Error creating file.");
            }
        }
        if (getCommit(commit) == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit c = getCommit(commit);
        if (!c.getMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String blobName = c.getMap().get(fileName);
        String blobContents = readObject(join(BLOBS, blobName), String.class);
        writeContents(cwdFile, blobContents);
    }

    public static void checkoutBranch(String newBranch) {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        if (!bm.getBranchToHead().containsKey(newBranch)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (newBranch.equals(bm.getCurBranch())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        clear();
        bm.changeBranch(newBranch);
        bm.setHead(newBranch, bm.headSha());
        Commit c2 = getHead();
        for (String s : c2.getMap().keySet()) {
            File f = join(CWD, s);
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    System.out.println("Error creating file.");
                }
            }
            String blobName = c2.getMap().get(s);
            String blobContents = readObject(join(BLOBS, blobName), String.class);
            writeContents(f, blobContents);
        }
        writeObject(BranchMapFile, BranchMap.class);
    }

    public static void branch(String name) {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        if (bm.getBranchToHead().containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        bm.getBranchToHead().put(name, bm.headSha());
        writeObject(BranchMapFile, bm);
    }

    public static void removeBranch(String name) {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        if (!bm.getBranchToHead().containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (name.equals(bm.getCurBranch())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        bm.removeBranch(name);
        writeObject(BranchMapFile, bm);
    }
    public static void reset(String commit) {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        if (getCommit(commit) == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit c = getCommit(commit);
        clear();
        for (String s : c.getMap().keySet())  {
            File f = join(CWD, s);
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    System.out.println("Error creating file.");
                }
            }
            String blobName = c.getMap().get(s);
            String blobContents = readObject(join(BLOBS, blobName), String.class);
            writeContents(f, blobContents);
        }
        bm.setHead(bm.getCurBranch(), c.getSha());
        writeObject(BranchMapFile, bm);

    }

    public static void clear() {
        for (String f : Objects.requireNonNull(CWD.list())) {
            if (f.equals(".gitlet")) {
                continue;
            }
            if (!trackedInBranch(f)) {
                System.out.println("There is an untracked file in the way;"
                       + " delete it, or add and commit it first.");
                return;
            }
        }
        for (File f : Objects.requireNonNull(CWD.listFiles())) {
            if (!f.equals(".gitlet")) {
                f.delete();
            }
        }
        for (File f : Objects.requireNonNull(ADD.listFiles())) {
            f.delete();
        }
        for (File f : Objects.requireNonNull(REMOVE.listFiles())) {
            f.delete();
        }
    }

    public static void makeRepositories() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists "
                   + "in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        COMMITS.mkdir();
        BLOBS.mkdir();
        STAGE.mkdir();
        ADD.mkdir();
        REMOVE.mkdir();
    }

    public static boolean alreadyCommitted(String name, String sha) {
        Commit c = getHead();
        if (c.getMap().containsKey(name) && c.getMap().get(name).equals(sha)) {
            return true;
        }
        return false;
    }

    public static boolean trackedInBranch(String file) {
        return getHead().getMap().containsKey(file);
    }

    public static Commit getHead() {
        BranchMap bm = readObject(BranchMapFile, BranchMap.class);
        String headSha = bm.headSha();
        Commit c = readObject(join(COMMITS, headSha.substring(0, 2), headSha), Commit.class);
        return c;
    }

    public static Commit getCommit(String sha) {
        if (sha.length() < 40) {
            File dir = join(COMMITS, sha.substring(0, 2));
            if (!dir.exists()) {
                return null;
            }
            if (dir.listFiles().length == 1) {
                for (File f : dir.listFiles()) {
                    return readObject(f, Commit.class);
                }
            } else {
                for (File f : dir.listFiles()) {
                    if (f.getName().startsWith(sha)) {
                        return readObject(f, Commit.class);
                    }
                }
            }
        }
        if (join(COMMITS, sha.substring(0, 2), sha).exists()) {
            return readObject(join(COMMITS, sha.substring(0, 2), sha), Commit.class);
        }
        return null;
    }

}
