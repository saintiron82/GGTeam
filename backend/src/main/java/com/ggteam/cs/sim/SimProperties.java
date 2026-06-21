package com.ggteam.cs.sim;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("sim")
@ConfigurationProperties(prefix = "app.sim")
public class SimProperties {
    private int count = 100;
    private int itemTarget = 500;
    private int durationMinutes = 20;
    private boolean jitter = false;
    private boolean autoStart = false;

    public int getCount() { return count; }
    public void setCount(int v) { this.count = v; }
    public int getItemTarget() { return itemTarget; }
    public void setItemTarget(int v) { this.itemTarget = v; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int v) { this.durationMinutes = v; }
    public boolean isJitter() { return jitter; }
    public void setJitter(boolean v) { this.jitter = v; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean v) { this.autoStart = v; }
}
