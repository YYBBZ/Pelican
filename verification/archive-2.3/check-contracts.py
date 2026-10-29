from pathlib import Path
import re,sqlite3,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[2];b=r/'app/src/main/java/app/pelican/check'
s=(b/'Store.java').read_text();create=s[s.index('public void onCreate'):s.index('public void onUpgrade')]
db=sqlite3.connect(':memory:')
for sql in re.findall(r'execSQL\("([^"\n]+)"\)',create):db.execute(sql)
assert any(row[1]=='inputTokenAlert' for row in db.execute('pragma table_info(runs)'))
for id,status,flag in [('pass','PASS',0),('alert','ERROR',1),('live','RUNNING',0),('queued','QUEUED',0)]:db.execute('insert into runs(id,lineId,status,inputTokenAlert,json) values(?,?,?,?,?)',(id,'line',status,flag,'{}'))
assert set(x[0] for x in db.execute("select id from runs where status NOT IN (?, 'RUNNING')",('QUEUED',)))=={'pass','alert'}
assert [x[0] for x in db.execute('select id from runs where inputTokenAlert=?',(1,))]==['alert']
main=(b/'MainActivity.java').read_text();engine=(b/'MonitorEngine.java').read_text();trend=(b/'TrendActivity.java').read_text();edit=(b/'LineEditActivity.java').read_text()
for name in ['本页全选','筛选全选','取消全选','反选本页']:assert name in main
assert 'if(!run.isActive())' in main and 'snapshot.completedOnly=true;' in main
assert 'request!=selectionRequest' in main and 'ids=store.getSelectableRunIds(snapshot)' in main
assert engine.index('TokenGuard.assess')<engine.index('baseline.addSuccessfulScore')
assert 'ImageView.ScaleType.FIT_CENTER' in main
assert 'new LineChart(this)' in trend and 'setScaleXEnabled(true)' in trend and 'setScaleYEnabled(false)' in trend
assert 'ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE' in trend
assert 'if(!Double.isFinite(value)){addSet' in trend and 'segment=new ArrayList<>()' in trend
assert 'timeAxis' in trend and 'TrendSeries.select' in trend
assert 'initialForm=formFingerprint()' in edit and 'setOnClickListener(v->leave())' in edit
assert 'inputReference' not in edit and 'inputMultiplier' not in edit
assert 'c.inputTokenExcess' in edit and 'snapshot(true)' in edit
assert '500' not in (b/'ApiClient.java').read_text().split('private com.google.gson.JsonObject payload(')[1].split('private static class GenResult')[0] # no hidden minimum cap
manifest=ET.parse(r/'app/src/main/AndroidManifest.xml').getroot();android='{http://schemas.android.com/apk/res/android}'
assert manifest.find('application').get(android+'roundIcon')=='@mipmap/ic_launcher'
assert (r/'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml').exists()
print('PASS SQLite schema and completed-only selection/alert filter contracts')
print('PASS preserved batch selection, fixed-SVG preview, strict editor and zoom/landscape graph contracts')
