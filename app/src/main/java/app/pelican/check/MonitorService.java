package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class MonitorService extends android.app.Service {
    public static final java.lang.String ACTION_RELOAD = "app.pelican.check.RELOAD";
    public static final java.lang.String ACTION_RUN_NOW = "app.pelican.check.RUN_NOW";
    public static final java.lang.String ACTION_STOP = "app.pelican.check.STOP";
    public static final java.lang.String ACTION_UPDATE = "app.pelican.check.UPDATE";
    private static final java.lang.String CHANNEL_ALERT = "alerts";
    private static final java.lang.String CHANNEL_MONITOR = "monitor";
    public static final java.lang.String EXTRA_LINE_ID = "lineId";
    public static final java.lang.String EXTRA_RUN_ID = "runId";
    private app.pelican.check.MonitorEngine engine;
    private java.util.concurrent.ExecutorService pool;
    private volatile boolean stopping;
    private app.pelican.check.Store store;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.Map<java.lang.String, app.pelican.check.RunData> running = new java.util.LinkedHashMap();
    private final java.lang.Runnable ticker = new java.lang.Runnable() { // from class: app.pelican.check.MonitorService.1
        @Override // java.lang.Runnable
        public void run() {
            app.pelican.check.MonitorService.this.tick();
            app.pelican.check.MonitorService.this.handler.postDelayed(this, 15000L);
        }
    };

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.store = app.pelican.check.Store.get(this);
        this.engine = new app.pelican.check.MonitorEngine(this);
        this.pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        createChannels();
        startMonitorForeground();
        this.handler.post(this.ticker);
    }

    @Override // android.app.Service
    public int onStartCommand(android.content.Intent intent, int i, int i2) {
        java.lang.String action = intent == null ? ACTION_RELOAD : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return 2;
        }
        startMonitorForeground();
        if (ACTION_RUN_NOW.equals(action)) {
            java.lang.String stringExtra = intent.getStringExtra(EXTRA_LINE_ID);
            if (stringExtra != null) {
                submit(stringExtra, true);
            }
        } else {
            reloadSchedules();
        }
        return 2;
    }

    private void reloadSchedules() {
        java.util.List<app.pelican.check.LineConfig> lines = this.store.getLines();
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        for (app.pelican.check.LineConfig lineConfig : lines) {
            if (lineConfig.enabled && lineConfig.nextRunAt <= 0) {
                lineConfig.nextRunAt = 3000 + jCurrentTimeMillis;
                this.store.saveLine(lineConfig);
            } else if (!lineConfig.enabled && lineConfig.nextRunAt != 0) {
                lineConfig.nextRunAt = 0L;
                this.store.saveLine(lineConfig);
            }
        }
        sendUpdate();
        maybeStopWhenIdle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tick() {
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        for (app.pelican.check.LineConfig lineConfig : this.store.getLines()) {
            if (lineConfig.enabled && lineConfig.nextRunAt > 0 && lineConfig.nextRunAt <= jCurrentTimeMillis) {
                submit(lineConfig.id, false);
            }
        }
    }

    private void submit(final java.lang.String str, boolean z) {
        final app.pelican.check.LineConfig line;
        if (this.stopping || (line = this.store.getLine(str)) == null) {
            return;
        }
        synchronized (this.running) {
            if (this.running.containsKey(str)) {
                return;
            }
            final app.pelican.check.RunData runDataPrepare = this.engine.prepare(line);
            this.running.put(str, runDataPrepare);
            sendUpdate();
            this.pool.execute(new java.lang.Runnable() { // from class: app.pelican.check.MonitorService.2
                @Override // java.lang.Runnable
                public void run() {
                    final app.pelican.check.RunData runDataRunPrepared = app.pelican.check.MonitorService.this.engine.runPrepared(line, runDataPrepare);
                    app.pelican.check.MonitorService.this.handler.post(new java.lang.Runnable() { // from class: app.pelican.check.MonitorService.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            synchronized (app.pelican.check.MonitorService.this.running) {
                                app.pelican.check.MonitorService.this.running.remove(str);
                            }
                            if (app.pelican.check.MonitorService.this.stopping) {
                                return;
                            }
                            app.pelican.check.LineConfig line2 = app.pelican.check.MonitorService.this.store.getLine(str);
                            if (line2 != null) {
                                line2.nextRunAt = line2.enabled ? java.lang.System.currentTimeMillis() + (((long) java.lang.Math.max(1, line2.intervalMin)) * 60000) : 0L;
                                app.pelican.check.MonitorService.this.store.saveLine(line2);
                                app.pelican.check.MonitorService.this.maybeQualityAlert(line, line2, runDataRunPrepared);
                            }
                            app.pelican.check.MonitorService.this.sendUpdate();
                            app.pelican.check.MonitorService.this.maybeStopWhenIdle();
                        }
                    });
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeQualityAlert(app.pelican.check.LineConfig lineConfig, app.pelican.check.LineConfig lineConfig2, app.pelican.check.RunData runData) {
        if (runData == null || lineConfig2 == null || !lineConfig2.notifyEnabled) {
            return;
        }
        if (QualityPolicy.isQualityIssue(runData)) {
            int i = app.pelican.check.Prefs.get(this).getInt("notifyThreshold", 2);
            int i2 = lineConfig == null ? 0 : lineConfig.consecutiveIssues;
            if (lineConfig2.consecutiveIssues >= i && i2 < i) {
                ((android.app.NotificationManager) getSystemService("notification")).notify(lineConfig2.id.hashCode(), new android.app.Notification.Builder(this, CHANNEL_ALERT).setSmallIcon(app.pelican.check.R.drawable.ic_notification).setContentTitle(lineConfig2.displayName() + " 连续异常").setContentText(runData.qualitySummary.isEmpty()?"检测质量连续异常，请打开历史查看":runData.qualitySummary).setStyle(new android.app.Notification.BigTextStyle().bigText(runData.qualitySummary.isEmpty()?"检测质量连续异常，请打开历史查看":runData.qualitySummary)).setContentIntent(mainIntent()).setAutoCancel(true).build());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeStopWhenIdle() {
        synchronized (this.running) {
            if (this.running.isEmpty()) {
                java.util.Iterator<app.pelican.check.LineConfig> it = this.store.getLines().iterator();
                while (it.hasNext()) {
                    if (it.next().enabled) {
                        return;
                    }
                }
                stopSelf();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUpdate() {
        if (this.stopping) {
            return;
        }
        android.content.Intent intent = new android.content.Intent(ACTION_UPDATE);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
        startMonitorForeground();
    }

    private void startMonitorForeground() {
        int size;
        if (this.stopping) {
            return;
        }
        java.util.Iterator<app.pelican.check.LineConfig> it = this.store.getLines().iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().enabled) {
                i++;
            }
        }
        synchronized (this.running) {
            size = this.running.size();
        }
        java.lang.String str = i == 0 ? "等待手动检测" : "监控 " + i + " 条线路";
        if (size > 0) {
            str = str + " · 正在检测 " + size;
        }
        android.app.Notification notificationBuild = new android.app.Notification.Builder(this, CHANNEL_MONITOR).setSmallIcon(app.pelican.check.R.drawable.ic_notification).setContentTitle("Pelican 运行中").setContentText(str).setContentIntent(mainIntent()).setOngoing(true).build();
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(okhttp3.internal.ws.WebSocketProtocol.CLOSE_CLIENT_GOING_AWAY, notificationBuild, 1);
        } else {
            startForeground(okhttp3.internal.ws.WebSocketProtocol.CLOSE_CLIENT_GOING_AWAY, notificationBuild);
        }
    }

    private android.app.PendingIntent mainIntent() {
        return android.app.PendingIntent.getActivity(this, 0, new android.content.Intent(this, (java.lang.Class<?>) app.pelican.check.MainActivity.class), 201326592);
    }

    private void createChannels() {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService("notification");
        android.app.NotificationChannel notificationChannel = new android.app.NotificationChannel(CHANNEL_MONITOR, "监控运行状态", 2);
        notificationChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.createNotificationChannel(new android.app.NotificationChannel(CHANNEL_ALERT, "连续异常提醒", 3));
    }

    @Override // android.app.Service
    public void onTaskRemoved(android.content.Intent intent) {
        stopSelf();
        super.onTaskRemoved(intent);
    }

    @Override // android.app.Service
    public void onDestroy() {
        this.stopping = true;
        this.handler.removeCallbacksAndMessages(null);
        if (this.engine != null) {
            this.engine.cancelAll();
        }
        if (this.pool != null) {
            this.pool.shutdownNow();
        }
        synchronized (this.running) {
            for (app.pelican.check.RunData runData : this.running.values()) {
                app.pelican.check.RunData run = this.store.getRun(runData.id);
                if (run != null && run.isActive()) {
                    run.status = "FAIL";
                    run.interrupted = true;
                    run.endedAt = java.lang.System.currentTimeMillis();
                    run.stage = "检测中断";
                    run.summary = "监控服务已停止，本轮没有完成";
                    this.store.insertRun(run);
                }
                app.pelican.check.LiveRuns.remove(runData.id);
            }
            this.running.clear();
        }
        sendBroadcast(new android.content.Intent(ACTION_UPDATE).setPackage(getPackageName()));
        stopForeground(true);
        super.onDestroy();
    }

    @Override // android.app.Service
    public android.os.IBinder onBind(android.content.Intent intent) {
        return null;
    }
}
