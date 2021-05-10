package com.digkas.refactoringminer;

import java.util.Objects;

public class CustomCodeRange {
    private final Integer startLine;
    private final Integer endLine;
    private final String filePath;
    private String refactoringType;

    public CustomCodeRange(int startLine, int endLine, String path) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.filePath = path;
    }

    public CustomCodeRange(int startLine, int endLine, String path, String refactoringType) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.filePath = path;
        this.refactoringType = refactoringType;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getRefactoringType() {
        return refactoringType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (Objects.isNull(o))
            return false;
        if (this.getClass() != o.getClass())
            return false;
        CustomCodeRange otherCodeRange = (CustomCodeRange) o;
        return this.getFilePath().equals(otherCodeRange.getFilePath()) &&
                this.getStartLine().equals(otherCodeRange.getStartLine()) &&
                this.getEndLine().equals(otherCodeRange.getEndLine());
    }

    @Override
    public String toString() {
        return "Filepath: " + this.getFilePath() + " | StartLine: " + this.getStartLine() + " | EndLine: " + this.getEndLine();
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLine, endLine, filePath);
    }
}
