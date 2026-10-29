# 历史开发补丁，仅保留改动过程；不是构建步骤，不要对当前源码重复执行。
from pathlib import Path
b=Path(__file__).resolve().parents[1]/'app/src/main/java/app/pelican/check'
p=b/'MainActivity.java';s=p.read_text();a=s.index('    public void showSettings()');z=s.index('    private android.widget.LinearLayout settingsCard(',a)
s=s[:a]+'''    public void showSettings() {
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
        android.widget.LinearLayout trust=settingsCard(body,"输入检查与组件","先核对自己的请求；计数不确定时不判断注入。");
        settingsAction(trust,"新版输入检查如何工作","实际发送 JSON / 本地分词 / 可靠计数 / 固定差额",()->new android.app.AlertDialog.Builder(this).setTitle("严格输入检查").setMessage("每次请求先验证：只有一条 user 消息、提示词逐字一致、没有额外 system/tools、输出上限未被改动，并保留正文与 SHA-256。\\n\\n本地 JTokkit 使用真实分词表，不按字符除以 4。无法确认中转模型或消息封装时，只显示估算、不触发疑似注入。Responses 若支持标准计数接口，可复用同一份输入预检，再按你设置的超出数量检查。\\n\\n同站预检不是独立于中转站的证明。旧版历史保留原结果，不能伪称经过新版复核。").setPositiveButton("明白",null).show());
        settingsAction(trust,"关于 Pelican 2.3","离线保存 · 不上传遥测 · 查看开源组件",()->new android.app.AlertDialog.Builder(this).setTitle("Pelican 2.3").setMessage("OkHttp / Gson / AndroidSVG / MPAndroidChart：Apache-2.0\\nJTokkit：MIT\\n\\n图表采用 MPAndroidChart，分词采用 JTokkit。检测与输入检查都是辅助指标，不能单凭一项结果确认降智或注入。详细许可随安装包附带。").setPositiveButton("关闭",null).show());
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
        android.widget.Spinner retry=app.pelican.check.Ui.choiceField(form,"本轮最多补试",java.util.Arrays.asList("不补试","1 次","2 次"));retry.setSelection(app.pelican.check.Prefs.get(this).getInt("defaultRetries",2));
        android.widget.EditText tolerance=app.pelican.check.Ui.number(this,"500",String.valueOf(app.pelican.check.Prefs.get(this).getInt("defaultTokenExcess",500)));labeled(form,"新线路容许多出的输入 Token",tolerance);
        android.widget.EditText notify=app.pelican.check.Ui.number(this,"2",String.valueOf(app.pelican.check.Prefs.get(this).getInt("notifyThreshold",2)));labeled(form,"连续异常达到几次通知（全部线路）",notify);
        app.pelican.check.Ui.add(form,app.pelican.check.Ui.text(this,"只有通知次数立即影响全部线路，其他设置仅用于新建线路。",11,app.pelican.check.Ui.MUTED),8);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(form);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("检测默认值").setView(scroll).setNegativeButton("取消",null).setNeutralButton("定时规则应用到已有线路",null).setPositiveButton("保存默认值",null).create();
        dialog.setOnShowListener(d->{
            android.view.View.OnClickListener commit=v->{try{
                int p=Integer.parseInt(period.getText().toString().trim()),t=Integer.parseInt(tolerance.getText().toString().trim()),n=Integer.parseInt(notify.getText().toString().trim()),r=retry.getSelectedItemPosition();
                if(p<1||p>1440||t<0||t>10000000||n<1||n>100)throw new IllegalArgumentException();
                if(v==dialog.getButton(-3)){
                    new android.app.AlertDialog.Builder(this).setTitle("修改已有线路的定时规则？").setMessage("将现有线路改为每 "+p+" 分钟检测、最多补试 "+r+" 次。不会立即运行，不改变提示词、密钥、Token 差额和其他设置。").setNegativeButton("取消",null).setPositiveButton("应用",(x,w)->{for(app.pelican.check.LineConfig line:store.getLines()){line.intervalMin=p;line.retryCount=r;if(line.enabled)line.nextRunAt=System.currentTimeMillis()+p*60000L;store.saveLine(line);}startService(new android.content.Intent(this,app.pelican.check.MonitorService.class).setAction(app.pelican.check.MonitorService.ACTION_RELOAD));app.pelican.check.Ui.toast(this,"已有线路的定时规则已更新");dialog.dismiss();}).show();return;
                }
                app.pelican.check.Prefs.get(this).edit().putInt("defaultInterval",p).putInt("defaultRetries",r).putInt("defaultTokenExcess",t).putInt("notifyThreshold",n).apply();dialog.dismiss();app.pelican.check.Ui.toast(this,"默认值已保存");
            }catch(Exception error){app.pelican.check.Ui.toast(this,"请检查数字：间隔 1～1440，差额 0～1000 万，通知 1～100");}};
            dialog.getButton(-1).setOnClickListener(commit);dialog.getButton(-3).setOnClickListener(commit);
        });dialog.show();
    }

''' +s[z:]
# Distinguish old historical warnings from the new request-audited strict algorithm.
s=s.replace('historyCard.tokenAlert.setVisibility(runData.inputTokenAlert ? 0 : 8);','historyCard.tokenAlert.setVisibility(runData.inputTokenAlert ? 0 : 8);\n        historyCard.tokenAlert.setText(runData.inputAuditVersion>=2?"输入 Token 差额异常":"旧版输入告警 · 未复核");')
p.write_text(s)
p=b/'RunData.java';s=p.read_text().replace('    public int inputTokenExcessUsed = 500;','    public int inputAuditVersion;\n    public int inputTokenExcessUsed = 500;',1);p.write_text(s)
p=b/'TokenGuard.java';s=p.read_text().replace('        run.inputTokenAlert=false;','        run.inputAuditVersion=2;\n        run.inputTokenAlert=false;',1);p.write_text(s)
p=b/'RunDetailActivity.java';s=p.read_text();old='''        java.util.Iterator<java.lang.String> it = sampleData.evidence.iterator();''';assert old in s;s=s.replace(old,'''        InputAudit.Evidence audit=sampleData.inputAudit;
        if(audit!=null){
            android.widget.LinearLayout box=app.pelican.check.Ui.column(this);
            box.setPadding(app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12),app.pelican.check.Ui.dp(this,12));
            box.setBackground(app.pelican.check.Ui.round(this,app.pelican.check.Ui.PALE_GRAY,12));
            box.addView(app.pelican.check.Ui.heading(this,"本机输入自检",14));
            String count=audit.countReliable&&audit.expectedInputTokens!=null?"预检输入 "+audit.expectedInputTokens+" Token":"本地估算 "+(audit.estimatedInputTokens>0?audit.estimatedInputTokens:"—")+" Token（不据此告警）";
            app.pelican.check.Ui.add(box,app.pelican.check.Ui.text(this,(audit.contentVerified?"提示词逐字核对通过":"输入内容未通过自检")+" · "+audit.messageCount+" 条消息\\n"+audit.promptCharacters+" 字符 / "+audit.promptBytes+" UTF-8 字节\\n请求正文 "+audit.bodyBytes+" 字节 · "+(audit.wireBytesVerified?"发送字节核对通过":"未取得完整发送字节确认")+"\\n"+count+"\\n接口回报 "+token(sampleData.inTokens)+" 输入 Token\\n"+audit.countSource+"\\n"+audit.countNote,12,app.pelican.check.Ui.TEXT),6);
            final android.widget.TextView request=app.pelican.check.Ui.text(this,audit.bodyJson+"\\n\\n正文 SHA-256: "+audit.bodySha256+"\\n提示词 SHA-256: "+audit.promptSha256,11,app.pelican.check.Ui.TEXT);request.setTextIsSelectable(true);request.setTypeface(android.graphics.Typeface.MONOSPACE);request.setVisibility(8);
            android.widget.Button expand=app.pelican.check.Ui.button(this,"核对实际发送 JSON ▾",false);app.pelican.check.Ui.add(box,expand,8);app.pelican.check.Ui.add(box,request,8);expand.setOnClickListener(v->{boolean show=request.getVisibility()!=0;request.setVisibility(show?0:8);expand.setText(show?"收起实际请求 ▴":"核对实际发送 JSON ▾");});
            app.pelican.check.Ui.add(linearLayoutCard,box,10);
        }
        java.util.Iterator<java.lang.String> it = sampleData.evidence.iterator();''')
s=s.replace('"⚠ 输入 Token 异常 / 疑似注入\\n本次 "','(sampleData.inputAudit==null?"旧版参考告警（未按新版复核）\\n本次 ":"⚠ 输入 Token 差额异常 / 疑似注入\\n本次 ")')
p.write_text(s)
print('Settings reorganized into preferences/runtime/data/input sections; audit evidence card and legacy-warning distinction added.')
