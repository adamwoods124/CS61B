package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
            commitFile.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating commit file.");
        }
        // Saving the commit persistently
        writeObject(commitFile, initialCommit);

        // Create BranchMap and save persistently
        try {
            BRANCHES.createNewFile();
        } catch (IOException e) {
            System.out.println("Error creating branch file.");
        }
        BranchMap branchMap = new BranchMap(initialCommit.getSha());
        writeObject(BRANCHES, branchMap);
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
        // Get sha of the current head commit, and create a new commit that is a clone of it
        String headSha = getHead().getSha();
        LinkedList<String> l = new LinkedList<>();
        l.add(headSha);
        Commit c = new Commit(message, l);

        for (File f : ADD.listFiles()) {
            c.getMap().put(f.getName(), sha1(readContentsAsString(f)));
        }
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
        // Save branchMap
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        branchMap.newCommit(c.getSha(), branchMap.getCurBranch());
        writeObject(BRANCHES, branchMap);

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
            c.getMap().remove(fileName);
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
                        System.out.printf("===\ncommit %1$s\nMerge: %2$s %3$s\nDate: %4$s\n%5$s\n\n"
                               , c.getSha(), c.getDate(), c.getParents().get(0).substring(0, 7),
                                c.getParents().get(1).substring(0, 7), c.getMessage());
                    }
                }
            }
        }
    }

    public static void find(String message) {
        boolean found = false;
        for (File n : Objects.requireNonNull(COMMITS.listFiles())) {
            if (!n.getName().equals("branches")) {
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
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        System.out.println("=== Branches ===");
        for (String s : Objects.requireNonNull(plainFilenamesIn(BRANCHES))) {
            if (branchMap.getCurBranch().equals(s)) {
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
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        if (!branchMap.getBranches().containsKey(newBranch)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (newBranch.equals(branchMap.getCurBranch())) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        clear();
        branchMap.changeBranch(newBranch);
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
        writeObject(BRANCHES, branchMap);
    }

    public static void branch(String name) {
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        branchMap.addBranch(getHead().getSha(), name);
        writeObject(BRANCHES, branchMap);
    }

    public static void removeBranch(String name) {
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        if (!branchMap.getBranches().containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (name.equals(branchMap.getCurBranch())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branchMap.getBranches().remove(name);
        writeObject(BRANCHES, branchMap);
    }
    public static void reset(String commit) {
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
        writeObject(join(COMMITS, c.getSha()), c);
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        branchMap.newCommit(commit, branchMap.getCurBranch());
        writeObject(BRANCHES, branchMap);
    }


    public static void merge(String otherBranch) {
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        /*
        if (mergeFailure(otherBranch)) {
            System.out.println("Encountered a merge conflict.");
            return;
        }
         */
        Commit other = getCommit(branchMap.getBranches().get(otherBranch));
        Commit head = getHead();
        Commit split = latestAncestor(otherBranch);
        LinkedList<String> l = new LinkedList<>();
        l.add(getHead().getSha());
        l.add(branchMap.getBranches().get(otherBranch));
        Commit merge = new Commit("Merged " + otherBranch + "into " + branchMap.getCurBranch() + ".", l);
        if (split.getSha().equals(other.getSha())) {
            return;
        }
        if (split.getSha().equals(head.getSha())) {
            checkoutBranch(otherBranch);
            return;
        }
        LinkedList<String> savedFiles = new LinkedList<>();
        LinkedList<String> filesToStage = new LinkedList<>();

        for (String file : split.getMap().keySet()) {
            // case 1
            if (!other.getMap().get(file).equals(split.getMap().get(file))
                    && head.getMap().get(file).equals(split.getMap().get(file))) {
                    merge.getMap().put(file, other.getMap().get(file));
                    filesToStage.add(file);
            }
            // case 2
            else if (!head.getMap().get(file).equals(split.getMap().get(file))
                    && other.getMap().get(file).equals(split.getMap().get(file))) {
                merge.getMap().put(file, head.getMap().get(file));
            }
            // case 3.1
            else if ((!head.getMap().containsKey(file) && !other.getMap().containsKey(file))) {
                if(join(CWD, file).exists()) {
                    savedFiles.add(file);
                }
            }
            // case 3.2
            else if (head.getMap().get(file).equals(other.getMap().get(file))) {
                merge.getMap().put(file, head.getMap().get(file));
            }
            // case 4
            else if (!split.getMap().containsKey(file) && head.getMap().containsKey(file) && !other.getMap().containsKey(file)) {
                merge.getMap().put(file, head.getMap().get(file));
            }
            // case 5
            else if (!split.getMap().containsKey(file) && !head.getMap().containsKey(file) && other.getMap().containsKey(file)) {
                merge.getMap().put(file, other.getMap().get(file));
                filesToStage.add(file);
            }
            // case 6
            else if (split.getMap().containsKey(file) && split.getMap().get(file).equals(other.getMap().get(file))
            && !head.getMap().containsKey(file)) {
                merge.getMap().remove(file);
            }
            // case 7
            else if (!head.getMap().get(file).equals(other.getMap().get(file))) {
                File f = join(CWD, file);
                savedFiles.add(file);
                if (!f.exists()) {
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        System.out.println("Error creating file.");
                    }
                }
                String cur = "";
                if (head.getMap().containsKey(file)) {
                    cur = readContentsAsString(join(BLOBS, head.getMap().get(file)));
                }
                String otherContents = "";
                if(other.getMap().containsKey(file)) {
                    otherContents = readContentsAsString(join(BLOBS, other.getMap().get(file)));
                }
                writeContents(f, "<<<<<<< HEAD\n", cur, "=======", otherContents, ">>>>>>>");
                File f2 = join(BLOBS, sha1(f));
                byte[] b = serialize(readContentsAsString(f));
                writeContents(f2, b);
            }
        }
        for (File f : CWD.listFiles()) {
            if(f.getName().equals(".gitlet") || savedFiles.contains(f.getName())) {
                continue;
            }
            f.delete();
        }
        for (String s : filesToStage) {
            File f = join(ADD, s);
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.out.println("Error adding file to staging area.");
            }
        }
        File mergeCommit = join(COMMITS, sha1(merge));
        try {
            mergeCommit.createNewFile();
        } catch (IOException e) {
            System.out.println("Error.");
        }
        writeObject(mergeCommit, merge);
        writeObject(BRANCHES, branchMap);
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

    public static boolean mergeFailure(String otherBranch) {
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        Commit head = getHead();
        Commit other = getCommit(readContentsAsString(join(BRANCHES, otherBranch)));
        if (!join(BRANCHES, otherBranch).exists()) {
            //System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (Objects.requireNonNull(ADD.list()).length > 0 ||
                Objects.requireNonNull(REMOVE.list()).length > 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (branchMap.getMap().get(head).contains(otherBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        for (String file : CWD.list()) {
            if (!head.getMap().containsKey(file) && !other.getMap().containsKey(file)) {
                System.out.println("There is an untracked file in the way;"
                       + " delete it, or add and commit it first.");
                return true;
            }
        }
        return false;
    }

    public static boolean trackedInBranch(String file) {
        return getHead().getMap().containsKey(file);
    }

    public static Commit getHead() {
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        String headSha = branchMap.getBranches().get(branchMap.getCurBranch());
        Commit c = readObject(join(COMMITS, headSha.substring(0, 2), headSha), Commit.class);
        return c;
    }

    public static Commit getCommit(String sha) {
        if (sha.length() < 40) {
            File dir = join(COMMITS, sha.substring(0, 2));
            if (!dir.exists()) {
                return null;
            }
            for (File f : dir.listFiles()) {
                if (f.getName().startsWith(sha)) {
                    return readObject(f, Commit.class);
                }
            }
        }
        if (join(COMMITS, sha.substring(0, 2), sha).exists()) {
            return readObject(join(COMMITS, sha.substring(0, 2), sha), Commit.class);
        }
        return null;
    }

    /**
     * Finds latest common ancestor of current branch and given branch
     * @param otherBranch name of branch to check
     * @return first commit that has both current branch and other branch as parents
     */
    public static Commit latestAncestor(String otherBranch) {
        Commit c = getHead();
        BranchMap branchMap = readObject(BRANCHES, BranchMap.class);
        File log = join(CWD, "log");
        try {
            log.createNewFile();
        } catch (IOException e) {
            System.out.println("a");
        }

        while (c.getParents() != null) {
            if (branchMap.getMap().get(c.getSha()).contains(otherBranch)) {
                return c;
            }
            if (c.getParents().size() > 1) {
                Commit c2 = getCommit(c.getParents().get(0));
                if (c2.getMap().get(c).contains(branchMap.getCurBranch())) {
                    c = c2;
                } else {
                    c = getCommit(c.getParents().get(1));
                }
            } else {
                c = getCommit(c.getParents().get(0));
            }
        }

        return branchMap.getMap().get(c).contains(otherBranch) ? c : null;
    }

}
