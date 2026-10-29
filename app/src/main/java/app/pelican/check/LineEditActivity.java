package app.pelican.check;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

/** One coherent editor: connection, task, schedule, safety; advanced controls stay out of the way. */
public class LineEditActivity extends Activity {
    private Store store;private ApiClient api;private LineConfig config;private boolean isNew,oldEnabled,saved;private long oldInterval;private int fetchGeneration;
    private boolean sequenceEditedBeforeRecreate;
    private String initialForm="",cacheIdentity="";
    private EditText name,base,key,model,interval,maxTokens,temperature,excess,lower,samples,retries,inputRatio,complexity;
    private Spinner protocol,effort,inputMode;
    private SequenceEditor sequenceEditor;
    private TextView reasoningNote,retryNote;
    private Switch enabled,retryPartial,notifyEnabled,auditEnabled;
    private TextView connectionStatus,baselineStatus;
    private Button fetchButton,checkButton;
    private ProgressDialog previewDialog;
    private int dp(float n){return Ui.dp(this,n);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);Ui.systemBars(this);store=Store.get(this);api=new ApiClient(this);
        String id=getIntent().getStringExtra(MonitorService.EXTRA_LINE_ID);config=id==null?null:store.getLine(id);isNew=config==null;
        if(config==null){config=LineConfig.createNew();config.intervalMin=Prefs.get(this).getInt("defaultInterval",10);config.retryCount=Prefs.get(this).getInt("defaultRetries",2);config.inputTokenExcess=Prefs.get(this).getInt("defaultTokenExcess24",800);}
        oldEnabled=config.enabled;oldInterval=config.intervalMin;
        if(state!=null&&state.containsKey("form")){config=Json.gson().fromJson(state.getString("form"),LineConfig.class);isNew=state.getBoolean("new",isNew);sequenceEditedBeforeRecreate=state.getBoolean("sequenceEdited",false);}
        config.normalize();cacheIdentity=config.cachedModelsIdentity;build();initialForm=formFingerprint();
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::leave);
    }
    private void build(){
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);LinearLayout top=Ui.row(this);top.setPadding(dp(16),dp(8),dp(16),dp(8));top.setBackgroundColor(Color.WHITE);
        Button back=Ui.button(this,"‹",false);back.setContentDescription("返回，不丢弃未保存的修改");top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));back.setOnClickListener(v->leave());
        TextView title=Ui.heading(this,isNew?"新建线路":"编辑线路",20);title.setPadding(dp(12),0,0,0);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button save=Ui.button(this,"保存",true);top.addView(save);save.setOnClickListener(v->save());root.addView(top);
        ScrollView scroll=new ScrollView(this);LinearLayout body=Ui.column(this);body.setPadding(dp(16),dp(16),dp(16),dp(30));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        LinearLayout connect=section(body,"01 连接","先接通，再选择模型。密钥不进入备份。");
        name=field(connect,"线路名称","例如：主力线路",config.name,false);
        protocol=choice(connect,"接口协议",Arrays.asList("OpenAI Chat 兼容","Anthropic Messages","OpenAI Responses"),"anthropic".equals(config.protocol)?1:"responses".equals(config.protocol)?2:0);
        base=field(connect,"接口地址","https://example.com 或 https://example.com/v1",config.baseUrl,false);base.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);base.setAutofillHints((String[])null);
        LinearLayout keyRow=Ui.row(this);key=Ui.input(this,"粘贴 API 密钥",config.apiKey);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setAutofillHints((String[])null);key.setSaveEnabled(false);
        Ui.add(connect,Ui.text(this,"API 密钥",12,Ui.MUTED),10);keyRow.addView(key,new LinearLayout.LayoutParams(0,-2,1));Button reveal=Ui.button(this,"显示",false);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2);rp.leftMargin=dp(8);keyRow.addView(reveal,rp);Ui.add(connect,keyRow,5);
        reveal.setOnClickListener(v->{boolean hidden=key.getTransformationMethod()!=null;key.setTransformationMethod(hidden?null:android.text.method.PasswordTransformationMethod.getInstance());reveal.setText(hidden?"隐藏":"显示");key.setSelection(key.length());});
        model=field(connect,"模型 ID","支持手动输入，也可以获取后选择",config.model,false);
        LinearLayout actions=Ui.row(this);fetchButton=Ui.button(this,"获取并选择模型",true);checkButton=Ui.button(this,"检查连接",false);Ui.action(actions,fetchButton,1.3f);Ui.action(actions,checkButton,1);Ui.add(connect,actions,10);
        connectionStatus=Ui.text(this,"未发起连接检查",12,Ui.MUTED);connectionStatus.setTextIsSelectable(true);Ui.add(connect,connectionStatus,8);
        fetchButton.setOnClickListener(v->fetch(true));checkButton.setOnClickListener(v->fetch(false));
        if(!config.cachedModels.isEmpty()){Button cached=Ui.button(this,"查看已有缓存 · "+config.cachedModels.size()+" 个",false);Ui.add(connect,cached,6);cached.setOnClickListener(v->pickCached());}
        LinearLayout task=section(body,"02 检测任务","预设提示词原样发送；自定义提示词不追加、不裁剪。");
        sequenceEditor=new SequenceEditor(this,config,()->refreshBaseline());Ui.add(task,sequenceEditor,8);
        samples=field(task,"每轮样本（1～99 张）","1",String.valueOf(config.sampleCount),true);
        watchInteger(samples,1,99,null);
        Button preview=Ui.button(this,"预览下一轮实际请求 / 输入自检",false);Ui.add(task,preview,10);preview.setOnClickListener(v->preview());
        LinearLayout advanced=fold(task,"生成参数","思考强度、温度和输出上限",false);
        effort=choice(advanced,"思考强度",Arrays.asList(ReasoningPolicy.LABELS),Arrays.asList(ReasoningPolicy.VALUES).indexOf(config.effort));
        maxTokens=field(advanced,"每个样本最大输出 Token","16000",String.valueOf(config.maxTokens),true);
        temperature=field(advanced,"温度（Chat / Messages）","0.7",String.valueOf(config.temperature),true);
        reasoningNote=Ui.text(this,"",11,Ui.MUTED);Ui.add(advanced,reasoningNote,8);
        effort.setOnItemSelectedListener(selection(this::refreshReasoning));protocol.setOnItemSelectedListener(selection(this::refreshReasoning));
        maxTokens.addTextChangedListener(watcher(this::refreshReasoning));refreshReasoning();
        LinearLayout schedule=section(body,"03 定时与提醒","保存只安排后续任务；要立即运行，请回监控页点检测。");
        enabled=toggle(schedule,"启用定时监控",config.enabled);interval=field(schedule,"检测间隔（分钟）","10",String.valueOf(config.intervalMin),true);
        notifyEnabled=toggle(schedule,"连续异常时通知我",config.notifyEnabled);
        LinearLayout retry=fold(schedule,"故障重试","只补试临时故障，不反复请求错误密钥",false);
        retries=field(retry,"本轮最多补试（0～20 次）","2",String.valueOf(config.retryCount),true);
        retryNote=Ui.text(this,"每次补试都可能重复计费",12,Ui.ORANGE);Ui.add(retry,retryNote,6);
        watchInteger(retries,0,20,()->retryNote.setVisibility(integer(retries,0,false)>5?View.VISIBLE:View.GONE));
        retryPartial=toggle(retry,"断流后也补试（可能重复计费）",config.retryPartial);
        LinearLayout guard=section(body,"04 输入检查","先检查自己的请求，再核对接口用量。");
        auditEnabled=toggle(guard,"启用输入 Token 异常筛查",config.tokenAuditEnabled);
        inputMode=choice(guard,"判定方式（两种方式不同时生效）",Arrays.asList("按数量","按倍率"),"ratio".equals(config.inputTokenMode)?1:0);
        excess=field(guard,"容许多出的输入 Token","800",String.valueOf(config.inputTokenExcess),true);
        inputRatio=field(guard,"达到本地估算的多少倍判异常","5",String.valueOf(config.inputTokenRatio),true);
        inputMode.setOnItemSelectedListener(selection(()->{boolean ratio=inputMode.getSelectedItemPosition()==1;inputRatio.setEnabled(ratio);excess.setEnabled(!ratio);}));
        inputRatio.setEnabled("ratio".equals(config.inputTokenMode));excess.setEnabled(!"ratio".equals(config.inputTokenMode));
        Ui.add(guard,Ui.text(this,"本地估算直接作为筛查基准。估算不可用或为 0 时，用“接口输入 Token ＞ 请求正文字节数”补充筛查。只在卡片和详情提示，不弹窗、不发送系统通知。",12,Ui.MUTED),8);
        Ui.add(guard,Ui.text(this,"蓝色“注入”表示命中输入异常规则，疑似注入，不能据此确认；规则不是精确注入鉴定。",11,Ui.MUTED),6);
        LinearLayout baseline=section(body,"05 复杂度判断","与 Token 检查分开，保留原来的质量基线。");
        baselineStatus=Ui.text(this,"",12,Ui.MUTED);baseline.addView(baselineStatus);
        complexity=field(baseline,"自动复杂度下限倍率（0.5～1.5）","0.9",String.valueOf(config.complexityMultiplier),true);
        LinearLayout manual=fold(baseline,"高级质量设置","手动固定下限优先，不再乘倍率；历史基线保留",false);lower=field(manual,"手动复杂度下限（0～100）","留空使用自动下限 × 倍率",config.baselineLowerOverride==null?"":String.valueOf(config.baselineLowerOverride),true);
        complexity.addTextChangedListener(watcher(this::refreshBaseline));lower.addTextChangedListener(watcher(this::refreshBaseline));model.addTextChangedListener(watcher(this::refreshBaseline));refreshBaseline();
        Button reset=Ui.button(this,"重新收集质量基线",false);Ui.add(manual,reset,10);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("重新收集质量基线？").setMessage("清除本线路的自动复杂度基线，历史记录和 SVG 保留。").setNegativeButton("取消",null).setPositiveButton("重建",(d,w)->{store.resetBaselines(config.id);baselineStatus.setText("已重置：接下来 5 次成功检测重新收集");}).show());
        if(!isNew){Button delete=Ui.button(this,"删除线路…",false);delete.setTextColor(Ui.RED);Ui.add(body,delete,18);delete.setOnClickListener(v->confirmDelete());}
    }
    private LinearLayout section(LinearLayout parent,String title,String subtitle){LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,title,16));Ui.add(card,Ui.text(this,subtitle,12,Ui.MUTED),5);Ui.add(parent,card,parent.getChildCount()==0?0:14);return card;}
    private LinearLayout fold(LinearLayout parent,String title,String summary,boolean expanded){LinearLayout box=Ui.column(this);Button toggle=Ui.button(this,title+"  ▾",false);Ui.add(parent,toggle,12);Ui.add(parent,box,4);box.setVisibility(expanded?View.VISIBLE:View.GONE);toggle.setOnClickListener(v->{boolean show=box.getVisibility()!=View.VISIBLE;box.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(title+(show?"  ▴":"  ▾"));});Ui.add(box,Ui.text(this,summary,11,Ui.MUTED),4);return box;}
    private EditText field(LinearLayout parent,String label,String hint,String value,boolean number){Ui.add(parent,Ui.text(this,label,12,Ui.MUTED),10);EditText edit=number?Ui.number(this,hint,value):Ui.input(this,hint,value);edit.setContentDescription(label);Ui.add(parent,edit,5);return edit;}
    private Spinner choice(LinearLayout parent,String label,List<String> choices,int selected){Spinner s=Ui.choiceField(parent,label,choices);s.setSelection(Math.max(0,Math.min(choices.size()-1,selected)));return s;}
    private Switch toggle(LinearLayout parent,String text,boolean checked){Switch s=new Switch(this);s.setText(text);s.setTextSize(13);s.setTextColor(Ui.TEXT);s.setChecked(checked);s.setMinHeight(dp(48));Ui.add(parent,s,8);return s;}
    private int integer(EditText edit,int fallback,boolean validate){try{return Integer.parseInt(edit.getText().toString().trim());}catch(Exception e){if(validate){edit.setError("请填写整数");edit.requestFocus();throw new IllegalArgumentException("请检查数字设置");}return fallback;}}
    private double decimal(EditText edit,double fallback,boolean validate){try{double n=Double.parseDouble(edit.getText().toString().trim());if(!Double.isFinite(n))throw new NumberFormatException();return n;}catch(Exception e){if(validate){edit.setError("请填写有效数字");throw new IllegalArgumentException("请检查数字设置");}return fallback;}}
    private LineConfig snapshot(boolean validate){
        LineConfig c=Json.gson().fromJson(Json.gson().toJson(config),LineConfig.class);c.name=name.getText().toString().trim();c.baseUrl=base.getText().toString().trim();c.apiKey=key.getText().toString();c.model=model.getText().toString().trim();c.protocol=new String[]{"openai","anthropic","responses"}[protocol.getSelectedItemPosition()];
        sequenceEditor.apply(c);c.sampleCount=integer(samples,config.sampleCount,validate);c.effort=ReasoningPolicy.VALUES[effort.getSelectedItemPosition()];
        c.maxTokens=integer(maxTokens,config.maxTokens,validate);c.temperature=decimal(temperature,config.temperature,validate);c.intervalMin=integer(interval,config.intervalMin,validate);
        c.inputTokenMode=inputMode.getSelectedItemPosition()==1?"ratio":"amount";
        c.inputTokenExcess=integer(excess,config.inputTokenExcess,validate&&"amount".equals(c.inputTokenMode));c.inputTokenRatio=decimal(inputRatio,config.inputTokenRatio,validate&&"ratio".equals(c.inputTokenMode));
        c.complexityMultiplier=decimal(complexity,config.complexityMultiplier,validate);c.retryCount=integer(retries,config.retryCount,validate);
        c.retryPartial=retryPartial.isChecked();c.notifyEnabled=notifyEnabled.isChecked();c.enabled=enabled.isChecked();c.tokenAuditEnabled=auditEnabled.isChecked();
        String manual=lower.getText().toString().trim();c.baselineLowerOverride=manual.isEmpty()?null:decimal(lower,0,validate);
        if(validate){
            if(c.name.isEmpty()||c.model.isEmpty())throw new IllegalArgumentException("请填写线路名称和模型 ID");c.baseUrl=RequestInputs.base(c.baseUrl);c.apiKey=RequestInputs.key(c.apiKey);c.validateSettings();
        }
        return c;
    }
    private String identity(LineConfig c){try{return InputAudit.sha256((RequestInputs.base(c.baseUrl)+"\n"+c.protocol+"\n"+RequestInputs.key(c.apiKey)).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){return "";}}
    private void fetch(boolean choose){
        final LineConfig c=snapshot(false);try{c.baseUrl=RequestInputs.base(c.baseUrl);c.apiKey=RequestInputs.key(c.apiKey);}catch(Exception e){connectionStatus.setText(e.getMessage());connectionStatus.setTextColor(Ui.RED);return;}
        final String requested=identity(c);final int ticket=++fetchGeneration;fetchButton.setEnabled(false);checkButton.setEnabled(false);connectionStatus.setText("正在读取模型列表… 不发送生成请求");connectionStatus.setTextColor(Ui.MUTED);
        new Thread(()->{ApiClient.ModelResult r=api.fetchModels(c);runOnUiThread(()->{if(isFinishing()||isDestroyed()||ticket!=fetchGeneration)return;fetchButton.setEnabled(true);checkButton.setEnabled(true);if(!requested.equals(identity(snapshot(false)))){connectionStatus.setText("连接信息已修改，请重新检查");return;}connectionStatus.setText(r.message+(r.endpoint.isEmpty()?"":"\n"+r.endpoint));connectionStatus.setTextColor(r.ok?Ui.GREEN:Ui.RED);if(r.ok&&!r.models.isEmpty()){config.cachedModels=new ArrayList<>(r.models);config.modelsFetchedAt=System.currentTimeMillis();cacheIdentity=requested;config.cachedModelsIdentity=requested;if(!r.apiBase.isEmpty()){base.setText(r.apiBase);config.resolvedApiBase=r.apiBase;}cacheIdentity=identity(snapshot(false));config.cachedModelsIdentity=cacheIdentity;if(choose)ModelPickerDialog.show(this,config.cachedModels,model.getText().toString(),picked->model.setText(picked));}});},"pelican-models").start();
    }
    private void pickCached(){if(config.cachedModels.isEmpty()){fetch(true);return;}if(!cacheIdentity.equals(identity(snapshot(false)))){new AlertDialog.Builder(this).setTitle("这份缓存来自旧连接").setMessage("当前地址、协议或密钥已改变。建议重新获取，避免选到不适用的模型。").setNegativeButton("取消",null).setNeutralButton("仍查看缓存",(d,w)->ModelPickerDialog.show(this,config.cachedModels,model.getText().toString(),p->model.setText(p))).setPositiveButton("重新获取",(d,w)->fetch(true)).show();}else ModelPickerDialog.show(this,config.cachedModels,model.getText().toString(),p->model.setText(p));}
    private void preview(){
        final LineConfig c=snapshot(false);if(c.model.isEmpty()){Ui.toast(this,"先填写模型 ID");return;}String prompt=c.promptText(c.promptForRun(0));
        final ProgressDialog loading=ProgressDialog.show(this,"输入自检","正在本地分词和核对 JSON，不发送网络请求…",true,false);previewDialog=loading;
        new Thread(()->{try{InputAudit.Evidence e=api.preview(c,prompt);runOnUiThread(()->{if(isFinishing()||isDestroyed())return;loading.dismiss();ScrollView scroll=new ScrollView(this);TextView text=Ui.text(this,"自检通过：1 条 user 消息，与原提示词逐字一致\n提示词 "+e.promptCharacters+" 字符 / "+e.promptBytes+" UTF-8 字节\n请求正文 "+e.bodyBytes+" 字节\n本地估算 "+e.estimatedInputTokens+" Token\n"+e.countNote+"\n\n实际将发送的 JSON（不含认证头）：\n"+e.bodyJson,12,Ui.TEXT);text.setTextIsSelectable(true);text.setPadding(dp(20),dp(16),dp(20),dp(16));scroll.addView(text);new AlertDialog.Builder(this).setTitle("实际请求预览").setView(scroll).setPositiveButton("关闭",null).show();});}catch(Exception e){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){loading.dismiss();new AlertDialog.Builder(this).setTitle("输入自检未通过").setMessage(e.getMessage()).setPositiveButton("返回修改",null).show();}});}},"pelican-input-preview").start();
    }
    private void save(){try{LineConfig c=snapshot(true);
        synchronized(store){LineConfig latest=store.getLine(c.id);if(latest!=null){c.lastRunAt=latest.lastRunAt;c.lastStatus=latest.lastStatus;c.lastBadge=latest.lastBadge;c.consecutiveIssues=latest.consecutiveIssues;
            if(!sequenceEditor.edited()&&!sequenceEditedBeforeRecreate){c.promptSequence=latest.promptSequence;c.sequenceIndex=latest.sequenceIndex;c.sequenceBlocked=latest.sequenceBlocked;c.sequenceFailureNote=latest.sequenceFailureNote;c.lastSequenceRunId=latest.lastSequenceRunId;}
            if(c.enabled&&oldEnabled&&oldInterval==c.intervalMin)c.nextRunAt=latest.nextRunAt;}
            if(!c.enabled)c.nextRunAt=0;else if(!oldEnabled||oldInterval!=c.intervalMin||c.nextRunAt<=0)c.nextRunAt=System.currentTimeMillis()+c.intervalMin*60000L;store.saveLine(c);}
        startService(new Intent(this,MonitorService.class).setAction(MonitorService.ACTION_RELOAD));saved=true;setResult(RESULT_OK);finish();
    }catch(Exception e){new AlertDialog.Builder(this).setTitle("暂时不能保存").setMessage(e.getMessage()).setPositiveButton("返回修改",null).show();}}
    private void leave(){if(saved||initialForm.equals(formFingerprint())){finish();return;}new AlertDialog.Builder(this).setTitle("有修改尚未保存").setMessage("保存后返回，还是放弃本次修改？").setNegativeButton("继续编辑",null).setNeutralButton("放弃修改",(d,w)->finish()).setPositiveButton("保存",(d,w)->save()).show();}
    @Override public void onBackPressed(){leave();}
    private void confirmDelete(){if(LiveRuns.forLine(config.id)!=null){Ui.toast(this,"当前轮次尚未结束，暂不能删除线路");return;}CheckBox history=new CheckBox(this);history.setText("同时删除本线路的历史与 SVG（不可恢复）");history.setTextColor(Ui.RED);history.setPadding(dp(20),dp(10),dp(16),dp(10));new AlertDialog.Builder(this).setTitle("删除这条线路？").setMessage("默认保留历史记录。").setView(history).setNegativeButton("取消",null).setPositiveButton("删除线路",(d,w)->{if(LiveRuns.forLine(config.id)!=null){Ui.toast(this,"当前检测未结束，未删除");return;}store.deleteLine(config.id,history.isChecked());startService(new Intent(this,MonitorService.class).setAction(MonitorService.ACTION_RELOAD));saved=true;setResult(RESULT_OK);finish();}).show();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("form",Json.gson().toJson(snapshot(false)));b.putBoolean("new",isNew);b.putBoolean("sequenceEdited",sequenceEditor.edited()||sequenceEditedBeforeRecreate);}
    @Override protected void onDestroy(){fetchGeneration++;api.cancelAll();if(previewDialog!=null)previewDialog.dismiss();super.onDestroy();}
    private String formFingerprint(){
        StringBuilder out=new StringBuilder();
        for(EditText edit:new EditText[]{name,base,key,model,interval,maxTokens,temperature,excess,lower,samples,retries,inputRatio,complexity}){String value=edit.getText().toString();out.append(value.length()).append(':').append(value);}
        for(Spinner spinner:new Spinner[]{protocol,effort,inputMode})out.append('|').append(spinner.getSelectedItemPosition());
        for(Switch toggle:new Switch[]{enabled,retryPartial,notifyEnabled,auditEnabled})out.append('|').append(toggle.isChecked());
        out.append(sequenceEditor.fingerprint());
        return InputAudit.sha256(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private android.text.TextWatcher watcher(Runnable task){return new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){task.run();}public void afterTextChanged(android.text.Editable e){}};}
    private AdapterView.OnItemSelectedListener selection(Runnable task){return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){task.run();}public void onNothingSelected(AdapterView<?> p){}};}
    private void watchInteger(EditText edit,int min,int max,Runnable task){
        Runnable check=()->{String text=edit.getText().toString().trim();try{int n=Integer.parseInt(text);edit.setError(n<min||n>max?"请输入 "+min+"～"+max:null);}catch(Exception e){edit.setError("请输入 "+min+"～"+max+" 的整数");}if(task!=null)task.run();};edit.addTextChangedListener(watcher(check));check.run();
    }
    private void refreshReasoning(){
        if(reasoningNote==null||effort==null||maxTokens==null)return;
        LineConfig c=new LineConfig();c.protocol=new String[]{"openai","anthropic","responses"}[protocol.getSelectedItemPosition()];c.effort=ReasoningPolicy.VALUES[effort.getSelectedItemPosition()];c.maxTokens=integer(maxTokens,0,false);
        reasoningNote.setText(ReasoningPolicy.description(c));
        try{ReasoningPolicy.validate(c);maxTokens.setError(null);}catch(Exception e){maxTokens.setError(e.getMessage());}
    }
    private void refreshBaseline(){
        if(baselineStatus==null||complexity==null||lower==null||sequenceEditor==null)return;
        LineConfig c=Json.gson().fromJson(Json.gson().toJson(config),LineConfig.class);c.complexityMultiplier=decimal(complexity,.9,false);String raw=lower.getText().toString().trim();c.baselineLowerOverride=raw.isEmpty()?null:decimal(lower,0,false);
        if(!Double.isFinite(c.complexityMultiplier)||c.complexityMultiplier<.5||c.complexityMultiplier>1.5){complexity.setError("倍率须为 0.5～1.5");return;}complexity.setError(null);
        PromptSequence.Entry next=sequenceEditor.first();Baseline b=store.getBaseline(c.id,model.getText().toString().trim(),next.id);
        baselineStatus.setText("下一轮："+next.label+"\n"+QualityPolicy.equation(c,b));
    }
}
