/*
 * Copyright (c) 2010 Stefan Wolf
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

package com.jenkins.jplugin.gleaning;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Class which keeps the configuration for the graphviz
 * executable.
 */
public class GleaningProperty extends AbstractDescribableImpl<GleaningProperty> {

    @DataBoundConstructor
    public GleaningProperty() {
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GleaningProperty> {
        private boolean gleaningEnabled = true;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) {
            final JSONObject go = o.optJSONObject("gleaning");
            if (go != null) {
                gleaningEnabled = true;
            } else {
                gleaningEnabled = false;
            }
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Display name";
        }

        public boolean isGleaningEnabled() {
            return gleaningEnabled;
        }

        public synchronized void setGleaningEnabled(boolean gleaningEnabled) {
            this.gleaningEnabled = gleaningEnabled;
            save();
        }

    }

}
