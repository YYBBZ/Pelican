from pathlib import Path
p=Path('/workspace/Pelican/app/src/main/java/app/pelican/check/LineEditActivity.java');s=p.read_text()
s=s.replace('private EditText name,base,key,model,customPrompt,interval,maxTokens,temperature,excess,lower;','private EditText name,base,key,model,interval,maxTokens,temperature,excess,lower,samples,retries,inputRatio,complexity;')
s=s.replace('private Spinner protocol,promptMode,promptPreset,samples,effort,retries;','private Spinner protocol,effort,inputMode;\n    private SequenceEditor sequenceEditor;\n    private TextView reasoningNote,retryNote;')
s=s.replace('config=new LineConfig();','config=LineConfig.createNew();').replace('getInt("defaultTokenExcess",500)','getInt("defaultTokenExcess24",800)')
s=s.replace('cacheIdentity=config.cachedModelsIdentity;build();','config.normalize();cacheIdentity=config.cachedModelsIdentity;build();')
a=s.index('        promptMode=choice(');z=s.index('        LinearLayout advanced=fold(',a)
s=s[:a]+'''        sequenceEditor=new SequenceEditor(this,config,()->refreshBaseline());Ui.add(task,sequenceEditor,8);
        samples=field(task,"每轮样本（1～99 张）","1",String.valueOf(config.sampleCount),true);
        watchInteger(samples,1,99,null);
        Button preview=Ui.button(this,"预览下一轮实际请求 / 输入自检",false);Ui.add(task,preview,10);preview.setOnClickListener(v->preview());
''' +s[z:]
s=s.replace('effort=choice(advanced,"思考强度",Arrays.asList("服务端默认","关闭","低","中","高"),Arrays.asList("auto","none","low","medium","high").indexOf(config.effort));','effort=choice(advanced,"思考强度",Arrays.asList(ReasoningPolicy.LABELS),Arrays.asList(ReasoningPolicy.VALUES).indexOf(config.effort));')
s=s.replace('''        Ui.add(advanced,Ui.text(this,"不自动提高输出上限或降低思考强度。Responses 的温度使用服务端默认值。",11,Ui.MUTED),8);''','''        reasoningNote=Ui.text(this,"",11,Ui.MUTED);Ui.add(advanced,reasoningNote,8);
        effort.setOnItemSelectedListener(selection(this::refreshReasoning));protocol.setOnItemSelectedListener(selection(this::refreshReasoning));
        maxTokens.addTextChangedListener(watcher(this::refreshReasoning));refreshReasoning();''')
s=s.replace('''        retries=choice(retry,"本轮最多补试",Arrays.asList("不补试","1 次","2 次"),Math.max(0,Math.min(2,config.retryCount)));retryPartial=toggle(retry,"断流后也补试（可能重复计费）",config.retryPartial);''','''        retries=field(retry,"本轮最多补试（0～20 次）","2",String.valueOf(config.retryCount),true);
        retryNote=Ui.text(this,"每次补试都可能重复计费",12,Ui.ORANGE);Ui.add(retry,retryNote,6);
        watchInteger(retries,0,20,()->retryNote.setVisibility(integer(retries,0,false)>5?View.VISIBLE:View.GONE));
        retryPartial=toggle(retry,"断流后也补试（可能重复计费）",config.retryPartial);''')
a=s.index('        auditEnabled=toggle(guard,');z=s.index('        LinearLayout baseline=section(',a)
s=s[:a]+'''        auditEnabled=toggle(guard,"启用输入 Token 异常筛查",config.tokenAuditEnabled);
        inputMode=choice(guard,"判定方式（两种方式不同时生效）",Arrays.asList("按数量","按倍率"),"ratio".equals(config.inputTokenMode)?1:0);
        excess=field(guard,"容许多出的输入 Token","800",String.valueOf(config.inputTokenExcess),true);
        inputRatio=field(guard,"达到本地估算的多少倍判异常","5",String.valueOf(config.inputTokenRatio),true);
        inputMode.setOnItemSelectedListener(selection(()->{boolean ratio=inputMode.getSelectedItemPosition()==1;inputRatio.setEnabled(ratio);excess.setEnabled(!ratio);}));
        inputRatio.setEnabled("ratio".equals(config.inputTokenMode));excess.setEnabled(!"ratio".equals(config.inputTokenMode));
        Ui.add(guard,Ui.text(this,"本地估算直接作为筛查基准。估算不可用或为 0 时，用“接口输入 Token ＞ 请求正文字节数”补充筛查。只在卡片和详情提示，不弹窗、不发送系统通知。",12,Ui.MUTED),8);
        Ui.add(guard,Ui.text(this,"蓝色“注入”表示命中输入异常规则，疑似注入，不能据此确认；规则不是精确注入鉴定。",11,Ui.MUTED),6);
''' +s[z:]
a=s.index('        Baseline b=store.getBaseline(');z=s.index('        Button reset=',a)
s=s[:a]+'''        baselineStatus=Ui.text(this,"",12,Ui.MUTED);baseline.addView(baselineStatus);
        complexity=field(baseline,"自动复杂度下限倍率（0.5～1.5）","0.9",String.valueOf(config.complexityMultiplier),true);
        LinearLayout manual=fold(baseline,"高级质量设置","手动固定下限优先，不再乘倍率；历史基线保留",false);lower=field(manual,"手动复杂度下限（0～100）","留空使用自动下限 × 倍率",config.baselineLowerOverride==null?"":String.valueOf(config.baselineLowerOverride),true);
        complexity.addTextChangedListener(watcher(this::refreshBaseline));lower.addTextChangedListener(watcher(this::refreshBaseline));model.addTextChangedListener(watcher(this::refreshBaseline));refreshBaseline();
''' +s[z:]
a=s.index('        c.promptMode=');z=s.index('        return c;',a)
s=s[:a]+'''        sequenceEditor.apply(c);c.sampleCount=integer(samples,config.sampleCount,validate);c.effort=ReasoningPolicy.VALUES[effort.getSelectedItemPosition()];
        c.maxTokens=integer(maxTokens,config.maxTokens,validate);c.temperature=decimal(temperature,config.temperature,validate);c.intervalMin=integer(interval,config.intervalMin,validate);
        c.inputTokenMode=inputMode.getSelectedItemPosition()==1?"ratio":"amount";
        c.inputTokenExcess=integer(excess,config.inputTokenExcess,validate&&"amount".equals(c.inputTokenMode));c.inputTokenRatio=decimal(inputRatio,config.inputTokenRatio,validate&&"ratio".equals(c.inputTokenMode));
        c.complexityMultiplier=decimal(complexity,config.complexityMultiplier,validate);c.retryCount=integer(retries,config.retryCount,validate);
        c.retryPartial=retryPartial.isChecked();c.notifyEnabled=notifyEnabled.isChecked();c.enabled=enabled.isChecked();c.tokenAuditEnabled=auditEnabled.isChecked();
        String manual=lower.getText().toString().trim();c.baselineLowerOverride=manual.isEmpty()?null:decimal(lower,0,validate);
        if(validate){
            if(c.name.isEmpty()||c.model.isEmpty())throw new IllegalArgumentException("请填写线路名称和模型 ID");c.baseUrl=RequestInputs.base(c.baseUrl);c.apiKey=RequestInputs.key(c.apiKey);c.validateSettings();
        }
''' +s[z:]
s=s.replace('String prompt=c.promptText(LineConfig.promptById(c.promptId));','String prompt=c.promptText(c.promptForRun(0));')
s=s.replace('''c.consecutiveIssues=latest.consecutiveIssues;if(c.enabled''','''c.consecutiveIssues=latest.consecutiveIssues;
            if(!sequenceEditor.edited()){c.promptSequence=latest.promptSequence;c.sequenceIndex=latest.sequenceIndex;c.sequenceBlocked=latest.sequenceBlocked;c.sequenceFailureNote=latest.sequenceFailureNote;c.lastSequenceRunId=latest.lastSequenceRunId;}
            if(c.enabled''')
s=s.replace('''new EditText[]{name,base,key,model,customPrompt,interval,maxTokens,temperature,excess,lower}''','''new EditText[]{name,base,key,model,interval,maxTokens,temperature,excess,lower,samples,retries,inputRatio,complexity}''').replace('''new Spinner[]{protocol,promptMode,promptPreset,samples,effort,retries}''','''new Spinner[]{protocol,effort,inputMode}''')
s=s.replace('''        return InputAudit.sha256(out.toString().getBytes''','''        out.append(sequenceEditor.fingerprint());
        return InputAudit.sha256(out.toString().getBytes''')
idx=s.rfind('}');s=s[:idx]+'''    private android.text.TextWatcher watcher(Runnable task){return new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){task.run();}public void afterTextChanged(android.text.Editable e){}};}
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
        baselineStatus.setText("下一轮："+next.label+"\\n"+QualityPolicy.equation(c,b));
    }
''' +s[idx:];p.write_text(s);print('Editor now has drag/drop sequence list, 99-sample/20-retry inputs, seven explicit reasoning levels, amount/ratio screening and effective-baseline equation.')
