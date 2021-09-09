package com.digkas.refactoringminer;

import com.digkas.refactoringminer.entities.interest.FileInterestContribution;
import com.digkas.refactoringminer.entities.principal.DiffEntry;
import com.digkas.refactoringminer.entities.principal.Method;
import com.digkas.refactoringminer.entities.principal.PrincipalResponseEntity;
import com.google.gson.Gson;
import gr.uom.java.xmi.diff.CodeRange;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public class CustomRefactoringHandler extends RefactoringHandler {
    private final String url;
    private Double refactoringInterestContribution;
    private Double refactoringInterestContributionPercentage;
    private Double refactoringPrincipalContribution;
    private Double newCodeInterestContribution;
    private Double newCodeInterestContributionPercentage;
    private Double newCodePrincipalContribution;
    private Map<String, Set<CustomCodeRange>> refactoredFiles;
    private List<DiffEntry> diffEntries;

    public CustomRefactoringHandler(String url) {
        this.url = url;
        this.refactoringInterestContribution = 0.0;
        this.refactoringInterestContributionPercentage = 0.0;
        this.refactoringPrincipalContribution = 0.0;
        this.newCodeInterestContribution = 0.0;
        this.newCodeInterestContributionPercentage = 0.0;
        this.newCodePrincipalContribution = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        diffEntries = getDiffEntriesAtCommit(url, commitId);
        /* if refactoring list is not empty, print refactoring contribution, else only new code contribution is printed */
        if (!refactorings.isEmpty()) {
            refactoredFiles = findAllRefactoredFiles(refactorings);
            printRefactoringContribution(commitId);
        }

        if (Objects.nonNull(diffEntries) && !diffEntries.isEmpty())
            printNewCodeContribution(commitId);
    }

    /**
     * Finds all refactorings, along with their code ranges
     * that may exist in specific a commit.
     *
     * @param refactorings a Map object containing file paths (key) and code ranges (value)
     * @return the Map object containing info about existing refactorings
     */
    private Map<String, Set<CustomCodeRange>> findAllRefactoredFiles(List<Refactoring> refactorings) {
        Map<String, Set<CustomCodeRange>> refactoredFiles = new HashMap<>();
        for (Refactoring r : refactorings) {
            for (ImmutablePair<String, String> c : r.getInvolvedClassesAfterRefactoring()) {
                Set<CustomCodeRange> codeRangeSet = findAllRefactoringCodeRanges(r);
                try {
                    refactoredFiles.get(c.getLeft()).addAll(codeRangeSet);
                } catch (NullPointerException e) {
                    refactoredFiles.put(c.getLeft(), codeRangeSet);
                }
            }
        }
        return refactoredFiles;
    }

    /**
     * Finds all code ranges for a refactoring that may exist
     * in a specific commit.
     *
     * @param r a Refactoring object
     * @return a Set object containing all code ranges for the refactoring
     */
    private Set<CustomCodeRange> findAllRefactoringCodeRanges(Refactoring r) {
        Set<CustomCodeRange> codeRangeSet = new HashSet<>();
        for (CodeRange codeRange : r.rightSide())
            codeRangeSet.add(new CustomCodeRange(codeRange.getStartLine(), codeRange.getEndLine(), codeRange.getFilePath(), r.getRefactoringType().toString()));
        return codeRangeSet;
    }

    /**
     * Finds and prints refactorings' contribution for a specific commit.
     *
     * @param commitId a String representation of the commit id (SHA)
     */
    private void printRefactoringContribution(String commitId) {
        boolean printCompound = false;
        try {
            for (Map.Entry<String, Set<CustomCodeRange>> entry : refactoredFiles.entrySet()) {
                for (DiffEntry diffEntry : diffEntries) {
                    Double fileInterestContribution = getFileInterestContribution(url, commitId, diffEntry.getNewFilePath());
                    Double fileInterestContributionPercentage = getFileInterestContributionPercentage(url, commitId, diffEntry.getNewFilePath());
                    if (entry.getKey().equals(diffEntry.getNewFilePath())) {
                        if (isEntire(diffEntry)) {
                            appendRefactoringPrincipalContribution(diffEntry.getFileContribution());
                            appendRefactoringInterestContribution(fileInterestContribution, fileInterestContributionPercentage);
                            printCompound = true;
                            print(commitId,
                                    diffEntry.getNewFilePath(),
                                    "REFACTORING",
                                    "ENTIRE",
                                    diffEntry.getFileContribution(), fileInterestContribution, fileInterestContributionPercentage,
                                    findRefactoringType(diffEntry.getNewFilePath()));
                            continue;
                        }
                        if (Objects.isNull(diffEntry.getMethods()))
                            continue;
                        Set<String> visitedFilePaths = new HashSet<>();
                        for (Method method : diffEntry.getMethods()) {
                            if (entry.getValue().contains(new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath()))) {
                                for (CustomCodeRange cr : entry.getValue()) {
                                    if (new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath()).equals(cr)) {
                                        if (Objects.nonNull(method.getContribution()) && !method.getContribution().isNaN()) {
                                            if (canVisitFile(visitedFilePaths, method.getPath())) {
                                                visitedFilePaths.add(method.getPath());
                                                appendRefactoringInterestContribution(fileInterestContribution, fileInterestContributionPercentage);
                                            }
                                            appendRefactoringPrincipalContribution(method.getContribution());
                                            printCompound = true;
                                            print(commitId,
                                                    entry.getKey() + " - " + method.getName(),
                                                    "REFACTORING",
                                                    "METHOD",
                                                    method.getContribution(),
                                                    fileInterestContribution, fileInterestContributionPercentage,
                                                    findRefactoringType(diffEntry.getNewFilePath(), new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath())));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "REFACTORING", "COMPOUND", refactoringPrincipalContribution, refactoringInterestContribution, refactoringInterestContributionPercentage, "");
    }

    /**
     * Appends a refactoring's principal contribution to total refactoring
     * contribution.
     *
     * @param contribution Double value representing the contribution of file or method
     *                     containing the refactoring to project's principal
     */
    private void appendRefactoringPrincipalContribution(Double contribution) {
        refactoringPrincipalContribution += contribution;
    }

    /**
     * Appends a refactoring's interest contribution to the total refactoring
     * contribution value.
     *
     * @param fileInterestContribution           Double value representing the file's contribution
     *                                           to project's interest
     * @param fileInterestContributionPercentage Double value representing the file's
     *                                           contribution to project's interest percentage
     */
    private void appendRefactoringInterestContribution(Double fileInterestContribution, Double fileInterestContributionPercentage) {
        refactoringInterestContribution += fileInterestContribution;
        refactoringInterestContributionPercentage += fileInterestContributionPercentage;
    }

    /**
     * Finds & returns the refactoring type, given the file's path.
     * Returns null if no type is found.
     *
     * @param path a String representation of a java file's path
     * @return a String representation of refactoring's type, null if not found
     */
    private String findRefactoringType(String path) {
        for (Map.Entry<String, Set<CustomCodeRange>> entry : refactoredFiles.entrySet()) {
            if (entry.getKey().equals(path)) {
                for (CustomCodeRange cr : entry.getValue()) {
                    return cr.getRefactoringType();
                }
            }
        }
        return null;
    }

    /**
     * Finds & returns the refactoring type, given the file's path & refactoring's code range.
     * Returns null if no type is found.
     *
     * @param path            a String representation of a java file's path
     * @param customCodeRange a CustomCodeRange object containing refactoring's start & end line
     * @return a String representation of refactoring's type, null if not found
     */
    private String findRefactoringType(String path, CustomCodeRange customCodeRange) {
        for (Map.Entry<String, Set<CustomCodeRange>> entry : refactoredFiles.entrySet()) {
            if (entry.getKey().equals(path)) {
                for (CustomCodeRange cr : entry.getValue()) {
                    if (cr.equals(customCodeRange))
                        return cr.getRefactoringType();
                }
            }
        }
        return null;
    }

    /**
     * Finds and prints new code's contribution for a specific commit.
     *
     * @param commitId a String representation of the commit id (SHA)
     */
    private void printNewCodeContribution(String commitId) {

        if (Objects.isNull(diffEntries))
            return;

        boolean printCompound = false;
        for (DiffEntry diffEntry : diffEntries) {
            Double fileInterestContribution = getFileInterestContribution(url, commitId, diffEntry.getNewFilePath());
            Double fileInterestContributionPercentage = getFileInterestContributionPercentage(url, commitId, diffEntry.getNewFilePath());
            if (isEntire(diffEntry) && !isRefactoring(diffEntry)) {
                printCompound = true;
                appendNewCodePrincipalContribution(diffEntry.getFileContribution());
                appendNewCodeInterestContribution(fileInterestContribution, fileInterestContributionPercentage);
                print(commitId,
                        diffEntry.getNewFilePath(),
                        "NEW_FILE",
                        "ENTIRE",
                        diffEntry.getFileContribution(), fileInterestContribution, fileInterestContributionPercentage,
                        diffEntry.getChangeType());
                continue;
            }
            if (Objects.isNull(diffEntry.getMethods()))
                continue;
            Set<String> visitedFilePaths = new HashSet<>();
            for (Method method : diffEntry.getMethods()) {
                if (Objects.nonNull(method.getContribution()) && Objects.nonNull(method.getPath())) {
                    if (hasRefactoring(method))
                        continue;
                    appendNewCodePrincipalContribution(method.getContribution());
                    if (canVisitFile(visitedFilePaths, diffEntry.getNewFilePath())) {
                        visitedFilePaths.add(method.getPath());
                        appendNewCodeInterestContribution(fileInterestContribution, fileInterestContributionPercentage);
                    }
                    printCompound = true;
                    print(commitId, getNewCodeFileName(diffEntry, method), getNewCodeChangeType(diffEntry), getNewCodeGranularity(diffEntry, method), Objects.nonNull(method.getContribution()) ? method.getContribution() : 0.0, fileInterestContribution, fileInterestContributionPercentage, getNewCodeComment(diffEntry, method));
                }
            }
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "NEW", "COMPOUND", newCodePrincipalContribution, newCodeInterestContribution, newCodeInterestContributionPercentage, "");
    }

    /**
     * Returns true if file has not already been visited (if its path
     * is not included in the fileSet provided).
     *
     * @param fileSet a Set object, containing String representations of file paths
     * @param path    a String representation of a java file's path
     * @return true if file set does not contain file's path, false otherwise
     */
    private boolean canVisitFile(Set<String> fileSet, String path) {
        return !fileSet.contains(path);
    }

    /**
     * Returns appropriate granularity for printing,
     * according to a diff entry or method.
     *
     * @param diffEntry a DiffEntry object
     * @param method    a Method object
     * @return the String representation of a granularity level
     */
    private String getNewCodeGranularity(DiffEntry diffEntry, Method method) {
        String granularity = "";
        if (diffEntry.getChangeType().equals("ADD"))
            granularity = "ENTIRE";
        else if (method.getClassifier().endsWith("Insert"))
            granularity = "METHOD";
        return granularity;
    }

    /**
     * Returns appropriate change type for printing,
     * according to a diff entry.
     *
     * @param diffEntry a DiffEntry object
     * @return the String representation of a change type
     */
    private String getNewCodeChangeType(DiffEntry diffEntry) {
        return diffEntry.getChangeType().equals("ADD") ? "NEW_FILE" : "NEW_METHOD";
    }

    /**
     * Returns appropriate comment for printing,
     * according to a diff entry or method.
     *
     * @param diffEntry a DiffEntry object
     * @param method    a Method object
     * @return the String representation of a comment
     */
    private String getNewCodeComment(DiffEntry diffEntry, Method method) {
        String comment = "";
        if (diffEntry.getChangeType().equals("ADD")) {
            comment = diffEntry.getChangeType();
        } else if (method.getClassifier().endsWith("Insert")) {
            comment = method.getClassifier();
        }
        return comment;
    }

    /**
     * Returns appropriate file name for printing,
     * according to a diff entry or method. File name
     * may contain a method name, separated by a dash
     *
     * @param diffEntry a DiffEntry object
     * @param method    a Method object
     * @return the String representation of a comment
     */
    private String getNewCodeFileName(DiffEntry diffEntry, Method method) {
        String file = diffEntry.getNewFilePath();
        if (method.getClassifier().endsWith("Insert"))
            file += " - " + method.getName();
        return file;
    }

    /**
     * Return whether a diff entry is entire. Entire
     * is considered only when the diff entry is a
     * result of a file creation.
     *
     * @param diffEntry a DiffEntry object
     * @return true if diff entry is entire, false otherwise
     */
    private boolean isEntire(DiffEntry diffEntry) {
        return Objects.nonNull(diffEntry.getFileContribution());
    }

    /**
     * Appends a new code's principal contribution to total refactoring
     * contribution.
     *
     * @param contribution Double value representing the contribution of file or method
     *                     that's considered as new code to project's principal
     */
    private void appendNewCodePrincipalContribution(Double contribution) {
        newCodePrincipalContribution += contribution;
    }

    /**
     * Appends a new code's interest contribution to the total refactoring
     * contribution value.
     *
     * @param fileInterestContribution           Double value representing the file's contribution
     *                                           to project's interest
     * @param fileInterestContributionPercentage Double value representing the file's
     *                                           contribution to project's interest percentage
     */
    private void appendNewCodeInterestContribution(Double fileInterestContribution, Double fileInterestContributionPercentage) {
        newCodeInterestContribution += fileInterestContribution;
        newCodeInterestContributionPercentage += fileInterestContributionPercentage;
    }

    /**
     * Return whether a diff entry consists a refactoring
     * itself.
     *
     * @param diffEntry a DiffEntry object
     * @return true if diff entry is a refactoring, false otherwise
     */
    private boolean isRefactoring(DiffEntry diffEntry) {
        return Objects.nonNull(refactoredFiles.get(diffEntry.getNewFilePath()));
    }

    /**
     * Return whether a method contains a refactoring
     *
     * @param method a DiffEntry object
     * @return true if method contains a refactoring, false otherwise
     */
    private boolean hasRefactoring(Method method) {
        if (Objects.nonNull(method.getContribution()) && Objects.nonNull(method.getPath())) {
            return (Objects.nonNull(refactoredFiles.get(method.getPath())) && refactoredFiles.get(method.getPath())
                    .stream()
                    .filter(codeRange -> Objects.nonNull(codeRange.getStartLine()) && Objects.nonNull(codeRange.getEndLine()))
                    .anyMatch(codeRange -> method.getStartLine().equals(codeRange.getStartLine()) && method.getEndLine().equals(codeRange.getEndLine())));
        }
        return false;
    }

    /**
     * Appends a row with analysis info
     *
     * @param commitId              a String representation of a commit id (SHA)
     * @param file                  a String representation of a file path (with or without method name)
     * @param type                  a String representation of a change type
     * @param granularity           a String representation of granularity level
     * @param principalContribution contribution to total principal (sum of entire commit)
     * @param interestContribution  contribution to total interest (sum of entire commit)
     * @param comment               a String representation of a comment
     */
    private void print(String commitId, String file, String type, String granularity, Double principalContribution, Double interestContribution, String comment) {
//        System.out.printf("%s\t%s\t%s\t%s\t%s\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, comment);
        Globals.append(String.format("%s\t%s\t%s\t%s\t%s\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, comment));
    }

    /**
     * Appends a row with analysis info
     *
     * @param commitId                       a String representation of a commit id (SHA)
     * @param file                           a String representation of a file path (with or without method name)
     * @param type                           a String representation of a change type
     * @param granularity                    a String representation of granularity level
     * @param principalContribution          contribution to total principal (sum of entire commit)
     * @param interestContribution           contribution to total interest (sum of entire commit)
     * @param interestContributionPercentage contribution to total interest percentage (sum of entire commit)
     * @param comment                        a String representation of a comment
     */
    private void print(String commitId, String file, String type, String granularity, Double principalContribution, Double interestContribution, Double interestContributionPercentage, String comment) {
        // System.out.printf("%s\t%s\t%s\t%s\t%s\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, comment);
        Globals.append(String.format("%s\t%s\t%s\t%s\t%s\t%g\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, interestContributionPercentage, comment));
    }

    /**
     * Retrieves all diff entries (list of DiffEntry objects) from
     * digkasgeo's new code API.
     *
     * @param url      a String representation of repo's url
     * @param commitId a String representation of a commit id (SHA)
     * @return a list with DiffEntry objects (if exist), null otherwise
     */
    private List<DiffEntry> getDiffEntriesAtCommit(String url, String commitId) {
        HttpResponse<JsonNode> httpResponse;
//        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:8989/api/dzisis/study-by-commit?url=" + url + "&sha=" + commitId).asJson();
            PrincipalResponseEntity[] responseEntities = new Gson().fromJson(httpResponse.getBody().toString(), PrincipalResponseEntity[].class);
            List<DiffEntry> diffEntries = new ArrayList<>();
            for (PrincipalResponseEntity entity : responseEntities)
                diffEntries.addAll(entity.getDiffEntries());
            return diffEntries;
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves interest contribution, given the repo's url,
     * a commit id (SHA) & a java file path
     *
     * @param url      a String representation of repo's url
     * @param commitId a String representation of a commit id (SHA)
     * @param filePath a String representation of a java file's path
     * @return a Double value, representing the contribution to project's interest (if exist), 0.0 otherwise
     */
    private Double getFileInterestContribution(String url, String commitId, String filePath) {
        HttpResponse<JsonNode> httpResponse;
//        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:3992/api/interestDensityContributionByCommitAndFile?url=" + url + "&sha=" + commitId + "&filePath=" + filePath).asJson();
            FileInterestContribution fileInterestContribution = new Gson().fromJson(httpResponse.getBody().toString(), FileInterestContribution.class);
            return Objects.isNull(fileInterestContribution.getContribution()) ? 0.0 : fileInterestContribution.getContribution();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Retrieves interest contribution percentage, given the repo's url,
     * a commit id (SHA) & a java file path
     *
     * @param url      a String representation of repo's url
     * @param commitId a String representation of a commit id (SHA)
     * @param filePath a String representation of a java file's path
     * @return a Double value, representing the percentage of contribution to project's interest (if exist), 0.0 otherwise
     */
    private Double getFileInterestContributionPercentage(String url, String commitId, String filePath) {
        HttpResponse<JsonNode> httpResponse;
        try {
            httpResponse = Unirest.get("http://195.251.210.147:3992/api/interestDensityContributionPercentageByCommitAndFile?url=" + url + "&sha=" + commitId + "&filePath=" + filePath).asJson();
            FileInterestContribution fileInterestContributionPercentage = new Gson().fromJson(httpResponse.getBody().toString(), FileInterestContribution.class);
            return Objects.isNull(fileInterestContributionPercentage.getContribution()) ? 0.0 : fileInterestContributionPercentage.getContribution();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
