package io.snw.obsidiandestroyer;

public class BlockTimer {
    private long timeToLive = 10000;

    public BlockTimer(long timeToLive) {
        this.timeToLive = timeToLive + System.currentTimeMillis();
    }

    public long getTimeToLive() {
        return timeToLive;
    }
}
