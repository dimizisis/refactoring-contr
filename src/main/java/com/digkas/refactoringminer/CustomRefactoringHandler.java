package com.digkas.refactoringminer;

import com.digkas.refactoringminer.entities.principal.DiffEntry;
import com.digkas.refactoringminer.entities.principal.Method;
import com.digkas.refactoringminer.entities.principal.PrincipalResponseEntity;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import gr.uom.java.xmi.diff.CodeRange;
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
    private Double refactoringInterestContribution;
    private Double refactoringPrincipalContribution;
    private Double newCodeInterestContribution;
    private Double newCodePrincipalContribution;
    private final Map<String, Set<CustomCodeRange>> refactoredFiles;
    private List<DiffEntry> diffEntries;

    public CustomRefactoringHandler() {
        this.refactoringInterestContribution = 0.0;
        this.refactoringPrincipalContribution = 0.0;
        this.newCodeInterestContribution = 0.0;
        this.newCodePrincipalContribution = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        diffEntries = getDiffEntriesAtCommit(commitId);
        /* if refactoring list is not empty, print refactoring contribution, else only new code contribution is printed */
        if (!refactorings.isEmpty())
            printRefactoringContribution(commitId, refactorings);

        if (Objects.nonNull(diffEntries) && !diffEntries.isEmpty())
            printNewCodeContribution(commitId);
    }

    private void printRefactoringContribution(String commitId, List<Refactoring> refactorings) {
        boolean printCompound = false;
        for (Refactoring r : refactorings) {
            for (ImmutablePair<String, String> c : r.getInvolvedClassesAfterRefactoring()) {
                Set<CustomCodeRange> codeRangeLst = new HashSet<>();
                for (CodeRange codeRange : r.rightSide()) {
                    codeRangeLst.add(new CustomCodeRange(codeRange.getStartLine(), codeRange.getEndLine(), codeRange.getFilePath(), r.getRefactoringType().toString()));
                }
                try {
                    refactoredFiles.get(c.getLeft()).addAll(codeRangeLst);
                } catch (NullPointerException e) {
                    refactoredFiles.put(c.getLeft(), codeRangeLst);
                }
            }
//                    refactoringInterestContribution += getFileInterest(c.getLeft()); // to modify (apply contribution's formula)
                refactoringInterestContribution = 0.0; // tmp
        }

        printCompound = printMethodPrincipalContribution(commitId);

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "REFACTORING", "COMPOUND", refactoringPrincipalContribution, refactoringInterestContribution, "");
    }

    private boolean printMethodPrincipalContribution(String commitId) {
        boolean printCompound = false;
        try {
            for (Map.Entry<String, Set<CustomCodeRange>> entry : refactoredFiles.entrySet()) {
                for (DiffEntry diffEntry : diffEntries) {
                    if (entry.getKey().equals(diffEntry.getNewFilePath())) {
                        if (Objects.nonNull(diffEntry.getFileContribution())) {
                            refactoringPrincipalContribution += diffEntry.getFileContribution();
                            printCompound = true;
                            print(commitId,
                                    diffEntry.getNewFilePath(),
                                    "REFACTORING",
                                    "ENTIRE",
                                    diffEntry.getFileContribution(), refactoringInterestContribution,
                                    findRefactoringType(diffEntry.getNewFilePath()));
                            continue;
                        }
                        if (Objects.isNull(diffEntry.getMethods()))
                            continue;
                        for (Method method : diffEntry.getMethods()) {
                            if (entry.getValue().contains(new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath()))) {
                                for (CustomCodeRange cr : entry.getValue()) {
                                    if (new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath()).equals(cr)) {
                                        if (Objects.nonNull(method.getContribution()) && !method.getContribution().isNaN()) {
                                            refactoringPrincipalContribution += method.getContribution();
                                            printCompound = true;
                                            print(commitId,
                                                    entry.getKey() + " - " + method.getName(),
                                                    "REFACTORING",
                                                    "METHOD",
                                                    method.getContribution(), refactoringInterestContribution,
                                                    findRefactoringType(diffEntry.getNewFilePath(), new CustomCodeRange(method.getStartLine(), method.getEndLine(), method.getPath())));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return printCompound;
    }

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

    private void printNewCodeContribution(String commitId) {

        if (Objects.isNull(diffEntries))
            return;

        boolean printCompound = false;
        for (DiffEntry diffEntry : diffEntries) {
                if (Objects.nonNull(diffEntry.getFileContribution())) {
                    if (Objects.nonNull(refactoredFiles.get(diffEntry.getNewFilePath())))
                        continue;
                    newCodePrincipalContribution += diffEntry.getFileContribution();
                    printCompound = true;
                    print(commitId,
                            diffEntry.getNewFilePath(),
                            "NEW_FILE",
                            "ENTIRE",
                            diffEntry.getFileContribution(), newCodeInterestContribution,
                            diffEntry.getChangeType());
                    continue;
                }
                if (Objects.isNull(diffEntry.getMethods()))
                    continue;
                for (Method method : diffEntry.getMethods()) {
                    if (Objects.nonNull(method.getContribution()) && Objects.nonNull(method.getPath())) {
                        if (Objects.nonNull(refactoredFiles.get(method.getPath())) && refactoredFiles.get(method.getPath())
                                .stream()
                                .filter(codeRange -> Objects.nonNull(codeRange.getStartLine()) && Objects.nonNull(codeRange.getEndLine()))
                                .anyMatch(codeRange -> method.getStartLine().equals(codeRange.getStartLine()) && method.getEndLine().equals(codeRange.getEndLine())))
                            continue;
                        newCodePrincipalContribution += method.getContribution();
//                    newCodeInterestContribution += getFileInterest(method.getPath()); // to modify (apply contribution's formula)
                        newCodeInterestContribution += 0.0; // tmp
                        String granularity = "";
                        String changeType = diffEntry.getChangeType().equals("ADD") ? "NEW_FILE" : "NEW_METHOD";
                        String comment = "";
                        String file = diffEntry.getNewFilePath();
                        if (diffEntry.getChangeType().equals("ADD")) {
                            granularity = "ENTIRE";
                            comment = diffEntry.getChangeType();
                        }
                        else if (method.getClassifier().endsWith("Insert")) {
                            file += " - " + method.getName();
                            granularity = "METHOD";
                            comment = method.getClassifier();
                        }
                        printCompound = true;
                        print(commitId, file, changeType, granularity, method.getContribution(), newCodeInterestContribution, comment);
                    }
                }
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "NEW", "COMPOUND", newCodePrincipalContribution, newCodeInterestContribution, "");
    }

    private void print(String commitId, String file, String type, String granularity, Double principalContribution, Double interestContribution, String comment) {
        System.out.printf("%s\t%s\t%s\t%s\t%s\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, comment);
        Globals.append(String.format("%s\t%s\t%s\t%s\t%s\t%g\t%s\n", commitId, file, type, granularity, new DecimalFormat("0.0000000000").format(principalContribution), interestContribution, comment));
    }

    private List<DiffEntry> getDiffEntriesAtCommit(String commitId) {
        HttpResponse<JsonNode> httpResponse;
        Unirest.setTimeouts(0, 0);
        try {
            httpResponse = Unirest.get("http://195.251.210.147:8989/api/dzisis/study-by-commit?url=https://github.com/apache/commons-io&sha=" + commitId).asJson();
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
}
