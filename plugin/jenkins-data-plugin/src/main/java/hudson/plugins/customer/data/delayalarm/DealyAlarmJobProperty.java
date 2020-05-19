/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.customer.data.delayalarm;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.regex.Pattern;

/**
 * @author wanghf
 * @Desc   延迟告警， 配置格式 ： 30 08 * * *
 */
public class DealyAlarmJobProperty extends JobProperty<Job<?, ?>> {

    private final String delayAlarmConfig;

    @DataBoundConstructor
    public DealyAlarmJobProperty(final String delayAlarmConfig) {

        this.delayAlarmConfig = delayAlarmConfig;
    }

    public String getDelayAlarmConfig() {

        return delayAlarmConfig;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends Job> jobType
        ) {

            return true;
        }

        @Override
        public JobProperty<?> newInstance(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            final JSONObject watcherData = formData.getJSONObject("delayAlarmEnabled");
            if (watcherData.isNullObject()){
                return null;
            }

            final String delayTime = watcherData.getString( "delayAlarmConfig" );
            if (delayTime == null || delayTime.isEmpty()) {
                return null;
            }

            return new DealyAlarmJobProperty(delayTime);
        }

        public FormValidation doCheckDelayAlarmConfig(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.warning("Empty Time!");
            }

            if(isHourAndMinute(value)){
                return FormValidation.ok();
            } else{
                return FormValidation.error("Time Format eg. 30 08 * * * or 30+10 * * * * or 30 * * * *");
            }
        }

        public static boolean isHourAndMinute(String str){
            Pattern pattern = Pattern.compile("^([0-5][0-9](\\+[0-5][0-9])?) ((1|0?)[0-9]|2[0-3]|\\*) \\* \\* \\*$");
            return pattern.matcher(str).matches();
        }
        @Override
        public String getDisplayName() {

            return "延迟告警";
        }
    }
}
