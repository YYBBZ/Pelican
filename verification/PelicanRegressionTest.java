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
    static InputAudit.Evidence estimateEvidence(int estimate,int bytes){InputAudit.Evidence e=new InputAudit.Evidence();e.estimatedInputTokens=estimate;e.bodyBytes=bytes;e.contentVerified=true;e.wireBytesVerified=true;return e;}
    static RunData fixture(int reported,int estimate){RunData r=run("a",1,reported);r.model="model";r.samples.get(0).inputAudit=estimateEvidence(estimate,300);return r;}
    static void strictPolicy(){
        LineConfig c=config();c.inputTokenExcess=800;
        RunData r=fixture(900,100);TokenGuard.assess(r,c);check(!r.inputTokenAlert,"差额刚好800不告警");
        r=fixture(901,100);TokenGuard.assess(r,c);check(r.inputTokenAlert,"差额801超过800触发");
        r=fixture(26688,26);TokenGuard.assess(r,c);check(r.inputTokenAlert,"复现26对26688明确出现输入异常");
        check(r.samples.get(0).inputDifference==26662,"详情差额准确");check(r.inputAuditVersion==4&&r.inputTokenNote.contains("不能据此确认"),"版本与疑似免责声明正确");
        int count=r.samples.get(0).evidence.size();TokenGuard.assess(r,c);check(count==r.samples.get(0).evidence.size(),"重复判定不堆积证据");
        c.inputTokenMode="ratio";c.inputTokenRatio=5;
        r=fixture(499,100);TokenGuard.assess(r,c);check(!r.inputTokenAlert,"低于5倍不命中");
        r=fixture(500,100);TokenGuard.assess(r,c);check(r.inputTokenAlert,"刚好5倍即命中");
        c.inputTokenExcess=1000000;TokenGuard.assess(r,c);check(r.inputTokenAlert,"倍率模式不同时依赖数量阈值");
        c.inputTokenMode="amount";TokenGuard.assess(r,c);check(!r.inputTokenAlert,"数量模式不同时依赖倍率");
        c.inputTokenExcess=800;r=fixture(301,0);TokenGuard.assess(r,c);check(r.inputTokenAlert&&r.samples.get(0).inputRuleUsed.contains("补充字节"),"估算0时按字节补充规则");
        r=fixture(300,0);TokenGuard.assess(r,c);check(!r.inputTokenAlert,"接口输入等于字节数不命中");
        r=fixture(299,-1);TokenGuard.assess(r,c);check(!r.inputTokenAlert,"估算不可用时字节规则正常边界");
        r=fixture(0,100);TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.samples.get(0).inputDecision.equals("UNAVAILABLE"),"缺失输入用量明确无法判定");
        r=fixture(1000,0);r.samples.get(0).inputAudit.bodyBytes=0;TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("无法判定"),"估算和字节数均缺失不当作通过");
        r=fixture(1000,100);r.samples.get(0).inputAudit.contentVerified=false;TokenGuard.assess(r,c);check(!r.inputTokenAlert,"本机内容未校验不误判上游");
        r=fixture(1000,100);r.samples.get(0).inputAudit=null;TokenGuard.assess(r,c);check(!r.inputTokenAlert,"没有发送证据不能凭空重建参考");
        c.tokenAuditEnabled=false;r=fixture(10000,100);TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.inputTokenNote.contains("关闭"),"线路可关闭输入筛查");
        c.tokenAuditEnabled=true;r=fixture(100,100);r.samples.add(fixture(5000,100).samples.get(0));TokenGuard.assess(r,c);check(r.inputTokenAlert,"任一张异常则整轮输入异常");
        r.qualityStatus="PASS";check(!QualityPolicy.isQualityIssue(r),"输入异常单独不算连续质量异常");check(QualityPolicy.nextConsecutive(3,r)==0,"输入异常但质量通过时不增加质量计数");
        r.qualityStatus="SUSPECT";check(QualityPolicy.isQualityIssue(r)&&QualityPolicy.nextConsecutive(3,r)==4,"同时质量偏低仍计入原通知规则");
        LineConfig old=LineConfig.fromJson("{\"name\":\"legacy\",\"promptId\":\"pelican_zh\"}");check(old.inputTokenExcess==800&&old.inputTokenRatio==5&&old.complexityMultiplier==.9,"旧线路缺字段按新版默认值补齐");
        check(old.promptSequence.size()==1&&PromptSequence.current(old).id.equals("pelican_zh"),"旧固定/轮换均按所选题初始化单条顺序");
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
    static void reasoningAndLimits()throws Exception{
        ApiClient api=mock(r->response(r,200,successBody(),"application/json"));
        for(String protocol:new String[]{"openai","responses","anthropic"})for(String effort:ReasoningPolicy.VALUES){
            LineConfig c=LineConfig.createNew();c.model="fixture-model";c.baseUrl="https://fixture.invalid/v1";c.apiKey="fixture";c.protocol=protocol;c.effort=effort;c.maxTokens=100000;
            JsonObject sent=object(api.preview(c,"plain prompt").bodyJson);
            check(sent.get("model").getAsString().equals(c.model),protocol+" / "+effort+" 模型字段保留");
            if(effort.equals("auto"))check(!sent.has("reasoning_effort")&&!sent.has("reasoning")&&!sent.has("thinking"),protocol+" auto 不发送思考参数");
            else if(protocol.equals("anthropic")){
                if(effort.equals("none"))check(sent.getAsJsonObject("thinking").get("type").getAsString().equals("disabled"),"Anthropic none 显式关闭");
                else check(sent.getAsJsonObject("thinking").get("budget_tokens").getAsInt()==ReasoningPolicy.budget(c),"Anthropic "+effort+" 预算与显示一致");
            }else{String value=protocol.equals("responses")?sent.getAsJsonObject("reasoning").get("effort").getAsString():sent.get("reasoning_effort").getAsString();check(value.equals(effort),protocol+" "+effort+" 英文原样发送");}
            check(ReasoningPolicy.description(c).contains(effort.equals("auto")?"不包含":"实际发送"),protocol+" "+effort+" 有实际发送说明");
        }
        LineConfig c=LineConfig.createNew();c.protocol="anthropic";c.effort="xhigh";c.maxTokens=32000;check(ReasoningPolicy.budget(c)==32000,"xhigh 固定32000预算");rejected(()->c.validateSettings(),"输出刚好等于思考预算不能保存");c.maxTokens=32001;c.validateSettings();check(c.maxTokens==32001,"输出大于预算可保存，不自动抬升");
        c.effort="max";check(ReasoningPolicy.budget(c)==64000,"max 固定64000预算");c.maxTokens=64001;c.validateSettings();
        c.protocol="openai";c.effort="auto";c.sampleCount=10;c.retryCount=10;c.validateSettings();check(c.sampleCount==10&&RetryPolicy.maxAttempts(c.retryCount)==11,"10张和10次补试通过校验且不隐藏截断");
        c.sampleCount=99;c.retryCount=20;c.validateSettings();check(RetryPolicy.maxAttempts(20)==21,"99张和20次补试最大边界");c.sampleCount=100;rejected(()->c.validateSettings(),"样本100拒绝");c.sampleCount=0;rejected(()->c.validateSettings(),"样本0拒绝");c.sampleCount=1;c.retryCount=21;rejected(()->c.validateSettings(),"补试21拒绝");c.retryCount=-1;rejected(()->c.validateSettings(),"补试负数拒绝");
        check(!RetryPolicy.retryStatus(503,"unsupported reasoning_effort xhigh"),"上游503携带明确参数错误也不补试");check(!RetryPolicy.retryStatus(400,"invalid parameter max"),"400参数错误不补试");check(RetryPolicy.retryStatus(503,"temporarily overloaded"),"真正临时503继续按原规则补试");
    }
    static void thresholdsAndSequence(){
        LineConfig line=LineConfig.createNew();Baseline base=new Baseline();base.lower=87d;
        near(QualityPolicy.lower(line,base),78.3,"自动87乘默认0.9等于78.3");check(QualityPolicy.equation(line,base).contains("87 × 0.9 = 78.3"),"生效下限直接显示算式");
        line.complexityMultiplier=1.5;near(QualityPolicy.lower(line,base),100,"高倍率结果封顶100");check(QualityPolicy.equation(line,base).contains("封顶"),"封顶明确标注");line.baselineLowerOverride=42d;near(QualityPolicy.lower(line,base),42,"手填固定值优先不再乘倍率");check(QualityPolicy.equation(line,base).contains("不乘倍率"),"固定值说明正确");
        line=LineConfig.createNew();String[] expected={"pelican_en","pelican_zh","saturn","garfield","pelican_en"};
        for(int i=0;i<expected.length;i++){
            RunData r=new RunData();PromptSequence.capture(line,r);check(r.promptKey.equals(expected[i]),"按轮次循环第"+i+"题，无关时钟");
            r.samples.add(fixture(50000,26).samples.get(0));r.samples.get(0).svgOk=false;r.inputTokenAlert=true;r.status="ERROR";
            PromptSequence.finish(line,r);check(r.sequenceAdvanced,"请求完整即前移，不受SVG或输入异常影响");
            int next=line.sequenceIndex;PromptSequence.finish(line,r);check(line.sequenceIndex==next,"同一轮重复结束不重复推进");
        }
        RunData failed=new RunData();PromptSequence.capture(line,failed);failed.samples.add(new RunData.SampleData());int index=line.sequenceIndex;PromptSequence.finish(line,failed);check(line.sequenceIndex==index&&line.sequenceBlocked,"整轮全部请求失败不前移");check(line.sequenceFailureNote.contains("未前移"),"失败停留有明确说明");
        line.promptSequence.add(new PromptSequence.Entry("custom-test","自己命名","保持\n原样提示词", ""));line.sequenceIndex=line.promptSequence.size()-1;
        RunData custom=new RunData();PromptSequence.capture(line,custom);check(custom.promptLabel.equals("自己命名")&&custom.promptTextUsed.equals("保持\n原样提示词"),"自定义题目可执行并保存名称正文");
        line.intervalMin=20;check(line.promptForRun(0).id.equals(line.promptForRun(Long.MAX_VALUE).id),"间隔和时间不改变所选题目");
        LineConfig restored=LineConfig.fromJson(Json.gson().toJson(line));check(restored.sequenceIndex==line.sequenceIndex&&PromptSequence.current(restored).id.equals("custom-test"),"顺序位次持久化恢复");
        RunData oldQueued=new RunData();PromptSequence.capture(restored,oldQueued);String locked=oldQueued.promptTextUsed;restored.promptSequence.get(restored.sequenceIndex).text="new edit";oldQueued.samples.add(fixture(100,26).samples.get(0));int before=restored.sequenceIndex;PromptSequence.finish(restored,oldQueued);check(oldQueued.promptTextUsed.equals(locked)&&restored.sequenceIndex==before,"运行时修改顺序表不改变本轮提示词或覆盖新安排");
        LineConfig independent=LineConfig.createNew();check(independent.sequenceIndex==0,"各条线路位次互不影响");
    }
    static void rejudge(){
        LineConfig c=LineConfig.createNew();c.inputTokenExcess=800;RunData r=fixture(26688,26);String raw=Json.gson().toJson(r);check(TokenGuard.reassess(r,c),"旧记录可本地重判");check(r.status.equals("ERROR")&&r.inputTokenAlert,"旧通过记录重判后变错误并标注注入");check(r.originalRunJson.equals(raw)&&r.originalStatus.equals("PASS"),"原始记录和原始状态完整保留");check(r.reassessmentNote.equals("按 2.4 规则重新判定"),"重判注明规则版本");check(!TokenGuard.reassess(r,c),"同版本迁移不会重复覆写原记录");
        r=fixture(900,26);c.inputTokenExcess=1000;TokenGuard.reassess(r,c);check(!r.inputTokenAlert,"重判采用线路自己的容许数量");r=fixture(900,26);TokenGuard.reassess(r,LineConfig.createNew());check(r.inputTokenAlert,"线路缺失时可采用默认800");
        c.inputTokenMode="ratio";c.inputTokenRatio=5;r=fixture(130,26);TokenGuard.reassess(r,c);check(r.inputTokenAlert,"旧记录使用所属线路倍率设置");
        r=fixture(900,26);r.samples.get(0).inputAudit=null;String status=r.status;TokenGuard.reassess(r,c);check(r.reassessmentNote.startsWith("无法重判")&&r.status.equals(status),"旧记录缺依据明确无法重判且不捏造新状态");
        r=fixture(900,26);r.status="RUNNING";check(!TokenGuard.reassess(r,c),"正在检测的记录不参与迁移");
        r=fixture(900,26);r.samples.get(0).inputAudit.bodyJson="x";TokenGuard.assess(r,c);check(!r.inputTokenAlert&&r.samples.get(0).inputDecision.equals("UNAVAILABLE"),"正文证据损坏时不按其误报");
    }
    public static void main(String[] args)throws Exception{requestInputs();models();audit();strictPolicy();parserAndUsage();trends();reasoningAndLimits();thresholdsAndSequence();rejudge();System.out.println("\nPassed "+passed+" Pelican 2.4 local regression checks. Network replaced by fixtures; no device UI claims.");}
}
