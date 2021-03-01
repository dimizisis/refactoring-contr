
package com.digkas.refactoringminer.api.interest;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InterestIndicators {

    @SerializedName("columns")
    @Expose
    private List<Column> columns = null;
    @SerializedName("rows")
    @Expose
    private List<Row> rows = null;

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

}
