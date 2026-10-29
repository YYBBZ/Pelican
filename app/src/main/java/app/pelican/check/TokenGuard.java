package app.pelican.check;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** User-configured heuristic screening, never proof of prompt injection. */
public final class TokenGuard {
    public static final int VERSION=4;
    public static final String DISCLAIMER="命中输入异常规则，疑似注入，不能据此确认";
    private TokenGuard(){}
    public static String signature(LineConfig line,String prompt){return InputAudit.sha256((line.baseUrl.trim()+"\n"+line.protocol+"\n"+line.model+"\n"+line.effort+"\n"+prompt).getBytes(StandardCharsets.UTF_8));}
    public static double median(List<Double> values){if(values.isEmpty())return Double.NaN;List<Double> sorted=new ArrayList<>(values);Collections.sort(sorted);int n=sorted.size();return n%2==1?sorted.get(n/2):(sorted.get(n/2-1)+sorted.get(n/2))/2;}
    public static double inputMedian(RunData run){List<Double> v=new ArrayList<>();for(RunData.SampleData s:run.samples)if(s.requestOk&&s.inTokens>0)v.add((double)s.inTokens);return median(v);}
    public static void assess(RunData run,LineConfig line){
        run.inputAuditVersion=VERSION;run.inputTokenAlert=false;run.inputTokenBaseline=null;run.inputTokenReferenceRuns=0;
        run.inputTokenExcessUsed=line.inputTokenExcess;run.inputTokenModeUsed=line.inputTokenMode;run.inputTokenRatioUsed=line.inputTokenRatio;
        int evaluated=0,unavailable=0;
        for(RunData.SampleData s:run.samples){
            evaluate(s,line);
            if("ALERT".equals(s.inputDecision)){run.inputTokenAlert=true;evaluated++;}
            else if("NORMAL".equals(s.inputDecision))evaluated++;
            else unavailable++;
        }
        if(!line.tokenAuditEnabled)run.inputTokenNote="输入 Token 异常筛查已关闭，保留实际请求与用量记录";
        else if(run.inputTokenAlert)run.inputTokenNote=DISCLAIMER+(unavailable>0?"；另有 "+unavailable+" 张样本无法判定":"");
        else if(unavailable>0||evaluated==0)run.inputTokenNote="部分或全部样本无法判定（缺少用量或实际请求依据），未将缺失数据当作通过";
        else run.inputTokenNote="未命中所选输入异常筛查规则（不等于确认没有注入）";
    }
    public static void evaluate(RunData.SampleData s,LineConfig line){
        s.inputTokenAlert=false;s.inputTokenReference=null;s.inputDifference=null;s.inputRuleUsed="";
        if(s.evidence==null)s.evidence=new ArrayList<>();
        s.evidence.removeIf(note->note!=null&&(note.startsWith("输入 Token 差额异常：")||note.startsWith("输入异常筛查：")));
        InputAudit.Evidence e=s.inputAudit;
        if(e!=null&&e.estimatedInputTokens>0)s.inputDifference=(long)s.inTokens-e.estimatedInputTokens;
        if(!line.tokenAuditEnabled){s.inputDecision="DISABLED";s.inputDecisionNote="本线路输入异常筛查已关闭";return;}
        if(s.inTokens<=0){unavailable(s,"接口未回传有效输入用量");return;}
        if(e==null||!e.contentVerified){unavailable(s,"缺少通过逐字核验的实际请求，不把 APP 自身输入问题归为上游注入");return;}
        if(e.bodyJson!=null&&!e.bodyJson.isEmpty()){
            byte[] bytes=e.bodyJson.getBytes(StandardCharsets.UTF_8);
            if(e.bodyBytes!=bytes.length||(e.bodySha256!=null&&!e.bodySha256.isEmpty()&&!e.bodySha256.equals(InputAudit.sha256(bytes)))){
                unavailable(s,"保存的实际请求字节或校验值不一致，无法判定");return;
            }
        }
        boolean alert;
        if(e.estimatedInputTokens>0){
            s.inputTokenReference=(double)e.estimatedInputTokens;
            if("ratio".equals(line.inputTokenMode)){
                s.inputRuleUsed="按倍率：接口回报 ≥ 本地估算 × "+QualityPolicy.number(line.inputTokenRatio);
                alert=Double.isFinite(line.inputTokenRatio)&&line.inputTokenRatio>=1&&s.inTokens>=e.estimatedInputTokens*line.inputTokenRatio;
            }else{
                s.inputRuleUsed="按数量：接口回报 − 本地估算 ＞ "+line.inputTokenExcess+" Token";
                alert=line.inputTokenExcess>=0&&(long)s.inTokens-e.estimatedInputTokens>line.inputTokenExcess;
            }
        }else{
            if(e.bodyBytes<=0){unavailable(s,"估算不可用且实际请求正文字节数缺失");return;}
            s.inputRuleUsed="补充字节规则：本地估算不可用或为 0，接口回报输入 Token ＞ 本次请求正文 "+e.bodyBytes+" UTF-8 字节";
            alert=s.inTokens>e.bodyBytes;
        }
        s.inputTokenAlert=alert;s.inputDecision=alert?"ALERT":"NORMAL";
        s.inputDecisionNote=alert?DISCLAIMER:"未命中当前筛查规则";
        if(alert)s.evidence.add("输入异常筛查："+s.inputRuleUsed+"；"+DISCLAIMER);
    }
    private static void unavailable(RunData.SampleData s,String why){s.inputDecision="UNAVAILABLE";s.inputDecisionNote="无法判定："+why;}
    public static String detail(RunData.SampleData s){
        InputAudit.Evidence e=s.inputAudit;
        return "本地估算数量："+(e!=null&&e.estimatedInputTokens>0?e.estimatedInputTokens+" Token":"不可用")
          +"\n接口回报数量："+(s.inTokens>0?s.inTokens+" Token":"未回传")
          +"\n两者差额："+(s.inputDifference==null?"不可计算":s.inputDifference+" Token")
          +"\n判定方式及容许值："+(s.inputRuleUsed==null||s.inputRuleUsed.isEmpty()?"未执行":s.inputRuleUsed)
          +"\n最终判定："+(s.inputTokenAlert?"异常 · ":"")+(s.inputDecisionNote==null||s.inputDecisionNote.isEmpty()?"旧记录，尚未重判":s.inputDecisionNote);
    }
    /** No network, no notification, no baseline edits, and the complete pre-migration record stays intact. */
    public static boolean reassess(RunData run,LineConfig line){
        if(run==null||run.isActive()||run.inputAuditVersion>=VERSION)return false;
        if(run.originalRunJson==null||run.originalRunJson.isEmpty())run.originalRunJson=Json.gson().toJson(run);
        run.originalStatus=run.status;String originalSummary=run.summary;
        run.qualityStatus=QualityPolicy.status(run);
        assess(run,line);int evaluated=0,missing=0;
        for(RunData.SampleData s:run.samples){if("ALERT".equals(s.inputDecision)||"NORMAL".equals(s.inputDecision))evaluated++;else missing++;}
        run.reassessedAt=System.currentTimeMillis();
        run.reassessmentNote=evaluated==0?"无法重判：缺少有效输入用量或本地请求依据":("按 2.4 规则重新判定"+(missing>0?"；部分样本无法重判":""));
        if(evaluated>0){run.status=run.inputTokenAlert?"ERROR":run.qualityStatus;run.summary=(run.inputTokenAlert?"输入 Token 异常 / 疑似注入；":"")+"按 2.4 规则重新判定；原记录摘要："+originalSummary;}
        return true;
    }
}
