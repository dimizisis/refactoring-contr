package com.digkas.refactoringminer;

import com.digkas.refactoringminer.entities.interest.InterestIndicatorsResponseEntity;
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
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public class CustomRefactoringHandler extends RefactoringHandler {
    private Double refactoringInterestContr;
    private Double refactoringPrincipalContr;
    private Double newCodeInterestContr;
    private Double newCodePrincipalContr;
    private Map<String, List<List<Integer>>> refactoredFiles;
    private InterestIndicatorsResponseEntity interestResponse;
    private PrincipalResponseEntity[] principalResponse;
    private List<DiffEntry> diffEntries;

    public CustomRefactoringHandler() {
        this.refactoringInterestContr = 0.0;
        this.refactoringPrincipalContr = 0.0;
        this.newCodeInterestContr = 0.0;
        this.newCodePrincipalContr = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    public CustomRefactoringHandler(PrincipalResponseEntity[] principalResponse, InterestIndicatorsResponseEntity interestResponse) {
        this.interestResponse = interestResponse;
        this.principalResponse = principalResponse;
        this.refactoringInterestContr = 0.0;
        this.refactoringPrincipalContr = 0.0;
        this.newCodeInterestContr = 0.0;
        this.newCodePrincipalContr = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        diffEntries = getDiffEntriesAtCommit(commitId);
        /* if refactoring list is not empty, print refactoring contribution, else only new code contribution is printed */
        if (!refactorings.isEmpty())
            printRefactoringContr(commitId, refactorings);

        if (Objects.nonNull(diffEntries) && !diffEntries.isEmpty())
            printNewCodeContr(commitId);
    }

    private void printRefactoringContr(String commitId, List<Refactoring> refactorings) {
        boolean printCompound = false;
        for (Refactoring r : refactorings) {
            for (ImmutablePair<String, String> c : r.getInvolvedClassesAfterRefactoring()) {
                for (CodeRange codeRange : r.rightSide()) {
                    List<List<Integer>> codeRangeLst = new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(codeRange.getStartLine(), codeRange.getEndLine()))));
                    refactoredFiles.put(codeRange.getFilePath(), codeRangeLst);
                    if (printMethodPrincipalContr(commitId, c.getLeft(), codeRangeLst, r.getRefactoringType().toString()))
                        printCompound = true;
//                    refactoringInterestContr += getFileInterest(c.getLeft()); // to modify (apply contribution's formula)
                    refactoringInterestContr = 0.0; // tmp
                }
            }
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "REFACTORING", "COMPOUND", refactoringPrincipalContr, refactoringInterestContr, "");
    }

    private boolean printMethodPrincipalContr(String commitId, String file, List<List<Integer>> codeRanges, String refactoringType) {
        boolean printCompound = false;
        try {
            if (Objects.nonNull(diffEntries)) {
                for (DiffEntry diffEntry : diffEntries) {
                    if (diffEntry.getNewFilePath().equals(file)) {
                        if (Objects.nonNull(diffEntry.getFileContribution()) && !diffEntry.getFileContribution().isNaN()) {
                            refactoringPrincipalContr += diffEntry.getFileContribution();
                            printCompound = true;
                            print(commitId,
                                    file,
                                    "REFACTORING",
                                    "ENTIRE",
                                    diffEntry.getFileContribution(), refactoringInterestContr,
                                    refactoringType);
                            continue;
                        }
                        if (Objects.isNull(diffEntry.getMethods()))
                            continue;
                        for (Method method : diffEntry.getMethods()) {
                            if (codeRanges.stream().anyMatch(codeRange -> method.getStartLine() >= codeRange.get(0) && method.getEndLine() <= codeRange.get(1))) {
                                if (Objects.nonNull(method.getContribution()) && !method.getContribution().isNaN()) {
                                    refactoringPrincipalContr += method.getContribution();
//                                    if (Globals.METHOD_REFACTORINGS.contains(refactoringType)) {
                                        printCompound = true;
                                        print(commitId,
                                                file + " - " + method.getName(),
                                                "REFACTORING",
                                                "METHOD",
                                                method.getContribution(), refactoringInterestContr,
                                                refactoringType);
//                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return printCompound;
    }

    private void printNewCodeContr(String commitId) {

        if (Objects.isNull(diffEntries))
            return;

        boolean printCompound = false;
        for (DiffEntry diffEntry : diffEntries) {
                if (Objects.nonNull(diffEntry.getFileContribution())) {
                    newCodePrincipalContr += diffEntry.getFileContribution();
                    printCompound = true;
                    print(commitId,
                            diffEntry.getNewFilePath(),
                            "NEW_FILE",
                            "ENTIRE",
                            diffEntry.getFileContribution(), newCodeInterestContr,
                            diffEntry.getChangeType());
                    continue;
                }
                if (Objects.isNull(diffEntry.getMethods()))
                    continue;
                for (Method method : diffEntry.getMethods()) {
                    if (Objects.nonNull(method.getContribution()) && Objects.nonNull(method.getPath())) {
                        if (Objects.nonNull(refactoredFiles.get(method.getPath())) && refactoredFiles.get(method.getPath())
                                .stream()
                                .filter(codeRange -> Objects.nonNull(codeRange.get(0)) && Objects.nonNull(codeRange.get(1)))
                                .anyMatch(codeRange -> codeRange.get(0).equals(method.getStartLine()) && codeRange.get(1).equals(method.getEndLine())))
                            return;
                        newCodePrincipalContr += method.getContribution();
//                    newCodeInterestContr += getFileInterest(method.getPath()); // to modify (apply contribution's formula)
                        newCodeInterestContr += 0.0; // tmp
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
                        print(commitId, file, changeType, granularity, method.getContribution(), newCodeInterestContr, comment);
                    }
                }
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "NEW", "COMPOUND", newCodePrincipalContr, newCodeInterestContr, "");
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

    private double getFileInterest(String file) {
        try {
            if (Objects.nonNull(interestResponse)) {
                return interestResponse.getInterestIndicators().getRows()
                        .stream()
                        .filter(row -> file.toLowerCase().contains(row.getName().toLowerCase() + ".java"))
                        .findFirst()
                        .get().getInterest();
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }
}
