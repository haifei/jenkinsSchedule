package com.jenkins.jplugin.dependency.enums;

/**
 * 2020-04-12 add by wanghf
 */
public enum LogicSymbol {

    OR('|'), AND('&'), NO('#');

    LogicSymbol(char value) {
        this.value = value;
    }

    public char value;

    public static LogicSymbol getLogicSymbol(char value) {
        for (LogicSymbol logicSymbol : LogicSymbol.values()) {
            if (value == (logicSymbol.value)) {
                return logicSymbol;
            }
        }
        return LogicSymbol.NO;
    }

}
