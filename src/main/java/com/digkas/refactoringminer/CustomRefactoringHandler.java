package com.digkas.refactoringminer;

import com.digkas.refactoringminer.api.interest.InterestIndicatorsResponseEntity;
import com.digkas.refactoringminer.api.principal.DiffEntry;
import com.digkas.refactoringminer.api.principal.Method;
import com.digkas.refactoringminer.api.principal.PrincipalResponseEntity;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.util.*;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public class CustomRefactoringHandler extends RefactoringHandler {
    private double refactoringInterestContr;
    private double refactoringPrincipalContr;
    private double newCodeInterestContr;
    private double newCodePrincipalContr;
    private Map<String, List<List<Integer>>> refactoredFiles;
    private InterestIndicatorsResponseEntity interestResponse;
    private PrincipalResponseEntity[] principalResponse;

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
        /* if refactoring list is not empty, print refactoring contribution, else only new code contribution is printed */
        if (!refactorings.isEmpty())
            printRefactoringContr(commitId, refactorings);
        printNewCodeContr(commitId);
    }

    private void printRefactoringContr(String commitId, List<Refactoring> refactorings) {
        refactorings.forEach(r -> {
            r.getInvolvedClassesAfterRefactoring()
                    .forEach(c -> {
                        r.rightSide().forEach(codeRange -> refactoredFiles.put(codeRange.getFilePath(), new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(codeRange.getStartLine(), codeRange.getEndLine()))))));
                        refactoringPrincipalContr = getFilePrincipalContr(c.getLeft());
                        refactoringInterestContr += getFileInterest(c.getLeft()); // to modify (apply contribution's formula)
                        print(commitId,
                                c.getLeft(),
                                "Refactoring",
                                Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()) ? "Method" : "Entire",
                                refactoringPrincipalContr, refactoringInterestContr,
                                r.getRefactoringType().toString());
                    });
        });

        /* print compounded */
        print(commitId, "Compound", "Refactoring", "Compound", refactoringPrincipalContr, refactoringInterestContr, "");
    }

    private void printNewCodeContr(String commitId) {
        DiffEntry diffEntry = new DiffEntry(); // getDiffEntryAtCommit(commitId);
        diffEntry
                .getMethods()
                .stream()
                .filter(method -> !method.getContribution().isNaN() && method.getStartLine() != refactoredFiles.get(method.getPath()).get(0).get(0)) // to be compared with all List<Integer>
                .forEach(method -> {
                    newCodePrincipalContr += method.getContribution();
                    newCodeInterestContr += getFileInterest(method.getPath()); // to modify (apply contribution's formula)
                    print(commitId, diffEntry.getNewFilePath(), diffEntry.getChangeType(), "Method", newCodePrincipalContr, newCodeInterestContr, diffEntry.getChangeType());
                });

        /* print compounded */
        print(commitId, "Compound", "New", "Compound", newCodePrincipalContr, newCodeInterestContr, "");
    }

    private void print(String commitId, String file, String type, String granularity, Double principalContribution, Double interestContribution, String comment){
        System.out.printf("%s\t%s\t%s\t%s\t%g\t%g\t%s\n", commitId, file, type, granularity, principalContribution, interestContribution, comment);
        Globals.output.append(String.format("%s\t%s\t%s\t%s\t%g\t%g\t%s\n", commitId, file, type, granularity, principalContribution, interestContribution, comment));
    }

    private double getFilePrincipalContr(String file) {
        double contributionSum = 0.0;
        try {
            if (Objects.nonNull(principalResponse)) {
                for (PrincipalResponseEntity principalResponseEntity : principalResponse) {
                    contributionSum = principalResponseEntity
                            .getDiffEntries()
                            .stream()
                            .filter(diffEntry -> diffEntry.getNewFilePath().equals(file))
                            .mapToDouble(diffEntry -> diffEntry.getMethods()
                                    .stream().mapToDouble(Method::getContribution).sum()).sum();
                    if (contributionSum != 0.0)
                        return contributionSum;
                }
            }
        } catch (Exception ignored) {}
        return contributionSum;
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
        } catch (Exception ignored) {}
        return 0.0;
    }
}
