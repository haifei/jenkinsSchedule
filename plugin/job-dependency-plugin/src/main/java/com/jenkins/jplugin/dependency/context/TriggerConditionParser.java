package com.jenkins.jplugin.dependency.context;

import com.jenkins.jplugin.dependency.constant.Constants;
import com.jenkins.jplugin.dependency.enums.DateType;
import com.jenkins.jplugin.dependency.enums.LogicSymbol;
import com.jenkins.jplugin.dependency.exception.FormValidationException;
import com.jenkins.jplugin.dependency.exception.JobDependencyException;
import com.jenkins.jplugin.dependency.exception.JobDependencyRuntimeException;
import com.jenkins.jplugin.dependency.pojo.JobDependencyProperty;
import com.jenkins.jplugin.dependency.pojo.TriggerConditionInfo;
import com.jenkins.jplugin.dependency.utils.DateRange;
import com.jenkins.jplugin.dependency.utils.Utils;
import hudson.model.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2020-04-12 add by wanghf
 */
public class TriggerConditionParser {

    private static final Logger logger = LoggerFactory.getLogger(TriggerConditionParser.class);

    private static final String triggerConditionANDRegular = "^\\s*[H|D|W|M|h|d|w|m]\\s*=(\\s*..\\s*&)+\\s*[^\\|]{2}\\s*$";
    private static final String triggerConditionORRegular = "^\\s*[H|D|W|M|h|d|w|m]\\s*=(\\s*..\\s*\\|)+\\s*[^\\|]{2}\\s*$";
    private static final String triggerConditionEqualRegular = "^\\s*[H|D|W|M|h|d|w|m]\\s*=\\s*..\\s*$";
    //针对 @$ ,暂不考虑 #$
    private static final String triggerConditionDayRegular = "^\\s*[D|d]\\s*=\\s*..\\s*$";

    //针对 *$ 和 %$
    private static final String triggerConditionContextRegular = "^\\s*[H|D|M|h|d|m]\\s*=\\s*..\\s*([+|-]\\s*.{1,2}\\s*){0,1}$";

    private Pattern pattern_and;
    private Pattern pattern_or;
    private Pattern pattern_equal;
    private Pattern pattern_day;
    private Pattern pattern_context;

    private static final String EQUAL = "=";

    private JobDependencyProperty jobProperty;
    private TaskListener listener;
    private String triggerConditionStr;

    public TriggerConditionParser(JobDependencyProperty jobProperty, TaskListener listener) {
        this(jobProperty);
        this.listener = listener;
    }

    public TriggerConditionParser(JobDependencyProperty jobProperty) {
        this.jobProperty = jobProperty;
        this.triggerConditionStr = jobProperty.getTriggerCondition();
        pattern_and = Pattern.compile(triggerConditionANDRegular);
        pattern_or = Pattern.compile(triggerConditionORRegular);
        pattern_equal = Pattern.compile(triggerConditionEqualRegular);
        pattern_day = Pattern.compile(triggerConditionDayRegular);
        pattern_context = Pattern.compile(triggerConditionContextRegular);
    }

    public TriggerConditionInfo parse() {

        String[] strs = triggerConditionStr.split(EQUAL);
        char dateType = strs[0].toUpperCase().trim().charAt(0);
        String dateExpression = strs[1].trim();

        return new TriggerConditionInfo(DateType.getDateType(dateType), findLogicSymbolFromExpresstion(dateExpression), dateSplit(dateExpression));

    }

    public void checkBeforeParse() throws FormValidationException {
        Matcher matcher_and = pattern_and.matcher(triggerConditionStr);
        Matcher matcher_or = pattern_or.matcher(triggerConditionStr);
        Matcher matcher_equal = pattern_equal.matcher(triggerConditionStr);
        Matcher matcher_day = pattern_day.matcher(triggerConditionStr);
        Matcher matcher_context = pattern_context.matcher(triggerConditionStr);

        if (triggerConditionStr.contains(Constants.DATE_OF_CALCULATION_CONTEXT)
                || triggerConditionStr.contains(Constants.DATE_RELY_ON_ONESELF)) {

            if (LogicSymbol.NO != findLogicSymbolFromExpresstion(triggerConditionStr)) {
                throw new FormValidationException(String.format("条件参数格式错误.详见文档.condition:%s", triggerConditionStr));
            }

            if (!matcher_context.matches()) {
                throw new FormValidationException(String.format("条件参数格式错误.详见文档.condition:%s", triggerConditionStr));
            }
        } else if (triggerConditionStr.contains(Constants.DAY_OF_ANY_MONTH)) {
            if (!matcher_day.matches()) {
                throw new FormValidationException(String.format("条件参数格式错误.详见文档.condition:%s", triggerConditionStr));
            }
        } else if (!matcher_and.matches()
                && !matcher_or.matches()
                && !matcher_equal.matches()) {
            throw new FormValidationException(String.format("条件参数格式错误.详见文档.condition:%s", triggerConditionStr));
        }
    }

    public void checkAfterParse() throws JobDependencyException {

        String[] strs = triggerConditionStr.split(EQUAL);
        char dateType = strs[0].toUpperCase().trim().charAt(0);
        String dateExpression = strs[1].trim();
        List<String> dateSplit = dateSplit(dateExpression);

        if (dateExpression.contains(Constants.DATE_RELY_ON_ONESELF)
                || dateExpression.contains(Constants.DATE_OF_CALCULATION_CONTEXT)) {
            return;
        }

        logger.info(String.format("Check date range.Condition:%s", triggerConditionStr));
        switch (dateType) {
            case 'H':
                DateRange.checkHourdates(dateSplit);
                break;
            case 'D':
                DateRange.checkDayDates(dateSplit);
                break;
            case 'W':
                DateRange.checkWeekDates(dateSplit);
                break;
            case 'M':
                DateRange.checkMonthDates(dateSplit);
                break;
            default:
                throw new JobDependencyRuntimeException(String.format("不支持的类型:%s.支持的类型:H|D|W|M", dateType));
        }
        logger.info(String.format("Check data range successfullu.Condition:%s", triggerConditionStr));
    }

    private List<String> dateSplit(String dateExpression) {

        String[] strs = null;

        if (dateExpression.indexOf(LogicSymbol.AND.value) != -1) {
            strs = dateExpression.split(String.valueOf(LogicSymbol.AND.value));
        } else if (dateExpression.indexOf(LogicSymbol.OR.value) != -1) {
            strs = dateExpression.split("\\|");
        } else {
            strs = new String[]{dateExpression};
        }
        return Utils.trimForList(Arrays.asList(strs));
    }

    private LogicSymbol findLogicSymbolFromExpresstion(String dateExpression) {

        if (dateExpression.indexOf(LogicSymbol.AND.value) != -1) {
            return LogicSymbol.AND;
        } else if (dateExpression.indexOf(LogicSymbol.OR.value) != -1) {
            return LogicSymbol.OR;
        } else {
            return LogicSymbol.NO;
        }
    }

}
