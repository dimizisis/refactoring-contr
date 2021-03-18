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
    static {
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
