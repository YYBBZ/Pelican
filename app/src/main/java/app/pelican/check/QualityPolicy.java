package app.pelican.check;
import java.util.*;

/** One source for effective quality thresholds and quality-only notification decisions. */
public final class QualityPolicy {
    private QualityPolicy(){}
    public static Double lower(LineConfig line,Baseline baseline){
        if(line.baselineLowerOverride!=null)return line.baselineLowerOverride;
        return baseline==null||baseline.lower==null?null:Math.min(100,baseline.lower*line.complexityMultiplier);
    }
    public static String number(double n){return java.math.BigDecimal.valueOf(n).setScale(3,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    public static String equation(LineConfig line,Baseline baseline){
        if(line.baselineLowerOverride!=null)return "生效下限 "+number(line.baselineLowerOverride)+"（手动固定值，不乘倍率）";
        if(baseline==null||baseline.lower==null)return "自动基线 "+(baseline==null?0:baseline.values.size())+" / 5 轮 · 倍率 "+number(line.complexityMultiplier)+"，尚未形成下限";
        double raw=baseline.lower*line.complexityMultiplier;
        return "生效下限 "+number(baseline.lower)+" × "+number(line.complexityMultiplier)+" = "+number(Math.min(100,raw))+(raw>100?"（已按 100 封顶）":"");
    }
    public static String status(RunData run){
        if(run.interrupted||run.samples==null||run.samples.isEmpty())return "FAIL";
        boolean failed=false,low=false;
        for(RunData.SampleData s:run.samples){
            if(s==null||!s.requestOk){failed=true;continue;}
            if(!s.svgOk||(s.cheats!=null&&!s.cheats.isEmpty())||(s.modelEcho!=null&&!s.modelEcho.isEmpty()&&!s.modelEcho.equals(run.model)))return "ERROR";
            if(run.lowerUsed!=null&&s.score<run.lowerUsed)low=true;
        }
        if(failed)return "FAIL";
        return low?"SUSPECT":"PASS";
    }
    public static boolean isQualityIssue(RunData run){String status=run.qualityStatus;if(status==null||status.isEmpty())status=status(run);return "SUSPECT".equals(status)||"ERROR".equals(status);}
    public static int nextConsecutive(int previous,RunData run){
        if(isQualityIssue(run))return previous+1;
        String status=run.qualityStatus;
        return "PASS".equals(status)?0:previous;
    }
}
