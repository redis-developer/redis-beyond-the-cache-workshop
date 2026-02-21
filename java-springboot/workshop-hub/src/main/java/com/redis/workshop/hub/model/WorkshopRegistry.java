package com.redis.workshop.hub.model;

import java.util.List;

public class WorkshopRegistry {
    private int version;
    private List<Workshop> workshops;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Workshop> getWorkshops() {
        return workshops;
    }

    public void setWorkshops(List<Workshop> workshops) {
        this.workshops = workshops;
    }
}
