
package com.digkas.refactoringminer.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Column {

    @SerializedName("label")
    @Expose
    private String label;
    @SerializedName("field")
    @Expose
    private String field;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

}
