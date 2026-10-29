package app.pelican.check;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure data helpers shared by charts and regression tests. Token metrics are per-sample medians. */
public final class TrendMetrics {
    public static final String[] TITLES={"复杂度","总耗时","输入 Token","输出 Token","思考 Token"};
    private TrendMetrics(){}
    public static double value(RunData run,int metric){
        if(run.isActive())return Double.NaN;
        if(metric==0)return run.hasScore()?run.score:Double.NaN;
        if(metric==1)return run.endedAt>run.startedAt?(run.endedAt-run.startedAt)/1000.0:Double.NaN;
        List<Double> values=new ArrayList<>();
        for(RunData.SampleData sample:run.samples){
            if(!sample.requestOk)continue;
            if(metric==2&&sample.inTokens>0)values.add((double)sample.inTokens);
            if(metric==3&&sample.outTokens>0)values.add((double)sample.outTokens);
            if(metric==4&&sample.reasonTokens!=null&&sample.reasonTokens>=0)values.add(sample.reasonTokens.doubleValue());
        }
        return TokenGuard.median(values);
    }
    public static String group(RunData run){
        return run.lineId+"\n"+run.model+"\n"+run.promptKey+"\n"+(run.inputTokenSignature==null?"":run.inputTokenSignature);
    }
    public static String format(double value,int metric,boolean compact){
        if(Double.isNaN(value)||Double.isInfinite(value))return "—";
        if(compact&&Math.abs(value)>=1000000)return String.format(Locale.CHINA,"%.1fM",value/1000000);
        if(compact&&Math.abs(value)>=10000)return String.format(Locale.CHINA,"%.1fk",value/1000);
        if(metric==1)return String.format(Locale.CHINA,"%.1f",value);
        return String.format(Locale.CHINA,compact?"%.0f":"%,.0f",value);
    }
    public static String unit(int metric){return metric==0?"分":metric==1?"秒":"Token / 样本";}
    public static double niceStep(double span){
        if(!(span>0)||!Double.isFinite(span))return 1;
        double raw=span/4.0,base=Math.pow(10,Math.floor(Math.log10(raw))),x=raw/base;
        return (x<=1?1:x<=2?2:x<=2.5?2.5:x<=5?5:10)*base;
    }
}
