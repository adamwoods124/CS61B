package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.TreeMap;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  contains message, date, parents, and a map to all blobs tracked by the commit
 *  @author Adam Woods
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private String message;
    /** Timestamp */
    private Date date;
    /** Parent of the commit */
    private LinkedList<String> parents;
    //public static String parent;
    private TreeMap<String, String> map;
    private LinkedList<String> branch;

    public Commit() {
        this.message = "initial commit";
        this.date = new Date(0);
        this.parents = null;
        this.map = new TreeMap<>();
        branch = new LinkedList<>();
        branch.add("master");
    }

    public Commit(String message, String parent, LinkedList<String> branch) {
        this.message = message;
        this.date = new Date();
        this.parents = new LinkedList<>();
        this.parents.add(parent);
        this.branch = new LinkedList<>();
        for (String s : branch) {
            this.branch.add(s);
        }
        File commitsFolder = join(System.getProperty("user.dir"), ".gitlet", "commits");
        File thisFolder = join(commitsFolder, parent.substring(0, 2), parent);
        Commit c = Utils.readObject(thisFolder, Commit.class);
        this.map = c.map;
        File stage = Utils.join(System.getProperty("user.dir"), ".gitlet", "stage", "add");
        for (File s : stage.listFiles()) {
            this.map.put(s.getName(), readContentsAsString(s));
        }
    }

    public String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("E MMM d hh:mm:ss y Z");
        return df.format(date);
    }

    public String getMessage() {
        return message;
    }

    public LinkedList<String> getParents() {
        return parents;
    }

    public TreeMap<String, String> getMap() {
        return map;
    }

    public String getSha() {
        return sha1(serialize(this));
    }

    public LinkedList getBranch() {
        return branch;
    }

}
