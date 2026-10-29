import app.pelican.check.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import android.app.Application;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.sql.*;
import java.util.*;
import java.lang.reflect.*;
import com.google.gson.*;
import static org.junit.Assert.*;

/** Real app Store + real SQLite SQL through test-only JDBC platform bridge, not device-native SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28,application=Application.class,shadows={SqlStoreRuntimeTest.Helper.class,SqlStoreRuntimeTest.Database.class})
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class SqlStoreRuntimeTest {
 static final List<Connection> connections=new ArrayList<>();
 @Implements(SQLiteOpenHelper.class)
 public static class Helper {
  @RealObject SQLiteOpenHelper real;
  Context context;String name;int version;SQLiteDatabase db;
  @Implementation protected void __constructor__(Context ctx,String n,SQLiteDatabase.CursorFactory factory,int v){context=ctx;name=n;version=v;}
  @Implementation public synchronized SQLiteDatabase getWritableDatabase(){
   if(db!=null)return db;
   try{java.io.File file=context.getDatabasePath(name);file.getParentFile().mkdirs();Connection c=DriverManager.getConnection("jdbc:sqlite:"+file.getPath());connections.add(c);db=Shadow.newInstanceOf(SQLiteDatabase.class);Database shadow=Shadow.extract(db);shadow.c=c;int old=0;try(Statement st=c.createStatement();ResultSet rows=st.executeQuery("PRAGMA user_version")){if(rows.next())old=rows.getInt(1);}
    if(old==0)real.onCreate(db);else if(old<version)real.onUpgrade(db,old,version);try(Statement st=c.createStatement()){st.execute("PRAGMA user_version="+version);}return db;
   }catch(Exception e){throw new RuntimeException(e);}
  }
  @Implementation public synchronized SQLiteDatabase getReadableDatabase(){return getWritableDatabase();}
  @Implementation public synchronized void close(){if(db!=null){((Database)Shadow.extract(db)).close();db=null;}}
 }
 @Implements(SQLiteDatabase.class)
 public static class Database {
  Connection c;boolean successful;
  private void bind(PreparedStatement p,Object[] args)throws Exception{if(args!=null)for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);}
  @Implementation public void execSQL(String sql){execSQL(sql,null);}
  @Implementation public void execSQL(String sql,Object[] args){try(PreparedStatement p=c.prepareStatement(sql)){bind(p,args);p.execute();}catch(Exception e){throw new RuntimeException(e);}}
  @Implementation public Cursor rawQuery(String sql,String[] args){
   try(PreparedStatement p=c.prepareStatement(sql)){bind(p,args);try(ResultSet rows=p.executeQuery()){ResultSetMetaData m=rows.getMetaData();String[] cols=new String[m.getColumnCount()];for(int i=0;i<cols.length;i++)cols[i]=m.getColumnLabel(i+1);MatrixCursor out=new MatrixCursor(cols);while(rows.next()){Object[] v=new Object[cols.length];for(int i=0;i<v.length;i++)v[i]=rows.getObject(i+1);out.addRow(v);}return out;}}
   catch(Exception e){throw new RuntimeException(e);}
  }
  @Implementation public Cursor query(String table,String[] cols,String selection,String[] args,String group,String having,String order,String limit){return rawQuery("SELECT "+(cols==null?"*":String.join(",",cols))+" FROM "+table+(selection==null||selection.isEmpty()?"":" WHERE "+selection)+(group==null?"":" GROUP BY "+group)+(having==null?"":" HAVING "+having)+(order==null?"":" ORDER BY "+order)+(limit==null?"":" LIMIT "+limit),args);}
  @Implementation public Cursor query(String table,String[] cols,String selection,String[] args,String group,String having,String order){return query(table,cols,selection,args,group,having,order,null);}
  @Implementation public long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){List<String> keys=new ArrayList<>(values.keySet());String[] marks=new String[keys.size()];Arrays.fill(marks,"?");String sql="INSERT "+(conflict==5?"OR REPLACE ":conflict==4?"OR IGNORE ":"")+"INTO "+table+"("+String.join(",",keys)+") VALUES("+String.join(",",marks)+")";Object[] args=new Object[keys.size()];for(int i=0;i<args.length;i++)args[i]=values.get(keys.get(i));execSQL(sql,args);return 1;}
  @Implementation public int delete(String table,String where,String[] args){try(PreparedStatement p=c.prepareStatement("DELETE FROM "+table+(where==null?"":" WHERE "+where))){bind(p,args);return p.executeUpdate();}catch(Exception e){throw new RuntimeException(e);}}
  @Implementation public void beginTransaction(){try{c.setAutoCommit(false);successful=false;}catch(Exception e){throw new RuntimeException(e);}}
  @Implementation public void setTransactionSuccessful(){successful=true;}
  @Implementation public void endTransaction(){try{if(successful)c.commit();else c.rollback();c.setAutoCommit(true);}catch(Exception e){throw new RuntimeException(e);}}
  @Implementation public boolean isOpen(){try{return !c.isClosed();}catch(Exception e){return false;}}
  @Implementation public void close(){try{c.close();}catch(Exception e){throw new RuntimeException(e);}}
 }
 @Before public void reset()throws Exception{Field f=Store.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);RuntimeEnvironment.getApplication().deleteDatabase("pelican.db");}
 @After public void close()throws Exception{for(Connection c:connections)c.close();connections.clear();}
 void waitFor(Store store)throws Exception{long end=System.currentTimeMillis()+20000;while(store.migrating24&&System.currentTimeMillis()<end)Thread.sleep(10);assertFalse("Migration timed out",store.migrating24);}
 RunData oldRun(String lineId,String id,boolean enough){RunData r=new RunData();r.id=id;r.lineId=lineId;r.lineName="原线路";r.model="model";r.promptKey="pelican_en";r.promptLabel="英文题";r.startedAt=100;r.endedAt=200;r.status="PASS";r.lowerUsed=65d;r.inputAuditVersion=2;RunData.SampleData s=new RunData.SampleData();s.index=1;s.requestOk=true;s.svgOk=true;s.score=90;s.modelEcho="model";s.inTokens=26688;s.outTokens=20;if(enough){s.inputAudit=new InputAudit.Evidence();s.inputAudit.contentVerified=true;s.inputAudit.wireBytesVerified=true;s.inputAudit.estimatedInputTokens=26;s.inputAudit.bodyBytes=300;}r.samples.add(s);return r;}
 @Test public void migrate23DatabaseRetainsAllRecordsBaselinesSchedulesAndOriginalEvidence()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());SQLiteDatabase db=store.getWritableDatabase();String oldJson="{\"id\":\"old-line\",\"name\":\"原线路\",\"model\":\"model\",\"apiKey\":\"isolated-fixture-secret\",\"promptMode\":\"rotate\",\"promptId\":\"pelican_zh\",\"enabled\":true,\"nextRunAt\":123456789,\"intervalMin\":20,\"sampleCount\":3,\"retryCount\":2,\"inputTokenExcess\":800}";
  db.execSQL("INSERT INTO lines(id,json,enabled,nextRunAt,updatedAt) VALUES(?,?,?,?,?)",new Object[]{"old-line",oldJson,1,123456789,1});
  Baseline baseline=new Baseline();baseline.lineId="old-line";baseline.model="model";baseline.promptKey="pelican_zh";baseline.key=Baseline.makeKey(baseline.lineId,baseline.model,baseline.promptKey);baseline.lower=87d;baseline.values=Arrays.asList(90d,91d,92d,93d,94d);store.saveBaseline(baseline);String preserved=Json.gson().toJson(store.getBaseline("old-line","model","pelican_zh"));
  RunData enough=oldRun("old-line","enough",true),missing=oldRun("old-line","missing",false);store.insertRun(enough);store.insertRun(missing);store.migrate24Async();waitFor(store);
  RunData revised=store.getRun("enough");assertEquals("ERROR",revised.status);assertTrue(revised.inputTokenAlert);assertEquals("PASS",revised.originalStatus);assertEquals(26688,revised.samples.get(0).inTokens);assertTrue(revised.originalRunJson.contains("26688"));assertEquals("按 2.4 规则重新判定",revised.reassessmentNote);assertTrue(store.getRun("missing").reassessmentNote.startsWith("无法重判"));
  LineConfig line=store.getLine("old-line");assertEquals(123456789,line.nextRunAt);assertEquals(20,line.intervalMin);assertEquals(3,line.sampleCount);assertEquals(2,line.retryCount);assertEquals("isolated-fixture-secret",line.apiKey);assertEquals(1,line.promptSequence.size());assertEquals("pelican_zh",PromptSequence.current(line).id);assertEquals(preserved,Json.gson().toJson(store.getBaseline("old-line","model","pelican_zh")));
  try(Cursor cursor=db.rawQuery("SELECT json FROM migration24_original_runs WHERE id='enough'",null)){assertTrue(cursor.moveToFirst());assertEquals("PASS",JsonParser.parseString(cursor.getString(0)).getAsJsonObject().get("status").getAsString());}
  Store.RunFilter f=new Store.RunFilter();f.special="token_alert";assertEquals(1,store.getRuns(f,20,0).size());assertEquals("enough",store.getRuns(f,20,0).get(0).id);
  String original=revised.originalRunJson;store.migrate24Async();waitFor(store);assertEquals(original,store.getRun("enough").originalRunJson);
  store.deleteRuns(Arrays.asList("enough"));assertNull(store.getRun("enough"));try(Cursor cursor=db.rawQuery("SELECT count(*) FROM migration24_original_runs WHERE id='enough'",null)){assertTrue(cursor.moveToFirst());assertEquals(0,cursor.getInt(0));}
  System.out.println("Real SQLite/JDBC: 2.3 upgrade retained schedule/key/baseline/history, locally rejudged with traceable originals, filters updated, deletion removed originals.");
 }
 @Test public void missingLineUses800AndPerLineRatioRemainsIndependent()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());LineConfig line=LineConfig.createNew();line.id="ratio-line";line.inputTokenMode="ratio";line.inputTokenRatio=5;line.inputTokenExcess=100000;store.saveLine(line);RunData byRatio=oldRun(line.id,"ratio",true);byRatio.samples.get(0).inTokens=130;store.insertRun(byRatio);RunData deleted=oldRun("deleted","deleted",true);deleted.samples.get(0).inTokens=827;store.insertRun(deleted);store.migrate24Async();waitFor(store);assertTrue(store.getRun("ratio").inputTokenAlert);assertTrue(store.getRun("deleted").inputTokenAlert);assertEquals("ratio",store.getRun("ratio").inputTokenModeUsed);assertEquals(800,store.getRun("deleted").inputTokenExcessUsed);
 }

 @Test public void backupRoundTripRetains24SequenceSettingsAndOriginalHistoryWithoutApiKey()throws Exception{
  android.content.Context context=RuntimeEnvironment.getApplication();Store store=Store.get(context);LineConfig line=LineConfig.createNew();line.id="backup-line";line.name="备份线路";line.apiKey="isolated-backup-secret";line.model="model";line.sequenceIndex=2;line.sampleCount=10;line.retryCount=10;line.inputTokenMode="ratio";line.inputTokenRatio=6.5;line.complexityMultiplier=1.1;line.promptSequence.add(new PromptSequence.Entry("custom-backup","自定义","原样内容\n末尾 ",""));store.saveLine(line);
  RunData run=oldRun(line.id,"backup-run",true);TokenGuard.reassess(run,line);store.insertRun(run);
  java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();BackupUtil.export(context,bytes);byte[] archive=bytes.toByteArray();
  java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(archive));for(java.util.zip.ZipEntry e;(e=zip.getNextEntry())!=null;){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];for(int n;(n=zip.read(buffer))>0;)out.write(buffer,0,n);assertFalse(new String(out.toByteArray(),"UTF-8").contains("isolated-backup-secret"));}
  store.close();Field f=Store.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,null);context.deleteDatabase("pelican.db");Store clean=Store.get(context);
  BackupUtil.Result result=BackupUtil.importBackup(context,new java.io.ByteArrayInputStream(archive));waitFor(clean);
  assertEquals(1,result.lines);LineConfig loaded=clean.getLine(line.id);assertEquals("",loaded.apiKey);assertFalse(loaded.enabled);assertEquals(5,loaded.promptSequence.size());assertEquals(2,loaded.sequenceIndex);assertEquals(10,loaded.sampleCount);assertEquals(10,loaded.retryCount);assertEquals(6.5,loaded.inputTokenRatio,.00001);assertEquals(1.1,loaded.complexityMultiplier,.00001);assertEquals("原样内容\n末尾 ",loaded.promptSequence.get(4).text);assertEquals(run.originalRunJson,clean.getRun(run.id).originalRunJson);
 }
}
