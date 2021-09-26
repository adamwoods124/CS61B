package gitlet;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class BranchMap implements Serializable {
    // Keep track of current branch
    private String curBranch;
    // Map of all branches to their head commit
    private TreeMap<String, String> branchToHead;
    // Map commits to the branches they are a part of
    private TreeMap<String, List<String>> commitToBranches;

    public BranchMap(String commit) {
        this.curBranch = "master";
        this.commitToBranches = new TreeMap<>();
        List<String> l = new LinkedList<>();
        l.add("master");
        this.commitToBranches.put(commit, l);
        this.branchToHead = new TreeMap<>();
        this.branchToHead.put("master", commit);
    }

    public String getCurBranch() {
        return this.curBranch;
    }

    public TreeMap<String, String> getBranches() {
        return this.branchToHead;
    }

    public TreeMap<String, List<String>> getMap() {
        return this.commitToBranches;
    }

    public void newCommit(String commit, String branch) {
        LinkedList<String> l = new LinkedList<>();
        l.add(branch);
        this.commitToBranches.put(commit, l);
        this.curBranch = branch;
        this.branchToHead.put(branch, commit);
    }

    public void addBranch(String commit, String branch) {
        this.branchToHead.put(branch, commit);
        this.commitToBranches.get(commit).add(branch);
    }

    public void changeBranch(String newBranch) {
        this.curBranch = newBranch;
    }

}
