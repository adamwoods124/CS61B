package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.LinkedList;
import java.util.TreeMap;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    public String message;
    /** Timestamp */
    public Date date;
    /** Parent of the commit */
    public LinkedList<String> parents;
    //public static String parent;
    public TreeMap<String, String> map;

    public Commit() {
        this.message = "initial commit";
        this.date = new Date(0);
        this.parents = null;
        this.map = new TreeMap<>();
    }

    public Commit(String message, String parent) {
        this.message = message;
        this.date = new Date();
        this.parents = new LinkedList<>();
        this.parents.add(parent);
        File f = Utils.join(System.getProperty("user.dir"), ".gitlet", "commits", parent.substring(0, 2), parent);
        Commit c = Utils.readObject(f, Commit.class);
        this.map = c.map;
        File stage = Utils.join(System.getProperty("user.dir"), ".gitlet", "stage", "add");
        for(File s : stage.listFiles()) {
            this.map.put(s.getName(), readContentsAsString(s));
        }
    }

    public String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("E MMM d hh:mm:ss y Z");
        return df.format(date);
    }

    public String getSha() {
        return sha1(serialize(this));
    }

}
