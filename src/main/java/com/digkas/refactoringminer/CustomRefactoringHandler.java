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
    private Double newCodePrincipalAddContr;
    private Map<String, List<List<Integer>>> refactoredFiles;
    private InterestIndicatorsResponseEntity interestResponse;
    private PrincipalResponseEntity[] principalResponse;
    private List<DiffEntry> diffEntries;

    public CustomRefactoringHandler() {
        this.refactoringInterestContr = 0.0;
        this.refactoringPrincipalContr = 0.0;
        this.newCodeInterestContr = 0.0;
        this.newCodePrincipalContr = 0.0;
        this.newCodePrincipalAddContr = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    public CustomRefactoringHandler(PrincipalResponseEntity[] principalResponse, InterestIndicatorsResponseEntity interestResponse) {
        this.interestResponse = interestResponse;
        this.principalResponse = principalResponse;
        this.refactoringInterestContr = 0.0;
        this.refactoringPrincipalContr = 0.0;
        this.newCodeInterestContr = 0.0;
        this.newCodePrincipalContr = 0.0;
        this.newCodePrincipalAddContr = 0.0;
        this.refactoredFiles = new HashMap<>();
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {
        diffEntries = getDiffEntriesAtCommit(commitId);
        /* if refactoring list is not empty, print refactoring contribution, else only new code contribution is printed */
        if (refactorings.stream().anyMatch(r -> Globals.CLASS_REFACTORINGS.contains(r.getRefactoringType().toString())
                || Globals.CLASS_REFACTORINGS.contains(r.getRefactoringType().toString())))
            printRefactoringContr(commitId, refactorings);

        if (Objects.nonNull(diffEntries) && !diffEntries.isEmpty())
            printNewCodeContr(commitId);
    }

    private void printRefactoringContr(String commitId, List<Refactoring> refactorings) {
        refactorings
                .stream()
                .filter(r -> Globals.CLASS_REFACTORINGS.contains(r.getRefactoringType().toString()) || Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()))
                .forEach(r -> r.getInvolvedClassesAfterRefactoring()
                        .forEach(c -> {
                            r.rightSide().forEach(codeRange -> {
                                List<List<Integer>> codeRangeLst = new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(codeRange.getStartLine(), codeRange.getEndLine()))));
                                refactoredFiles.put(codeRange.getFilePath(), codeRangeLst);
                                printMethodPrincipalContr(commitId, c.getLeft(), codeRangeLst, r.getRefactoringType().toString());
                            });
//                        refactoringInterestContr += getFileInterest(c.getLeft()); // to modify (apply contribution's formula)
                            refactoringInterestContr = 0.0; // tmp
                            print(commitId,
                                    c.getLeft(),
                                    "REFACTORING",
                                    Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()) ? "METHOD" : "ENTIRE",
                                    refactoringPrincipalContr, refactoringInterestContr,
                                    r.getRefactoringType().toString());
                        }));

        /* print compounded */
        print(commitId, "COMPOUND", "REFACTORING", "COMPOUND", refactoringPrincipalContr, refactoringInterestContr, "");
    }

    private void printMethodPrincipalContr(String commitId, String file, List<List<Integer>> codeRanges, String refactoringType) {
        try {
            if (Objects.nonNull(diffEntries)) {

                for (DiffEntry diffEntry : diffEntries) {
                    if (diffEntry.getNewFilePath().equals(file)) {
                        if (Objects.nonNull(diffEntry.getFileContribution()) && !diffEntry.getFileContribution().isNaN()) {
                            refactoringPrincipalContr += diffEntry.getFileContribution();
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
                                    if (Globals.METHOD_REFACTORINGS.contains(refactoringType)) {
                                        print(commitId,
                                                file + " - " + method.getName(),
                                                "REFACTORING",
                                                "METHOD",
                                                method.getContribution(), refactoringInterestContr,
                                                refactoringType);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void printNewCodeContr(String commitId) {

        if (Objects.isNull(diffEntries))
            return;

        boolean printCompound = false;
        for (DiffEntry diffEntry : diffEntries) {
                if (Objects.nonNull(diffEntry.getFileContribution())) {
                    newCodePrincipalContr += diffEntry.getFileContribution();
                    if (diffEntry.getChangeType().equals("ADD"))
                        newCodePrincipalAddContr += diffEntry.getFileContribution();
                    printCompound = true;
                    print(commitId,
                            diffEntry.getNewFilePath(),
                            diffEntry.getChangeType(),
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
                        if (method.getClassifier().equals("ADD"))
                            newCodePrincipalAddContr += method.getContribution();
                        newCodePrincipalContr += method.getContribution();
//                    newCodeInterestContr += getFileInterest(method.getPath()); // to modify (apply contribution's formula)
                        newCodeInterestContr += 0.0; // tmp
                        String granularity = "";
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
                        print(commitId, file, diffEntry.getChangeType(), granularity, method.getContribution(), newCodeInterestContr, comment);
                    }
                }
        }

        /* print compounded */
        if (printCompound)
            print(commitId, "COMPOUND", "NEW", "COMPOUND", newCodePrincipalContr, newCodeInterestContr, "");
        if (newCodePrincipalAddContr != 0.0)
            print(commitId, "ADD-COMPOUND", "NEW", "ADD-COMPOUND", newCodePrincipalAddContr, 0.0, "");
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
