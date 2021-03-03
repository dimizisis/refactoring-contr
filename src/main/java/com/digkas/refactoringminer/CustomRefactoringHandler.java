package com.digkas.refactoringminer;

import com.digkas.refactoringminer.api.interest.InterestIndicatorsResponseEntity;
import com.digkas.refactoringminer.api.principal.DiffEntry;
import com.digkas.refactoringminer.api.principal.PrincipalResponseEntity;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

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
    List<DiffEntry> diffEntries;

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
        printNewCodeContr(commitId);
    }

    private void printRefactoringContr(String commitId, List<Refactoring> refactorings) {
        refactorings
                .stream()
                .filter(r -> Globals.CLASS_REFACTORINGS.contains(r.getRefactoringType().toString()) || Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()))
                .forEach(r -> {
                    r.getInvolvedClassesAfterRefactoring()
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
                                        "Refactoring",
                                        Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()) ? "Method" : "Entire",
                                        refactoringPrincipalContr, refactoringInterestContr,
                                        r.getRefactoringType().toString());
                            });
                });

//        if (refactoringPrincipalContr == 0.0 && refactoringInterestContr == 0.0)
//            return;

        /* print compounded */
        print(commitId, "Compound", "Refactoring", "Compound", refactoringPrincipalContr, refactoringInterestContr, "");
    }

    private void printMethodPrincipalContr(String commitId, String file, List<List<Integer>> codeRanges, String refactoringType) {
        try {
            if (Objects.nonNull(diffEntries)) {
                diffEntries
                        .stream()
                        .filter(diffEntry -> diffEntry.getNewFilePath().equals(file))
                        .forEach(diffEntry -> diffEntry
                                .getMethods()
                                .stream()
                                .filter(method -> codeRanges.stream().anyMatch(codeRange -> method.getStartLine() >= codeRange.get(0) && method.getEndLine() <= codeRange.get(1)))
                                .forEach(method -> {
                                    refactoringPrincipalContr += method.getContribution();
                                    if (Globals.METHOD_REFACTORINGS.contains(refactoringType))
                                        print(commitId,
                                                file + " - " + method.getName(),
                                                "Refactoring",
                                                "Method",
                                                method.getContribution(), refactoringInterestContr,
                                                refactoringType);
                                }));
//                if (Globals.CLASS_REFACTORINGS.contains(refactoringType))
//                    print(commitId,
//                            file,
//                            "Refactoring",
//                            "Entire",
//                            refactoringPrincipalContr, refactoringInterestContr,
//                            refactoringType);
            }
        } catch (Exception ignored) {}
    }

    private void printNewCodeContr(String commitId) {

        if (Objects.isNull(diffEntries))
            return;

        diffEntries.forEach(diffEntry -> diffEntry
                .getMethods()
                .stream()
                .filter(method -> Objects.nonNull(method.getContribution()) && Objects.nonNull(method.getPath()))
                .forEach(method -> {
                    if (Objects.nonNull(refactoredFiles.get(method.getPath())) && refactoredFiles.get(method.getPath())
                            .stream()
                            .filter(codeRange -> Objects.nonNull(codeRange.get(0)) && Objects.nonNull(codeRange.get(1)))
                            .anyMatch(codeRange -> codeRange.get(0).equals(method.getStartLine()) && codeRange.get(1).equals(method.getEndLine())))
                        return;
                    newCodePrincipalContr += method.getContribution();
//                    newCodeInterestContr += getFileInterest(method.getPath()); // to modify (apply contribution's formula)
                    newCodeInterestContr += 0.0; // tmp
                    String granularity = "";
                    String comment = "";
                    String file = "";
                    if (diffEntry.getChangeType().equals("ADD")) {
                        file = diffEntry.getNewFilePath();
                        granularity = "Entire";
                        comment = diffEntry.getChangeType();
                    }
                    else if (method.getClassifier().endsWith("Insert")) {
                        file = diffEntry.getNewFilePath() + " - " + method.getName();
                        granularity = "Method";
                        comment = method.getClassifier();
                    }
                    print(commitId, file, diffEntry.getChangeType(), granularity, method.getContribution(), newCodeInterestContr, comment);
                }));

//        if (newCodePrincipalContr == 0.0 && newCodeInterestContr == 0.0)
//            return;

        /* print compounded */
        print(commitId, "Compound", "New", "Compound", newCodePrincipalContr, newCodeInterestContr, "");
    }

    private void print(String commitId, String file, String type, String granularity, Double principalContribution, Double interestContribution, String comment) {
        System.out.printf("%s\t%s\t%s\t%s\t%g\t%g\t%s\n", commitId, file, type, granularity, principalContribution, interestContribution, comment);
        Globals.output.append(String.format("%s\t%s\t%s\t%s\t%g\t%g\t%s\n", commitId, file, type, granularity, principalContribution, interestContribution, comment));
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
