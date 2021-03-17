package com.digkas.refactoringminer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Dimitrios Zisis <zdimitris@outlook.com>
 * Date: 18/02/2021
 */
public final class Globals {
    static StringBuilder output;
    static String[] outputHeaders;
    static AtomicInteger progress = new AtomicInteger(0);
    static Set<String> METHOD_REFACTORINGS;
    static Set<String> CLASS_REFACTORINGS;
    static {

        METHOD_REFACTORINGS = new HashSet<String>() {{
            add("CHANGE_RETURN_TYPE");
            add("CHANGE_VARIABLE_TYPE");
            add("CHANGE_PARAMETER_TYPE");
            add("PARAMETERIZE_VARIABLE");
            add("MERGE_VARIABLE");
            add("MERGE_PARAMETER");
            add("SPLIT_VARIABLE");
            add("SPLIT_PARAMETER");
            add("ADD_PARAMETER");
            add("REMOVE_PARAMETER");
        }};

        CLASS_REFACTORINGS = new HashSet<String>() {{
            add("PUSH_DOWN_OPERATION");
            add("EXTRACT_OPERATION");
            add("PULL_UP_OPERATION");
            add("MOVE_ATTRIBUTE");
            add("REPLACE_ATTRIBUTE");
            add("PULL_UP_ATTRIBUTE");
            add("PUSH_DOWN_ATTRIBUTE");
            add("EXTRACT_SUPERCLASS");
            add("EXTRACT_INTERFACE");
            add("EXTRACT_SUBCLASS");
            add("EXTRACT_CLASS");
            add("MOVE_RENAME_CLASS");
            add("CHANGE_ATTRIBUTE_TYPE");
            add("EXTRACT_ATTRIBUTE");
            add("MOVE_RENAME_ATTRIBUTE");
            add("EXTRACT_AND_MOVE_OPERATION");
            add("MOVE_AND_RENAME_OPERATION");
            add("MOVE_AND_INLINE_OPERATION");
            add("REPLACE_VARIABLE_WITH_ATTRIBUTE");
            add("MERGE_ATTRIBUTE");
            add("SPLIT_ATTRIBUTE");
        }};

        outputHeaders = new String[]{
                "CommitId\t",
                "InvolvedFile\t",
                "TypeOfChange\t",
                "Granularity\t",
                "TDContributionPrincipal\t",
                "TDContributionInterest\t",
                "Comment\n"
        };

        output = new StringBuilder();
    }

    public synchronized static void increaseProgress() { progress.incrementAndGet(); }

    public synchronized static void append(String s) { output.append(s); }
}
