package com.redis.workshop.hub.model;

import java.util.List;

public class WorkshopHealthcheck {
    private List<String> test;
    private String interval;
    private String timeout;
    private Integer retries;
    private String startPeriod;

    public List<String> getTest() {
        if (test == null || test.isEmpty()) {
            return List.of();
        }
        return List.copyOf(test);
    }

    public void setTest(List<String> test) {
        this.test = test;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(String startPeriod) {
        this.startPeriod = startPeriod;
    }
}
