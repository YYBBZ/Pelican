package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class LiveRuns {
    private static final int TEXT_LIMIT = 120000;
    private static final java.util.Map<java.lang.String, app.pelican.check.LiveRuns.Entry> entries = new java.util.LinkedHashMap();

    private LiveRuns() {
    }

    private static class Entry {
        java.lang.String json;
        java.lang.String lineId;
        java.lang.StringBuilder output;
        long published;
        java.lang.String stage;

        private Entry() {
            this.stage = "";
            this.output = new java.lang.StringBuilder();
        }
    }

    public static class Snapshot {
        public final java.lang.String output;
        public final app.pelican.check.RunData run;

        Snapshot(app.pelican.check.RunData runData, java.lang.String str) {
            this.run = runData;
            this.output = str;
        }
    }

    public static synchronized void publish(app.pelican.check.RunData runData, app.pelican.check.ApiClient.Progress progress) {
        app.pelican.check.LiveRuns.Entry entry = entries.get(runData.id);
        if (entry == null) {
            entry = new app.pelican.check.LiveRuns.Entry();
            entry.lineId = runData.lineId;
            entries.put(runData.id, entry);
        }
        if (progress != null) {
            if (progress.resetOutput) {
                entry.output.setLength(0);
            }
            if (progress.delta != null && !progress.delta.isEmpty()) {
                entry.output.append(progress.delta);
                if (entry.output.length() > TEXT_LIMIT) {
                    entry.output.delete(0, entry.output.length() - TEXT_LIMIT);
                }
            }
        }
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        if (entry.json == null || jCurrentTimeMillis - entry.published >= 250 || !entry.stage.equals(runData.stage) || progress == null || progress.completedAttempt != null || progress.resetOutput) {
            entry.json = app.pelican.check.Json.gson().toJson(runData);
            entry.published = jCurrentTimeMillis;
            entry.stage = runData.stage;
        }
    }

    public static synchronized app.pelican.check.LiveRuns.Snapshot read(java.lang.String str) {
        app.pelican.check.LiveRuns.Entry entry;
        entry = entries.get(str);
        return entry == null ? null : new app.pelican.check.LiveRuns.Snapshot((app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(entry.json, app.pelican.check.RunData.class), entry.output.toString());
    }

    public static synchronized app.pelican.check.RunData forLine(java.lang.String str) {
        for (app.pelican.check.LiveRuns.Entry entry : entries.values()) {
            if (entry.lineId.equals(str)) {
                return (app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(entry.json, app.pelican.check.RunData.class);
            }
        }
        return null;
    }

    public static synchronized void remove(java.lang.String str) {
        entries.remove(str);
    }

    public static synchronized boolean any() {
        return !entries.isEmpty();
    }
}
