package org.patinanetwork.codebloom.utilities;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerMetadataObject {

    private String name;
    private String version;
    private String description;
    private ArrayList<String> authors;

    public ServerMetadataObject(final String commitSha) {
        this.name = "Codebloom";
        this.version = Strings.isNullOrEmpty(commitSha) ? "unknown" : commitSha;
        this.description = "LeetCode leaderboard for Patina Network members to track progress and motivate each other.";
        this.authors = new ArrayList<>(Arrays.asList(
                "Alisha Zaman",
                "Alfardil Alam",
                "Angela Yu",
                "Tahmid Ahmed",
                "Arshadul Monir",
                "Nancy Huang",
                "Haoking Luo",
                "Maliha Tasnim",
                "Randy Dean",
                "Andrew Yu",
                "Isabella Lam"));
    }

    public ServerMetadataObject() {
        this("unknown");
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ArrayList<String> getAuthors() {
        return authors;
    }

    public void setAuthors(final ArrayList<String> authors) {
        this.authors = authors;
    }
}
