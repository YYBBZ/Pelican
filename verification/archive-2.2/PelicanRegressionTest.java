import app.pelican.check.*;
import com.google.gson.*;
import okhttp3.*;
import java.util.*;
import java.lang.reflect.*;

/** Local deterministic fixtures only. No user keys, no real relay and no claimed device test. */
public class PelicanRegressionTest {
    private static int passed;
    private static void check(boolean ok,String title){if(!ok)throw new AssertionError(title);passed++;System.out.println("PASS "+title);}
    private static void near(double a,double b,String title){check(Math.abs(a-b)<.0001,title);}
    private static RunData run(int input,long time){
        RunData r=new RunData();r.lineId="line-A";r.model="model-A";r.promptKey="prompt-A";r.inputTokenSignature="sig-A";r.startedAt=time;r.endedAt=time+1000;r.status="PASS";
        RunData.SampleData s=new RunData.SampleData();s.requestOk=true;s.svgOk=true;s.score=90;s.inTokens=input;s.outTokens=200;s.reasonTokens=20;r.score=90;r.samples.add(s);return r;
    }
    private static List<RunData> history(){return new ArrayList<>(Arrays.asList(run(100,3),run(110,2),run(90,1)));}
    private static Object field(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    public static void main(String[] args)throws Exception{
        check(!TokenGuard.exceeds(199,100,2),"低于倍数不告警");
        check(!TokenGuard.exceeds(200,100,2),"小数量翻倍但差值不足256不误报");
        check(!TokenGuard.exceeds(455,200,2),"差255不误报");
        check(TokenGuard.exceeds(456,200,2),"达到倍数且差256告警");
        check(!TokenGuard.exceeds(1000,0,2),"零参考值不可判定");
        check(!TokenGuard.exceeds(Double.NaN,100,2),"缺失数据不告警");
        near(TokenGuard.median(Arrays.asList(3d,1d,2d)),2,"奇数中位数");
        near(TokenGuard.median(Arrays.asList(4d,1d,3d,2d)),2.5,"偶数中位数");
        check(Double.isNaN(TokenGuard.median(Collections.emptyList())),"空中位数为缺失");
        RunData now=run(1000,10);TokenGuard.assess(now,history(),null,2);
        check(now.inputTokenAlert&&now.samples.get(0).inputTokenAlert,"自动基线标记整轮与样本");
        near(now.inputTokenBaseline,100,"历史中位数参考");
        check(now.inputTokenReferenceRuns==3,"参考轮数准确");
        check(now.samples.get(0).evidence.get(0).contains("疑似注入"),"证据包含明确但非定论的告警");
        check(now.samples.get(0).evidence.get(0).contains("不能单凭此确认"),"不把用量异常当成确证");
        now=run(105,10);TokenGuard.assess(now,history(),null,2);check(!now.inputTokenAlert,"正常波动不告警");
        now=run(1000,10);TokenGuard.assess(now,history().subList(0,2),null,2);check(!now.inputTokenAlert&&now.inputTokenBaseline==null,"不足3轮不伪称有基线");
        check(now.inputTokenNote.contains("不足时不判为正常"),"基线不足明确说明");
        now=run(1000,10);List<RunData> old=history();old.get(0).model="different";TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"不同模型不混入参考");
        old=history();old.get(0).lineId="different";TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"不同线路不混入参考");
        old=history();old.get(0).promptKey="different";TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"不同题目不混入参考");
        old=history();old.get(0).inputTokenSignature="different";TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"改过提示词或请求配置后重建参考");
        old=history();old.get(0).inputTokenSignature=null;TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"旧记录无指纹时不误比较");
        old=history();old.get(0).inputTokenAlert=true;TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"已知异常不污染参考");
        old=history();old.get(0).status="RUNNING";TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"运行中的数据不作参考");
        old=history();old.get(0).startedAt=20;TokenGuard.assess(now,old,null,2);check(now.inputTokenBaseline==null,"不会引用未来记录");
        TokenGuard.assess(now,Collections.emptyList(),100d,2);check(now.inputTokenAlert,"手动参考首次即可检测");
        TokenGuard.assess(now,Collections.emptyList(),100d,11);near(now.inputTokenMultiplier,2,"非法倍数回退默认");
        RunData multi=run(100,10);multi.samples.add(run(100,11).samples.get(0));multi.samples.add(run(100,12).samples.get(0));TokenGuard.assess(multi,history(),null,2);check(!multi.inputTokenAlert,"样本数变化不会让输入数量虚假翻倍");
        multi.samples.add(run(1000,13).samples.get(0));TokenGuard.assess(multi,history(),null,2);check(multi.inputTokenAlert,"单个样本输入突增不会被整轮平均掩盖");
        RunData noUsage=run(0,10);TokenGuard.assess(noUsage,history(),null,2);check(!noUsage.inputTokenAlert&&noUsage.inputTokenNote.contains("无法判断"),"缺失usage明确为无法判断");
        near(TrendMetrics.value(multi,2),100,"趋势输入按每样本中位数");
        RunData metrics=run(120,10);near(TrendMetrics.value(metrics,0),90,"复杂度指标");near(TrendMetrics.value(metrics,1),1,"整轮耗时指标");near(TrendMetrics.value(metrics,3),200,"输出Token指标");near(TrendMetrics.value(metrics,4),20,"思考Token指标");
        metrics.samples.get(0).reasonTokens=null;check(Double.isNaN(TrendMetrics.value(metrics,4)),"未回传思考Token不绘成0");
        metrics.samples.get(0).reasonTokens=0;near(TrendMetrics.value(metrics,4),0,"明确回传0的思考Token保留0");
        metrics.samples.get(0).inTokens=0;check(Double.isNaN(TrendMetrics.value(metrics,2)),"输入Token未回传不绘成0");
        metrics.endedAt=0;check(Double.isNaN(TrendMetrics.value(metrics,1)),"未完成耗时不绘成负数");
        metrics.status="RUNNING";check(Double.isNaN(TrendMetrics.value(metrics,0)),"进行中不纳入完成趋势");
        RunData groupA=run(100,1),groupB=run(100,2);check(TrendMetrics.group(groupA).equals(TrendMetrics.group(groupB)),"同组可连接");groupB.promptKey="other";check(!TrendMetrics.group(groupA).equals(TrendMetrics.group(groupB)),"不同题目不连线");
        check("—".equals(TrendMetrics.format(Double.POSITIVE_INFINITY,0,true)),"空统计不显示Infinity");
        check(TrendMetrics.niceStep(0)==1&&TrendMetrics.niceStep(100)>0,"刻度边界有效");
        LineConfig line=new LineConfig();line.baseUrl="https://fixture.invalid";line.model="fixture";line.apiKey="fixture-key";
        String sig=TokenGuard.signature(line,"prompt");check(sig.length()==64&&!sig.contains("fixture-key"),"请求指纹不包含明文密钥");
        line.apiKey="different-key";check(sig.equals(TokenGuard.signature(line,"prompt")),"更换密钥不改变输入参考");
        check(!sig.equals(TokenGuard.signature(line,"different-prompt")),"改提示词会改变指纹");
        line.effort="none";check(!sig.equals(TokenGuard.signature(line,"prompt")),"改思考强度会改变指纹");
        String json=Json.gson().toJson(now);RunData back=Json.gson().fromJson(json,RunData.class);check(back.inputTokenAlert&&back.samples.get(0).inputTokenAlert,"告警与证据可持久化往返");
        RunData legacy=Json.gson().fromJson("{\"id\":\"legacy\",\"samples\":[]}",RunData.class);check(!legacy.inputTokenAlert&&legacy.inputTokenNote!=null,"旧JSON兼容新增字段");
        Class<?> resultClass=Class.forName("app.pelican.check.ApiClient$GenResult");Constructor<?> cons=resultClass.getDeclaredConstructor();cons.setAccessible(true);Object result=cons.newInstance();
        ApiClient api=new ApiClient(null,new OkHttpClient.Builder().build());Method usage=ApiClient.class.getDeclaredMethod("applyUsage",JsonObject.class,resultClass);usage.setAccessible(true);
        usage.invoke(api,JsonParser.parseString("{\"input_tokens\":120,\"cache_read_input_tokens\":400,\"cache_creation_input_tokens\":80}").getAsJsonObject(),result);
        check((Integer)field(result,"inTokens")==600,"Anthropic缓存输入计入总输入");
        usage.invoke(api,JsonParser.parseString("{\"output_tokens\":25}").getAsJsonObject(),result);
        check((Integer)field(result,"inTokens")==600,"后续只有输出usage时不清零输入");check((Integer)field(result,"outTokens")==25,"后续输出usage正确更新");
        result=cons.newInstance();usage.invoke(api,JsonParser.parseString("{\"prompt_tokens\":600,\"completion_tokens\":50,\"cache_read_input_tokens\":400,\"prompt_tokens_details\":{\"cached_tokens\":400}}").getAsJsonObject(),result);
        check((Integer)field(result,"inTokens")==600,"OpenAI已含缓存的prompt_tokens不重复相加");
        result=cons.newInstance();usage.invoke(api,JsonParser.parseString("{\"input_tokens\":600,\"input_tokens_details\":{\"cached_tokens\":400},\"output_tokens\":20}").getAsJsonObject(),result);
        check((Integer)field(result,"inTokens")==600,"Responses已含缓存的input_tokens不重复相加");
        usage.invoke(api,new JsonObject(),result);check((Integer)field(result,"inTokens")==600&&(Integer)field(result,"outTokens")==20,"空usage不覆盖已有数字");
        result=cons.newInstance();usage.invoke(api,JsonParser.parseString("{\"input_tokens\":2147483647,\"cache_read_input_tokens\":2147483647}").getAsJsonObject(),result);check((Integer)field(result,"inTokens")==Integer.MAX_VALUE,"极端usage不会整数溢出为负数");
        System.out.println("\nPassed "+passed+" Pelican local regression checks. No real relay contacted.");
    }
}
