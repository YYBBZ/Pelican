package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class RunData {
    public int inputAuditVersion;
    public String inputTokenModeUsed="amount";
    public double inputTokenRatioUsed=5.0;
    public String qualityStatus="",qualitySummary="",lowerEquation="";
    public String promptTextUsed="",promptSequenceRevision="",sequenceNote="";
    public int promptSequenceIndex,promptSequenceSize;
    public boolean sequenceAdvanced;
    public String originalRunJson="",originalStatus="",reassessmentNote="";
    public long reassessedAt;

    public int inputTokenExcessUsed = 800;
    public boolean inputTokenAlert;
    public Double inputTokenBaseline = null;
    public int inputTokenReferenceRuns;
    public double inputTokenMultiplier = 2.0;
    public String inputTokenSignature = "";
    public String inputTokenNote = "";
    public long attemptStartedAt;
    public int currentAttempt;
    public int currentSample;
    public long endedAt;
    public boolean interrupted;
    public java.lang.String lineId;
    public java.lang.String lineName;
    public int maxAttempts;
    public java.lang.String model;
    public java.lang.String promptKey;
    public java.lang.String promptLabel;
    public int receivedChars;
    public long retryAt;
    public boolean retryFailed;
    public boolean retrySuccess;
    public long startedAt;
    public java.lang.String id = java.util.UUID.randomUUID().toString();
    public java.lang.String status = "FAIL";
    public int totalSamples = 1;
    public java.lang.String stage = "";
    public java.lang.String progressNote = "";
    public double score = 0.0d;
    public java.lang.String summary = "";
    public java.lang.String networkClass = "";
    public int baselineProgress = -1;
    public java.lang.Double lowerUsed = null;
    public java.util.List<app.pelican.check.RunData.SampleData> samples = new java.util.ArrayList();

    public static class AttemptData {
        public InputAudit.Evidence inputAudit = null;
        public long durationMs;
        public int httpStatus;
        public int number;
        public boolean partial;
        public int receivedChars;
        public boolean retryable;
        public long startedAt;
        public boolean success;
        public java.lang.String stage = "";
        public java.lang.String errorClass = "";
        public java.lang.String message = "";
        public java.lang.String compatibilityNote = "";
        public java.lang.String retryDecision = "";
        public java.lang.String outputPath = "";
        public transient java.lang.String responseTextForSave = "";
    }

    public static class SampleData {
        public int colors;
        public long finalAttemptMs;
        public InputAudit.Evidence inputAudit = null;
        public int inTokens;
        public boolean inputTokenAlert;
        public String inputDecision="",inputRuleUsed="",inputDecisionNote="";
        public Long inputDifference=null;

        public Double inputTokenReference = null;
        public int index;
        public int nodes;
        public int outTokens;
        public int pathLength;
        public boolean requestOk;
        public int score;
        public int size;
        public boolean svgOk;
        public long totalMs;
        public long ttftMs;
        public java.lang.String svgPath = "";
        public java.lang.String thumbPath = "";
        public java.lang.Integer reasonTokens = null;
        public java.lang.String modelEcho = "";
        public java.lang.String responseId = "";
        public java.util.List<java.lang.String> usageExtra = new java.util.ArrayList();
        public java.util.List<java.lang.String> evidence = new java.util.ArrayList();
        public java.util.List<java.lang.String> cheats = new java.util.ArrayList();
        public java.lang.String error = "";
        public java.lang.String errorClass = "";
        public java.util.List<app.pelican.check.RunData.AttemptData> attempts = new java.util.ArrayList();
        public transient java.lang.String rawTextForSave = "";
        public transient java.lang.String cleanedSvgForSave = "";
    }

    public boolean isActive() {
        return "QUEUED".equals(this.status) || "RUNNING".equals(this.status);
    }

    public boolean hasScore() {
        java.util.Iterator<app.pelican.check.RunData.SampleData> it = this.samples.iterator();
        while (it.hasNext()) {
            if (it.next().svgOk) {
                return true;
            }
        }
        return false;
    }
}
