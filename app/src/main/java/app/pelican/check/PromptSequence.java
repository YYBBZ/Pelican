package app.pelican.check;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Per-line deterministic circular queue. Time and interval never choose a prompt. */
public final class PromptSequence {
    private PromptSequence(){}
    public static class Entry {
        public String id="",label="",text="",builtinId="";
        public Entry(){}
        public Entry(String id,String label,String text,String builtin){this.id=id;this.label=label;this.text=text;this.builtinId=builtin;}
        public LineConfig.Prompt prompt(){return new LineConfig.Prompt(id,label,text);}
    }
    public static Entry builtin(LineConfig.Prompt p){return new Entry(p.id,p.label,p.text,p.id);}
    public static List<Entry> defaults(){List<Entry> list=new ArrayList<>();for(LineConfig.Prompt p:LineConfig.PROMPTS)if(!"custom".equals(p.id))list.add(builtin(p));return list;}
    public static void normalize(LineConfig line){
        if(line.promptSequence==null||line.promptSequence.isEmpty()){
            line.promptSequence=new ArrayList<>();LineConfig.Prompt old=LineConfig.promptById(line.promptId);
            line.promptSequence.add("custom".equals(old.id)?new Entry("custom",old.label,line.customPrompt==null?"":line.customPrompt,""):builtin(old));
            line.sequenceIndex=0;
        }
        line.sequenceIndex=Math.floorMod(line.sequenceIndex,line.promptSequence.size());
        if(line.sequenceRevision==null)line.sequenceRevision="";
    }
    public static Entry current(LineConfig line){normalize(line);return line.promptSequence.get(line.sequenceIndex);}
    public static String revision(LineConfig line){normalize(line);return InputAudit.sha256(Json.gson().toJson(line.promptSequence).getBytes(StandardCharsets.UTF_8));}
    public static List<Entry> upcoming(LineConfig line){normalize(line);List<Entry> entries=new ArrayList<>();for(int i=0;i<line.promptSequence.size();i++)entries.add(line.promptSequence.get((line.sequenceIndex+i)%line.promptSequence.size()));return entries;}
    public static void validate(LineConfig line){normalize(line);Set<String> ids=new HashSet<>();for(Entry e:line.promptSequence){if(e==null||e.id==null||e.id.isEmpty()||!ids.add(e.id))throw new IllegalArgumentException("题目编号重复或无效");if(e.label==null||e.label.trim().isEmpty())throw new IllegalArgumentException("请给每道题目填写名称");if(e.text==null||e.text.trim().isEmpty())throw new IllegalArgumentException("题目“"+e.label+"”的内容不能为空");}}
    public static void capture(LineConfig line,RunData run){
        normalize(line);Entry prompt=current(line);run.promptKey=prompt.id;run.promptLabel=prompt.label;run.promptTextUsed=prompt.text;
        run.promptSequenceRevision=revision(line);run.promptSequenceIndex=line.sequenceIndex;run.promptSequenceSize=line.promptSequence.size();
    }
    public static boolean anyRequestSucceeded(RunData run){if(run.samples!=null)for(RunData.SampleData s:run.samples)if(s!=null&&s.requestOk)return true;return false;}
    public static void finish(LineConfig latest,RunData run){
        normalize(latest);if(run.id.equals(latest.lastSequenceRunId))return;
        if(!run.promptSequenceRevision.equals(revision(latest))||!run.promptKey.equals(current(latest).id)){
            run.sequenceNote="检测期间顺序表已修改，保留新表安排";return;
        }
        latest.lastSequenceRunId=run.id;
        if(anyRequestSucceeded(run)){
            latest.sequenceIndex=(latest.sequenceIndex+1)%latest.promptSequence.size();latest.sequenceBlocked=false;latest.sequenceFailureNote="";
            run.sequenceAdvanced=true;run.sequenceNote="本轮至少一张请求完整返回，顺序前移";
        }else{
            latest.sequenceBlocked=true;latest.sequenceFailureNote="上轮全部请求未成功，位次未前移，下次仍使用本题";
            run.sequenceAdvanced=false;run.sequenceNote=latest.sequenceFailureNote;
        }
    }
}
