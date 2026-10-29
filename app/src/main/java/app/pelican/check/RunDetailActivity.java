package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class RunDetailActivity extends android.app.Activity {
    private android.widget.LinearLayout attemptTimeline;
    private android.widget.TextView badge;
    private android.widget.TextView baseline;
    private android.widget.LinearLayout liveCard;
    private android.widget.TextView liveMeta;
    private android.widget.TextView liveNote;
    private android.widget.TextView liveStage;
    private android.widget.TextView network;
    private boolean outputExpanded;
    private android.widget.TextView outputHint;
    private android.widget.ScrollView outputScroll;
    private android.widget.TextView outputText;
    private android.widget.Button outputToggle;
    private android.widget.LinearLayout resultCards;
    private boolean resultsShown;
    private long renderedReassessedAt=-1;
    private app.pelican.check.RunData run;
    private java.lang.String runId;
    private android.widget.ProgressBar spinner;
    private android.widget.TextView stats;
    private android.widget.TextView subtitle;
    private android.widget.TextView summaryText;
    private android.widget.TextView title;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.lang.String displayedOutput = "";
    private int timelineCount = -1;
    private final java.lang.Runnable refresh = new java.lang.Runnable() { // from class: app.pelican.check.RunDetailActivity.1
        @Override // java.lang.Runnable
        public void run() {
            app.pelican.check.RunDetailActivity.this.reload();
            if (app.pelican.check.RunDetailActivity.this.run != null && (app.pelican.check.RunDetailActivity.this.run.isActive()||Store.get(RunDetailActivity.this).migrating24)) {
                app.pelican.check.RunDetailActivity.this.handler.postDelayed(this, 500L);
            }
        }
    };

    @Override // android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        app.pelican.check.Ui.systemBars(this);
        this.runId = getIntent().getStringExtra(app.pelican.check.MonitorService.EXTRA_RUN_ID);
        app.pelican.check.LiveRuns.Snapshot snapshot = app.pelican.check.LiveRuns.read(this.runId);
        this.run = snapshot == null ? app.pelican.check.Store.get(this).getRun(this.runId) : snapshot.run;
        if (this.run == null) {
            app.pelican.check.Ui.toast(this, "这条记录已删除");
            finish();
        } else {
            build();
            update(snapshot);
        }
    }

    private void build() {
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        linearLayoutColumn.setBackgroundColor(app.pelican.check.Ui.BG);
        android.widget.LinearLayout linearLayoutRow = app.pelican.check.Ui.row(this);
        linearLayoutRow.setPadding(app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 10.0f));
        linearLayoutRow.setBackgroundColor(-1);
        android.widget.Button button = app.pelican.check.Ui.button(this, "‹ 返回", false);
        linearLayoutRow.addView(button);
        android.widget.TextView textViewHeading = app.pelican.check.Ui.heading(this, "检测详情", 20.0f);
        textViewHeading.setPadding(app.pelican.check.Ui.dp(this, 14.0f), 0, 0, 0);
        linearLayoutRow.addView(textViewHeading);
        linearLayoutColumn.addView(linearLayoutRow);
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.2
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.RunDetailActivity.this.finish();
            }
        });
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayoutColumn2 = app.pelican.check.Ui.column(this);
        linearLayoutColumn2.setPadding(app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 30.0f));
        scrollView.addView(linearLayoutColumn2);
        linearLayoutColumn.addView(scrollView, new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
        setContentView(linearLayoutColumn);
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        this.badge = app.pelican.check.Ui.statusBadge(this, this.run.status);
        linearLayoutCard.addView(this.badge);
        this.title = app.pelican.check.Ui.heading(this, this.run.lineName, 18.0f);
        app.pelican.check.Ui.add(linearLayoutCard, this.title, 10);
        this.subtitle = app.pelican.check.Ui.text(this, this.run.model + " · " + this.run.promptLabel, 13.0f, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(linearLayoutCard, this.subtitle, 4);
        app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, app.pelican.check.Ui.dateTime(this.run.startedAt), 12.0f, app.pelican.check.Ui.MUTED), 5);
        this.stats = app.pelican.check.Ui.text(this, "", 13.0f, app.pelican.check.Ui.TEXT);
        app.pelican.check.Ui.add(linearLayoutCard, this.stats, 6);
        this.baseline = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.ORANGE);
        app.pelican.check.Ui.add(linearLayoutCard, this.baseline, 5);
        this.summaryText = app.pelican.check.Ui.text(this, "", 13.0f, app.pelican.check.Ui.TEXT);
        this.summaryText.setTextIsSelectable(true);
        app.pelican.check.Ui.add(linearLayoutCard, this.summaryText, 7);
        this.network = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.BLUEGRAY);
        app.pelican.check.Ui.add(linearLayoutCard, this.network, 6);
        linearLayoutColumn2.addView(linearLayoutCard);
        this.liveCard = app.pelican.check.Ui.card(this);
        app.pelican.check.Ui.add(linearLayoutColumn2, this.liveCard, 12);
        android.widget.LinearLayout linearLayoutRow2 = app.pelican.check.Ui.row(this);
        this.spinner = new android.widget.ProgressBar(this);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(app.pelican.check.Ui.dp(this, 20.0f), app.pelican.check.Ui.dp(this, 20.0f));
        layoutParams.rightMargin = app.pelican.check.Ui.dp(this, 8.0f);
        linearLayoutRow2.addView(this.spinner, layoutParams);
        this.liveStage = app.pelican.check.Ui.heading(this, "实时过程", 16.0f);
        linearLayoutRow2.addView(this.liveStage, app.pelican.check.Ui.weight(this, 1.0f));
        this.liveCard.addView(linearLayoutRow2);
        this.liveMeta = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.TEXT);
        app.pelican.check.Ui.add(this.liveCard, this.liveMeta, 8);
        this.liveNote = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.ORANGE);
        app.pelican.check.Ui.add(this.liveCard, this.liveNote, 6);
        this.outputToggle = app.pelican.check.Ui.button(this, "展开实时返回文字 / SVG 代码 ▾", false);
        app.pelican.check.Ui.add(this.liveCard, this.outputToggle, 10);
        this.outputHint = app.pelican.check.Ui.text(this, "只显示接口返回的正文，不显示请求密钥。", 11.0f, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(this.liveCard, this.outputHint, 5);
        this.outputScroll = new android.widget.ScrollView(this);
        this.outputScroll.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_GRAY, 8.0f));
        this.outputScroll.setFillViewport(true);
        this.outputText = app.pelican.check.Ui.text(this, "等待接口返回正文…", 11.0f, app.pelican.check.Ui.TEXT);
        this.outputText.setTypeface(android.graphics.Typeface.MONOSPACE);
        this.outputText.setTextIsSelectable(true);
        this.outputText.setPadding(app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 10.0f));
        this.outputScroll.addView(this.outputText);
        this.outputScroll.setVisibility(8);
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, app.pelican.check.Ui.dp(this, 240.0f));
        layoutParams2.topMargin = app.pelican.check.Ui.dp(this, 8.0f);
        this.liveCard.addView(this.outputScroll, layoutParams2);
        this.outputToggle.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.RunDetailActivity.this.outputExpanded = !app.pelican.check.RunDetailActivity.this.outputExpanded;
                app.pelican.check.RunDetailActivity.this.outputScroll.setVisibility(app.pelican.check.RunDetailActivity.this.outputExpanded ? 0 : 8);
                app.pelican.check.RunDetailActivity.this.outputToggle.setText(app.pelican.check.RunDetailActivity.this.outputExpanded ? "收起返回内容 ▴" : "展开实时返回文字 / SVG 代码 ▾");
                if (app.pelican.check.RunDetailActivity.this.outputExpanded) {
                    app.pelican.check.RunDetailActivity.this.outputText.setText(app.pelican.check.RunDetailActivity.this.displayedOutput.isEmpty() ? "等待接口返回正文…" : app.pelican.check.RunDetailActivity.this.displayedOutput);
                }
            }
        });
        this.attemptTimeline = app.pelican.check.Ui.column(this);
        app.pelican.check.Ui.add(this.liveCard, this.attemptTimeline, 8);
        this.resultCards = app.pelican.check.Ui.column(this);
        linearLayoutColumn2.addView(this.resultCards);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reload() {
        app.pelican.check.LiveRuns.Snapshot snapshot = app.pelican.check.LiveRuns.read(this.runId);
        app.pelican.check.RunData run = snapshot == null ? app.pelican.check.Store.get(this).getRun(this.runId) : snapshot.run;
        if (run == null) {
            return;
        }
        this.run = run;
        update(snapshot);
    }

    private void update(app.pelican.check.LiveRuns.Snapshot snapshot) {
        int i;
        int i2;
        java.lang.String str;
        java.lang.String str2;
        boolean zIsActive = this.run.isActive();
        if (zIsActive) {
            i = app.pelican.check.Ui.BLUEGRAY;
        } else if ("PASS".equals(this.run.status)) {
            i = app.pelican.check.Ui.GREEN;
        } else if ("SUSPECT".equals(this.run.status)) {
            i = app.pelican.check.Ui.ORANGE;
        } else {
            i = "ERROR".equals(this.run.status) ? app.pelican.check.Ui.RED : app.pelican.check.Ui.BLUEGRAY;
        }
        this.badge.setText(app.pelican.check.Ui.statusText(this.run.status));
        this.badge.setTextColor(i);
        android.widget.TextView textView = this.badge;
        if (!zIsActive && !"PASS".equals(this.run.status)) {
            i2 = "ERROR".equals(this.run.status) ? app.pelican.check.Ui.PALE_RED : app.pelican.check.Ui.PALE_GRAY;
        } else {
            i2 = app.pelican.check.Ui.PALE_GREEN;
        }
        textView.setBackground(app.pelican.check.Ui.round(this, i2, 8.0f));
        this.stats.setText(zIsActive ? "本轮已过 " + app.pelican.check.Ui.elapsed(this.run.startedAt, 0L) + " · 共 " + this.run.totalSamples + " 个样本" : "整轮耗时 " + app.pelican.check.Ui.elapsed(this.run.startedAt, this.run.endedAt) + " · " + (this.run.hasScore() ? "平均复杂度 " + java.lang.Math.round(this.run.score) : "未生成有效图片，无评分"));
        this.baseline.setVisibility(zIsActive ? 8 : 0);
        android.widget.TextView textView2 = this.baseline;
        if (this.run.lowerUsed != null) {
            str = "本次使用下限：" + this.run.lowerUsed;
        } else if (this.run.baselineProgress >= 5) {
            str = "本次完成自动基线，下次开始比较";
        } else {
            str = this.run.baselineProgress >= 0 ? "基线收集中 " + this.run.baselineProgress + "/5（失败不计入）" : "";
        }
        textView2.setText(this.run.lowerEquation==null||this.run.lowerEquation.isEmpty()?str:this.run.lowerEquation);
        android.widget.TextView textView3 = this.summaryText;
        app.pelican.check.RunData runData = this.run;
        textView3.setText(zIsActive ? app.pelican.check.MonitorEngine.liveDescription(runData) : runData.summary
                + (runData.inputTokenNote == null || runData.inputTokenNote.isEmpty() ? "" : "\n" + runData.inputTokenNote)
                + (runData.sequenceNote==null||runData.sequenceNote.isEmpty()?"":"\n"+runData.sequenceNote)
                + (runData.reassessmentNote==null||runData.reassessmentNote.isEmpty()?"":"\n"+runData.reassessmentNote));
        textView3.setTextColor(runData.inputTokenAlert ? app.pelican.check.Ui.RED : app.pelican.check.Ui.TEXT);
        this.network.setText(new java.lang.StringBuilder().append(this.run.retryFailed ? "重试后仍失败 · " : this.run.retrySuccess ? "重试后成功 · " : "").append(this.run.networkClass).toString());
        this.network.setVisibility(this.network.getText().length() == 0 ? 8 : 0);
        this.spinner.setVisibility(zIsActive ? 0 : 8);
        this.liveStage.setText(zIsActive ? this.run.stage : "本轮已结束");
        android.widget.TextView textView4 = this.liveMeta;
        if (zIsActive) {
            str2 = "样本 " + java.lang.Math.max(1, this.run.currentSample) + "/" + this.run.totalSamples + " · " + (this.run.currentAttempt <= 1 ? "首次请求" : "第 " + (this.run.currentAttempt - 1) + " 次重试") + " · 最多 " + java.lang.Math.max(0, this.run.maxAttempts - 1) + " 次重试\n本次请求 " + (this.run.attemptStartedAt > 0 ? app.pelican.check.Ui.elapsed(this.run.attemptStartedAt, 0L) : "尚未开始") + " · 已收 " + this.run.receivedChars + " 字符";
        } else {
            str2 = "各次请求的耗时、重试原因和返回正文保留在下方时间线中。";
        }
        textView4.setText(str2);
        long jCurrentTimeMillis = this.run.retryAt - java.lang.System.currentTimeMillis();
        this.liveNote.setText(zIsActive ? this.run.progressNote + (jCurrentTimeMillis > 0 ? "\n距离重试还有 " + ((jCurrentTimeMillis + 999) / 1000) + " 秒" : "") : "");
        this.liveNote.setVisibility(this.liveNote.getText().length() > 0 ? 0 : 8);
        java.lang.String str3 = snapshot != null ? snapshot.output : "";
        if (zIsActive || !str3.isEmpty()) {
            updateOutput(str3);
        } else if (this.displayedOutput.isEmpty()) {
            this.outputToggle.setVisibility(8);
            this.outputHint.setVisibility(8);
            this.outputScroll.setVisibility(8);
        }
        java.util.Iterator<app.pelican.check.RunData.SampleData> it = this.run.samples.iterator();
        int size = 0;
        while (it.hasNext()) {
            size += it.next().attempts.size();
        }
        if (zIsActive && size != this.timelineCount) {
            int i3 = 0;
            for (app.pelican.check.RunData.SampleData sampleData : this.run.samples) {
                for (app.pelican.check.RunData.AttemptData attemptData : sampleData.attempts) {
                    int i4 = i3 + 1;
                    if (i3 >= java.lang.Math.max(0, this.timelineCount)) {
                        this.attemptTimeline.addView(attemptView(sampleData.index, attemptData));
                    }
                    i3 = i4;
                }
            }
            this.timelineCount = size;
        }
        if (!zIsActive && (!this.resultsShown||renderedReassessedAt!=run.reassessedAt)) {
            this.resultCards.removeAllViews();renderedReassessedAt=run.reassessedAt;
            this.resultsShown = true;
            this.attemptTimeline.removeAllViews();
            this.liveCard.setVisibility(this.displayedOutput.isEmpty() ? 8 : 0);
            java.util.Iterator<app.pelican.check.RunData.SampleData> it2 = this.run.samples.iterator();
            while (it2.hasNext()) {
                app.pelican.check.Ui.add(this.resultCards, sampleCard(it2.next()), 12);
            }
        }
    }

    private void updateOutput(java.lang.String str) {
        if (str.length() > 60000) {
            str = "（仅显示最近 60000 字符，完整正文在请求结束后保存）\n" + str.substring(str.length() - 60000);
        }
        if (str.equals(this.displayedOutput)) {
            return;
        }
        boolean z = this.outputScroll.getScrollY() + this.outputScroll.getHeight() >= this.outputText.getHeight() - app.pelican.check.Ui.dp(this, 24.0f);
        this.displayedOutput = str;
        if (this.outputExpanded) {
            android.widget.TextView textView = this.outputText;
            if (str.isEmpty()) {
                str = "等待接口返回正文…";
            }
            textView.setText(str);
            if (z) {
                this.outputScroll.post(new java.lang.Runnable() { // from class: app.pelican.check.RunDetailActivity.4
                    @Override // java.lang.Runnable
                    public void run() {
                        app.pelican.check.RunDetailActivity.this.outputScroll.fullScroll(130);
                    }
                });
            }
        }
        this.outputHint.setText(this.run.receivedChars > 0 ? "正在显示本次请求正文；重试时会切换到新请求，旧内容在时间线里保留。" : "等待首字时暂时没有正文，这是正常的。");
    }

    private android.widget.LinearLayout sampleCard(final app.pelican.check.RunData.SampleData sampleData) {
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        linearLayoutCard.addView(app.pelican.check.Ui.heading(this, "样本 " + sampleData.index + (sampleData.svgOk ? " · 复杂度 " + sampleData.score : ""), 16.0f));
        if (sampleData.svgOk) {
            android.widget.ImageView imageView = new android.widget.ImageView(this);
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            imageView.setBackground(app.pelican.check.Ui.bordered(this, -1, app.pelican.check.Ui.BORDER, 12.0f));
            android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, app.pelican.check.Ui.dp(this, 270.0f));
            layoutParams.topMargin = app.pelican.check.Ui.dp(this, 10.0f);
            linearLayoutCard.addView(imageView, layoutParams);
            app.pelican.check.SvgImages.load(imageView, sampleData.svgPath, sampleData.thumbPath, 1200, 1000);
            imageView.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.5
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    app.pelican.check.SvgImages.open(app.pelican.check.RunDetailActivity.this, sampleData);
                }
            });
            app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, "完整画布 · 点击放大", 11.0f, app.pelican.check.Ui.GREEN), 5);
            app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, "节点 " + sampleData.nodes + " · 颜色 " + sampleData.colors + " · 路径字符 " + sampleData.pathLength + " · " + app.pelican.check.Ui.bytes(sampleData.size), 12.0f, app.pelican.check.Ui.MUTED), 7);
        } else {
            android.widget.TextView textViewText = app.pelican.check.Ui.text(this, sampleData.error.isEmpty() ? "本样本没有有效 SVG" : sampleData.error, 14.0f, app.pelican.check.Ui.RED);
            textViewText.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_RED, 10.0f));
            textViewText.setPadding(app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 9.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 9.0f));
            app.pelican.check.Ui.add(linearLayoutCard, textViewText, 10);
        }
        app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, "含重试总耗时 " + secs(sampleData.totalMs) + "s · 最后一次首字 " + (sampleData.ttftMs > 0 ? secs(sampleData.ttftMs) + "s" : "—") + "\n输出 token " + token(sampleData.outTokens) + " · 输入 token " + token(sampleData.inTokens), 12.0f, app.pelican.check.Ui.TEXT), 8);
        android.widget.TextView inputResult=Ui.text(this,TokenGuard.detail(sampleData),12,sampleData.inputTokenAlert?0xFF2E63A3:Ui.TEXT);
        inputResult.setTextIsSelectable(true);inputResult.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10));
        inputResult.setBackground(Ui.round(this,sampleData.inputTokenAlert?0xFFEAF1FB:Ui.PALE_GRAY,12));Ui.add(linearLayoutCard,inputResult,10);
        InputAudit.Evidence audit=sampleData.inputAudit;
        if(audit!=null){
            android.widget.LinearLayout box=app.pelican.check.Ui.column(this);
            box.setPadding(app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12));
            box.setBackground(app.pelican.check.Ui.round(this,app.pelican.check.Ui.PALE_GRAY,12));
            box.addView(app.pelican.check.Ui.heading(this,"本机输入自检",14));
            String count="本地估算 "+(audit.estimatedInputTokens>0?audit.estimatedInputTokens:"—")+" Token";
            String countExplanation=run.inputAuditVersion>=4?"本地分词与消息封装估算，用于输入异常筛查，不是精确注入鉴定；估算不可用时使用字节补充规则":audit.countNote;
            app.pelican.check.Ui.add(box,app.pelican.check.Ui.text(this,(audit.contentVerified?"提示词逐字核对通过":"输入内容未通过自检")+" · "+audit.messageCount+" 条消息\n"+audit.promptCharacters+" 字符 / "+audit.promptBytes+" UTF-8 字节\n请求正文 "+audit.bodyBytes+" 字节 · "+(audit.wireBytesVerified?"发送字节核对通过":"未取得完整发送字节确认")+"\n"+count+"\n接口回报 "+token(sampleData.inTokens)+" 输入 Token\n"+audit.countSource+"\n"+countExplanation,12,app.pelican.check.Ui.TEXT),6);
            final android.widget.TextView request=app.pelican.check.Ui.text(this,audit.bodyJson+"\n\n正文 SHA-256: "+audit.bodySha256+"\n提示词 SHA-256: "+audit.promptSha256,11,app.pelican.check.Ui.TEXT);request.setTextIsSelectable(true);request.setTypeface(android.graphics.Typeface.MONOSPACE);request.setVisibility(8);
            android.widget.Button expand=app.pelican.check.Ui.button(this,"核对实际发送 JSON ▾",false);app.pelican.check.Ui.add(box,expand,8);app.pelican.check.Ui.add(box,request,8);expand.setOnClickListener(v->{boolean show=request.getVisibility()!=0;request.setVisibility(show?0:8);expand.setText(show?"收起实际请求 ▴":"核对实际发送 JSON ▾");});
            app.pelican.check.Ui.add(linearLayoutCard,box,10);
        }
        if(sampleData.index==1&&run.originalRunJson!=null&&!run.originalRunJson.isEmpty()){
            android.widget.Button original=Ui.button(this,"查看重判前的原始记录",false);Ui.add(linearLayoutCard,original,8);
            original.setOnClickListener(v->{android.widget.ScrollView scroll=new android.widget.ScrollView(this);android.widget.TextView text=Ui.text(this,run.originalRunJson,11,Ui.TEXT);text.setTextIsSelectable(true);text.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,12));scroll.addView(text);new android.app.AlertDialog.Builder(this).setTitle("重判前记录 · "+run.originalStatus).setView(scroll).setPositiveButton("关闭",null).show();});
        }
        java.util.Iterator<java.lang.String> it = sampleData.evidence.iterator();
        while (it.hasNext()) {
            app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, "• " + it.next(), 12.0f, app.pelican.check.Ui.MUTED), 3);
        }
        if (sampleData.svgOk) {
            android.widget.LinearLayout linearLayoutRow = app.pelican.check.Ui.row(this);
            android.widget.Button button = app.pelican.check.Ui.button(this, "复制 SVG", false);
            android.widget.Button button2 = app.pelican.check.Ui.button(this, "保存", false);
            final android.widget.Button button3 = app.pelican.check.Ui.button(this, "看源码", false);
            app.pelican.check.Ui.action(linearLayoutRow, button, 1.2f);
            app.pelican.check.Ui.action(linearLayoutRow, button2, 0.8f);
            app.pelican.check.Ui.action(linearLayoutRow, button3, 1.0f);
            app.pelican.check.Ui.add(linearLayoutCard, linearLayoutRow, 10);
            final android.widget.TextView textViewText2 = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.TEXT);
            textViewText2.setTypeface(android.graphics.Typeface.MONOSPACE);
            textViewText2.setTextIsSelectable(true);
            textViewText2.setVisibility(8);
            textViewText2.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_GRAY, 8.0f));
            app.pelican.check.Ui.add(linearLayoutCard, textViewText2, 8);
            button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.6
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    ((android.content.ClipboardManager) app.pelican.check.RunDetailActivity.this.getSystemService("clipboard")).setPrimaryClip(android.content.ClipData.newPlainText("SVG", app.pelican.check.FileUtil.readText(sampleData.svgPath)));
                    app.pelican.check.Ui.toast(app.pelican.check.RunDetailActivity.this, "SVG 已复制");
                }
            });
            button2.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.7
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    app.pelican.check.Ui.toast(app.pelican.check.RunDetailActivity.this, app.pelican.check.FileUtil.saveDownload(app.pelican.check.RunDetailActivity.this, "pelican-" + app.pelican.check.RunDetailActivity.this.run.id + "-" + sampleData.index + ".svg", app.pelican.check.FileUtil.readText(sampleData.svgPath), "image/svg+xml"));
                }
            });
            button3.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.8
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    boolean z = textViewText2.getVisibility() != 0;
                    if (z && textViewText2.getText().length() == 0) {
                        textViewText2.setText(app.pelican.check.FileUtil.readText(sampleData.svgPath));
                    }
                    textViewText2.setVisibility(z ? 0 : 8);
                    button3.setText(z ? "收起源码" : "看源码");
                }
            });
        }
        if (!sampleData.attempts.isEmpty()) {
            app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.heading(this, "请求与重试时间线", 14.0f), 12);
            java.util.Iterator<app.pelican.check.RunData.AttemptData> it2 = sampleData.attempts.iterator();
            while (it2.hasNext()) {
                linearLayoutCard.addView(attemptView(sampleData.index, it2.next()));
            }
        }
        return linearLayoutCard;
    }

    private android.widget.LinearLayout attemptView(int i, final app.pelican.check.RunData.AttemptData attemptData) {
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        java.lang.String str = "样本 " + i + " · " + (attemptData.number == 1 ? "首次请求" : "第 " + (attemptData.number - 1) + " 次重试") + " · " + (attemptData.success ? "成功" : "失败") + " · " + secs(attemptData.durationMs) + "s";
        if (attemptData.httpStatus > 0) {
            str = str + " · HTTP " + attemptData.httpStatus;
        }
        if (attemptData.receivedChars > 0) {
            str = str + "\n已收 " + attemptData.receivedChars + " 字符";
        }
        if (!attemptData.errorClass.isEmpty()) {
            str = str + "\n" + attemptData.errorClass;
        }
        if (!attemptData.message.isEmpty()) {
            str = str + "\n" + attemptData.message;
        }
        if (attemptData.retryDecision != null && !attemptData.retryDecision.isEmpty()) {
            str = str + "\n" + attemptData.retryDecision;
        }
        if (!attemptData.compatibilityNote.isEmpty()) {
            str = str + "\n" + attemptData.compatibilityNote;
        }
        android.widget.TextView textViewText = app.pelican.check.Ui.text(this, str, 12.0f, attemptData.success ? app.pelican.check.Ui.GREEN : app.pelican.check.Ui.RED);
        textViewText.setTextIsSelectable(true);
        app.pelican.check.Ui.add(linearLayoutColumn, textViewText, 6);
        if (attemptData.outputPath != null && !attemptData.outputPath.isEmpty()) {
            final android.widget.Button button = app.pelican.check.Ui.button(this, "查看这次请求的返回正文", false);
            app.pelican.check.Ui.add(linearLayoutColumn, button, 5);
            final android.widget.TextView textViewText2 = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.TEXT);
            textViewText2.setTypeface(android.graphics.Typeface.MONOSPACE);
            textViewText2.setTextIsSelectable(true);
            final android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
            scrollView.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_GRAY, 8.0f));
            scrollView.addView(textViewText2);
            scrollView.setVisibility(8);
            linearLayoutColumn.addView(scrollView, new android.widget.LinearLayout.LayoutParams(-1, app.pelican.check.Ui.dp(this, 200.0f)));
            button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.RunDetailActivity.9
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    boolean z = scrollView.getVisibility() != 0;
                    if (z && textViewText2.getText().length() == 0) {
                        textViewText2.setText(app.pelican.check.FileUtil.readText(attemptData.outputPath));
                    }
                    scrollView.setVisibility(z ? 0 : 8);
                    button.setText(z ? "收起返回正文" : "查看这次请求的返回正文");
                }
            });
        }
        return linearLayoutColumn;
    }

    private java.lang.String secs(long j) {
        return java.lang.String.format(java.util.Locale.ROOT, "%.2f", java.lang.Double.valueOf(j / 1000.0d));
    }

    private java.lang.String token(int i) {
        return i <= 0 ? "—" : java.lang.String.valueOf(i);
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.run != null) {
            this.handler.removeCallbacks(this.refresh);
            this.handler.post(this.refresh);
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.handler.removeCallbacks(this.refresh);
        super.onPause();
    }
}
