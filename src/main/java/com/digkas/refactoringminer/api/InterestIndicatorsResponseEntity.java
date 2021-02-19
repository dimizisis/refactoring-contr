
package com.digkas.refactoringminer.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InterestIndicatorsResponseEntity {

    @SerializedName("interestIndicators")
    @Expose
    private InterestIndicators interestIndicators;

    public InterestIndicators getInterestIndicators() {
        return interestIndicators;
    }

    public void setInterestIndicators(InterestIndicators interestIndicators) {
        this.interestIndicators = interestIndicators;
    }

}
