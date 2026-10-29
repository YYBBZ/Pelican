package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class MainActivity extends android.app.Activity {
    private static final int REQ_EDIT = 10;
    private static final int REQ_EXPORT = 41;
    private static final int REQ_IMPORT = 42;
    private android.widget.FrameLayout content;
    private android.widget.ScrollView currentScroll;
    private android.widget.Button deleteSelectedButton;
    private boolean fatal;
    private android.widget.LinearLayout filterBody;
    private int filterLinePos;
    private int filterModelPos;
    private int filterSpecialPos;
    private int filterStatusPos;
    private android.widget.Button filterToggle;
    private boolean filtersExpanded;
    private boolean historyDirty;
    private android.widget.TextView historyHint;
    private android.widget.LinearLayout historyList;
    private int historyOffset;
    private android.widget.Spinner lineFilter;
    private android.widget.Button manageButton;
    private boolean manageMode;
    private android.widget.TextView selectionHint;
    private boolean syncingSelection;
    private long selectionRequest;
    private android.widget.Spinner modelFilter;
    private android.widget.LinearLayout nav;
    private android.widget.Spinner specialFilter;
    private android.widget.Spinner statusFilter;
    private app.pelican.check.Store store;
    private int tab = 0;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final app.pelican.check.Store.RunFilter filter = new app.pelican.check.Store.RunFilter();
    private java.lang.String latestHistoryId = "";
    private boolean wasMigrating24;
    private final java.util.Map<java.lang.String, app.pelican.check.MainActivity.HistoryCard> historyCards = new java.util.HashMap();
    private final java.util.Map<java.lang.String, app.pelican.check.MainActivity.MonitorCard> monitorCards = new java.util.HashMap();
    private final java.util.List<app.pelican.check.RunData> history = new java.util.ArrayList();
    private final java.util.Set<java.lang.String> selected = new java.util.HashSet();
    private final java.util.Map<java.lang.String, android.widget.TextView> scheduleViews = new java.util.HashMap();
    private java.util.List<app.pelican.check.LineConfig> filterLines = new java.util.ArrayList();
    private final android.content.BroadcastReceiver updateReceiver = new android.content.BroadcastReceiver() { // from class: app.pelican.check.MainActivity.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            app.pelican.check.MainActivity.this.refreshLive();
        }
    };
    private final java.lang.Runnable countdown = new java.lang.Runnable() { // from class: app.pelican.check.MainActivity.2
        @Override // java.lang.Runnable
        public void run() {
            app.pelican.check.MainActivity.this.refreshLive();
            app.pelican.check.MainActivity.this.handler.postDelayed(this, 1000L);
        }
    };

    private static class HistoryCard {
        android.widget.CheckBox check;
        android.widget.ImageView image;
        android.widget.TextView link;
        boolean loadedImage;
        android.widget.TextView meta;
        android.widget.TextView placeholder;
        android.widget.TextView progress;
        android.widget.TextView retry;
        android.widget.TextView tokenAlert;
        android.widget.TextView usage;
        app.pelican.check.RunData run;
        android.widget.TextView status;

        private HistoryCard() {
        }
    }

    private static class MonitorCard {
        app.pelican.check.RunData active;
        android.widget.TextView baseline;
        android.widget.TextView last;
        android.widget.TextView live;
        android.widget.Button now;
        android.widget.TextView retry;
        android.widget.TextView status;

        private MonitorCard() {
        }
    }

    @Override // android.app.Activity
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        try {
            getWindow().setStatusBarColor(-1);
            getWindow().setNavigationBarColor(-1);
            getWindow().getDecorView().setSystemUiVisibility(8192);
            buildShell();
            showLoading();
            this.store = app.pelican.check.Store.get(this);
            app.pelican.check.LegacyMigration.runOnce(this);
            showMonitor();
            java.util.Iterator<app.pelican.check.LineConfig> it = this.store.getLines().iterator();
            while (it.hasNext()) {
                if (it.next().enabled) {
                    startService(new android.content.Intent(this, (java.lang.Class<?>) app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RELOAD));
                    return;
                }
            }
        } catch (java.lang.Throwable th) {
            showFatal(th);
        }
    }

    private void showLoading() {
        android.widget.LinearLayout linearLayoutScreenContent = screenContent(screenScroll());
        android.widget.TextView textViewText = app.pelican.check.Ui.text(this, "正在启动…", 16.0f, app.pelican.check.Ui.MUTED);
        textViewText.setPadding(app.pelican.check.Ui.dp(this, 20.0f), app.pelican.check.Ui.dp(this, 40.0f), app.pelican.check.Ui.dp(this, 20.0f), app.pelican.check.Ui.dp(this, 40.0f));
        linearLayoutScreenContent.addView(textViewText);
    }

    private void showFatal(java.lang.Throwable th) {
        this.fatal = true;
        final java.lang.String strSave = app.pelican.check.CrashLog.save(this, th);
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        linearLayoutColumn.setPadding(app.pelican.check.Ui.dp(this, 18.0f), app.pelican.check.Ui.dp(this, 18.0f), app.pelican.check.Ui.dp(this, 18.0f), app.pelican.check.Ui.dp(this, 30.0f));
        linearLayoutColumn.setBackgroundColor(app.pelican.check.Ui.BG);
        scrollView.addView(linearLayoutColumn);
        linearLayoutColumn.addView(app.pelican.check.Ui.heading(this, "启动失败，已保留错误日志", 21.0f));
        app.pelican.check.Ui.add(linearLayoutColumn, app.pelican.check.Ui.text(this, "请把下面的错误内容截图或复制发给我。日志也已尝试保存到「下载/Pelican」。", 14.0f, app.pelican.check.Ui.TEXT), 10);
        android.widget.TextView textViewText = app.pelican.check.Ui.text(this, strSave, 11.0f, app.pelican.check.Ui.RED);
        textViewText.setTextIsSelectable(true);
        textViewText.setPadding(app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 12.0f));
        textViewText.setBackground(app.pelican.check.Ui.bordered(this, -1, app.pelican.check.Ui.BORDER, 10.0f));
        app.pelican.check.Ui.add(linearLayoutColumn, textViewText, 12);
        android.widget.Button button = app.pelican.check.Ui.button(this, "复制完整错误", true);
        app.pelican.check.Ui.add(linearLayoutColumn, button, 12);
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.3
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                ((android.content.ClipboardManager) app.pelican.check.MainActivity.this.getSystemService("clipboard")).setPrimaryClip(android.content.ClipData.newPlainText("启动错误", strSave));
                app.pelican.check.Ui.toast(app.pelican.check.MainActivity.this, "错误内容已复制");
            }
        });
        android.widget.Button button2 = app.pelican.check.Ui.button(this, "重试启动", false);
        app.pelican.check.Ui.add(linearLayoutColumn, button2, 8);
        button2.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.4
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.fatal = false;
                app.pelican.check.MainActivity.this.recreate();
            }
        });
        setContentView(scrollView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0) {
            requestPermissions(new java.lang.String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
        }
    }

    private void buildShell() {
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        linearLayoutColumn.setBackgroundColor(app.pelican.check.Ui.BG);
        android.widget.LinearLayout linearLayoutColumn2 = app.pelican.check.Ui.column(this);
        linearLayoutColumn2.setPadding(app.pelican.check.Ui.dp(this, 18.0f), app.pelican.check.Ui.dp(this, 12.0f), app.pelican.check.Ui.dp(this, 18.0f), app.pelican.check.Ui.dp(this, 10.0f));
        linearLayoutColumn2.setBackgroundColor(-1);
        linearLayoutColumn2.addView(app.pelican.check.Ui.heading(this, "Pelican", 21.0f));
        linearLayoutColumn2.addView(app.pelican.check.Ui.text(this, "监控运行 · 历史实时查看 · 本地保存", 11.0f, app.pelican.check.Ui.MUTED));
        linearLayoutColumn.addView(linearLayoutColumn2);
        this.content = new android.widget.FrameLayout(this);
        linearLayoutColumn.addView(this.content, new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
        this.nav = app.pelican.check.Ui.row(this);
        this.nav.setPadding(app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 8.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 8.0f));
        this.nav.setBackgroundColor(-1);
        linearLayoutColumn.addView(this.nav);
        setContentView(linearLayoutColumn);
    }

    private void buildNav() {
        this.nav.removeAllViews();
        java.lang.String[] strArr = {"监控", "历史", "设置"};
        int i = 0;
        while (i < 3) {
            final int selectedTab = i;
            android.widget.Button button = app.pelican.check.Ui.button(this, strArr[i], this.tab == i);
            int i2 = -1;
            button.setBackground(app.pelican.check.Ui.round(this, this.tab == i ? app.pelican.check.Ui.GREEN : -1, 12.0f));
            if (this.tab != i) {
                i2 = app.pelican.check.Ui.MUTED;
            }
            button.setTextColor(i2);
            app.pelican.check.Ui.action(this.nav, button, 1.0f);
            button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.5
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    app.pelican.check.MainActivity.this.tab = selectedTab;
                    app.pelican.check.MainActivity.this.refreshCurrent();
                }
            });
            i++;
        }
    }

    private android.widget.ScrollView screenScroll() {
        this.content.removeAllViews();
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setFillViewport(true);
        this.currentScroll = scrollView;
        this.content.addView(scrollView, new android.widget.FrameLayout.LayoutParams(-1, -1));
        return scrollView;
    }

    private android.widget.LinearLayout screenContent(android.widget.ScrollView scrollView) {
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        linearLayoutColumn.setPadding(app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 14.0f), app.pelican.check.Ui.dp(this, 24.0f));
        scrollView.addView(linearLayoutColumn);
        return linearLayoutColumn;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshCurrent() {
        buildNav();
        if (this.tab != 0) {
            if (this.tab != 1) {
                showSettings();
                return;
            } else {
                showHistory();
                return;
            }
        }
        showMonitor();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showMonitor() {
        if (this.tab != 0) {
            return;
        }
        buildNav();
        android.widget.LinearLayout linearLayoutScreenContent = screenContent(screenScroll());
        this.scheduleViews.clear();
        this.monitorCards.clear();
        java.util.List<app.pelican.check.LineConfig> lines = this.store.getLines();
        java.util.Iterator<app.pelican.check.LineConfig> it = lines.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().enabled) {
                i++;
            }
        }
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        linearLayoutCard.addView(app.pelican.check.Ui.heading(this, "长期监控", 18.0f));
        app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, i + " / " + lines.size() + " 条线路已启用 · 每条线路独立计时", 12.0f, app.pelican.check.Ui.MUTED), 4);
        android.widget.LinearLayout linearLayoutRow = app.pelican.check.Ui.row(this);
        android.widget.Button button = app.pelican.check.Ui.button(this, "＋ 添加线路", true);
        app.pelican.check.Ui.action(linearLayoutRow, button, 1.15f);
        android.widget.Button button2 = app.pelican.check.Ui.button(this, "全部开始", false);
        app.pelican.check.Ui.action(linearLayoutRow, button2, 1.0f);
        android.widget.Button button3 = app.pelican.check.Ui.button(this, "全部暂停", false);
        app.pelican.check.Ui.action(linearLayoutRow, button3, 1.0f);
        app.pelican.check.Ui.add(linearLayoutCard, linearLayoutRow, 12);
        linearLayoutScreenContent.addView(linearLayoutCard);
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.6
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.editLine(null);
            }
        });
        button2.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.7
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.setAll(true);
            }
        });
        button3.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.8
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.setAll(false);
            }
        });
        if (lines.isEmpty()) {
            android.widget.TextView textViewText = app.pelican.check.Ui.text(this, "还没有线路。先添加一条，再决定手动检测还是开启定时监控。", 14.0f, app.pelican.check.Ui.MUTED);
            textViewText.setPadding(app.pelican.check.Ui.dp(this, 20.0f), app.pelican.check.Ui.dp(this, 40.0f), app.pelican.check.Ui.dp(this, 20.0f), app.pelican.check.Ui.dp(this, 40.0f));
            textViewText.setGravity(17);
            linearLayoutScreenContent.addView(textViewText);
            return;
        }
        java.util.Iterator<app.pelican.check.LineConfig> it2 = lines.iterator();
        while (it2.hasNext()) {
            linearLayoutScreenContent.addView(lineCard(it2.next()), topMargin(12));
        }
    }

    private android.widget.LinearLayout lineCard(final app.pelican.check.LineConfig lineConfig) {
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        final app.pelican.check.MainActivity.MonitorCard monitorCard = new app.pelican.check.MainActivity.MonitorCard();
        this.monitorCards.put(lineConfig.id, monitorCard);
        android.widget.LinearLayout linearLayoutRow = app.pelican.check.Ui.row(this);
        linearLayoutRow.addView(app.pelican.check.Ui.heading(this, lineConfig.displayName(), 17.0f), app.pelican.check.Ui.weight(this, 1.0f));
        monitorCard.status = app.pelican.check.Ui.statusBadge(this, lineConfig.lastStatus);
        linearLayoutRow.addView(monitorCard.status);
        linearLayoutCard.addView(linearLayoutRow);
        app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, lineConfig.model + " · " + ("anthropic".equals(lineConfig.protocol) ? "Anthropic" : "OpenAI 兼容"), 12.0f, app.pelican.check.Ui.MUTED), 5);
        monitorCard.live = app.pelican.check.Ui.text(this, "", 13.0f, app.pelican.check.Ui.GREEN_DARK);
        monitorCard.live.setPadding(app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 9.0f), app.pelican.check.Ui.dp(this, 10.0f), app.pelican.check.Ui.dp(this, 9.0f));
        monitorCard.live.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_GREEN, 10.0f));
        app.pelican.check.Ui.add(linearLayoutCard, monitorCard.live, 8);
        monitorCard.live.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.9
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                if (monitorCard.active != null) {
                    app.pelican.check.MainActivity.this.openRun(monitorCard.active.id);
                }
            }
        });
        monitorCard.retry = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.ORANGE);
        app.pelican.check.Ui.add(linearLayoutCard, monitorCard.retry, 5);
        android.widget.TextView textViewText = app.pelican.check.Ui.text(this, "", 13.0f, app.pelican.check.Ui.MUTED);
        this.scheduleViews.put(lineConfig.id, textViewText);
        app.pelican.check.Ui.add(linearLayoutCard, textViewText, 7);
        monitorCard.last = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(linearLayoutCard, monitorCard.last, 4);
        monitorCard.baseline = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(linearLayoutCard, monitorCard.baseline, 4);
        android.widget.LinearLayout linearLayoutRow2 = app.pelican.check.Ui.row(this);
        monitorCard.now = app.pelican.check.Ui.button(this, "立即检测", true);
        android.widget.Button button = app.pelican.check.Ui.button(this, "编辑", false);
        android.widget.Button button2 = app.pelican.check.Ui.button(this, lineConfig.enabled ? "暂停定时" : "开启定时", false);
        app.pelican.check.Ui.action(linearLayoutRow2, monitorCard.now, 1.15f);
        app.pelican.check.Ui.action(linearLayoutRow2, button, 0.8f);
        app.pelican.check.Ui.action(linearLayoutRow2, button2, 1.15f);
        app.pelican.check.Ui.add(linearLayoutCard, linearLayoutRow2, 12);
        monitorCard.now.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.10
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                monitorCard.now.setEnabled(false);
                monitorCard.now.setText("已提交");
                app.pelican.check.MainActivity.this.ensureNotificationPermission();
                app.pelican.check.MainActivity.this.startService(new android.content.Intent(app.pelican.check.MainActivity.this, (java.lang.Class<?>) app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RUN_NOW).putExtra(app.pelican.check.MonitorService.EXTRA_LINE_ID, lineConfig.id));
                app.pelican.check.Ui.toast(app.pelican.check.MainActivity.this, "已提交，可到历史页查看实时过程");
            }
        });
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.11
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.editLine(lineConfig.id);
            }
        });
        button2.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.12
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.LineConfig line = app.pelican.check.MainActivity.this.store.getLine(lineConfig.id);
                if (line == null) {
                    return;
                }
                line.enabled = !line.enabled;
                line.nextRunAt = line.enabled ? java.lang.System.currentTimeMillis() + 3000 : 0L;
                if (line.enabled) {
                    app.pelican.check.MainActivity.this.ensureNotificationPermission();
                }
                app.pelican.check.MainActivity.this.store.saveLine(line);
                app.pelican.check.MainActivity.this.startService(new android.content.Intent(app.pelican.check.MainActivity.this, (java.lang.Class<?>) app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RELOAD));
                app.pelican.check.MainActivity.this.showMonitor();
            }
        });
        updateMonitorCard(lineConfig, monitorCard);
        return linearLayoutCard;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void openRun(java.lang.String str) {
        startActivity(new android.content.Intent(this, (java.lang.Class<?>) app.pelican.check.RunDetailActivity.class).putExtra(app.pelican.check.MonitorService.EXTRA_RUN_ID, str));
    }

    private void updateMonitorCard(app.pelican.check.LineConfig lineConfig, app.pelican.check.MainActivity.MonitorCard monitorCard) {
        int i;
        int i2;
        java.lang.String string;
        java.lang.String str;
        app.pelican.check.RunData runDataForLine = app.pelican.check.LiveRuns.forLine(lineConfig.id);
        monitorCard.active = runDataForLine;
        java.lang.String str2 = runDataForLine == null ? lineConfig.lastStatus : runDataForLine.status;
        monitorCard.status.setText(app.pelican.check.Ui.statusText(str2));
        android.widget.TextView textView = monitorCard.status;
        if (runDataForLine != null) {
            i = app.pelican.check.Ui.GREEN;
        } else if ("FAIL".equals(str2)) {
            i = app.pelican.check.Ui.BLUEGRAY;
        } else if ("ERROR".equals(str2)) {
            i = app.pelican.check.Ui.RED;
        } else {
            i = "SUSPECT".equals(str2) ? app.pelican.check.Ui.ORANGE : app.pelican.check.Ui.GREEN;
        }
        textView.setTextColor(i);
        android.widget.TextView textView2 = monitorCard.status;
        if (runDataForLine != null) {
            i2 = app.pelican.check.Ui.PALE_GREEN;
        } else {
            i2 = "ERROR".equals(str2) ? app.pelican.check.Ui.PALE_RED : app.pelican.check.Ui.PALE_GRAY;
        }
        textView2.setBackground(app.pelican.check.Ui.round(this, i2, 8.0f));
        int i3 = 8;
        monitorCard.live.setVisibility(runDataForLine == null ? 8 : 0);
        if (runDataForLine != null) {
            monitorCard.live.setText(("QUEUED".equals(runDataForLine.status) ? "排队中" : "正在检测 · " + runDataForLine.stage) + "  ·  查看实时详情 →");
        }
        monitorCard.now.setEnabled(runDataForLine == null);
        monitorCard.now.setText(runDataForLine == null ? "立即检测" : "检测中…");
        monitorCard.last.setText("最近完成：" + app.pelican.check.Ui.time(lineConfig.lastRunAt) + " · 每轮 " + lineConfig.sampleCount + " 张 · 最多补试 " + lineConfig.retryCount + " 次");
        android.widget.TextView textView3 = monitorCard.retry;
        if (!lineConfig.lastBadge.isEmpty() && runDataForLine == null) {
            i3 = 0;
        }
        textView3.setVisibility(i3);
        monitorCard.retry.setText(lineConfig.lastBadge + (lineConfig.lastBadge.contains("仍失败") ? " · 点击历史查看原因" : ""));
        monitorCard.retry.setTextColor((lineConfig.lastBadge.contains("仍失败") || lineConfig.lastBadge.contains("Token 异常")) ? app.pelican.check.Ui.RED : app.pelican.check.Ui.ORANGE);
        PromptSequence.Entry next=PromptSequence.current(lineConfig);
        app.pelican.check.Baseline baseline = this.store.getBaseline(lineConfig.id,lineConfig.model,next.id);
        monitorCard.baseline.setText("第 "+(lineConfig.sequenceIndex+1)+" 道 / 共 "+lineConfig.promptSequence.size()+" 道 · 下一轮："+next.label
            +"\n"+QualityPolicy.equation(lineConfig,baseline)
            +(lineConfig.sequenceBlocked?"\n"+lineConfig.sequenceFailureNote:""));
        android.widget.TextView textView5 = this.scheduleViews.get(lineConfig.id);
        if (textView5 != null) {
            if (runDataForLine != null) {
                str = lineConfig.enabled ? "定时已启用 · 本轮结束后计时" : "手动检测 · 定时暂停";
            } else {
                str = lineConfig.enabled ? "每 " + lineConfig.intervalMin + " 分钟 · 下次 " + app.pelican.check.Ui.time(lineConfig.nextRunAt) : "定时已暂停";
            }
            textView5.setText(str);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshLive() {
        boolean migrating=store!=null&&store.migrating24;
        if(wasMigrating24&&!migrating&&tab==1){wasMigrating24=false;showHistory();return;}
        wasMigrating24=migrating;

        if (this.store == null || this.fatal) {
            return;
        }
        if (this.tab != 0) {
            if (this.tab != 1 || this.historyList == null) {
                return;
            }
            refreshHistoryLive();
            return;
        }
        for (app.pelican.check.LineConfig lineConfig : this.store.getLines()) {
            app.pelican.check.MainActivity.MonitorCard monitorCard = this.monitorCards.get(lineConfig.id);
            if (monitorCard != null) {
                updateMonitorCard(lineConfig, monitorCard);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAll(boolean z) {
        ensureNotificationPermission();
        this.store.setAllEnabled(z);
        startService(new android.content.Intent(this, (java.lang.Class<?>) app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RELOAD));
        showMonitor();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void editLine(java.lang.String str) {
        startActivityForResult(new android.content.Intent(this, (java.lang.Class<?>) app.pelican.check.LineEditActivity.class).putExtra(app.pelican.check.MonitorService.EXTRA_LINE_ID, str), 10);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showHistory() {
        if (this.tab != 1) {
            return;
        }
        buildNav();
        android.widget.LinearLayout linearLayoutScreenContent = screenContent(screenScroll());
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        android.widget.LinearLayout linearLayoutRow = app.pelican.check.Ui.row(this);
        linearLayoutRow.addView(app.pelican.check.Ui.heading(this, "历史", 18.0f), app.pelican.check.Ui.weight(this, 1.0f));
        android.widget.Button button = app.pelican.check.Ui.button(this, "趋势", false);
        this.manageButton = app.pelican.check.Ui.button(this, this.manageMode ? "完成" : "批量管理", false);
        linearLayoutCard.addView(linearLayoutRow);
        android.widget.LinearLayout historyActions=app.pelican.check.Ui.row(this);
        button.setBackground(Ui.round(this,Ui.BG,12));button.setTextColor(Ui.GREEN_DARK);
        manageButton.setBackground(Ui.round(this,Ui.BG,12));manageButton.setTextColor(Ui.GREEN_DARK);
        android.widget.LinearLayout.LayoutParams trendParams=new android.widget.LinearLayout.LayoutParams(0,-2,1);trendParams.rightMargin=Ui.dp(this,7);
        android.widget.LinearLayout.LayoutParams manageParams=new android.widget.LinearLayout.LayoutParams(0,-2,1);manageParams.leftMargin=Ui.dp(this,7);
        historyActions.addView(button,trendParams);historyActions.addView(manageButton,manageParams);Ui.add(linearLayoutCard,historyActions,10);
        final android.widget.Button button2 = app.pelican.check.Ui.button(this, this.filtersExpanded ? "收起筛选 ▴" : "线路 / 模型 / 状态筛选 ▾", false);
        this.filterToggle = button2;
        app.pelican.check.Ui.add(linearLayoutCard, button2, 9);
        this.filterBody = app.pelican.check.Ui.column(this);
        linearLayoutCard.addView(this.filterBody);
        this.filterBody.setVisibility(this.filtersExpanded ? 0 : 8);
        button2.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.13
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.filtersExpanded = !app.pelican.check.MainActivity.this.filtersExpanded;
                app.pelican.check.MainActivity.this.filterBody.setVisibility(app.pelican.check.MainActivity.this.filtersExpanded ? 0 : 8);
                button2.setText(app.pelican.check.MainActivity.this.filtersExpanded ? "收起筛选 ▴" : "线路 / 模型 / 状态筛选 ▾");
            }
        });
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.14
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.startActivity(new android.content.Intent(app.pelican.check.MainActivity.this, (java.lang.Class<?>) app.pelican.check.TrendActivity.class));
            }
        });
        this.manageButton.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.15
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.selectionRequest++;
                app.pelican.check.MainActivity.this.manageMode = !app.pelican.check.MainActivity.this.manageMode;
                app.pelican.check.MainActivity.this.selected.clear();
                app.pelican.check.MainActivity.this.showHistory();
            }
        });
        this.filterLines = this.store.getLines();
        java.util.ArrayList arrayList = new java.util.ArrayList();
        arrayList.add("全部线路");
        java.util.Iterator<app.pelican.check.LineConfig> it = this.filterLines.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().displayName());
        }
        android.widget.LinearLayout filterTop = app.pelican.check.Ui.row(this);
        android.widget.LinearLayout lineColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout modelColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout.LayoutParams halfA = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
        halfA.rightMargin = app.pelican.check.Ui.dp(this, 5);
        android.widget.LinearLayout.LayoutParams halfB = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
        halfB.leftMargin = app.pelican.check.Ui.dp(this, 5);
        filterTop.addView(lineColumn, halfA);filterTop.addView(modelColumn,halfB);this.filterBody.addView(filterTop);
        this.lineFilter = spinner(lineColumn, "线路", arrayList);
        java.util.ArrayList arrayList2 = new java.util.ArrayList();
        arrayList2.add("全部模型");
        arrayList2.addAll(this.store.getModels());
        this.modelFilter = spinner(modelColumn, "模型", arrayList2);
        android.widget.LinearLayout filterBottom = app.pelican.check.Ui.row(this);
        android.widget.LinearLayout statusColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout specialColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout.LayoutParams halfC = new android.widget.LinearLayout.LayoutParams(0, -2, 1);halfC.rightMargin=app.pelican.check.Ui.dp(this,5);
        android.widget.LinearLayout.LayoutParams halfD = new android.widget.LinearLayout.LayoutParams(0, -2, 1);halfD.leftMargin=app.pelican.check.Ui.dp(this,5);
        filterBottom.addView(statusColumn,halfC);filterBottom.addView(specialColumn,halfD);this.filterBody.addView(filterBottom);
        this.statusFilter = spinner(statusColumn, "检测状态", java.util.Arrays.asList("全部状态", "通过", "疑似降智", "异常", "检测失败", "进行中", "排队中"));
        this.specialFilter = spinner(specialColumn, "连接标记", java.util.Arrays.asList("全部记录", "重试成功", "重试失败", "本机网络中断", "上游异常", "输入 Token 异常"));
        int i = 0;
        for (int i2 = 0; i2 < this.filterLines.size(); i2++) {
            if (this.filterLines.get(i2).id.equals(this.filter.lineId)) {
                i = i2 + 1;
            }
        }
        if (i == 0) {
            this.filter.lineId = "";
        }
        this.lineFilter.setSelection(i);
        int iIndexOf = arrayList2.indexOf(this.filter.model);
        this.modelFilter.setSelection(java.lang.Math.max(0, iIndexOf));
        if (iIndexOf < 0) {
            this.filter.model = "";
        }
        this.statusFilter.setSelection(java.lang.Math.min(this.filterStatusPos, this.statusFilter.getCount() - 1));
        this.specialFilter.setSelection(java.lang.Math.min(this.filterSpecialPos, this.specialFilter.getCount() - 1));
        android.widget.Button button3 = app.pelican.check.Ui.button(this, "应用筛选", true);
        app.pelican.check.Ui.add(this.filterBody, button3, 10);
        button3.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.16
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.applyFilter();
            }
        });
        linearLayoutScreenContent.addView(linearLayoutCard);
        if (this.manageMode) {
            android.widget.LinearLayout selectionCard = app.pelican.check.Ui.card(this);
            this.selectionHint = app.pelican.check.Ui.text(this, "已选 0 条 · 检测中的记录不可选", 12, app.pelican.check.Ui.GREEN);
            selectionCard.addView(this.selectionHint);
            android.widget.LinearLayout selectRow=app.pelican.check.Ui.row(this);
            android.widget.Button page=app.pelican.check.Ui.button(this,"本页全选",false);
            android.widget.Button all=app.pelican.check.Ui.button(this,"筛选全选",false);
            app.pelican.check.Ui.action(selectRow,page,1);app.pelican.check.Ui.action(selectRow,all,1);
            app.pelican.check.Ui.add(selectionCard,selectRow,8);
            android.widget.LinearLayout inverseRow=app.pelican.check.Ui.row(this);
            android.widget.Button inverse=app.pelican.check.Ui.button(this,"反选本页",false);
            android.widget.Button clear=app.pelican.check.Ui.button(this,"取消全选",false);
            app.pelican.check.Ui.action(inverseRow,inverse,1);app.pelican.check.Ui.action(inverseRow,clear,1);
            app.pelican.check.Ui.add(selectionCard,inverseRow,6);
            app.pelican.check.Ui.add(linearLayoutScreenContent,selectionCard,10);
            page.setOnClickListener(v -> selectLoaded(false));inverse.setOnClickListener(v -> selectLoaded(true));
            all.setOnClickListener(v -> selectFiltered());clear.setOnClickListener(v -> {selectionRequest++;selected.clear();updateDeleteButton();});
        } else this.selectionHint=null;
        this.deleteSelectedButton = app.pelican.check.Ui.button(this, "删除所选", false);
        this.deleteSelectedButton.setTextColor(app.pelican.check.Ui.RED);
        this.deleteSelectedButton.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_RED, 12.0f));
        this.deleteSelectedButton.setVisibility(this.manageMode ? 0 : 8);
        app.pelican.check.Ui.add(linearLayoutScreenContent, this.deleteSelectedButton, 10);
        this.deleteSelectedButton.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.17
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.deleteSelected();
            }
        });
        this.historyHint = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.GREEN);
        app.pelican.check.Ui.add(linearLayoutScreenContent, this.historyHint, 10);
        this.historyHint.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.18
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                app.pelican.check.MainActivity.this.reloadHistoryCards();
            }
        });
        this.historyList = app.pelican.check.Ui.column(this);
        app.pelican.check.Ui.add(linearLayoutScreenContent, this.historyList, 4);
        reloadHistoryCards();
    }

    private android.widget.Spinner spinner(android.widget.LinearLayout parent, String label, java.util.List<String> values) {
        return app.pelican.check.Ui.choiceField(parent,label,values);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyFilter() {
        this.filterLinePos = this.lineFilter.getSelectedItemPosition();
        this.filterModelPos = this.modelFilter.getSelectedItemPosition();
        this.filterStatusPos = this.statusFilter.getSelectedItemPosition();
        this.filterSpecialPos = this.specialFilter.getSelectedItemPosition();
        this.filter.lineId = this.lineFilter.getSelectedItemPosition() > 0 ? this.filterLines.get(this.lineFilter.getSelectedItemPosition() - 1).id : "";
        this.filter.model = this.modelFilter.getSelectedItemPosition() > 0 ? java.lang.String.valueOf(this.modelFilter.getSelectedItem()) : "";
        int selectedItemPosition = this.statusFilter.getSelectedItemPosition();
        this.filter.status = selectedItemPosition == 0 ? "" : new java.lang.String[]{"PASS", "SUSPECT", "ERROR", "FAIL", "RUNNING", "QUEUED"}[selectedItemPosition - 1];
        int selectedItemPosition2 = this.specialFilter.getSelectedItemPosition();
        this.filter.special = selectedItemPosition2 != 0 ? new java.lang.String[]{"retry_success", "retry_failed", "local", "upstream", "token_alert"}[selectedItemPosition2 - 1] : "";
        this.selectionRequest++;
        this.selected.clear();
        reloadHistoryCards();
        this.filtersExpanded = false;
        this.filterBody.setVisibility(8);
        this.filterToggle.setText("线路 / 模型 / 状态筛选 ▾");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void appendHistoryPage() {
        java.util.List<app.pelican.check.RunData> runsBefore = this.store.getRunsBefore(this.filter, 30, this.history.isEmpty() ? null : this.history.get(this.history.size() - 1));
        this.historyOffset += runsBefore.size();
        if (this.historyOffset == runsBefore.size() && runsBefore.isEmpty()) {
            android.widget.TextView textViewText = app.pelican.check.Ui.text(this, "没有符合筛选条件的历史。", 14.0f, app.pelican.check.Ui.MUTED);
            textViewText.setPadding(20, 40, 20, 40);
            textViewText.setGravity(17);
            this.historyList.addView(textViewText);
            return;
        }
        for (app.pelican.check.RunData runData : runsBefore) {
            this.history.add(runData);
            addHistoryCard(runData, this.history.size());
        }
        updateDeleteButton();
        if (runsBefore.size() == 30) {
            final android.widget.Button button = app.pelican.check.Ui.button(this, "加载更多", false);
            app.pelican.check.Ui.add(this.historyList, button, 12);
            button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.19
                @Override // android.view.View.OnClickListener
                public void onClick(android.view.View view) {
                    app.pelican.check.MainActivity.this.historyList.removeView(button);
                    app.pelican.check.MainActivity.this.appendHistoryPage();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadHistoryCards() {
        this.historyOffset = 0;
        this.history.clear();
        this.historyCards.clear();
        this.historyList.removeAllViews();
        this.historyDirty = false;
        appendHistoryPage();
        this.latestHistoryId = this.history.isEmpty() ? "" : this.history.get(0).id;
        updateHistoryHint();
        updateDeleteButton();
    }

    private void updateHistoryHint() {
        if (this.historyHint == null) {
            return;
        }
        this.historyHint.setText(this.historyDirty ? "有新检测或状态变化 · 点此更新列表" : ((this.filter.lineId.isEmpty() && this.filter.model.isEmpty() && this.filter.status.isEmpty() && this.filter.special.isEmpty()) ? "全部记录" : "当前筛选结果") + " · 正在检测的记录可直接点开");
    }

    private void refreshHistoryLive() {
        java.util.List<app.pelican.check.RunData> runsBefore = this.store.getRunsBefore(this.filter, 1, null);
        if (!(runsBefore.isEmpty() ? "" : runsBefore.get(0).id).equals(this.latestHistoryId)) {
            this.historyDirty = true;
            if (!this.manageMode && this.currentScroll.getScrollY() < app.pelican.check.Ui.dp(this, 24.0f)) {
                reloadHistoryCards();
                return;
            }
        }
        for (app.pelican.check.MainActivity.HistoryCard historyCard : new java.util.ArrayList<app.pelican.check.MainActivity.HistoryCard>(this.historyCards.values())) {
            if (historyCard.run.isActive()) {
                app.pelican.check.LiveRuns.Snapshot snapshot = app.pelican.check.LiveRuns.read(historyCard.run.id);
                app.pelican.check.RunData run = snapshot == null ? this.store.getRun(historyCard.run.id) : snapshot.run;
                if (run != null) {
                    boolean z = !run.isActive();
                    historyCard.run = run;
                    updateHistoryCard(historyCard);
                    if (z && !this.filter.status.isEmpty()) {
                        this.historyDirty = true;
                    }
                }
            }
        }
        updateHistoryHint();
    }

    /* JADX WARN: Code duplicated, block: B:9:0x0025  */
    private void addHistoryCard(final app.pelican.check.RunData runData, int i) {
        android.widget.LinearLayout linearLayout2 = null;
        int childCount = this.historyList.getChildCount();
        if (childCount > 0) {
            android.view.View last = this.historyList.getChildAt(childCount - 1);
            if (last instanceof android.widget.LinearLayout && "historyRow1".equals(last.getTag()))
                linearLayout2 = (android.widget.LinearLayout) last;
        }
        if (linearLayout2 == null) {
            linearLayout2 = app.pelican.check.Ui.row(this);
            linearLayout2.setGravity(48); linearLayout2.setTag("historyRow1");
            this.historyList.addView(linearLayout2, topMargin(10));
        } else {
            if (linearLayout2.getChildCount() > 1) linearLayout2.removeViewAt(1);
            linearLayout2.setTag("historyRow2");
        }
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(this);
        linearLayoutColumn.setPadding(app.pelican.check.Ui.dp(this, 4.0f), 0, app.pelican.check.Ui.dp(this, 4.0f), 0);
        linearLayout2.addView(linearLayoutColumn, new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        if ("historyRow1".equals(linearLayout2.getTag())) {
            linearLayout2.addView(new android.view.View(this), new android.widget.LinearLayout.LayoutParams(0, 1, 1.0f));
        }
        final app.pelican.check.MainActivity.HistoryCard historyCard = new app.pelican.check.MainActivity.HistoryCard();
        historyCard.run = runData;
        this.historyCards.put(runData.id, historyCard);
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        linearLayoutCard.setPadding(app.pelican.check.Ui.dp(this, 9.0f), app.pelican.check.Ui.dp(this, 9.0f), app.pelican.check.Ui.dp(this, 9.0f), app.pelican.check.Ui.dp(this, 9.0f));
        linearLayoutColumn.addView(linearLayoutCard);
        android.widget.LinearLayout linearLayoutRow2 = app.pelican.check.Ui.row(this);
        linearLayoutRow2.addView(app.pelican.check.Ui.text(this, java.lang.String.format(java.util.Locale.getDefault(), "%02d", java.lang.Integer.valueOf(i)), 11.0f, app.pelican.check.Ui.MUTED), app.pelican.check.Ui.weight(this, 1.0f));
        if (this.manageMode) {
            historyCard.check = new android.widget.CheckBox(this);
            historyCard.check.setChecked(this.selected.contains(runData.id));
            historyCard.check.setEnabled(!runData.isActive());
            linearLayoutRow2.addView(historyCard.check);
            historyCard.check.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() { // from class: app.pelican.check.MainActivity.20
                @Override // android.widget.CompoundButton.OnCheckedChangeListener
                public void onCheckedChanged(android.widget.CompoundButton compoundButton, boolean z) {
                    java.util.Set set = app.pelican.check.MainActivity.this.selected;
                    if (z) {
                        set.add(runData.id);
                    } else {
                        set.remove(runData.id);
                    }
                    app.pelican.check.MainActivity.this.updateDeleteButton();
                }
            });
        }
        historyCard.tokenAlert = app.pelican.check.Ui.badge(this,"注入",0xFF2E63A3,0xFFEAF1FB);
        android.widget.LinearLayout.LayoutParams injectionParams=new android.widget.LinearLayout.LayoutParams(-2,-2);injectionParams.rightMargin=Ui.dp(this,5);
        linearLayoutRow2.addView(historyCard.tokenAlert,injectionParams);
        historyCard.status = app.pelican.check.Ui.statusBadge(this, runData.status);
        if (!this.manageMode) {
            linearLayoutRow2.addView(historyCard.status);
        }
        linearLayoutCard.addView(linearLayoutRow2);
        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
        frameLayout.setBackground(app.pelican.check.Ui.round(this, app.pelican.check.Ui.PALE_GRAY, 10.0f));
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, app.pelican.check.Ui.dp(this, 145.0f));
        layoutParams.topMargin = app.pelican.check.Ui.dp(this, 7.0f);
        linearLayoutCard.addView(frameLayout, layoutParams);
        historyCard.image = new android.widget.ImageView(this);
        historyCard.image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        frameLayout.addView(historyCard.image, new android.widget.FrameLayout.LayoutParams(-1, -1));
        historyCard.placeholder = app.pelican.check.Ui.text(this, "", 12.0f, app.pelican.check.Ui.MUTED);
        historyCard.placeholder.setGravity(17);
        historyCard.placeholder.setPadding(app.pelican.check.Ui.dp(this, 8.0f), 0, app.pelican.check.Ui.dp(this, 8.0f), 0);
        frameLayout.addView(historyCard.placeholder, new android.widget.FrameLayout.LayoutParams(-1, -1));
        historyCard.retry = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.ORANGE);
        app.pelican.check.Ui.add(linearLayoutCard, historyCard.retry, 6);
        android.widget.TextView textViewHeading = app.pelican.check.Ui.heading(this, runData.lineName, 13.0f);
        textViewHeading.setMaxLines(1);
        textViewHeading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        app.pelican.check.Ui.add(linearLayoutCard, textViewHeading, 7);
        android.widget.TextView textViewText = app.pelican.check.Ui.text(this, runData.model, 10.0f, app.pelican.check.Ui.MUTED);
        textViewText.setMaxLines(1);
        textViewText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        app.pelican.check.Ui.add(linearLayoutCard, textViewText, 2);
        historyCard.meta = app.pelican.check.Ui.text(this, "", 10.0f, app.pelican.check.Ui.TEXT);
        app.pelican.check.Ui.add(linearLayoutCard, historyCard.meta, 4);
        historyCard.usage = app.pelican.check.Ui.text(this, "", 10, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(linearLayoutCard,historyCard.usage,4);
        historyCard.progress = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.BLUEGRAY);
        historyCard.progress.setMaxLines(3);
        app.pelican.check.Ui.add(linearLayoutCard, historyCard.progress, 5);
        historyCard.link = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.GREEN);
        app.pelican.check.Ui.add(linearLayoutCard, historyCard.link, 5);
        updateHistoryCard(historyCard);
        linearLayoutCard.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.MainActivity.21
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                if (!app.pelican.check.MainActivity.this.manageMode) {
                    app.pelican.check.MainActivity.this.openRun(runData.id);
                } else if (historyCard.run.isActive()) {
                    app.pelican.check.Ui.toast(app.pelican.check.MainActivity.this, "正在检测，结束后才可删除");
                } else {
                    historyCard.check.setChecked(!historyCard.check.isChecked());
                }
            }
        });
    }

    private void updateHistoryCard(app.pelican.check.MainActivity.HistoryCard historyCard) {
        int i;
        int i2;
        java.lang.String str;
        java.lang.String str2;
        app.pelican.check.RunData runData = historyCard.run;
        historyCard.status.setText(app.pelican.check.Ui.statusText(runData.status));
        historyCard.tokenAlert.setVisibility(runData.inputTokenAlert ? 0 : 8);
        historyCard.tokenAlert.setText("注入");
        long in=0,out=0;for(app.pelican.check.RunData.SampleData sample:runData.samples){in+=sample.inTokens;out+=sample.outTokens;}
        historyCard.usage.setVisibility(in>0||out>0?0:8);
        historyCard.usage.setText("输入 " + compactTokens(in) + " · 输出 " + compactTokens(out));
        if (runData.isActive()) {
            i = app.pelican.check.Ui.BLUEGRAY;
        } else if ("PASS".equals(runData.status)) {
            i = app.pelican.check.Ui.GREEN;
        } else if ("ERROR".equals(runData.status)) {
            i = app.pelican.check.Ui.RED;
        } else {
            i = "SUSPECT".equals(runData.status) ? app.pelican.check.Ui.ORANGE : app.pelican.check.Ui.BLUEGRAY;
        }
        if ("PASS".equals(runData.status) || runData.isActive()) {
            i2 = app.pelican.check.Ui.PALE_GREEN;
        } else {
            i2 = "ERROR".equals(runData.status) ? app.pelican.check.Ui.PALE_RED : app.pelican.check.Ui.PALE_GRAY;
        }
        historyCard.status.setTextColor(i);
        historyCard.status.setBackground(app.pelican.check.Ui.round(this, i2, 8.0f));
        if (!historyCard.loadedImage && !runData.samples.isEmpty()) {
            app.pelican.check.RunData.SampleData sampleData = runData.samples.get(0);
            if (sampleData.svgOk && sampleData.svgPath != null && !sampleData.svgPath.isEmpty()) {
                historyCard.loadedImage = true;
                app.pelican.check.SvgImages.load(historyCard.image, sampleData.svgPath, sampleData.thumbPath, 600, 600);
            }
        }
        historyCard.placeholder.setVisibility(historyCard.loadedImage ? 8 : 0);
        android.widget.TextView textView = historyCard.placeholder;
        if (runData.isActive()) {
            str = "QUEUED".equals(runData.status) ? "排队中" : "正在检测…\n点开查看实时输出";
        } else {
            str = runData.interrupted ? "检测中断" : "没有生成图片\n点开查看原因";
        }
        textView.setText(str);
        historyCard.retry.setVisibility((runData.retryFailed || runData.retrySuccess) ? 0 : 8);
        historyCard.retry.setText(runData.retryFailed ? "重试后仍失败" : "重试后成功");
        historyCard.retry.setTextColor(runData.retryFailed ? app.pelican.check.Ui.RED : app.pelican.check.Ui.ORANGE);
        android.widget.TextView textView2 = historyCard.meta;
        java.lang.StringBuilder sbAppend = new java.lang.StringBuilder().append(app.pelican.check.Ui.time(runData.startedAt));
        if (runData.isActive()) {
            str2 = "";
        } else {
            str2 = runData.hasScore() ? " · 复杂度 " + java.lang.Math.round(runData.score) : " · 无评分";
        }
        textView2.setText(sbAppend.append(str2).append("\n").append(runData.promptLabel==null?"旧题目":runData.promptLabel)
            .append(runData.reassessmentNote==null||runData.reassessmentNote.isEmpty()?"":"\n"+runData.reassessmentNote).toString());
        historyCard.progress.setVisibility(runData.isActive() ? 0 : 8);
        if (runData.isActive()) {
            historyCard.progress.setText(app.pelican.check.MonitorEngine.liveDescription(runData) + " · 已过 " + app.pelican.check.Ui.elapsed(runData.startedAt, 0L));
        }
        historyCard.link.setText(runData.isActive() ? "查看实时过程 →" : "查看结果 →");
        if (historyCard.check != null) {
            historyCard.check.setEnabled(!runData.isActive());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDeleteButton() {
        if (this.deleteSelectedButton != null) {
            this.deleteSelectedButton.setText("删除所选（" + this.selected.size() + "）");
            this.deleteSelectedButton.setEnabled(!this.selected.isEmpty());
        }
        if(this.selectionHint!=null)this.selectionHint.setText("已选 "+selected.size()+" 条 · 当前已加载 "+history.size()+" 条");
        if(syncingSelection)return;syncingSelection=true;
        for(HistoryCard card:historyCards.values())if(card.check!=null){
            boolean checked=selected.contains(card.run.id);
            if(card.check.isChecked()!=checked)card.check.setChecked(checked);
        }
        syncingSelection=false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteSelected() {
        if (this.selected.isEmpty()) {
            app.pelican.check.Ui.toast(this, "先选择要删除的记录");
        } else {
            new android.app.AlertDialog.Builder(this).setTitle("批量删除").setMessage("确定删除 " + this.selected.size() + " 次检测及其 SVG 文件？").setNegativeButton("取消", (android.content.DialogInterface.OnClickListener) null).setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() { // from class: app.pelican.check.MainActivity.22
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(android.content.DialogInterface dialogInterface, int i) {
                    app.pelican.check.MainActivity.this.store.deleteRuns(new java.util.ArrayList(app.pelican.check.MainActivity.this.selected));
                    app.pelican.check.MainActivity.this.selected.clear();
                    app.pelican.check.MainActivity.this.showHistory();
                }
            }).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showSettings() {
        if(tab!=2)return;buildNav();
        android.widget.LinearLayout body=screenContent(screenScroll());
        android.widget.LinearLayout intro=app.pelican.check.Ui.card(this);
        intro.addView(app.pelican.check.Ui.heading(this,"按你的节奏运行",20));
        app.pelican.check.Ui.add(intro,app.pelican.check.Ui.text(this,"新线路默认值、后台提醒与本地数据，分开管理。",12,app.pelican.check.Ui.MUTED),6);
        body.addView(intro);
        android.widget.LinearLayout defaults=settingsCard(body,"检测偏好","默认值只影响新建线路，已有线路不会悄悄改变。");
        settingsAction(defaults,"新线路默认值","周期 / 重试 / 输入差额 / 连续异常通知",()->showDefaults());
        android.widget.LinearLayout runtime=settingsCard(body,"后台与提醒","不做开机自启；从最近任务划掉应用会停止监控。");
        settingsAction(runtime,"系统通知","允许后才能收到运行状态和异常提醒",()->safeSettings(new android.content.Intent("android.settings.APP_NOTIFICATION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",getPackageName())));
        settingsAction(runtime,"电池与后台运行","小米系统的省电限制可能影响定时",()->new android.app.AlertDialog.Builder(this).setTitle("后台运行说明").setMessage("监控使用前台服务。需要长期运行时，可在系统里允许后台活动，并按需取消电池优化。系统仍可能限制后台，应用不会宣称绝对准时。").setNegativeButton("知道了",null).setPositiveButton("打开电池设置",(d,w)->safeSettings(new android.content.Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"))).show());
        android.widget.LinearLayout data=settingsCard(body,"数据与备份","检测历史保留在本机；备份不包含 API 密钥。");
        final android.widget.TextView bytes=app.pelican.check.Ui.text(this,"正在统计历史占用…",12,app.pelican.check.Ui.MUTED);app.pelican.check.Ui.add(data,bytes,8);
        new Thread(()->{long value=store.historyBytes();runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())bytes.setText("历史与图片占用  "+app.pelican.check.Ui.bytes(value));});},"pelican-storage").start();
        android.widget.LinearLayout backup=app.pelican.check.Ui.row(this);android.widget.Button export=app.pelican.check.Ui.button(this,"导出备份",true),imp=app.pelican.check.Ui.button(this,"导入备份",false);
        app.pelican.check.Ui.action(backup,export,1);app.pelican.check.Ui.action(backup,imp,1);app.pelican.check.Ui.add(data,backup,10);export.setOnClickListener(v->beginExport());imp.setOnClickListener(v->beginImport());
        settingsAction(data,"管理历史","去历史页按条件选择，或批量删除",()->{tab=1;showHistory();});
        android.widget.LinearLayout trust=settingsCard(body,"输入检查与组件","先核对实际请求，再用本地估算筛查异常。");
        settingsAction(trust,"输入异常筛查与旧历史重判","本地估算 / 数量或倍率 / 字节补充规则",()->new android.app.AlertDialog.Builder(this).setTitle("输入异常筛查").setMessage("本地估算直接用于筛查，可选数量或倍率。估算不可用或为 0 时，使用接口输入 Token 大于请求正文字节数的补充规则。\n\n蓝色“注入”仅表示疑似异常，不能据此确认。输入异常不弹窗、不发送系统通知，也不计入连续质量异常。\n\n旧历史只在本地重判，保留原记录，不调用接口；资料不足明确标注无法重判。"+"\n\n"+(store.migration24Note.isEmpty()?Prefs.get(this).getString("migration24Note","未发现需要重判的旧历史"):store.migration24Note)).setPositiveButton("明白",null).show());
        settingsAction(trust,"关于 Pelican 2.4","离线保存 · 不上传遥测 · 查看开源组件",()->new android.app.AlertDialog.Builder(this).setTitle("Pelican 2.4").setMessage("OkHttp / Gson / AndroidSVG / MPAndroidChart：Apache-2.0\nJTokkit：MIT\n\n图表采用 MPAndroidChart，分词采用 JTokkit。检测与输入检查都是辅助指标，不能单凭一项结果确认降智或注入。详细许可随安装包附带。").setPositiveButton("关闭",null).show());
        android.widget.LinearLayout danger=settingsCard(body,"谨慎操作","只在确认已备份后删除。");
        android.widget.Button clear=app.pelican.check.Ui.button(this,"清理全部已完成历史…",false);clear.setTextColor(app.pelican.check.Ui.RED);app.pelican.check.Ui.add(danger,clear,8);
        clear.setOnClickListener(v->{android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("删除全部已完成历史？").setMessage("检测指标、请求记录与 SVG 将永久删除；进行中的记录和线路配置保留。此操作不可撤销。").setNegativeButton("取消",null).setPositiveButton("删除已完成历史",(d,w)->{store.clearHistory(null);showSettings();}).create();dialog.setOnShowListener(d->dialog.getButton(-1).setTextColor(app.pelican.check.Ui.RED));dialog.show();});
    }

    private void settingsAction(android.widget.LinearLayout parent,String title,String subtitle,Runnable action){
        android.widget.LinearLayout row=app.pelican.check.Ui.row(this);row.setPadding(0,app.pelican.check.Ui.dp(this,10),0,app.pelican.check.Ui.dp(this,10));row.setMinimumHeight(app.pelican.check.Ui.dp(this,64));
        android.widget.LinearLayout labels=app.pelican.check.Ui.column(this);labels.addView(app.pelican.check.Ui.heading(this,title,14));app.pelican.check.Ui.add(labels,app.pelican.check.Ui.text(this,subtitle,11,app.pelican.check.Ui.MUTED),3);row.addView(labels,new android.widget.LinearLayout.LayoutParams(0,-2,1));
        android.widget.TextView arrow=app.pelican.check.Ui.text(this,"›",23,app.pelican.check.Ui.MUTED);arrow.setPadding(app.pelican.check.Ui.dp(this,12),0,0,0);row.addView(arrow);row.setContentDescription(title+"，"+subtitle);row.setBackground(app.pelican.check.Ui.round(this,android.graphics.Color.TRANSPARENT,10));row.setOnClickListener(v->action.run());app.pelican.check.Ui.add(parent,row,4);
    }
    private void safeSettings(android.content.Intent intent){try{startActivity(intent);}catch(Exception e){app.pelican.check.Ui.toast(this,"系统未提供此入口，请在应用信息中调整");}}
    private void showDefaults(){
        android.widget.LinearLayout form=app.pelican.check.Ui.column(this);form.setPadding(app.pelican.check.Ui.dp(this,20),0,app.pelican.check.Ui.dp(this,20),app.pelican.check.Ui.dp(this,8));
        android.widget.EditText period=app.pelican.check.Ui.number(this,"10",String.valueOf(app.pelican.check.Prefs.get(this).getInt("defaultInterval",10)));labeled(form,"新线路检测间隔（分钟）",period);
        android.widget.EditText retry=Ui.number(this,"2",String.valueOf(Prefs.get(this).getInt("defaultRetries",2)));labeled(form,"本轮最多补试（0～20）",retry);
        android.widget.TextView retryCost=Ui.text(this,"每次补试都可能重复计费",12,Ui.ORANGE);Ui.add(form,retryCost,5);
        Runnable checkRetry=()->{try{int count=Integer.parseInt(retry.getText().toString().trim());retry.setError(count<0||count>20?"请输入 0～20":null);retryCost.setVisibility(count>5?0:8);}catch(Exception ex){retry.setError("请输入 0～20 的整数");retryCost.setVisibility(8);}};
        retry.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int after){}public void onTextChanged(CharSequence s,int a,int before,int count){checkRetry.run();}public void afterTextChanged(android.text.Editable e){}});checkRetry.run();
        android.widget.EditText tolerance=app.pelican.check.Ui.number(this,"800",String.valueOf(app.pelican.check.Prefs.get(this).getInt("defaultTokenExcess24",800)));labeled(form,"新线路容许多出的输入 Token",tolerance);
        android.widget.EditText notify=app.pelican.check.Ui.number(this,"2",String.valueOf(app.pelican.check.Prefs.get(this).getInt("notifyThreshold",2)));labeled(form,"连续异常达到几次通知（全部线路）",notify);
        app.pelican.check.Ui.add(form,app.pelican.check.Ui.text(this,"只有通知次数立即影响全部线路，其他设置仅用于新建线路。",11,app.pelican.check.Ui.MUTED),8);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(form);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("检测默认值").setView(scroll).setNegativeButton("取消",null).setNeutralButton("定时规则应用到已有线路",null).setPositiveButton("保存默认值",null).create();
        dialog.setOnShowListener(d->{
            android.view.View.OnClickListener commit=v->{try{
                int p=Integer.parseInt(period.getText().toString().trim()),t=Integer.parseInt(tolerance.getText().toString().trim()),n=Integer.parseInt(notify.getText().toString().trim()),r=Integer.parseInt(retry.getText().toString().trim());
                if(p<1||p>1440||t<0||t>10000000||n<1||n>100||r<0||r>20)throw new IllegalArgumentException();
                if(v==dialog.getButton(-3)){
                    new android.app.AlertDialog.Builder(this).setTitle("修改已有线路的定时规则？").setMessage("将现有线路改为每 "+p+" 分钟检测、最多补试 "+r+" 次。不会立即运行，不改变提示词、密钥、Token 差额和其他设置。").setNegativeButton("取消",null).setPositiveButton("应用",(x,w)->{for(app.pelican.check.LineConfig line:store.getLines()){line.intervalMin=p;line.retryCount=r;if(line.enabled)line.nextRunAt=System.currentTimeMillis()+p*60000L;store.saveLine(line);}startService(new android.content.Intent(this,app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RELOAD));app.pelican.check.Ui.toast(this,"已有线路的定时规则已更新");dialog.dismiss();}).show();return;
                }
                app.pelican.check.Prefs.get(this).edit().putInt("defaultInterval",p).putInt("defaultRetries",r).putInt("defaultTokenExcess24",t).putInt("notifyThreshold",n).apply();dialog.dismiss();app.pelican.check.Ui.toast(this,"默认值已保存");
            }catch(Exception error){app.pelican.check.Ui.toast(this,"请检查数字：间隔 1～1440，差额 0～1000 万，通知 1～100");}};
            dialog.getButton(-1).setOnClickListener(commit);dialog.getButton(-3).setOnClickListener(commit);
        });dialog.show();
    }

    private android.widget.LinearLayout settingsCard(android.widget.LinearLayout linearLayout, java.lang.String str, java.lang.String str2) {
        android.widget.LinearLayout linearLayoutCard = app.pelican.check.Ui.card(this);
        linearLayoutCard.addView(app.pelican.check.Ui.heading(this, str, 17.0f));
        app.pelican.check.Ui.add(linearLayoutCard, app.pelican.check.Ui.text(this, str2, 12.0f, app.pelican.check.Ui.MUTED), 4);
        app.pelican.check.Ui.add(linearLayout, linearLayoutCard, 12);
        return linearLayoutCard;
    }

    private void labeled(android.widget.LinearLayout linearLayout, java.lang.String str, android.widget.EditText editText) {
        app.pelican.check.Ui.add(linearLayout, app.pelican.check.Ui.text(this, str, 12.0f, app.pelican.check.Ui.MUTED), 10);
        app.pelican.check.Ui.add(linearLayout, editText, 5);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int parseInt(java.lang.String str, int i) {
        try {
            return java.lang.Integer.parseInt(str.trim());
        } catch (java.lang.Throwable th) {
            return i;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void beginExport() {
        java.lang.String str = "pelican-backup-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.getDefault()).format(new java.util.Date()) + ".zip";
        android.content.Intent intent = new android.content.Intent("android.intent.action.CREATE_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("application/zip");
        intent.putExtra("android.intent.extra.TITLE", str);
        startActivityForResult(intent, REQ_EXPORT);
    }

    private boolean canImport() {
        if (app.pelican.check.LiveRuns.any() || this.store.hasActiveRuns()) {
            app.pelican.check.Ui.toast(this, "请等待当前检测结束再导入备份");
            return false;
        }
        java.util.Iterator<app.pelican.check.LineConfig> it = this.store.getLines().iterator();
        while (it.hasNext()) {
            if (it.next().enabled) {
                app.pelican.check.Ui.toast(this, "请先暂停全部定时线路再导入备份");
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void beginImport() {
        if (canImport()) {
            android.content.Intent intent = new android.content.Intent("android.intent.action.OPEN_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("*/*");
            startActivityForResult(intent, REQ_IMPORT);
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, final android.content.Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 10 && i2 == -1) {
            refreshCurrent();
            return;
        }
        if (i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        if (i == REQ_EXPORT) {
            app.pelican.check.Ui.toast(this, "正在导出…");
            new java.lang.Thread(new java.lang.Runnable() { // from class: app.pelican.check.MainActivity.30
                @Override // java.lang.Runnable
                public void run() {
                    java.lang.String resultMessage;
                    try {
                        java.io.OutputStream outputStreamOpenOutputStream = app.pelican.check.MainActivity.this.getContentResolver().openOutputStream(intent.getData());
                        app.pelican.check.BackupUtil.export(app.pelican.check.MainActivity.this, outputStreamOpenOutputStream);
                        outputStreamOpenOutputStream.close();
                        resultMessage = "备份导出完成";
                    } catch (java.lang.Throwable th) {
                        resultMessage = "导出失败：" + th.getMessage();
                    }
                    final java.lang.String str = resultMessage;
                    app.pelican.check.MainActivity.this.runOnUiThread(new java.lang.Runnable() { // from class: app.pelican.check.MainActivity.30.1
                        @Override // java.lang.Runnable
                        public void run() {
                            app.pelican.check.Ui.toast(app.pelican.check.MainActivity.this, str);
                        }
                    });
                }
            }).start();
        } else if (i == REQ_IMPORT && canImport()) {
            app.pelican.check.Ui.toast(this, "正在导入…");
            new java.lang.Thread(new java.lang.Runnable() { // from class: app.pelican.check.MainActivity.31
                @Override // java.lang.Runnable
                public void run() {
                    java.lang.String resultMessage;
                    try {
                        java.io.InputStream inputStreamOpenInputStream = app.pelican.check.MainActivity.this.getContentResolver().openInputStream(intent.getData());
                        app.pelican.check.BackupUtil.Result resultImportBackup = app.pelican.check.BackupUtil.importBackup(app.pelican.check.MainActivity.this, inputStreamOpenInputStream);
                        inputStreamOpenInputStream.close();
                        resultMessage = resultImportBackup.message;
                    } catch (java.lang.Throwable th) {
                        resultMessage = "导入失败：" + th.getMessage();
                    }
                    final java.lang.String str = resultMessage;
                    app.pelican.check.MainActivity.this.runOnUiThread(new java.lang.Runnable() { // from class: app.pelican.check.MainActivity.31.1
                        @Override // java.lang.Runnable
                        public void run() {
                            app.pelican.check.Ui.toast(app.pelican.check.MainActivity.this, str);
                            app.pelican.check.MainActivity.this.showSettings();
                        }
                    });
                }
            }).start();
        }
    }

    private android.widget.LinearLayout.LayoutParams topMargin(int i) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = app.pelican.check.Ui.dp(this, i);
        return layoutParams;
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.fatal) {
            return;
        }
        android.content.IntentFilter intentFilter = new android.content.IntentFilter(app.pelican.check.MonitorService.ACTION_UPDATE);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(this.updateReceiver, intentFilter, 4);
        } else {
            registerReceiver(this.updateReceiver, intentFilter);
        }
        this.handler.removeCallbacks(this.countdown);
        this.handler.post(this.countdown);
        if (this.tab == 0) {
            showMonitor();
        } else {
            buildNav();
            refreshLive();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.handler.removeCallbacks(this.countdown);
        try {
            unregisterReceiver(this.updateReceiver);
        } catch (java.lang.Throwable th) {
        }
        super.onPause();
    }

    private String compactTokens(long value) {
        return value >= 10000 ? String.format(java.util.Locale.CHINA,"%.1fk",value/1000.0) : String.valueOf(value);
    }
    private void selectLoaded(boolean inverse) {
        selectionRequest++;
        for(RunData run:history)if(!run.isActive()){
            if(inverse&&selected.contains(run.id))selected.remove(run.id);else selected.add(run.id);
        }
        updateDeleteButton();
    }
    private void selectFiltered() {
        final long request=++selectionRequest;
        final Store.RunFilter snapshot=new Store.RunFilter();
        snapshot.lineId=filter.lineId;snapshot.model=filter.model;snapshot.status=filter.status;snapshot.special=filter.special;
        snapshot.completedOnly=true;
        if(selectionHint!=null)selectionHint.setText("正在选择当前筛选下全部已完成记录…");
        new Thread(() -> {
            try {
                final java.util.List<String> ids=store.getSelectableRunIds(snapshot);
                runOnUiThread(() -> {
                    if(isFinishing()||isDestroyed()||!manageMode||tab!=1||request!=selectionRequest)return;
                    selected.addAll(ids);updateDeleteButton();
                    app.pelican.check.Ui.toast(this,"已选择筛选结果共 "+ids.size()+" 条（含未加载记录）");
                });
            }catch(Exception e){runOnUiThread(() -> {if(request==selectionRequest){updateDeleteButton();app.pelican.check.Ui.toast(this,"选择失败，请重试");}});}
        },"pelican-select").start();
    }
}
