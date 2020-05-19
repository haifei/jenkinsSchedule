package com.jenkins.jplugin.dependency.pojo;

import com.jenkins.jplugin.dependency.enums.DateType;
import com.jenkins.jplugin.dependency.enums.LogicSymbol;

import java.util.List;

/**
 * 2020-04-12 add by wanghf
 */
public class TriggerConditionInfo {

    private DateType dateType;

    private LogicSymbol logicSymbol;

    private List<String> dateList;

    public TriggerConditionInfo(DateType dateType, LogicSymbol logicSymbol, List<String> dateList) {
        this.dateType = dateType;
        this.logicSymbol = logicSymbol;
        this.dateList = dateList;
    }

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public LogicSymbol getLogicSymbol() {
        return logicSymbol;
    }

    public void setLogicSymbol(LogicSymbol logicSymbol) {
        this.logicSymbol = logicSymbol;
    }

    public List<String> getDateList() {
        return dateList;
    }

    public void setDateList(List<String> dateList) {
        this.dateList = dateList;
    }
}
