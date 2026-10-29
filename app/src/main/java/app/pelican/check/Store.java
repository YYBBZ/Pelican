package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class Store extends android.database.sqlite.SQLiteOpenHelper {
    private static app.pelican.check.Store instance;

    /* JADX INFO: renamed from: app, reason: collision with root package name */
    private final android.content.Context f6app;

    public static class RunFilter {
        public boolean completedOnly;
        public java.lang.String lineId = "";
        public java.lang.String model = "";
        public java.lang.String status = "";
        public java.lang.String special = "";
    }

    public static synchronized app.pelican.check.Store get(android.content.Context context) {
        if (instance == null) {
            instance = new app.pelican.check.Store(context.getApplicationContext());
        }
        return instance;
    }

    private Store(android.content.Context context) {
        super(context, "pelican.db", (android.database.sqlite.SQLiteDatabase.CursorFactory) null, 3);
        this.f6app = context;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(android.database.sqlite.SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE lines(id TEXT PRIMARY KEY,json TEXT NOT NULL,enabled INTEGER DEFAULT 0,nextRunAt INTEGER DEFAULT 0,createdAt INTEGER DEFAULT 0,updatedAt INTEGER NOT NULL)");
        sQLiteDatabase.execSQL("CREATE TABLE runs(id TEXT PRIMARY KEY,lineId TEXT NOT NULL,lineName TEXT,model TEXT,promptKey TEXT,status TEXT,startedAt INTEGER,score REAL,retryFailed INTEGER,retrySuccess INTEGER,networkClass TEXT,json TEXT NOT NULL)");
        sQLiteDatabase.execSQL("ALTER TABLE runs ADD COLUMN inputTokenAlert INTEGER DEFAULT 0");
        sQLiteDatabase.execSQL("CREATE INDEX idx_runs_time ON runs(startedAt DESC)");
        sQLiteDatabase.execSQL("CREATE INDEX idx_runs_line ON runs(lineId,startedAt DESC)");
        sQLiteDatabase.execSQL("CREATE TABLE baselines(key TEXT PRIMARY KEY,lineId TEXT NOT NULL,model TEXT,promptKey TEXT,json TEXT NOT NULL,updatedAt INTEGER NOT NULL)");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(android.database.sqlite.SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 3) sQLiteDatabase.execSQL("ALTER TABLE runs ADD COLUMN inputTokenAlert INTEGER DEFAULT 0");
        if (i < 2) {
            try {
                sQLiteDatabase.execSQL("ALTER TABLE lines ADD COLUMN createdAt INTEGER DEFAULT 0");
            } catch (java.lang.Throwable th) {
            }
        }
    }

    public synchronized void saveLine(app.pelican.check.LineConfig lineConfig) {
        lineConfig.normalize();
        android.content.ContentValues contentValues = new android.content.ContentValues();
        contentValues.put("id", lineConfig.id);
        contentValues.put("json", app.pelican.check.Json.gson().toJson(lineConfig));
        contentValues.put("enabled", java.lang.Integer.valueOf(lineConfig.enabled ? 1 : 0));
        contentValues.put("nextRunAt", java.lang.Long.valueOf(lineConfig.nextRunAt));
        contentValues.put("createdAt", java.lang.Long.valueOf(lineConfig.createdAt));
        contentValues.put("updatedAt", java.lang.Long.valueOf(java.lang.System.currentTimeMillis()));
        getWritableDatabase().insertWithOnConflict("lines", null, contentValues, 5);
    }

    public synchronized java.util.List<app.pelican.check.LineConfig> getLines() {
        java.util.ArrayList arrayList;
        arrayList = new java.util.ArrayList();
        android.database.Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT json FROM lines ORDER BY createdAt ASC", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                arrayList.add(LineConfig.fromJson(cursorRawQuery.getString(0)));
            } catch (java.lang.Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        return arrayList;
    }

    public synchronized app.pelican.check.LineConfig getLine(java.lang.String str) {
        android.database.Cursor cursorQuery = getReadableDatabase().query("lines", new java.lang.String[]{"json"}, "id=?", new java.lang.String[]{str}, null, null, null, "1");
        try {
            if (cursorQuery.moveToFirst()) {
                app.pelican.check.LineConfig lineConfig = LineConfig.fromJson(cursorQuery.getString(0));
                cursorQuery.close();
                return lineConfig;
            }
            cursorQuery.close();
            return null;
        } catch (java.lang.Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    public synchronized void deleteLine(java.lang.String str, boolean z) {
        getWritableDatabase().delete("lines", "id=?", new java.lang.String[]{str});
        getWritableDatabase().delete("baselines", "lineId=?", new java.lang.String[]{str});
        if (z) {
            clearHistory(str);
        }
    }

    public synchronized void setAllEnabled(boolean z) {
        for (app.pelican.check.LineConfig lineConfig : getLines()) {
            lineConfig.enabled = z;
            lineConfig.nextRunAt = z ? java.lang.System.currentTimeMillis() + 3000 : 0L;
            saveLine(lineConfig);
        }
    }

    public synchronized void insertRun(app.pelican.check.RunData runData) {
        android.content.ContentValues contentValues = new android.content.ContentValues();
        contentValues.put("id", runData.id);
        contentValues.put(app.pelican.check.MonitorService.EXTRA_LINE_ID, runData.lineId);
        contentValues.put("lineName", runData.lineName);
        contentValues.put("model", runData.model);
        contentValues.put("promptKey", runData.promptKey);
        contentValues.put("status", runData.status);
        contentValues.put("startedAt", java.lang.Long.valueOf(runData.startedAt));
        contentValues.put("score", java.lang.Double.valueOf(runData.score));
        int i = 1;
        contentValues.put("retryFailed", java.lang.Integer.valueOf(runData.retryFailed ? 1 : 0));
        if (!runData.retrySuccess) {
            i = 0;
        }
        contentValues.put("retrySuccess", java.lang.Integer.valueOf(i));
        contentValues.put("networkClass", runData.networkClass);
        contentValues.put("inputTokenAlert", runData.inputTokenAlert ? 1 : 0);
        contentValues.put("json", app.pelican.check.Json.gson().toJson(runData));
        getWritableDatabase().insertWithOnConflict("runs", null, contentValues, 5);
    }

    public synchronized void recoverInterruptedRuns() {
        android.database.Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT json FROM runs WHERE status IN ('QUEUED','RUNNING')", null);
        java.util.ArrayList<app.pelican.check.RunData> arrayList = new java.util.ArrayList();
        while (cursorRawQuery.moveToNext()) {
            try {
                arrayList.add((app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(cursorRawQuery.getString(0), app.pelican.check.RunData.class));
            } catch (java.lang.Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        for (app.pelican.check.RunData runData : arrayList) {
            runData.status = "FAIL";
            runData.interrupted = true;
            runData.endedAt = java.lang.System.currentTimeMillis();
            runData.stage = "检测中断";
            runData.summary = "上次进程已结束，本轮未完成；没有自动重新请求";
            LineConfig recoveredLine=getLine(runData.lineId);
            if(recoveredLine!=null&&runData.promptSequenceRevision!=null&&!runData.promptSequenceRevision.isEmpty()){
                PromptSequence.finish(recoveredLine,runData);saveLine(recoveredLine);
            }
            insertRun(runData);
        }
    }

    public synchronized boolean hasActiveRuns() {
        boolean zMoveToFirst;
        android.database.Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT 1 FROM runs WHERE status IN ('QUEUED','RUNNING') LIMIT 1", null);
        try {
            zMoveToFirst = cursorRawQuery.moveToFirst();
            cursorRawQuery.close();
        } catch (java.lang.Throwable th) {
            cursorRawQuery.close();
            throw th;
        }
        return zMoveToFirst;
    }

    private static class QueryParts {
        java.util.List<java.lang.String> args;
        java.lang.String where;

        private QueryParts() {
            this.where = "";
            this.args = new java.util.ArrayList();
        }

        void add(java.lang.String str, java.lang.String str2) {
            if (this.where.length() > 0) {
                this.where += " AND ";
            }
            this.where += str;
            this.args.add(str2);
        }
    }

    private app.pelican.check.Store.QueryParts parts(app.pelican.check.Store.RunFilter runFilter) {
        app.pelican.check.Store.QueryParts queryParts = new app.pelican.check.Store.QueryParts();
        if (runFilter == null) {
            return queryParts;
        }
        if (runFilter.completedOnly) {
            queryParts.add("status NOT IN (?, 'RUNNING')", "QUEUED");
        }
        if (!runFilter.lineId.isEmpty()) {
            queryParts.add("lineId=?", runFilter.lineId);
        }
        if (!runFilter.model.isEmpty()) {
            queryParts.add("model=?", runFilter.model);
        }
        if (!runFilter.status.isEmpty()) {
            queryParts.add("status=?", runFilter.status);
        }
        if ("retry_success".equals(runFilter.special)) {
            queryParts.add("retrySuccess=?", "1");
        }
        if ("retry_failed".equals(runFilter.special)) {
            queryParts.add("retryFailed=?", "1");
        }
        if ("local".equals(runFilter.special)) {
            queryParts.add("networkClass=?", "本机网络中断");
        }
        if ("upstream".equals(runFilter.special)) {
            queryParts.add("networkClass LIKE ?", "%上游%");
        }
        if ("token_alert".equals(runFilter.special)) queryParts.add("inputTokenAlert=?", "1");
        return queryParts;
    }

    public synchronized java.util.List<app.pelican.check.RunData> getRuns(app.pelican.check.Store.RunFilter runFilter, int i, int i2) {
        java.util.ArrayList arrayList;
        app.pelican.check.Store.QueryParts queryPartsParts = parts(runFilter);
        arrayList = new java.util.ArrayList();
        android.database.Cursor cursorQuery = getReadableDatabase().query("runs", new java.lang.String[]{"json"}, queryPartsParts.where, (java.lang.String[]) queryPartsParts.args.toArray(new java.lang.String[0]), null, null, "startedAt DESC, id DESC", i2 + "," + java.lang.Math.max(1, i));
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add((app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(cursorQuery.getString(0), app.pelican.check.RunData.class));
            } catch (java.lang.Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public synchronized java.util.List<app.pelican.check.RunData> getRunsBefore(app.pelican.check.Store.RunFilter runFilter, int i, app.pelican.check.RunData runData) {
        java.util.ArrayList arrayList;
        app.pelican.check.Store.QueryParts queryPartsParts = parts(runFilter);
        if (runData != null) {
            if (!queryPartsParts.where.isEmpty()) {
                queryPartsParts.where += " AND ";
            }
            queryPartsParts.where += "(startedAt < ? OR (startedAt = ? AND id < ?))";
            queryPartsParts.args.add(java.lang.String.valueOf(runData.startedAt));
            queryPartsParts.args.add(java.lang.String.valueOf(runData.startedAt));
            queryPartsParts.args.add(runData.id);
        }
        android.database.Cursor cursorQuery = getReadableDatabase().query("runs", new java.lang.String[]{"json"}, queryPartsParts.where, (java.lang.String[]) queryPartsParts.args.toArray(new java.lang.String[0]), null, null, "startedAt DESC, id DESC", java.lang.String.valueOf(java.lang.Math.max(1, i)));
        arrayList = new java.util.ArrayList();
        while (cursorQuery.moveToNext()) {
            try {
                app.pelican.check.RunData runData2 = (app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(cursorQuery.getString(0), app.pelican.check.RunData.class);
                if (runData2 != null) {
                    arrayList.add(runData2);
                }
            } catch (java.lang.Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public synchronized java.util.List<app.pelican.check.RunData> getAllRuns() {
        return getRuns(new app.pelican.check.Store.RunFilter(), kotlin.time.DurationKt.NANOS_IN_MILLIS, 0);
    }

    public synchronized app.pelican.check.RunData getRun(java.lang.String str) {
        android.database.Cursor cursorQuery = getReadableDatabase().query("runs", new java.lang.String[]{"json"}, "id=?", new java.lang.String[]{str}, null, null, null, "1");
        try {
            if (cursorQuery.moveToFirst()) {
                app.pelican.check.RunData runData = (app.pelican.check.RunData) app.pelican.check.Json.gson().fromJson(cursorQuery.getString(0), app.pelican.check.RunData.class);
                cursorQuery.close();
                return runData;
            }
            cursorQuery.close();
            return null;
        } catch (java.lang.Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    public synchronized java.util.List<java.lang.String> getModels() {
        java.util.LinkedHashSet linkedHashSet;
        linkedHashSet = new java.util.LinkedHashSet();
        android.database.Cursor cursorRawQuery = getReadableDatabase().rawQuery("SELECT DISTINCT model FROM runs WHERE model IS NOT NULL AND model!='' ORDER BY model ASC", null);
        while (cursorRawQuery.moveToNext()) {
            try {
                linkedHashSet.add(cursorRawQuery.getString(0));
            } catch (java.lang.Throwable th) {
                cursorRawQuery.close();
                throw th;
            }
        }
        cursorRawQuery.close();
        return new java.util.ArrayList(linkedHashSet);
    }

    public synchronized void deleteRuns(java.util.List<java.lang.String> list) {
        for (java.lang.String str : list) {
            app.pelican.check.RunData run = getRun(str);
            if (run == null || !run.isActive()) {
                if (run != null) {
                    deleteFiles(run);
                }
                getWritableDatabase().delete("runs", "id=?", new java.lang.String[]{str});
                deleteMigrationOriginal(str);
            }
        }
    }

    public synchronized void clearHistory(java.lang.String str) {
        app.pelican.check.Store.RunFilter runFilter = new app.pelican.check.Store.RunFilter();
        runFilter.lineId = str == null ? "" : str;
        for (app.pelican.check.RunData runData : getRuns(runFilter, kotlin.time.DurationKt.NANOS_IN_MILLIS, 0)) {
            if (!runData.isActive()) {
                deleteFiles(runData);
                deleteMigrationOriginal(runData.id);
            }
        }
        if (str == null || str.isEmpty()) {
            getWritableDatabase().delete("runs", "status NOT IN ('QUEUED','RUNNING')", null);
        } else {
            getWritableDatabase().delete("runs", "lineId=? AND status NOT IN ('QUEUED','RUNNING')", new java.lang.String[]{str});
        }
    }

    private void deleteFiles(app.pelican.check.RunData runData) {
        if (runData.samples == null) {
            return;
        }
        for (app.pelican.check.RunData.SampleData sampleData : runData.samples) {
            try {
                if (sampleData.svgPath != null && !sampleData.svgPath.isEmpty()) {
                    new java.io.File(sampleData.svgPath).delete();
                }
            } catch (java.lang.Throwable th) {
            }
            try {
                if (sampleData.thumbPath != null && !sampleData.thumbPath.isEmpty()) {
                    new java.io.File(sampleData.thumbPath).delete();
                }
            } catch (java.lang.Throwable th2) {
            }
            for (app.pelican.check.RunData.AttemptData attemptData : sampleData.attempts) {
                try {
                    if (attemptData.outputPath != null && !attemptData.outputPath.isEmpty()) {
                        new java.io.File(attemptData.outputPath).delete();
                    }
                } catch (java.lang.Throwable th3) {
                }
            }
        }
    }

    public synchronized app.pelican.check.Baseline getBaseline(java.lang.String str, java.lang.String str2, java.lang.String str3) {
        java.lang.String strMakeKey = app.pelican.check.Baseline.makeKey(str, str2, str3);
        android.database.Cursor cursorQuery = getReadableDatabase().query("baselines", new java.lang.String[]{"json"}, "key=?", new java.lang.String[]{strMakeKey}, null, null, null, "1");
        try {
            if (cursorQuery.moveToFirst()) {
                app.pelican.check.Baseline baseline = (app.pelican.check.Baseline) app.pelican.check.Json.gson().fromJson(cursorQuery.getString(0), app.pelican.check.Baseline.class);
                cursorQuery.close();
                return baseline;
            }
            cursorQuery.close();
            app.pelican.check.Baseline baseline2 = new app.pelican.check.Baseline();
            baseline2.key = strMakeKey;
            baseline2.lineId = str;
            baseline2.model = str2;
            baseline2.promptKey = str3;
            return baseline2;
        } catch (java.lang.Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    public synchronized void saveBaseline(app.pelican.check.Baseline baseline) {
        baseline.updatedAt = java.lang.System.currentTimeMillis();
        android.content.ContentValues contentValues = new android.content.ContentValues();
        contentValues.put("key", baseline.key);
        contentValues.put(app.pelican.check.MonitorService.EXTRA_LINE_ID, baseline.lineId);
        contentValues.put("model", baseline.model);
        contentValues.put("promptKey", baseline.promptKey);
        contentValues.put("json", app.pelican.check.Json.gson().toJson(baseline));
        contentValues.put("updatedAt", java.lang.Long.valueOf(baseline.updatedAt));
        getWritableDatabase().insertWithOnConflict("baselines", null, contentValues, 5);
    }

    public synchronized void resetBaselines(java.lang.String str) {
        getWritableDatabase().delete("baselines", "lineId=?", new java.lang.String[]{str});
    }

    public synchronized java.util.List<app.pelican.check.Baseline> getBaselines() {
        java.util.ArrayList arrayList;
        arrayList = new java.util.ArrayList();
        android.database.Cursor cursorQuery = getReadableDatabase().query("baselines", new java.lang.String[]{"json"}, null, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            try {
                arrayList.add((app.pelican.check.Baseline) app.pelican.check.Json.gson().fromJson(cursorQuery.getString(0), app.pelican.check.Baseline.class));
            } catch (java.lang.Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public synchronized long historyBytes() {
        long length;
        length = 0;
        length = 0 + new java.io.File(this.f6app.getDatabasePath("pelican.db").getPath()).length();
        return length + dirSize(new java.io.File(this.f6app.getFilesDir(), "svgs")) + dirSize(new java.io.File(this.f6app.getFilesDir(), "thumbs")) + dirSize(new java.io.File(this.f6app.getFilesDir(), "responses"));
    }

    private long dirSize(java.io.File file) {
        java.io.File[] fileArrListFiles = file.listFiles();
        long jDirSize = 0;
        if (fileArrListFiles != null) {
            for (java.io.File file2 : fileArrListFiles) {
                jDirSize += file2.isDirectory() ? dirSize(file2) : file2.length();
            }
        }
        return jDirSize;
    }

    public synchronized java.util.List<String> getSelectableRunIds(RunFilter filter) {
        QueryParts query=parts(filter);
        java.util.List<String> ids=new java.util.ArrayList<>();
        android.database.Cursor cursor=getReadableDatabase().query("runs",new String[]{"id"},query.where,query.args.toArray(new String[0]),null,null,"startedAt DESC, id DESC");
        try{while(cursor.moveToNext())ids.add(cursor.getString(0));}finally{cursor.close();}
        return ids;
    }

    public volatile boolean migrating24;
    public volatile String migration24Note="";
    public void migrate24Async(){
        synchronized(this){if(migrating24)return;migrating24=true;}
        new Thread(()->{
            int changed=0,missing=0;
            try{
                // A SQLite-only snapshot retains exact original history without exposing API keys.
                synchronized(this){getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS migration24_original_runs(id TEXT PRIMARY KEY,json TEXT NOT NULL)");
                    for(LineConfig line:getLines())saveLine(line);}
                RunData before=null;RunFilter filter=new RunFilter();filter.completedOnly=true;
                while(true){
                    java.util.List<RunData> page=getRunsBefore(filter,50,before);if(page.isEmpty())break;
                    for(RunData run:page){
                        before=run;if(run.inputAuditVersion>=TokenGuard.VERSION)continue;
                        synchronized(this){
                            RunData current=getRun(run.id);if(current==null||current.isActive()||current.inputAuditVersion>=TokenGuard.VERSION)continue;
                            android.content.ContentValues raw=new android.content.ContentValues();raw.put("id",current.id);raw.put("json",Json.gson().toJson(current));
                            LineConfig line=getLine(current.lineId);if(line==null)line=LineConfig.createNew();
                            if(TokenGuard.reassess(current,line)){
                                android.database.sqlite.SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
                                try{
                                    db.insertWithOnConflict("migration24_original_runs",null,raw,4);insertRun(current);
                                    LineConfig actualLine=getLine(current.lineId);
                                    if(actualLine!=null&&actualLine.lastRunAt>0&&actualLine.lastRunAt==current.endedAt){
                                        actualLine.lastStatus=current.status;
                                        if(current.inputTokenAlert)actualLine.lastBadge="输入 Token 异常 / 疑似注入";
                                        else if(actualLine.lastBadge!=null&&actualLine.lastBadge.contains("Token"))actualLine.lastBadge="";
                                        saveLine(actualLine);
                                    }
                                    db.setTransactionSuccessful();
                                }finally{db.endTransaction();}
                                changed++;if(current.reassessmentNote.startsWith("无法重判"))missing++;
                            }
                        }
                    }
                    migration24Note="正在本地重判旧历史："+changed+" 条";
                    try{Thread.sleep(5);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
                }
                migration24Note="2.4 本地重判完成："+changed+" 条，其中 "+missing+" 条无法重判；未调用接口";
                Prefs.get(f6app).edit().putString("migration24Note",migration24Note).putBoolean("migration24Done",true).apply();
            }catch(Throwable e){migration24Note="旧历史重判暂未完成，原始记录保留；下次启动继续";CrashLog.save(f6app,e);}
            finally{migrating24=false;}
        },"pelican-24-migration").start();
    }
    private void deleteMigrationOriginal(String id){
        android.database.Cursor cursor=getReadableDatabase().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='migration24_original_runs'",null);
        boolean exists;try{exists=cursor.moveToFirst();}finally{cursor.close();}
        if(exists)getWritableDatabase().delete("migration24_original_runs","id=?",new String[]{id});
    }
}
