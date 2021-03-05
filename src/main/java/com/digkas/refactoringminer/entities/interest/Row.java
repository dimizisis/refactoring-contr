
package com.digkas.refactoringminer.entities.interest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Row {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("Interest")
    @Expose
    private Double interest;
    @SerializedName("IP")
    @Expose
    private Double iP;
    @SerializedName("MPC")
    @Expose
    private Double mPC;
    @SerializedName("DIT")
    @Expose
    private Double dIT;
    @SerializedName("NOCC")
    @Expose
    private Double nOCC;
    @SerializedName("RFC")
    @Expose
    private Double rFC;
    @SerializedName("LCOM")
    @Expose
    private Double lCOM;
    @SerializedName("WMC")
    @Expose
    private Double wMC;
    @SerializedName("DAC")
    @Expose
    private Double dAC;
    @SerializedName("NOM")
    @Expose
    private Double nOM;
    @SerializedName("LOC")
    @Expose
    private Double lOC;
    @SerializedName("NOP")
    @Expose
    private Double nOP;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getInterest() {
        return interest;
    }

    public void setInterest(Double interest) {
        this.interest = interest;
    }

    public Double getIP() {
        return iP;
    }

    public void setIP(Double iP) {
        this.iP = iP;
    }

    public Double getMPC() {
        return mPC;
    }

    public void setMPC(Double mPC) {
        this.mPC = mPC;
    }

    public Double getDIT() {
        return dIT;
    }

    public void setDIT(Double dIT) {
        this.dIT = dIT;
    }

    public Double getNOCC() {
        return nOCC;
    }

    public void setNOCC(Double nOCC) {
        this.nOCC = nOCC;
    }

    public Double getRFC() {
        return rFC;
    }

    public void setRFC(Double rFC) {
        this.rFC = rFC;
    }

    public Double getLCOM() {
        return lCOM;
    }

    public void setLCOM(Double lCOM) {
        this.lCOM = lCOM;
    }

    public Double getWMC() {
        return wMC;
    }

    public void setWMC(Double wMC) {
        this.wMC = wMC;
    }

    public Double getDAC() {
        return dAC;
    }

    public void setDAC(Double dAC) {
        this.dAC = dAC;
    }

    public Double getNOM() {
        return nOM;
    }

    public void setNOM(Double nOM) {
        this.nOM = nOM;
    }

    public Double getLOC() {
        return lOC;
    }

    public void setLOC(Double lOC) {
        this.lOC = lOC;
    }

    public Double getNOP() {
        return nOP;
    }

    public void setNOP(Double nOP) {
        this.nOP = nOP;
    }

}
