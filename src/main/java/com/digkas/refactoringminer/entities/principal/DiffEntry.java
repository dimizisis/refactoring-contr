package com.digkas.refactoringminer.entities.principal;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class DiffEntry {

	@SerializedName("oldFilePath")
	@Expose
	private String oldFilePath;
	@SerializedName("newFilePath")
	@Expose
	private String newFilePath;
	@SerializedName("changeType")
	@Expose
	private String changeType;
	@SerializedName("methods")
	@Expose
	private List<Method> methods = null;
	@SerializedName("fileContribution")
	@Expose
	private Double fileContribution;

	public String getOldFilePath() {
		return oldFilePath;
	}

	public void setOldFilePath(String oldFilePath) {
		this.oldFilePath = oldFilePath;
	}

	public String getNewFilePath() {
		return newFilePath;
	}

	public Double getFileContribution() { return fileContribution; }

	public void setFileContribution(Double fileContribution) { this.fileContribution = fileContribution; }

	public void setNewFilePath(String newFilePath) {
		this.newFilePath = newFilePath;
	}

	public String getChangeType() {
		return changeType;
	}

	public void setChangeType(String changeType) {
		this.changeType = changeType;
	}

	public List<Method> getMethods() {
		return methods;
	}

	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

}
