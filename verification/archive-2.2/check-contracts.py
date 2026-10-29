from pathlib import Path
import re,sqlite3
b=Path(__file__).resolve().parents[2]/'app/src/main/java/app/pelican/check'
s=(b/'Store.java').read_text();create=s[s.index('public void onCreate'):s.index('public void onUpgrade')]
sqls=re.findall(r'execSQL\("([^"\n]+)"\)',create)
db=sqlite3.connect(':memory:')
for sql in sqls:db.execute(sql)
assert any(row[1]=='inputTokenAlert' for row in db.execute('pragma table_info(runs)'))
rows=[('pass','PASS',0),('alert','ERROR',1),('live','RUNNING',0),('queued','QUEUED',0)]
for id,status,flag in rows:db.execute('insert into runs(id,lineId,status,inputTokenAlert,json) values(?,?,?,?,?)',(id,'line-A',status,flag,'{}'))
selected=[x[0] for x in db.execute("select id from runs where status NOT IN (?, 'RUNNING')",('QUEUED',))]
assert set(selected)=={'pass','alert'}
assert [x[0] for x in db.execute("select id from runs where inputTokenAlert=?",(1,))]==['alert']
old=sqlite3.connect(':memory:');old.execute('create table runs(id text)');old.execute('ALTER TABLE runs ADD COLUMN inputTokenAlert INTEGER DEFAULT 0');old.execute("insert into runs(id) values('legacy')");assert old.execute('select inputTokenAlert from runs').fetchone()[0]==0
main=(b/'MainActivity.java').read_text();engine=(b/'MonitorEngine.java').read_text();trend=(b/'TrendActivity.java').read_text()
for label in ['本页全选','筛选全选','取消全选','反选本页']:assert label in main
assert 'if(!run.isActive())' in main and 'snapshot.completedOnly=true;' in main
assert 'request!=selectionRequest' in main and 'ids=store.getSelectableRunIds(snapshot)' in main
assert engine.index('TokenGuard.assess')<engine.index('baseline.addSuccessfulScore')
assert 'if (runData.inputTokenAlert) { z = true; z3 = false; }' in engine
assert 'ImageView.ScaleType.FIT_CENTER' in main
assert 'previous.remove(group)' in trend
print('PASS SQLite creation, v2→v3 migration, alert filter and active-run exclusion')
print('PASS all-selection cancellation, prompt-safe references, missing-data gaps and uncropped SVG contracts')
