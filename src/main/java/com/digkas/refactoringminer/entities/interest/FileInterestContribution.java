package com.digkas.refactoringminer.entities.interest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dimitrios Zisis <zisisndimitris@gmail.com>
 */
public class FileInterestContribution {
    @SerializedName("sha")
    @Expose
    private String sha;
    @SerializedName("filePath")
    @Expose
    private String filePath;
    @SerializedName("contribution")
    @Expose
    private Double contribution;

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Double getContribution() {
        return contribution;
    }

    public void setContribution(Double contribution) {
        this.contribution = contribution;
    }
}
