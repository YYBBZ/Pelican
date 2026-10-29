package app.pelican.check;
import java.util.*;

/** Platform-independent grouping, visible-domain and gap planning for the chart. */
public final class TrendSeries {
    private TrendSeries(){}
    public static LinkedHashMap<String,List<RunData>> groups(List<RunData> runs){
        LinkedHashMap<String,List<RunData>> out=new LinkedHashMap<>();
        for(RunData run:runs)out.computeIfAbsent(TrendMetrics.group(run),k->new ArrayList<>()).add(run);
        return out;
    }
    public static String label(RunData run){return (run.lineName==null?"未命名线路":run.lineName)+" · "+run.model+"\n"+(run.promptLabel==null?run.promptKey:run.promptLabel);}
    public static LinkedHashSet<String> select(List<RunData> runs,Set<String> previous,int metric){
        LinkedHashMap<String,List<RunData>> groups=groups(runs);LinkedHashSet<String> out=new LinkedHashSet<>();
        for(String key:previous)if(groups.containsKey(key))out.add(key);
        if(out.isEmpty())for(int i=runs.size()-1;i>=0;i--)if(Double.isFinite(TrendMetrics.value(runs.get(i),metric))){out.add(TrendMetrics.group(runs.get(i)));break;}
        if(out.isEmpty()&&!groups.isEmpty())out.add(groups.keySet().iterator().next());return out;
    }
    public static List<RunData> visible(List<RunData> runs,Set<String> selected){List<RunData> out=new ArrayList<>();for(RunData r:runs)if(selected.contains(TrendMetrics.group(r)))out.add(r);return out;}
    public static long origin(List<RunData> runs,int metric){for(RunData r:runs)if(Double.isFinite(TrendMetrics.value(r,metric)))return r.startedAt;return runs.isEmpty()?0:runs.get(0).startedAt;}
    public static float x(RunData run,int sequence,boolean time,long origin){return time?(float)((run.startedAt-origin)/60000.0):sequence;}
    public static Map<String,Integer> colors(Set<String> selected,Map<String,Integer> previous,int[] palette){
        LinkedHashMap<String,Integer> out=new LinkedHashMap<>();Set<Integer> used=new HashSet<>();
        for(String key:selected){Integer color=previous.get(key);
            if(color==null||used.contains(color)){color=null;for(int next:palette)if(!used.contains(next)){color=next;break;}}
            if(color==null)color=palette[0];out.put(key,color);used.add(color);
        }
        return out;
    }
}
