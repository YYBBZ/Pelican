package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class MonitorEngine {
    private final app.pelican.check.ApiClient api;

    /* JADX INFO: renamed from: app, reason: collision with root package name */
    private final android.content.Context f5app;
    private volatile boolean stopped;
    private final app.pelican.check.Store store;

    public MonitorEngine(android.content.Context context) {
        this.f5app = context.getApplicationContext();
        this.store = app.pelican.check.Store.get(this.f5app);
        this.api = new app.pelican.check.ApiClient(this.f5app);
    }

    public void cancelAll() {
        this.stopped = true;
        this.api.cancelAll();
    }

    public app.pelican.check.RunData prepare(app.pelican.check.LineConfig lineConfig) {
        app.pelican.check.RunData runData = new app.pelican.check.RunData();
        runData.lineId = lineConfig.id;
        runData.lineName = lineConfig.displayName();
        runData.model = lineConfig.model;
        runData.startedAt = java.lang.System.currentTimeMillis();
        runData.status = "QUEUED";
        runData.stage = "排队中";
        lineConfig.normalize();
        PromptSequence.capture(lineConfig,runData);
        if(lineConfig.sampleCount<1||lineConfig.sampleCount>99)throw new IllegalArgumentException("每轮样本必须为 1～99 张，未擅自截断数量");
        runData.totalSamples = lineConfig.sampleCount;
        runData.maxAttempts = app.pelican.check.RetryPolicy.maxAttempts(lineConfig.retryCount);
        app.pelican.check.Baseline baseline = this.store.getBaseline(lineConfig.id, lineConfig.model, runData.promptKey);
        runData.lowerUsed = QualityPolicy.lower(lineConfig,baseline);
        runData.lowerEquation=QualityPolicy.equation(lineConfig,baseline);
        runData.baselineProgress = runData.lowerUsed == null ? baseline.values.size() : -1;
        runData.summary = "已加入检测队列";
        this.store.insertRun(runData);
        app.pelican.check.LiveRuns.publish(runData, null);
        return runData;
    }

    public app.pelican.check.RunData runLine(java.lang.String str) {
        app.pelican.check.LineConfig line = this.store.getLine(str);
        if (line == null) {
            return null;
        }
        return runLine(line);
    }

    public app.pelican.check.RunData runLine(app.pelican.check.LineConfig lineConfig) {
        return runPrepared(lineConfig, prepare(lineConfig));
    }

    public app.pelican.check.RunData runPrepared(app.pelican.check.LineConfig lineConfig, final app.pelican.check.RunData runData) {
        try {
            lineConfig.validateSettings();
            java.lang.String strPromptText = runData.promptTextUsed;
            if(strPromptText==null||strPromptText.isEmpty())strPromptText=lineConfig.promptText(lineConfig.promptForRun(0));
            runData.inputTokenSignature = app.pelican.check.TokenGuard.signature(lineConfig, strPromptText);
            if (strPromptText != null && !strPromptText.trim().isEmpty() && !lineConfig.baseUrl.trim().isEmpty() && !lineConfig.apiKey.trim().isEmpty() && !lineConfig.model.trim().isEmpty()) {
                runData.status = "RUNNING";
                runData.stage = "准备检测";
                this.store.insertRun(runData);
                app.pelican.check.LiveRuns.publish(runData, null);
                char c = 0;
                int i = 0;
                boolean z = false;
                boolean z2 = false;
                int i2 = 1;
                boolean z3 = true;
                double d = 0.0d;
                while (i2 <= runData.totalSamples && !this.stopped && !java.lang.Thread.currentThread().isInterrupted()) {
                    final app.pelican.check.RunData.SampleData sampleData = new app.pelican.check.RunData.SampleData();
                    sampleData.index = i2;
                    runData.samples.add(sampleData);
                    runData.currentSample = i2;
                    final long[] jArr = new long[1];
                    jArr[c] = 0;
                    app.pelican.check.RunData.SampleData sampleDataGenerateSample = this.api.generateSample(lineConfig, strPromptText, i2, new app.pelican.check.ApiClient.ProgressListener() { // from class: app.pelican.check.MonitorEngine.1
                        @Override // app.pelican.check.ApiClient.ProgressListener
                        public void onProgress(app.pelican.check.ApiClient.Progress progress) {
                            if (app.pelican.check.MonitorEngine.this.stopped) {
                                return;
                            }
                            runData.currentAttempt = progress.attempt;
                            runData.stage = progress.stage;
                            if (progress.resetOutput) {
                                runData.receivedChars = 0;
                                runData.progressNote = progress.note;
                                runData.retryAt = 0L;
                                runData.attemptStartedAt = progress.attemptStartedAt;
                            }
                            if (progress.receivedChars > 0) {
                                runData.receivedChars = progress.receivedChars;
                            }
                            if (progress.note != null && !progress.note.isEmpty()) {
                                runData.progressNote = progress.note;
                            }
                            if (progress.retryAt > 0) {
                                runData.retryAt = progress.retryAt;
                                runData.progressNote = progress.note;
                            }
                            if (progress.completedAttempt != null) {
                                app.pelican.check.MonitorEngine.this.saveResponse(runData, sampleData.index, progress.completedAttempt);
                                sampleData.attempts.add(progress.completedAttempt);
                            }
                            app.pelican.check.LiveRuns.publish(runData, progress);
                            long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
                            if (jCurrentTimeMillis - jArr[0] >= 1200 || progress.completedAttempt != null || progress.resetOutput || progress.retryAt > 0) {
                                runData.summary = app.pelican.check.MonitorEngine.liveDescription(runData);
                                app.pelican.check.MonitorEngine.this.store.insertRun(runData);
                                jArr[0] = jCurrentTimeMillis;
                            }
                        }
                    });
                    runData.samples.set(i2 - 1, sampleDataGenerateSample);
                    if (sampleDataGenerateSample.svgOk) {
                        saveSampleFiles(runData, sampleDataGenerateSample);
                        d += (double) sampleDataGenerateSample.score;
                        i++;
                    } else {
                        z3 = false;
                    }
                    if (!sampleDataGenerateSample.requestOk) {
                        if (sampleDataGenerateSample.attempts.size() > 1) {
                            runData.retryFailed = true;
                        }
                        z2 = true;
                        z3 = false;
                    } else if (sampleDataGenerateSample.attempts.size() > 1) {
                        runData.retrySuccess = true;
                    }
                    if (sampleDataGenerateSample.requestOk && (!sampleDataGenerateSample.svgOk || !sampleDataGenerateSample.cheats.isEmpty() || (!sampleDataGenerateSample.modelEcho.isEmpty() && !sampleDataGenerateSample.modelEcho.equals(lineConfig.model)))) {
                        z = true;
                        z3 = false;
                    }
                    if (runData.networkClass.isEmpty()) {
                        runData.networkClass = lastErrorClass(sampleDataGenerateSample);
                    }
                    if (!this.stopped) {
                        app.pelican.check.LiveRuns.publish(runData, null);
                        this.store.insertRun(runData);
                    }
                    i2++;
                    c = 0;
                }
                runData.score = i == 0 ? 0.0d : d / ((double) i);
                if (!this.stopped && !java.lang.Thread.currentThread().isInterrupted() && runData.samples.size() >= runData.totalSamples) {
                    app.pelican.check.TokenGuard.assess(runData, lineConfig);
                if (runData.inputTokenAlert) z3 = false;
                app.pelican.check.Baseline baseline = this.store.getBaseline(lineConfig.id, lineConfig.model, runData.promptKey);
                    java.lang.Double d2 = QualityPolicy.lower(lineConfig,baseline);
                    boolean z4 = d2 == null && baseline.values.size() < 5;
                    runData.lowerUsed = d2;
                    runData.lowerEquation=QualityPolicy.equation(lineConfig,baseline);
                    runData.baselineProgress = z4 ? baseline.values.size() : -1;
                    if (z4 && z3 && i == runData.totalSamples) {
                        baseline.addSuccessfulScore(runData.score);
                        this.store.saveBaseline(baseline);
                        runData.baselineProgress = baseline.values.size();
                    }
                    if (z) {
                        runData.status = "ERROR";
                    } else if (!z2) {
                        runData.status = "PASS";
                        if (!z4 && d2 != null) {
                            for (app.pelican.check.RunData.SampleData sampleData2 : runData.samples) {
                                if (sampleData2.svgOk && sampleData2.score < d2.doubleValue()) {
                                    runData.status = "SUSPECT";
                                    break;
                                }
                            }
                        }
                    } else {
                        runData.status = "FAIL";
                    }
                    runData.qualityStatus=runData.status;
                    if(runData.inputTokenAlert)runData.status="ERROR";
                    runData.qualitySummary=makeSummary(runData);
                    runData.summary = runData.qualitySummary;
                if (runData.inputTokenAlert) runData.summary = "输入 Token 异常 / 疑似注入；" + runData.summary;
                    return finish(lineConfig, runData);
                }
                runData.status = "FAIL";
                runData.interrupted = true;
                runData.summary = "监控已停止，本轮检测中断";
                return finish(lineConfig, runData);
            }
            runData.status = "FAIL";
            runData.summary = "线路配置不完整：请检查地址、密钥、模型和题目";
            return finish(lineConfig, runData);
        } catch (java.lang.Throwable th) {
            runData.status = "FAIL";
            runData.summary = "检测内部异常：" + th.getClass().getSimpleName() + " · " + th.getMessage();
            try {
                app.pelican.check.CrashLog.save(this.f5app, th);
            } catch (java.lang.Throwable th2) {
            }
        }
        return finish(lineConfig, runData);
    }

    private app.pelican.check.RunData finish(app.pelican.check.LineConfig lineConfig, app.pelican.check.RunData runData) {
        runData.endedAt = java.lang.System.currentTimeMillis();
        runData.stage = runData.interrupted ? "检测中断" : "检测完成";
        runData.retryAt = 0L;
        if(runData.qualityStatus==null||runData.qualityStatus.isEmpty())runData.qualityStatus=QualityPolicy.status(runData);
        updateLine(lineConfig, runData);
        this.store.insertRun(runData);
        app.pelican.check.LiveRuns.remove(runData.id);
        return runData;
    }

    public static java.lang.String liveDescription(app.pelican.check.RunData runData) {
        if ("QUEUED".equals(runData.status)) {
            return "排队中，等待空闲检测位";
        }
        java.lang.String str = "样本 " + java.lang.Math.max(1, runData.currentSample) + "/" + runData.totalSamples + " · " + runData.stage;
        if (runData.currentAttempt > 0) {
            str = str + " · " + (runData.currentAttempt == 1 ? "首次请求" : "第 " + (runData.currentAttempt - 1) + " 次重试");
        }
        return runData.receivedChars > 0 ? str + " · 已收 " + runData.receivedChars + " 字符" : str;
    }

    private java.lang.String makeSummary(app.pelican.check.RunData runData) {
        java.util.ArrayList arrayList = new java.util.ArrayList();
        String status=runData.qualityStatus==null||runData.qualityStatus.isEmpty()?runData.status:runData.qualityStatus;
        if ("PASS".equals(status)) {
            arrayList.add("SVG 与复杂度检查通过");
        }
        if ("SUSPECT".equals(status)) {
            arrayList.add("至少一张低于复杂度下限 " + runData.lowerUsed);
        }
        if ("ERROR".equals(status)) {
            arrayList.add("发现换模型、无有效 SVG 或取巧等硬异常");
        }
        if ("FAIL".equals(status)) {
            for (app.pelican.check.RunData.SampleData sampleData : runData.samples) {
                if (!sampleData.error.isEmpty()) {
                    arrayList.add(sampleData.error);
                    break;
                }
            }
            if (arrayList.isEmpty()) {
                arrayList.add("请求失败");
            }
        }
        if (runData.retrySuccess) {
            arrayList.add("重试后成功");
        }
        if (runData.retryFailed) {
            arrayList.add("重试后仍失败");
        }
        return join(arrayList, "；");
    }

    private void updateLine(app.pelican.check.LineConfig lineConfig, app.pelican.check.RunData runData) {
        java.lang.String str;
        synchronized (this.store) {
            app.pelican.check.LineConfig line = this.store.getLine(lineConfig.id);
            if (line == null) {
                return;
            }
            line.lastRunAt = runData.endedAt;
            line.lastStatus = runData.status;
            if (runData.retryFailed) {
                str = "重试后仍失败";
            } else {
                str = runData.retrySuccess ? "重试后成功" : "";
            }
            if (runData.inputTokenAlert) str = "输入 Token 异常 / 疑似注入" + (str.isEmpty() ? "" : " · " + str);
            line.lastBadge = str;
            line.consecutiveIssues=QualityPolicy.nextConsecutive(line.consecutiveIssues,runData);
            PromptSequence.finish(line,runData);
            this.store.saveLine(line);
        }
    }

    private java.lang.String lastErrorClass(app.pelican.check.RunData.SampleData sampleData) {
        for (int size = sampleData.attempts.size() - 1; size >= 0; size--) {
            app.pelican.check.RunData.AttemptData attemptData = sampleData.attempts.get(size);
            if (!attemptData.success && attemptData.errorClass != null && !attemptData.errorClass.isEmpty()) {
                return attemptData.errorClass;
            }
        }
        return sampleData.errorClass == null ? "" : sampleData.errorClass;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveResponse(app.pelican.check.RunData runData, int i, app.pelican.check.RunData.AttemptData attemptData) {
        try {
            if (attemptData.responseTextForSave != null && !attemptData.responseTextForSave.isEmpty()) {
                java.io.File file = new java.io.File(this.f5app.getFilesDir(), "responses");
                file.mkdirs();
                java.io.File file2 = new java.io.File(file, runData.id + "_" + i + "_" + attemptData.number + ".txt");
                write(file2, attemptData.responseTextForSave);
                attemptData.outputPath = file2.getAbsolutePath();
                attemptData.responseTextForSave = "";
                return;
            }
            attemptData.responseTextForSave = "";
        } catch (java.lang.Throwable th) {
        }
    }

    private void saveSampleFiles(app.pelican.check.RunData runData, app.pelican.check.RunData.SampleData sampleData) {
        try {
            java.io.File file = new java.io.File(this.f5app.getFilesDir(), "svgs");
            file.mkdirs();
            java.io.File file2 = new java.io.File(file, runData.id + "_" + sampleData.index + ".svg");
            java.lang.String str = sampleData.cleanedSvgForSave;
            if (str != null && !str.isEmpty()) {
                write(file2, str);
                sampleData.svgPath = file2.getAbsolutePath();
                java.io.File file3 = new java.io.File(this.f5app.getFilesDir(), "thumbs");
                file3.mkdirs();
                java.io.File file4 = new java.io.File(file3, runData.id + "_" + sampleData.index + "_fit2.png");
                if (app.pelican.check.SvgAnalyzer.saveThumb(str, file4, 720, 520)) {
                    sampleData.thumbPath = file4.getAbsolutePath();
                }
                sampleData.rawTextForSave = "";
                sampleData.cleanedSvgForSave = "";
                return;
            }
            sampleData.rawTextForSave = "";
            sampleData.cleanedSvgForSave = "";
        } catch (java.lang.Throwable th) {
        }
    }

    private void write(java.io.File file, java.lang.String str) throws java.lang.Exception {
        java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file);
        try {
            fileOutputStream.write(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fileOutputStream.flush();
        } finally {
            fileOutputStream.close();
        }
    }

    private static java.lang.String join(java.util.List<java.lang.String> list, java.lang.String str) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        for (java.lang.String str2 : list) {
            if (sb.length() > 0) {
                sb.append(str);
            }
            sb.append(str2);
        }
        return sb.toString();
    }
}
