package com.jenkins.jplugin.dependency.pojo;

import com.google.common.base.Joiner;
import com.jenkins.jplugin.dependency.constant.Constants;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 2020-04-12 add by wanghf
 */
public class UpstreamJobParams {

    private String upstreamJobParams;

    @DataBoundConstructor
    public UpstreamJobParams(String upstreamJobParams) {
        this.upstreamJobParams = upstreamJobParams;
    }

    public String getUpstreamJobParams() {
        return upstreamJobParams;
    }

    public void setUpstreamJobParams(String upstreamJobParams) {
        this.upstreamJobParams = upstreamJobParams;
    }

    public String revisedJobParams(Map<String, String> oldNewJobName) {

        List<Param> params = getJobParams();

        for (Map.Entry<String, String> entry : oldNewJobName.entrySet()) {
            for (Param param : params) {
                if (param instanceof JobParam) {
                    if (((JobParam) param).getName().equals(entry.getKey())) {
                        ((JobParam) param).setName(entry.getValue());
                    }
                } else if (param instanceof JobDateParam) {
                    if (((JobDateParam) param).equals(entry.getKey())) {
                        ((JobDateParam) param).setName(entry.getValue());
                    }
                }
            }
        }
        return Joiner.on(Constants.MULTI_JOB_PARAM_JOINER).join(params);
    }

    /**
     * 把匹配的任务的参数删掉
     *
     * @param jobName
     * @return
     */
    public String deleteMatchJobParam(String jobName) {

        List<Param> params = getJobParams();
        Param param = null;

        for (Param _param : params) {
            if (_param instanceof JobParam) {
                if (((JobParam) _param).getName().equals(jobName)) {
                    param = _param;
                }
            } else if (_param instanceof JobDateParam) {
                if (((JobDateParam) _param).equals(jobName)) {
                    param = _param;
                }
            }
        }
        if (param != null){
            params.remove(param);
        }

        return Joiner.on(Constants.MULTI_JOB_PARAM_JOINER).join(params);
    }

    private List<Param> getJobParams() {

        List<Param> paramList = new ArrayList<>();
        Param param = null;

        String[] _params = upstreamJobParams.split(",");
        if (_params != null
                && _params.length != 0
                && !StringUtils.isEmpty(_params[0])) {

            for (String str : _params) {
                String[] jobParams = str.split("\\.");

                if (jobParams.length == 2) {
                    param = new JobParam(jobParams[0], jobParams[1]);
                } else if (jobParams.length == 3) {
                    param = new JobDateParam(jobParams[0], jobParams[1], jobParams[2]);
                } else {
                    param = new Param(str);
                }
                paramList.add(param);
            }
        }
        return paramList;
    }
}
