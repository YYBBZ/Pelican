from pathlib import Path
b=Path('/workspace/Pelican/app/src/main/java/app/pelican/check')
def patch(name,old,new):
 p=b/(name+'.java');s=p.read_text();assert s.count(old)==1,(name,s.count(old),old[:65]);p.write_text(s.replace(old,new))
def replace_method(name,signature,new):
 p=b/(name+'.java');s=p.read_text();a=s.index(signature);z=s.index('{',a);depth=1;i=z+1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 p.write_text(s[:a]+new+s[i:])
p=b/'Ui.java';s=p.read_text();idx=s.rfind('}');s=s[:idx]+'''
    public static android.widget.Spinner choice(final android.content.Context c, java.util.List<String> values) {
        android.widget.Spinner spinner = new android.widget.Spinner(c, android.widget.Spinner.MODE_DROPDOWN);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, values) {
            @Override public android.view.View getView(int position, android.view.View convert, android.view.ViewGroup parent) {
                android.widget.LinearLayout row = Ui.row(c);
                row.setPadding(dp(c, 12), 0, dp(c, 12), 0); row.setMinimumHeight(dp(c, 46));
                android.widget.TextView label = Ui.text(c, getItem(position), 14, TEXT);
                label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(label, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
                android.widget.TextView arrow = Ui.text(c, "⌄", 17, GREEN); arrow.setPadding(dp(c, 8),0,0,0);row.addView(arrow);
                return row;
            }
            @Override public android.view.View getDropDownView(int position, android.view.View convert, android.view.ViewGroup parent) {
                android.widget.TextView text = Ui.text(c, getItem(position), 14, TEXT);
                text.setPadding(dp(c, 16),dp(c, 12),dp(c, 16),dp(c, 12));
                text.setMinHeight(dp(c, 50)); text.setMaxLines(3); text.setGravity(android.view.Gravity.CENTER_VERTICAL);
                text.setBackgroundColor(CARD); return text;
            }
        };
        spinner.setAdapter(adapter);spinner.setMinimumHeight(dp(c, 46));
        spinner.setBackground(Ui.bordered(c, CARD, BORDER, 10));
        return spinner;
    }

    public static android.widget.Spinner choiceField(android.widget.LinearLayout parent, String label, java.util.List<String> values) {
        android.content.Context c = parent.getContext();
        Ui.add(parent, Ui.text(c, label, 11, MUTED), 8);
        android.widget.Spinner spinner=choice(c,values);spinner.setContentDescription(label);
        Ui.add(parent,spinner,4);return spinner;
    }
''' +s[idx:];p.write_text(s)
patch('MainActivity','    private boolean manageMode;', '''    private boolean manageMode;
    private android.widget.TextView selectionHint;
    private boolean syncingSelection;
    private long selectionRequest;''')
patch('MainActivity','        android.widget.TextView retry;\n        app.pelican.check.RunData run;', '''        android.widget.TextView retry;
        android.widget.TextView tokenAlert;
        android.widget.TextView usage;
        app.pelican.check.RunData run;''')
patch('MainActivity','app.pelican.check.Ui.heading(this, "鹈鹕监控", 21.0f)', 'app.pelican.check.Ui.heading(this, "Pelican", 21.0f)')
patch('MainActivity','        this.lineFilter = spinner(this.filterBody, "线路", arrayList);','''        android.widget.LinearLayout filterTop = app.pelican.check.Ui.row(this);
        android.widget.LinearLayout lineColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout modelColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout.LayoutParams halfA = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
        halfA.rightMargin = app.pelican.check.Ui.dp(this, 5);
        android.widget.LinearLayout.LayoutParams halfB = new android.widget.LinearLayout.LayoutParams(0, -2, 1);
        halfB.leftMargin = app.pelican.check.Ui.dp(this, 5);
        filterTop.addView(lineColumn, halfA);filterTop.addView(modelColumn,halfB);this.filterBody.addView(filterTop);
        this.lineFilter = spinner(lineColumn, "线路", arrayList);''')
patch('MainActivity','        this.modelFilter = spinner(this.filterBody, "模型", arrayList2);','        this.modelFilter = spinner(modelColumn, "模型", arrayList2);')
patch('MainActivity','        this.statusFilter = spinner(this.filterBody, "状态", java.util.Arrays.asList("全部状态", "通过", "疑似降智", "异常", "检测失败", "进行中", "排队中"));\n        this.specialFilter = spinner(this.filterBody, "连接标记", java.util.Arrays.asList("全部记录", "重试成功", "重试失败", "本机网络中断", "上游异常"));','''        android.widget.LinearLayout filterBottom = app.pelican.check.Ui.row(this);
        android.widget.LinearLayout statusColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout specialColumn = app.pelican.check.Ui.column(this);
        android.widget.LinearLayout.LayoutParams halfC = new android.widget.LinearLayout.LayoutParams(0, -2, 1);halfC.rightMargin=app.pelican.check.Ui.dp(this,5);
        android.widget.LinearLayout.LayoutParams halfD = new android.widget.LinearLayout.LayoutParams(0, -2, 1);halfD.leftMargin=app.pelican.check.Ui.dp(this,5);
        filterBottom.addView(statusColumn,halfC);filterBottom.addView(specialColumn,halfD);this.filterBody.addView(filterBottom);
        this.statusFilter = spinner(statusColumn, "检测状态", java.util.Arrays.asList("全部状态", "通过", "疑似降智", "异常", "检测失败", "进行中", "排队中"));
        this.specialFilter = spinner(specialColumn, "连接标记", java.util.Arrays.asList("全部记录", "重试成功", "重试失败", "本机网络中断", "上游异常", "输入 Token 异常"));''')
replace_method('MainActivity','    private android.widget.Spinner spinner(','''    private android.widget.Spinner spinner(android.widget.LinearLayout parent, String label, java.util.List<String> values) {
        return app.pelican.check.Ui.choiceField(parent,label,values);
    }''')
patch('MainActivity','new java.lang.String[]{"retry_success", "retry_failed", "local", "upstream"}[selectedItemPosition2 - 1]', 'new java.lang.String[]{"retry_success", "retry_failed", "local", "upstream", "token_alert"}[selectedItemPosition2 - 1]')
patch('MainActivity','        this.selected.clear();\n        reloadHistoryCards();','        this.selectionRequest++;\n        this.selected.clear();\n        reloadHistoryCards();')
patch('MainActivity','                app.pelican.check.MainActivity.this.manageMode = !app.pelican.check.MainActivity.this.manageMode;', '                app.pelican.check.MainActivity.this.selectionRequest++;\n                app.pelican.check.MainActivity.this.manageMode = !app.pelican.check.MainActivity.this.manageMode;')
patch('MainActivity','        this.deleteSelectedButton = app.pelican.check.Ui.button(this, "删除所选", false);', '''        if (this.manageMode) {
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
        this.deleteSelectedButton = app.pelican.check.Ui.button(this, "删除所选", false);''')
patch('MainActivity','        historyCard.retry = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.ORANGE);', '''        historyCard.tokenAlert = app.pelican.check.Ui.text(this, "输入 Token 异常", 11, app.pelican.check.Ui.RED);
        historyCard.tokenAlert.setPadding(app.pelican.check.Ui.dp(this,6),app.pelican.check.Ui.dp(this,5),app.pelican.check.Ui.dp(this,6),app.pelican.check.Ui.dp(this,5));
        historyCard.tokenAlert.setBackground(app.pelican.check.Ui.round(this,app.pelican.check.Ui.PALE_RED,7));
        app.pelican.check.Ui.add(linearLayoutCard,historyCard.tokenAlert,6);
        historyCard.retry = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.ORANGE);''')
patch('MainActivity','        historyCard.progress = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.BLUEGRAY);','''        historyCard.usage = app.pelican.check.Ui.text(this, "", 10, app.pelican.check.Ui.MUTED);
        app.pelican.check.Ui.add(linearLayoutCard,historyCard.usage,4);
        historyCard.progress = app.pelican.check.Ui.text(this, "", 11.0f, app.pelican.check.Ui.BLUEGRAY);''')
patch('MainActivity','        historyCard.status.setText(app.pelican.check.Ui.statusText(runData.status));','''        historyCard.status.setText(app.pelican.check.Ui.statusText(runData.status));
        historyCard.tokenAlert.setVisibility(runData.inputTokenAlert ? 0 : 8);
        long in=0,out=0;for(app.pelican.check.RunData.SampleData sample:runData.samples){in+=sample.inTokens;out+=sample.outTokens;}
        historyCard.usage.setVisibility(in>0||out>0?0:8);
        historyCard.usage.setText("输入 " + compactTokens(in) + " · 输出 " + compactTokens(out));''')
replace_method('MainActivity','    public void updateDeleteButton()', '''    public void updateDeleteButton() {
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
    }''')
p=b/'MainActivity.java';s=p.read_text();idx=s.rfind('}');s=s[:idx]+'''
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
''' +s[idx:];p.write_text(s)
patch('MainActivity','        updateHistoryHint();\n    }\n\n    private void updateHistoryHint()', '        updateHistoryHint();\n        updateDeleteButton();\n    }\n\n    private void updateHistoryHint()')
# Refresh counts after pagination, without changing stable keyset pagination.
patch('MainActivity','        if (runsBefore.size() == 30) {', '        updateDeleteButton();\n        if (runsBefore.size() == 30) {')
# Index and filter token alerts without scanning JSON for every history refresh.
patch('Store','"pelican.db", (android.database.sqlite.SQLiteDatabase.CursorFactory) null, 2)', '"pelican.db", (android.database.sqlite.SQLiteDatabase.CursorFactory) null, 3)')
patch('Store','        sQLiteDatabase.execSQL("CREATE INDEX idx_runs_time ON runs(startedAt DESC)");', '        sQLiteDatabase.execSQL("ALTER TABLE runs ADD COLUMN inputTokenAlert INTEGER DEFAULT 0");\n        sQLiteDatabase.execSQL("CREATE INDEX idx_runs_time ON runs(startedAt DESC)");')
patch('Store','    public void onUpgrade(android.database.sqlite.SQLiteDatabase sQLiteDatabase, int i, int i2) {','''    public void onUpgrade(android.database.sqlite.SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 3) sQLiteDatabase.execSQL("ALTER TABLE runs ADD COLUMN inputTokenAlert INTEGER DEFAULT 0");''')
# Locate insertRun's unique networkClass content-value before writing.
p=b/'Store.java';s=p.read_text();old='contentValues.put("networkClass", runData.networkClass);';assert s.count(old)==1;s=s.replace(old,old+'\n        contentValues.put("inputTokenAlert", runData.inputTokenAlert ? 1 : 0);');p.write_text(s)
patch('Store','        return queryParts;\n    }\n\n    public synchronized java.util.List<app.pelican.check.RunData> getRuns(', '''        if ("token_alert".equals(runFilter.special)) queryParts.add("inputTokenAlert=?", "1");
        return queryParts;
    }

    public synchronized java.util.List<app.pelican.check.RunData> getRuns(''')
p=b/'Store.java';s=p.read_text();idx=s.rfind('}');s=s[:idx]+'''
    public synchronized java.util.List<String> getSelectableRunIds(RunFilter filter) {
        QueryParts query=parts(filter);
        java.util.List<String> ids=new java.util.ArrayList<>();
        android.database.Cursor cursor=getReadableDatabase().query("runs",new String[]{"id"},query.where,query.args.toArray(new String[0]),null,null,"startedAt DESC, id DESC");
        try{while(cursor.moveToNext())ids.add(cursor.getString(0));}finally{cursor.close();}
        return ids;
    }
''' +s[idx:];p.write_text(s)
print('Updated: compact two-column filters; consistent pickers; input/output usage and alert badges; async filtered all-select; selection safety; database migration v3.')
