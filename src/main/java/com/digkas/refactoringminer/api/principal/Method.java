package com.digkas.refactoringminer.api.principal;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author George Digkas <digasgeo@gmail.com>
 *
 */
public class Method {

	@SerializedName("path")
	@Expose
	private String path;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("startPos")
	@Expose
	private Integer startPos;
	@SerializedName("endPos")
	@Expose
	private Integer endPos;
	@SerializedName("startLine")
	@Expose
	private Integer startLine;
	@SerializedName("endLine")
	@Expose
	private Integer endLine;
	@SerializedName("classifier")
	@Expose
	private String classifier;
	@SerializedName("contribution")
	@Expose
	private Double contribution;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStartPos() {
		return startPos;
	}

	public void setStartPos(Integer startPos) {
		this.startPos = startPos;
	}

	public Integer getEndPos() {
		return endPos;
	}

	public void setEndPos(Integer endPos) {
		this.endPos = endPos;
	}

	public Integer getStartLine() {
		return startLine;
	}

	public void setStartLine(Integer startLine) {
		this.startLine = startLine;
	}

	public Integer getEndLine() {
		return endLine;
	}

	public void setEndLine(Integer endLine) {
		this.endLine = endLine;
	}

	public String getClassifier() {
		return classifier;
	}

	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}

	public Double getContribution() {
		return contribution;
	}

	public void setContribution(Double contribution) {
		this.contribution = contribution;
	}

}
