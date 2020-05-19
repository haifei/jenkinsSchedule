package com.jenkins.jplugin.gleaning.model;

/**
 * 2020-04-21  add by wanghf
 */
public abstract class Task {
        private int id;
        private String name;
        private String lastContext;
        private String lastBuildTime;
        private String owner;
        private String isChecked;
        private boolean isDeleted = false;

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getIsChecked() {
            return isChecked;
        }

        public void setIsChecked(String isChecked) {
            this.isChecked = isChecked;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLastContext() {
            return lastContext;
        }

        public void setLastContext(String lastContext) {
            this.lastContext = lastContext;
        }

        public String getLastBuildTime() {
            return lastBuildTime;
        }

        public void setLastBuildTime(String lastBuildTime) {
            this.lastBuildTime = lastBuildTime;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

}
