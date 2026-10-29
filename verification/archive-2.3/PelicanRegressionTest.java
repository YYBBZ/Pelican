import app.pelican.check.*;
import com.google.gson.*;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.*;
import okhttp3.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic local fixtures only. Interceptors replace networking; no real relay or device UI. */
public class PelicanRegressionTest {
    static int passed;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed++;System.out.println("PASS "+name);}
    static void near(double a,double b,String name){check(Math.abs(a-b)<.0001,name);}
    interface Work{void run()throws Exception;}
    static void rejected(Work work,String name){try{work.run();}catch(Exception e){check(true,name);return;}throw new AssertionError(name);}
    static LineConfig config(){LineConfig c=new LineConfig();c.baseUrl="https://fixture.invalid/v1";c.apiKey="fixture-secret";c.model="gpt-4o-mini-2024-07-18";c.maxTokens=512;c.retryCount=0;c.effort="auto";return c;}
    static JsonObject object(String json){return JsonParser.parseString(json).getAsJsonObject();}
    static JsonObject payload(LineConfig c,String prompt){JsonObject o=new JsonObject();o.addProperty("model",c.model);o.addProperty("stream",true);JsonArray a=new JsonArray();JsonObject m=new JsonObject();m.addProperty("role","user");m.addProperty("content",prompt);a.add(m);o.add("responses".equals(c.protocol)?"input":"messages",a);o.addProperty("responses".equals(c.protocol)?"max_output_tokens":"max_tokens",c.maxTokens);return o;}
    static RunData run(String line,long time,int input){RunData r=new RunData();r.lineId=line;r.lineName=line;r.model="model";r.promptKey="prompt";r.promptLabel="题目";r.inputTokenSignature="signature";r.status="PASS";r.startedAt=time;r.endedAt=time+1000;r.score=90;RunData.SampleData s=new RunData.SampleData();s.requestOk=true;s.svgOk=true;s.score=90;s.inTokens=input;s.outTokens=20;s.reasonTokens=5;r.samples.add(s);return r;}
    static Response response(Request r,int code,String body,String type){return new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(code).message("local fixture").header("Content-Type",type).body(ResponseBody.create(body,MediaType.get(type))).build();}
    static Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    static Object result()throws Exception{Constructor<?> c=Class.forName("app.pelican.check.ApiClient$GenResult").getDeclaredConstructor();c.setAccessible(true);return c.newInstance();}
    static Method method(String name,Class<?>... types)throws Exception{Method m=ApiClient.class.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
    static ApiClient mock(java.util.function.Function<Request,Response> callback){return new ApiClient(null,new OkHttpClient.Builder().addInterceptor(chain->callback.apply(chain.request())).build());}
    static String successBody(){return "{\"model\":\"gpt-4o-mini-2024-07-18\",\"choices\":[{\"message\":{\"content\":\"hello\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":2}}";}
    static void requestInputs()throws Exception{
        check(RequestInputs.key("  fixture-secret \n").equals("fixture-secret"),"密钥首尾空白规范化");
        check(RequestInputs.key("\uFEFFfixture-secret\u200B").equals("fixture-secret"),"密钥首尾不可见格式字符规范化");
        check(RequestInputs.key("Bearer fixture-secret").equals("fixture-secret"),"避免重复 Bearer 前缀");
        check(RequestInputs.key("\"fixture-secret\"").equals("fixture-secret"),"成对引号可安全移除");
        rejected(()->RequestInputs.key("fixture secret"),"密钥中间空格拒绝，不擅自拼接");
        rejected(()->RequestInputs.key("fixture\nsecret"),"密钥中间换行拒绝");
        rejected(()->RequestInputs.key("测试密钥"),"非英文密钥拒绝");
        rejected(()->RequestInputs.key("   "),"空密钥拒绝");
        check(RequestInputs.base("https://fixture.invalid/v1/chat/completions/").equals("https://fixture.invalid/v1"),"完整生成地址可规范化");
        check(RequestInputs.endpoints("https://fixture.invalid","/models").get(0).equals("https://fixture.invalid/v1/models"),"域名默认优先 /v1/models");
        check(RequestInputs.endpoints("https://fixture.invalid/v1","/models").equals(Arrays.asList("https://fixture.invalid/v1/models","https://fixture.invalid/models")),"显式 v1 不重复拼接");
        rejected(()->RequestInputs.base("fixture.invalid"),"缺协议地址拒绝");
        rejected(()->RequestInputs.base("https://user:pass@fixture.invalid"),"地址不得夹带认证信息");
        rejected(()->RequestInputs.base("https://fixture.invalid?key=x"),"地址不得夹带密钥查询参数");
        rejected(()->RequestInputs.base("https://fixture.invalid/#page"),"网页锚点不是 API 地址");
        LineConfig c=config();c.baseUrl="https://fixture.invalid";c.resolvedApiBase=c.baseUrl;
        check(RequestInputs.generationEndpoint(c,"/chat/completions").equals(c.baseUrl+"/chat/completions"),"成功解析的无版本路径保留");
        check(!RequestInputs.safeError("failure fixture-secret",c.apiKey).contains(c.apiKey),"错误信息隐藏密钥");
        LineConfig copy=c.copyWithoutKey();c.cachedModelsIdentity="test-identity";copy=c.copyWithoutKey();check(copy.apiKey.isEmpty()&&copy.cachedModelsIdentity.isEmpty(),"备份配置不含密钥或认证指纹");
    }
    static void models()throws Exception{
        check(ApiClient.parseModelIds("{\"data\":[{\"id\":\"a\"},{\"id\":\"a\"},{\"id\":\"b\"}]}").equals(Arrays.asList("a","b")),"标准模型列表去重");
        check(ApiClient.parseModelIds("{\"result\":{\"items\":[{\"name\":\"model-a\"}]}}").equals(Arrays.asList("model-a")),"嵌套模型列表解析");
        AtomicInteger count=new AtomicInteger();ApiClient a=mock(r->{count.incrementAndGet();return response(r,401,"{\"code\":\"API_KEY_DISABLED\"}","application/json");});
        ApiClient.ModelResult m=a.fetchModels(config());check(!m.ok&&m.status==401&&count.get()==1,"401 明确报错且不轮换认证重试");check(m.message.contains("API_KEY_DISABLED"),"保留站点明确的停用原因");
        a=mock(r->response(r,200,"<html>landing page</html>","text/html"));check(!a.fetchModels(config()).ok,"网站首页不误报成功模型列表");
        a=mock(r->response(r,200,"{\"error\":\"blocked\"}","application/json"));check(!a.fetchModels(config()).ok,"HTTP200 错误正文不误报成功");
        a=mock(r->response(r,200,"{\"data\":[]}","application/json"));m=a.fetchModels(config());check(m.ok&&m.models.isEmpty()&&m.message.contains("手动"),"空列表保留手动填写入口");
        count.set(0);a=mock(r->{count.incrementAndGet();return response(r,r.url().encodedPath().equals("/v1/models")?404:200,"{\"data\":[{\"id\":\"a\"}]}","application/json");});m=a.fetchModels(config());check(m.ok&&count.get()==2&&m.apiBase.equals("https://fixture.invalid"),"仅在路径404后回退并记住成功地址");
        count.set(0);a=mock(r->{count.incrementAndGet();return response(r,302,"redirect","text/plain").newBuilder().header("Location","https://other.invalid").build();});m=a.fetchModels(config());check(!m.ok&&count.get()==1&&m.message.contains("重定向"),"重定向不自动转发认证信息");
        final boolean[] normalized={false};a=mock(r->{normalized[0]="Bearer fixture-secret".equals(r.header("Authorization"));return response(r,200,"{\"data\":[]}","application/json");});LineConfig c=config();c.apiKey=" Bearer fixture-secret ";a.fetchModels(c);check(normalized[0],"实际认证头规范化正确");
    }
    static void audit()throws Exception{
        LineConfig c=config();String prompt="鹈鹕 🚲\n只返回 \"SVG\"。\n";
        InputAudit.Frozen frozen=InputAudit.freeze(c,prompt,payload(c,prompt));InputAudit.Evidence e=frozen.evidence;
        check(e.contentVerified&&e.messageCount==1,"逐字输入校验通过");check(e.promptBytes==prompt.getBytes(StandardCharsets.UTF_8).length,"UTF8 字节数匹配实际字符串");check(e.promptCharacters==prompt.codePointCount(0,prompt.length()),"表情按 Unicode 码点计字符");
        check(JsonParser.parseString(e.bodyJson).getAsJsonObject().getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString().equals(prompt),"中文表情引号换行和尾换行原样保存");
        check(e.bodyBytes==frozen.bytes().length&&e.bodySha256.equals(InputAudit.sha256(frozen.bytes())),"实际序列化正文长度与摘要一致");
        byte[] bytes=frozen.bytes();bytes[0]^=1;check(!frozen.matches(bytes)&&frozen.matches(frozen.bytes()),"冻结正文不受外部数组修改影响");
        check(!e.bodyJson.contains(c.apiKey),"认证密钥不进入正文证据");check(!e.wireBytesVerified,"尚未发送时不会冒充已确认发送");check(!e.countReliable&&e.expectedInputTokens==null,"本地分词不冒充完整精确输入计数");check(e.estimatedInputTokens>0,"本地真实分词器可用");
        c.model="unknown-relay-alias";e=InputAudit.freeze(c,prompt,payload(c,prompt)).evidence;check(!e.countReliable&&e.countNote.contains("未确认"),"未知中转别名明确只估算");c=config();
        final LineConfig cfg=c;
        rejected(()->InputAudit.freeze(cfg,prompt,payload(cfg,prompt+prompt)),"重复拼接提示词会阻止发送");
        rejected(()->InputAudit.freeze(cfg,prompt,payload(cfg,prompt.substring(1))),"漏发提示词会阻止发送");
        rejected(()->InputAudit.freeze(cfg,prompt,payload(cfg,prompt+" ")),"额外空白也会被逐字检查发现");
        JsonObject extra=payload(c,prompt);extra.addProperty("system","extra");rejected(()->InputAudit.freeze(cfg,prompt,extra),"额外系统提示词不通过自检");
        JsonObject duplicate=payload(c,prompt);duplicate.getAsJsonArray("messages").add(duplicate.getAsJsonArray("messages").get(0).deepCopy());rejected(()->InputAudit.freeze(cfg,prompt,duplicate),"额外消息不通过自检");
        JsonObject cap=payload(c,prompt);cap.addProperty("max_tokens",513);rejected(()->InputAudit.freeze(cfg,prompt,cap),"不允许悄悄提高输出上限");
        JsonObject unknown=payload(c,prompt);unknown.addProperty("unconfigured_input","x");rejected(()->InputAudit.freeze(cfg,prompt,unknown),"未声明输入字段被自检发现");
        Encoding enc=Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);check(enc.countTokensOrdinary("hello world")==2,"真实分词表 hello world 参考向量");
        check(enc.countTokensOrdinary("tiktoken is great!")==6,"真实分词表官方示例参考向量");
        check(InputAudit.sha256("abc".getBytes(StandardCharsets.UTF_8)).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"SHA256 标准参考向量");
    }
    static InputAudit.Evidence reliable(){InputAudit.Evidence e=new InputAudit.Evidence();e.contentVerified=true;e.wireBytesVerified=true;e.countReliable=true;e.expectedInputTokens=100;return e;}
    static void strictPolicy(){
        InputAudit.Evidence e=reliable();check(!InputAudit.shouldAlert(e,600,500),"差额刚好等于容许数量不告警");check(InputAudit.shouldAlert(e,601,500),"超过固定容许数量才告警");check(InputAudit.shouldAlert(e,101,0),"零容许量的边界正确");
        e.countReliable=false;check(!InputAudit.shouldAlert(e,999999,0),"即使数字极高也不拿估算判注入");e=reliable();e.contentVerified=false;check(!InputAudit.shouldAlert(e,9999,0),"APP内容自检未通过时不指向上游");e=reliable();e.wireBytesVerified=false;check(!InputAudit.shouldAlert(e,9999,0),"发送未确认时不作确定差额判断");e=reliable();check(!InputAudit.shouldAlert(e,0,0),"未回传用量不是正常或注入");check(!InputAudit.shouldAlert(e,1000,-1),"非法负容许量不产生误报");
        LineConfig c=config();RunData r=run("a",1,601);r.samples.get(0).inputAudit=reliable();TokenGuard.assess(r,c);check(r.inputTokenAlert&&r.samples.get(0).inputTokenAlert&&r.inputAuditVersion==2,"可靠参考触发样本和整轮标记");
        check(r.inputTokenBaseline==null&&r.inputTokenReferenceRuns==0,"新版不再依赖历史或手填参考");
        int evidenceCount=r.samples.get(0).evidence.size();TokenGuard.assess(r,c);check(evidenceCount==r.samples.get(0).evidence.size(),"重复检查不堆积同一条证据");
        c.tokenAuditEnabled=false;TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("关闭"),"关闭告警仍保留输入校验记录");
        c.tokenAuditEnabled=true;r.samples.get(0).inputAudit.countReliable=false;TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("估算"),"严格模式跳过估算告警");
        r.samples.get(0).inputAudit=reliable();r.samples.get(0).inTokens=0;TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("缺少有效"),"无usage明确未判断而不是通过");
        r.samples.get(0).inTokens=999;r.samples.get(0).requestOk=false;TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("缺少有效"),"失败请求不冒充用量核验成功");
        RunData legacy=Json.gson().fromJson("{\"id\":\"old\",\"samples\":[]}",RunData.class);check(legacy.inputAuditVersion==0,"旧记录不伪装经过新版复核");
        LineConfig old=Json.gson().fromJson("{\"name\":\"legacy\"}",LineConfig.class);check(old.inputTokenExcess==500&&old.tokenAuditEnabled,"旧线路补入安全默认值");
    }
    static void countingApi()throws Exception{
        LineConfig c=config();c.protocol="responses";
        List<JsonObject> sent=new ArrayList<>();ApiClient a=mock(r->{okio.Buffer buf=new okio.Buffer();try{r.body().writeTo(buf);}catch(IOException ex){throw new RuntimeException(ex);}sent.add(object(buf.readUtf8()));return response(r,200,"{\"object\":\"response.input_tokens\",\"input_tokens\":23}","application/json");});
        InputAudit.Frozen f=InputAudit.freeze(c,"original prompt",payload(c,"original prompt"));method("prepareTokenCount",LineConfig.class,InputAudit.Frozen.class).invoke(a,c,f);
        check(f.evidence.countReliable&&f.evidence.expectedInputTokens==23,"标准 Responses 预检可形成参考计数");
        check(sent.get(0).get("input").equals(object(f.evidence.bodyJson).get("input")),"预检与生成使用完全相同输入");check(!sent.get(0).has("max_output_tokens")&&!sent.get(0).has("stream"),"预检不包含输出预算与流式开关");
        a=mock(r->response(r,404,"missing","text/plain"));f=InputAudit.freeze(c,"prompt",payload(c,"prompt"));method("prepareTokenCount",LineConfig.class,InputAudit.Frozen.class).invoke(a,c,f);check(!f.evidence.countReliable&&f.evidence.countNote.contains("404"),"计数接口404只回退估算");
        for(String invalid:new String[]{"{\"input_tokens\":23}","{\"object\":\"other\",\"input_tokens\":23}","{\"object\":\"response.input_tokens\",\"input_tokens\":2.5}","{\"object\":\"response.input_tokens\",\"input_tokens\":0}"}){
            final String body=invalid;a=mock(r->response(r,200,body,"application/json"));f=InputAudit.freeze(c,"prompt",payload(c,"prompt"));method("prepareTokenCount",LineConfig.class,InputAudit.Frozen.class).invoke(a,c,f);check(!f.evidence.countReliable,"拒绝非标准或无效计数 "+invalid);
        }
        AtomicInteger calls=new AtomicInteger();a=mock(r->{calls.incrementAndGet();return response(r,200,"{}","application/json");});c.protocol="openai";f=InputAudit.freeze(c,"prompt",payload(c,"prompt"));method("prepareTokenCount",LineConfig.class,InputAudit.Frozen.class).invoke(a,c,f);check(calls.get()==0&&!f.evidence.countReliable,"Chat 不偷偷套用 Responses 计数语义");
    }
    static void parserAndUsage()throws Exception{
        ApiClient a=mock(r->response(r,200,successBody(),"application/json"));Class<?> type=result().getClass();Method usage=method("applyUsage",JsonObject.class,type);Object res=result();
        usage.invoke(a,object("{\"input_tokens\":120,\"cache_read_input_tokens\":400,\"cache_creation_input_tokens\":80}"),res);check((Integer)field(res,"inTokens")==600,"Anthropic额外缓存输入合并");
        usage.invoke(a,object("{\"output_tokens\":25}"),res);check((Integer)field(res,"inTokens")==600&&(Integer)field(res,"outTokens")==25,"输出-only事件不清零输入");
        res=result();usage.invoke(a,object("{\"prompt_tokens\":600,\"completion_tokens\":50,\"cache_read_input_tokens\":400,\"prompt_tokens_details\":{\"cached_tokens\":400}}"),res);check((Integer)field(res,"inTokens")==600,"Chat缓存不双算");
        res=result();usage.invoke(a,object("{\"input_tokens\":600,\"input_tokens_details\":{\"cached_tokens\":400}}"),res);check((Integer)field(res,"inTokens")==600,"Responses缓存不双算");
        res=result();usage.invoke(a,object("{\"input_tokens\":2147483647,\"cache_read_input_tokens\":2147483647}"),res);check((Integer)field(res,"inTokens")==Integer.MAX_VALUE,"极端输入数不会溢出为负");
        String stream="data: {\"type\":\"response.created\",\"response\":{\"id\":\"r1\",\"model\":\"model\"}}\n\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"<svg/>\"}\n\ndata: {\"type\":\"response.completed\",\"response\":{\"id\":\"r1\",\"model\":\"model\",\"usage\":{\"input_tokens\":30,\"output_tokens\":4},\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"<svg/>\"}]}]}}\n\n";
        Method parse=method("parseStreamReader",BufferedReader.class,type,long.class,ApiClient.ProgressListener.class);res=result();parse.invoke(a,new BufferedReader(new StringReader(stream)),res,System.currentTimeMillis(),null);check(field(res,"text").equals("<svg/>"),"Responses最终帧不会重复拼接已收到正文");check((Integer)field(res,"inTokens")==30,"Responses流式usage正确解析");
        String completedOnly="data: {\"type\":\"response.completed\",\"response\":{\"id\":\"r2\",\"model\":\"model\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"<svg/>\"}]}]}}\n\n";
        res=result();parse.invoke(a,new BufferedReader(new StringReader(completedOnly)),res,System.currentTimeMillis(),null);check(field(res,"text").equals("<svg/>"),"只有最终帧也能取得正文");
        res=result();method("parseNonStream",String.class,type).invoke(a,"{\"status\":\"incomplete\",\"output\":[]}",res);check(!field(res,"error").equals(""),"非流式不完整响应不冒充完成");
        LineConfig c=config();InputAudit.Evidence preview=a.preview(c,"literal prompt");check(preview.bodyJson.contains("\"max_tokens\":512"),"实际请求输出上限保持512");c.protocol="anthropic";c.effort="high";rejected(()->a.preview(c,"prompt"),"过大思考预算报配置错误而非增加输出上限");c.effort="none";check(a.preview(c,"prompt").bodyJson.contains("\"disabled\""),"关闭Anthropic思考是显式设置");
    }
    static void trends(){
        RunData a=run("a",1000,100),b=run("b",2000,200),c=run("a",3600000,110);List<RunData> rows=Arrays.asList(a,b,c);
        Set<String> chosen=TrendSeries.select(rows,Collections.emptySet(),0);check(chosen.size()==1&&chosen.contains(TrendMetrics.group(c)),"趋势默认只显示最近一个有效组");
        chosen=TrendSeries.select(rows,new LinkedHashSet<>(Arrays.asList(TrendMetrics.group(b))),0);check(chosen.size()==1&&chosen.contains(TrendMetrics.group(b)),"手选组不会被最新记录偷偷替换");
        check(TrendSeries.visible(rows,chosen).size()==1,"未选中的其他组不会混入图表");
        Map<String,Integer> oldColors=new HashMap<>();oldColors.put("a",1);oldColors.put("b",1);
        Map<String,Integer> distinct=TrendSeries.colors(new LinkedHashSet<>(Arrays.asList("a","b")),oldColors,new int[]{1,2,3});
        check(new HashSet<>(distinct.values()).size()==2,"分别单选过的两个组再同图时仍使用不同颜色");
        check(distinct.get("a")==1,"无冲突分组保留原颜色");

        chosen=new LinkedHashSet<>(Arrays.asList(TrendMetrics.group(a),TrendMetrics.group(b)));check(TrendSeries.visible(rows,chosen).size()==3,"勾选后才能进行多组对比");
        near(TrendSeries.x(c,2,false,1000),2,"等间距模式不被小时空档挤压");near(TrendSeries.x(c,2,true,1000),(3600000-1000)/60000.0,"真实时间模式保留时间间隔");
        near(TrendMetrics.value(a,0),90,"复杂度指标正确");near(TrendMetrics.value(a,1),1,"耗时指标正确");near(TrendMetrics.value(a,2),100,"输入Token指标正确");near(TrendMetrics.value(a,3),20,"输出Token指标正确");near(TrendMetrics.value(a,4),5,"思考Token指标正确");
        a.samples.get(0).reasonTokens=null;check(Double.isNaN(TrendMetrics.value(a,4)),"未回传思考不绘为0");a.samples.get(0).reasonTokens=0;near(TrendMetrics.value(a,4),0,"明确回传0保留0");a.samples.get(0).inTokens=0;check(Double.isNaN(TrendMetrics.value(a,2)),"缺失输入用量留空");
        a.samples.add(run("a",3000,800).samples.get(0));a.samples.get(0).inTokens=100;near(TrendMetrics.value(a,2),450,"多样本取中位数而不是相加");
        String group=TrendMetrics.group(a);a.promptKey="different";check(!TrendMetrics.group(a).equals(group),"不同题目严格分组");
        a.status="RUNNING";check(Double.isNaN(TrendMetrics.value(a,0)),"进行中不计入完成趋势");
        check("—".equals(TrendMetrics.format(Double.NaN,0,true)),"空统计显示破折号");near(TokenGuard.median(Arrays.asList(4d,1d,3d,2d)),2.5,"偶数中位数正确");
    }
    public static void main(String[] args)throws Exception{requestInputs();models();audit();strictPolicy();countingApi();parserAndUsage();trends();System.out.println("\nPassed "+passed+" Pelican 2.3 local regression checks. Network replaced by fixtures; no device UI claims.");}
}
