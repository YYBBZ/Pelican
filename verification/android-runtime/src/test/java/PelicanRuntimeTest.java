import app.pelican.check.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowAlertDialog;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;

/** Android View/engine runtime tests. SQLite is explicitly replaced by an in-memory test repository on ARM. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28,application=Application.class,shadows={PelicanRuntimeTest.MemoryStore.class},instrumentedPackages={"app.pelican.check"})
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class PelicanRuntimeTest {
 @Implements(value=Store.class,isInAndroidSdk=false)
 public static class MemoryStore {
  static Store singleton;
  static final Map<String,LineConfig> lines=new LinkedHashMap<>();
  static final Map<String,RunData> runs=new LinkedHashMap<>();
  static final Map<String,Baseline> baselines=new LinkedHashMap<>();
  static <T>T clone(T value,Class<T> cls){return value==null?null:Json.gson().fromJson(Json.gson().toJson(value),cls);}
  @Implementation public static synchronized Store get(android.content.Context context){
   if(singleton==null)try{Constructor<Store> c=Store.class.getDeclaredConstructor(android.content.Context.class);c.setAccessible(true);singleton=c.newInstance(context);}catch(Exception e){throw new RuntimeException(e);}return singleton;
  }
  @Implementation public synchronized void saveLine(LineConfig line){line.normalize();lines.put(line.id,clone(line,LineConfig.class));}
  @Implementation public synchronized LineConfig getLine(String id){return clone(lines.get(id),LineConfig.class);}
  @Implementation public synchronized List<LineConfig> getLines(){List<LineConfig> out=new ArrayList<>();for(LineConfig c:lines.values())out.add(clone(c,LineConfig.class));return out;}
  @Implementation public synchronized void insertRun(RunData r){runs.put(r.id,clone(r,RunData.class));}
  @Implementation public synchronized RunData getRun(String id){return clone(runs.get(id),RunData.class);}
  @Implementation public synchronized List<RunData> getRuns(Store.RunFilter f,int limit,int offset){List<RunData> out=new ArrayList<>();for(RunData r:runs.values()){
   if(f!=null&&((f.completedOnly&&r.isActive())||(!f.lineId.isEmpty()&&!f.lineId.equals(r.lineId))||(!f.model.isEmpty()&&!f.model.equals(r.model))))continue;out.add(clone(r,RunData.class));}
   out.sort((a,b)->Long.compare(b.startedAt,a.startedAt));return new ArrayList<>(out.subList(Math.min(offset,out.size()),Math.min(out.size(),offset+limit)));
  }
  @Implementation public synchronized List<RunData> getRunsBefore(Store.RunFilter f,int limit,RunData before){List<RunData> list=getRuns(f,Integer.MAX_VALUE/2,0);if(before!=null)list.removeIf(r->r.startedAt>before.startedAt||(r.startedAt==before.startedAt&&r.id.compareTo(before.id)>=0));return new ArrayList<>(list.subList(0,Math.min(limit,list.size())));}
  @Implementation public synchronized List<String> getModels(){return new ArrayList<>();}
  @Implementation public synchronized Baseline getBaseline(String id,String model,String prompt){String key=Baseline.makeKey(id,model,prompt);Baseline b=baselines.get(key);if(b==null){b=new Baseline();b.key=key;b.lineId=id;b.model=model;b.promptKey=prompt;}return clone(b,Baseline.class);}
  @Implementation public synchronized void saveBaseline(Baseline b){baselines.put(b.key,clone(b,Baseline.class));}
  @Implementation public synchronized void resetBaselines(String id){baselines.entrySet().removeIf(e->id.equals(e.getValue().lineId));}
  @Implementation public synchronized boolean hasActiveRuns(){return false;}
  @Implementation public synchronized void recoverInterruptedRuns(){}
  @Implementation public synchronized long historyBytes(){return 0;}
 }
 static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 static Object call(Object target,String name,Class<?>[] types,Object... args)throws Exception{Method m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);try{return m.invoke(target,args);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}}
 @Before public void reset(){MemoryStore.singleton=null;MemoryStore.lines.clear();MemoryStore.runs.clear();MemoryStore.baselines.clear();}
 LineConfig line(){LineConfig c=LineConfig.createNew();c.name="fixture";c.baseUrl="https://fixture.invalid/v1";c.apiKey="test-fixture-key";c.model="fixture";c.maxTokens=512;c.retryCount=0;c.sampleCount=10;return c;}
 static String svg="<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"green\"/></svg>";
 ApiClient api(AtomicInteger count,int errorUntil,int permanent){return new ApiClient(null,new OkHttpClient.Builder().addInterceptor(chain->{int request=count.incrementAndGet();int status=permanent>0?permanent:request<=errorUntil?503:200;String body=status==200?"{\"model\":\"fixture\",\"choices\":[{\"message\":{\"content\":"+Json.gson().toJson(svg)+"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":26688,\"completion_tokens\":50}}":"{\"error\":\"temporarily overloaded\"}";return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("isolated fixture").header("Content-Type","application/json").body(ResponseBody.create(body,MediaType.get("application/json"))).build();}).build());}
 @Test public void tenSamplesActuallyExecuteWithOneFrozenPrompt()throws Exception{
  android.content.Context context=RuntimeEnvironment.getApplication();Store store=Store.get(context);LineConfig line=line();store.saveLine(line);AtomicInteger count=new AtomicInteger();
  MonitorEngine engine=new MonitorEngine(context);set(engine,"api",api(count,0,0));RunData queued=engine.prepare(line);String expected=queued.promptTextUsed;RunData done=engine.runPrepared(line,queued);
  assertEquals(10,count.get());assertEquals(10,done.samples.size());assertEquals(10,done.totalSamples);assertTrue(done.samples.stream().allMatch(s->s.requestOk&&s.svgOk));
  for(RunData.SampleData s:done.samples){assertNotNull(s.inputAudit);String prompt=Json.gson().fromJson(s.inputAudit.bodyJson,com.google.gson.JsonObject.class).getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString();assertEquals(expected,prompt);}
  assertEquals("ERROR",done.status);assertTrue(done.inputTokenAlert);assertEquals("PASS",done.qualityStatus);assertEquals(0,store.getLine(line.id).consecutiveIssues);assertEquals(1,store.getLine(line.id).sequenceIndex);
  assertTrue(done.sequenceAdvanced);System.out.println("10 actual samples executed, identical round prompt, token-only ERROR with zero quality notifications and one sequence advance.");
 }
 @Test public void allFailuresKeepPromptAndDoNotModifyBaselines()throws Exception{
  android.content.Context context=RuntimeEnvironment.getApplication();Store store=Store.get(context);LineConfig line=line();line.sampleCount=3;store.saveLine(line);AtomicInteger count=new AtomicInteger();MonitorEngine engine=new MonitorEngine(context);set(engine,"api",api(count,0,401));RunData done=engine.runLine(line);
  assertEquals(3,count.get());assertEquals(3,done.samples.size());assertEquals(0,store.getLine(line.id).sequenceIndex);assertTrue(store.getLine(line.id).sequenceBlocked);assertTrue(done.sequenceNote.contains("未前移"));assertEquals("FAIL",done.status);
 }
 @Test public void tenRetriesMakeElevenAttemptsAndParameterErrorsDoNotRetry()throws Exception{
  LineConfig line=line();line.retryCount=10;AtomicInteger count=new AtomicInteger();RunData.SampleData result=api(count,10,0).generateSample(line,"fixture",1);
  assertTrue(result.requestOk);assertEquals(11,count.get());assertEquals(11,result.attempts.size());
  AtomicInteger bad=new AtomicInteger();ApiClient client=new ApiClient(null,new OkHttpClient.Builder().addInterceptor(chain->{bad.incrementAndGet();return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(503).message("fixture").body(ResponseBody.create("{\"error\":\"unsupported reasoning_effort xhigh\"}",MediaType.get("application/json"))).build();}).build());
  result=client.generateSample(line,"fixture",1);assertFalse(result.requestOk);assertEquals(1,bad.get());assertEquals(1,result.attempts.size());
 }
 @Test public void editorSevenLevelsCustomCountsBudgetValidationAndNoSaveOnInvalid()throws Exception{
  ActivityController<LineEditActivity> ctl=Robolectric.buildActivity(LineEditActivity.class).setup();LineEditActivity activity=ctl.get();
  Spinner effort=(Spinner)field(activity,"effort");assertEquals(7,effort.getCount());assertTrue(effort.getItemAtPosition(5).toString().contains("xhigh"));assertTrue(effort.getItemAtPosition(6).toString().contains("max"));
  ((EditText)field(activity,"name")).setText("UI test");((EditText)field(activity,"base")).setText("https://fixture.invalid/v1");((EditText)field(activity,"key")).setText("fixture-key");((EditText)field(activity,"model")).setText("fixture");
  ((EditText)field(activity,"samples")).setText("10");((EditText)field(activity,"retries")).setText("10");
  assertEquals(View.VISIBLE,((TextView)field(activity,"retryNote")).getVisibility());
  LineConfig snapshot=(LineConfig)call(activity,"snapshot",new Class[]{boolean.class},true);assertEquals(10,snapshot.sampleCount);assertEquals(10,snapshot.retryCount);assertEquals(4,snapshot.promptSequence.size());
  ((EditText)field(activity,"samples")).setText("100");assertNotNull(((EditText)field(activity,"samples")).getError());boolean rejected=false;try{call(activity,"snapshot",new Class[]{boolean.class},true);}catch(Exception e){rejected=true;}assertTrue(rejected);
  ((EditText)field(activity,"samples")).setText("10");((Spinner)field(activity,"protocol")).setSelection(1);effort.setSelection(5);((EditText)field(activity,"maxTokens")).setText("32000");
  rejected=false;try{call(activity,"snapshot",new Class[]{boolean.class},true);}catch(Exception e){rejected=true;}assertTrue(rejected);
  ((EditText)field(activity,"maxTokens")).setText("32001");snapshot=(LineConfig)call(activity,"snapshot",new Class[]{boolean.class},true);assertEquals(32001,snapshot.maxTokens);assertEquals("xhigh",snapshot.effort);
  ctl.pause().stop().destroy();
 }
 static List<TextView> texts(View root){List<TextView> result=new ArrayList<>();if(root instanceof TextView)result.add((TextView)root);if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)result.addAll(texts(g.getChildAt(i)));}return result;}
 @Test public void sequenceEditorUpDownAndBlueBadgeMatchStatusGeometry()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();LineConfig line=line();SequenceEditor editor=new SequenceEditor(a,line,()->{});a.setContentView(editor);
  List<TextView> all=texts(editor);TextView down=all.stream().filter(t->t.getText().toString().equals("↓ 下移")&&t.isEnabled()).findFirst().get();down.performClick();assertEquals("pelican_zh",editor.first().id);assertTrue(editor.edited());editor.apply(line);assertEquals(0,line.sequenceIndex);assertEquals("pelican_zh",PromptSequence.current(line).id);
  TextView injected=Ui.badge(a,"注入",0xFF2E63A3,0xFFEAF1FB),status=Ui.statusBadge(a,"ERROR");assertEquals(status.getTextSize(),injected.getTextSize(),0);assertEquals(status.getPaddingTop(),injected.getPaddingTop());assertEquals(status.getPaddingBottom(),injected.getPaddingBottom());
 }
 @Test public void historyButtonsSeparateAndBadgeLeftOfStatus()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());LineConfig line=line();store.saveLine(line);RunData r=new RunData();r.lineId=line.id;r.lineName="fixture";r.model=line.model;r.promptKey="pelican_en";r.promptLabel="英文题";r.status="ERROR";r.inputTokenAlert=true;r.inputAuditVersion=4;r.startedAt=100;r.endedAt=200;store.insertRun(r);
  ActivityController<MainActivity> ctl=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=ctl.get();set(activity,"tab",1);call(activity,"showHistory",new Class[]{});
  List<TextView> all=texts(activity.getWindow().getDecorView());TextView trend=all.stream().filter(t->t.getText().toString().equals("趋势")).findFirst().get(),manage=all.stream().filter(t->t.getText().toString().equals("批量管理")).findFirst().get();
  assertSame(trend.getParent(),manage.getParent());ViewGroup row=(ViewGroup)trend.getParent();assertEquals(2,row.getChildCount());LinearLayout.LayoutParams tp=(LinearLayout.LayoutParams)trend.getLayoutParams(),mp=(LinearLayout.LayoutParams)manage.getLayoutParams();assertEquals(tp.weight,mp.weight,0);assertTrue(tp.rightMargin+mp.leftMargin>0);
  TextView injection=all.stream().filter(t->t.getText().toString().equals("注入")&&t.getVisibility()==View.VISIBLE).findFirst().get();ViewGroup badges=(ViewGroup)injection.getParent();assertTrue(badges.indexOfChild(injection)<badges.getChildCount()-1);
  ctl.pause().stop().destroy();
 }

 @Test public void dragDropAndCustomTopicCanBeAddedEditedRemoved()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();LineConfig line=line();SequenceEditor editor=new SequenceEditor(a,line,()->{});a.setContentView(editor);
  View first=editor.getChildAt(0),second=editor.getChildAt(1);
  Method obtain=android.view.DragEvent.class.getDeclaredMethod("obtain",int.class,float.class,float.class,Object.class,android.content.ClipDescription.class,android.content.ClipData.class,Class.forName("com.android.internal.view.IDragAndDropPermissions"),boolean.class);obtain.setAccessible(true);
  android.view.DragEvent started=(android.view.DragEvent)obtain.invoke(null,android.view.DragEvent.ACTION_DRAG_STARTED,10f,10f,Integer.valueOf(0),null,null,null,false);
  android.view.DragEvent drop=(android.view.DragEvent)obtain.invoke(null,android.view.DragEvent.ACTION_DROP,10f,10f,Integer.valueOf(0),null,null,null,false);
  assertTrue(second.dispatchDragEvent(started));assertTrue(second.dispatchDragEvent(drop));assertEquals("pelican_zh",editor.first().id);
  texts(editor).stream().filter(t->t.getText().toString().contains("添加题目")).findFirst().get().performClick();
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog chooser=ShadowAlertDialog.getLatestAlertDialog();chooser.getListView().performItemClick(chooser.getListView().getChildAt(0),0,0);
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();AlertDialog input=ShadowAlertDialog.getLatestAlertDialog();List<TextView> fields=texts(input.getWindow().getDecorView());List<EditText> edits=new ArrayList<>();for(TextView t:fields)if(t instanceof EditText)edits.add((EditText)t);assertEquals(2,edits.size());edits.get(0).setText("自定义轮换题");edits.get(1).setText("保留原样 \n末尾  ");input.getButton(-1).performClick();
  editor.apply(line);assertEquals(5,line.promptSequence.size());assertEquals("自定义轮换题",line.promptSequence.get(4).label);assertEquals("保留原样 \n末尾  ",line.promptSequence.get(4).text);
  LineConfig one=LineConfig.createNew();one.promptSequence=new ArrayList<>(Arrays.asList(one.promptSequence.get(0)));SequenceEditor single=new SequenceEditor(a,one,()->{});assertFalse(texts(single).stream().filter(t->t.getText().toString().equals("删除")).findFirst().get().isEnabled());
 }
 @Test public void monitorEditorAndRunUseSameEffectiveThreshold()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());LineConfig line=line();line.sampleCount=1;line.complexityMultiplier=.9;store.saveLine(line);
  Baseline b=store.getBaseline(line.id,line.model,PromptSequence.current(line).id);b.lower=87d;store.saveBaseline(b);
  ActivityController<MainActivity> mainCtl=Robolectric.buildActivity(MainActivity.class).setup();MainActivity main=mainCtl.get();assertTrue(texts(main.getWindow().getDecorView()).stream().anyMatch(t->t.getText().toString().contains("87 × 0.9 = 78.3")));
  Intent intent=new Intent(RuntimeEnvironment.getApplication(),LineEditActivity.class).putExtra(MonitorService.EXTRA_LINE_ID,line.id);ActivityController<LineEditActivity> editCtl=Robolectric.buildActivity(LineEditActivity.class,intent).setup();assertTrue(((TextView)field(editCtl.get(),"baselineStatus")).getText().toString().contains("87 × 0.9 = 78.3"));
  MonitorEngine engine=new MonitorEngine(RuntimeEnvironment.getApplication());set(engine,"api",api(new AtomicInteger(),0,0));RunData run=engine.runLine(line);assertEquals(78.3,run.lowerUsed,.00001);assertEquals("生效下限 87 × 0.9 = 78.3",run.lowerEquation);assertEquals("SUSPECT",run.qualityStatus);
  mainCtl.pause().stop().destroy();editCtl.pause().stop().destroy();
 }
 @Test public void requestSuccessWithoutValidSvgStillAdvancesAndSettingsDoNotUndoLiveProgress()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());LineConfig line=line();line.sampleCount=1;store.saveLine(line);MonitorEngine engine=new MonitorEngine(RuntimeEnvironment.getApplication());
  ApiClient plain=new ApiClient(null,new OkHttpClient.Builder().addInterceptor(chain->new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture").header("Content-Type","application/json").body(ResponseBody.create("{\"choices\":[{\"message\":{\"content\":\"not svg\"},\"finish_reason\":\"stop\"}]}",MediaType.get("application/json"))).build()).build());
  set(engine,"api",plain);RunData r=engine.runLine(line);assertTrue(r.samples.get(0).requestOk);assertFalse(r.samples.get(0).svgOk);assertTrue(r.sequenceAdvanced);assertEquals(1,store.getLine(line.id).sequenceIndex);
 }

 @Test public void tokenOnlyNeverNotifiesButRealQualityIssueStillNotifies()throws Exception{
  RuntimeEnvironment.getApplication().getSharedPreferences("pelican_settings",0).edit().putInt("notifyThreshold",2).commit();
  org.robolectric.android.controller.ServiceController<MonitorService> controller=Robolectric.buildService(MonitorService.class).create();MonitorService service=controller.get();
  android.app.NotificationManager manager=(android.app.NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);org.robolectric.shadows.ShadowNotificationManager notifications=org.robolectric.Shadows.shadowOf(manager);
  int foregroundCount=notifications.size();LineConfig before=line(),after=line();before.consecutiveIssues=1;after.id=before.id;after.consecutiveIssues=2;after.notifyEnabled=true;RunData r=new RunData();r.status="ERROR";r.inputTokenAlert=true;r.qualityStatus="PASS";r.summary="输入异常";
  call(service,"maybeQualityAlert",new Class[]{LineConfig.class,LineConfig.class,RunData.class},before,after,r);assertEquals(foregroundCount,notifications.size());
  r.qualityStatus="SUSPECT";r.qualitySummary="至少一张低于复杂度下限 70";
  call(service,"maybeQualityAlert",new Class[]{LineConfig.class,LineConfig.class,RunData.class},before,after,r);assertEquals(foregroundCount+1,notifications.size());
  android.app.Notification notification=notifications.getNotification(after.id.hashCode());assertTrue(notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("复杂度下限"));assertFalse(notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("注入"));controller.destroy();
 }
 @Test public void detailShowsAllFiveInputDecisionLinesWithoutOldExemption()throws Exception{
  Store store=Store.get(RuntimeEnvironment.getApplication());LineConfig c=line();RunData r=new RunData();r.lineId=c.id;r.lineName="详情测试";r.model=c.model;r.promptKey="pelican_en";r.promptLabel="英文题";r.startedAt=100;r.endedAt=200;r.status="ERROR";
  RunData.SampleData sample=new RunData.SampleData();sample.index=1;sample.requestOk=true;sample.inTokens=26688;sample.inputAudit=new InputAudit.Evidence();sample.inputAudit.contentVerified=true;sample.inputAudit.estimatedInputTokens=26;sample.inputAudit.bodyBytes=300;r.samples.add(sample);TokenGuard.assess(r,c);store.insertRun(r);
  Intent intent=new Intent(RuntimeEnvironment.getApplication(),RunDetailActivity.class).putExtra(MonitorService.EXTRA_RUN_ID,r.id);ActivityController<RunDetailActivity> ctl=Robolectric.buildActivity(RunDetailActivity.class,intent).setup();
  List<TextView> all=texts(ctl.get().getWindow().getDecorView());String joined=all.stream().map(t->t.getText().toString()).collect(java.util.stream.Collectors.joining("\n"));
  for(String expected:new String[]{"本地估算数量：26 Token","接口回报数量：26688 Token","两者差额：26662 Token","判定方式及容许值：按数量","最终判定：异常"})assertTrue(expected,joined.contains(expected));assertFalse(joined.contains("不据此告警"));ctl.pause().stop().destroy();
 }
}
