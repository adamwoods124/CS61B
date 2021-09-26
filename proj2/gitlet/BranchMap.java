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

    public String headSha() {
        return this.branchToHead.get(curBranch);
    }
    public TreeMap<String, String> getBranchToHead() {
        return this.branchToHead;
    }

    public TreeMap<String, List<String>> getCommitToBranches() {
        return this.commitToBranches;
    }

    public void newCommit(String commit) {
        LinkedList<String> l = new LinkedList<>();
        l.add(curBranch);
        this.commitToBranches.put(commit, l);
        this.branchToHead.put(curBranch, commit);
    }

    public void addBranch(String commit, String branch) {
        this.branchToHead.put(branch, commit);
        this.commitToBranches.get(commit).add(branch);
    }

    public void changeBranch(String newBranch) {
        this.curBranch = newBranch;
    }

    public void setHead(String branch, String sha) {
        this.branchToHead.put(branch, sha);
    }

    public void removeBranch(String name) {
        branchToHead.remove(name);
    }
}
