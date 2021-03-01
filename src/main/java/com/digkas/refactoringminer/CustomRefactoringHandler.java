package com.digkas.refactoringminer;

import com.digkas.refactoringminer.api.interest.InterestIndicatorsResponseEntity;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;

import java.util.List;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public class CustomRefactoringHandler extends RefactoringHandler {
    private double refactoringInterestContr;
    private double refactoringPrincipalContr;
    private double newCodeInterestContr;
    private double newCodePrincipalContr;
    private InterestIndicatorsResponseEntity interestResponse;
//    private PrincipalIndicatorsResponseEntity principalResponse;

    public CustomRefactoringHandler(InterestIndicatorsResponseEntity interestResponse) {
        this.interestResponse = interestResponse;
    }

    @Override
    public void handle(String commitId, List<Refactoring> refactorings) {

        refactoringInterestContr = 0.0;
        refactoringPrincipalContr = 0.0;
        newCodeInterestContr = 0.0;
        newCodePrincipalContr = 0.0;

        /* if refactoring list is empty, only new code possible */
        if (refactorings.isEmpty()) {
//            newCode();
            compound(commitId, "New");
            return;
        }

        refactorings.forEach(r -> r.getInvolvedClassesAfterRefactoring().forEach(c -> refactoringInterestContr += getFileInterest(interestResponse, c.getLeft())));

        refactorings.forEach(r -> {
            r.getInvolvedClassesAfterRefactoring()
                    .forEach(c -> System.out.println(String.format("%s\t%s\t%s\t%s\t%g\t%s\n", commitId, c.getLeft(), "Refactoring",
                            Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()) ? "Method" : "Entire", getFileInterest(interestResponse, c.getLeft()), r.getRefactoringType().toString())));
        });

        refactorings.forEach(r -> {
            r.getInvolvedClassesAfterRefactoring()
                    .forEach(c -> Globals.output.append(String.format("%s\t%s\t%s\t%s\t%g\t%s\n", commitId, c.getLeft(), "Refactoring",
                            Globals.METHOD_REFACTORINGS.contains(r.getRefactoringType().toString()) ? "Method" : "Entire", getFileInterest(interestResponse, c.getLeft()), r.getRefactoringType().toString())));
        });
        compound(commitId, "Refactoring");
    }

    private void compound(String commitId, String type){
        Globals.output.append(String.format("%s\t%s\t%s\t%s\t%s\n", commitId, "Compound", type, "Compound", String.valueOf(refactoringInterestContr)));
    }

//    private double getFilePrincipal(InterestIndicatorsResponseEntity response, String file) {
//        try {
//            if (response != null) {
//                return response.getPrincipalIndicators().getRows()
//                        .stream()
//                        .filter(row -> file.toLowerCase().contains(row.getName().toLowerCase()+".java"))
//                        .findFirst()
//                        .get().getPrincipal();
//            }
//        } catch (Exception ignored) {}
//        return 0.0;
//    }

    private double getFileInterest(InterestIndicatorsResponseEntity response, String file) {
        try {
            if (response != null) {
                return response.getInterestIndicators().getRows()
                        .stream()
                        .filter(row -> file.toLowerCase().contains(row.getName().toLowerCase()+".java"))
                        .findFirst()
                        .get().getInterest();
            }
        } catch (Exception ignored) {}
        return 0.0;
    }
}
