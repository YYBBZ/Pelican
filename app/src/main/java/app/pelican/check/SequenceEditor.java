package app.pelican.check;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Accessible drag/drop list: same reorder action is available as explicit up/down buttons. */
public final class SequenceEditor extends LinearLayout {
    private final Activity activity;
    private final java.util.List<PromptSequence.Entry> entries=new ArrayList<>();
    private final String original;
    private final int startIndex;
    private final boolean blocked;
    private final String blockedNote;
    private final Runnable changed;
    public SequenceEditor(Activity activity,LineConfig line,Runnable changed){
        super(activity);this.activity=activity;this.changed=changed;setOrientation(VERTICAL);
        PromptSequence.normalize(line);for(PromptSequence.Entry e:PromptSequence.upcoming(line))entries.add(copy(e));
        original=Json.gson().toJson(entries);startIndex=line.sequenceIndex;blocked=line.sequenceBlocked;blockedNote=line.sequenceFailureNote;render();
    }
    private PromptSequence.Entry copy(PromptSequence.Entry e){return new PromptSequence.Entry(e.id,e.label,e.text,e.builtinId);}
    private int dp(float n){return Ui.dp(activity,n);}
    public boolean edited(){return !original.equals(Json.gson().toJson(entries));}
    public String fingerprint(){return Json.gson().toJson(entries);}
    public void apply(LineConfig line){if(edited()){line.promptSequence=new ArrayList<>();for(PromptSequence.Entry e:entries)line.promptSequence.add(copy(e));line.sequenceIndex=0;line.sequenceBlocked=false;line.sequenceFailureNote="";line.lastSequenceRunId="";}}
    public PromptSequence.Entry first(){return entries.get(0);}
    private void update(){render();if(changed!=null)changed.run();}
    private void move(int from,int to){if(from==to||from<0||to<0||from>=entries.size()||to>=entries.size())return;PromptSequence.Entry e=entries.remove(from);entries.add(to,e);update();announceForAccessibility("已移至第 "+(to+1)+" 位");}
    private void render(){
        removeAllViews();
        if(blocked&&!edited()){TextView note=Ui.text(activity,blockedNote,12,Ui.ORANGE);Ui.add(this,note,8);}
        for(int i=0;i<entries.size();i++){
            final int pos=i;PromptSequence.Entry entry=entries.get(i);
            LinearLayout card=Ui.column(activity);card.setPadding(dp(10),dp(8),dp(10),dp(8));card.setBackground(Ui.bordered(activity,0xFFFAFAF5,Ui.BORDER,12));
            LinearLayout title=Ui.row(activity);TextView drag=Ui.text(activity,"☰",20,Ui.MUTED);drag.setGravity(Gravity.CENTER);drag.setMinWidth(dp(40));drag.setMinHeight(dp(48));drag.setContentDescription("长按拖动第 "+(i+1)+" 道题");title.addView(drag);
            LinearLayout labels=Ui.column(activity);labels.addView(Ui.text(activity,i==0?"下一轮 · 第 "+(edited()?1:startIndex+1)+" 道 / 共 "+entries.size()+" 道":"第 "+(i+1)+" 位",11,i==0?Ui.GREEN:Ui.MUTED));labels.addView(Ui.heading(activity,entry.label,14));title.addView(labels,new LayoutParams(0,-2,1));
            Button edit=Ui.button(activity,"编辑",false);edit.setTextSize(12);edit.setContentDescription("编辑题目 "+entry.label);title.addView(edit);edit.setOnClickListener(v->edit(pos));card.addView(title);
            TextView preview=Ui.text(activity,entry.text,11,Ui.MUTED);preview.setMaxLines(2);Ui.add(card,preview,4);
            LinearLayout actions=Ui.row(activity);Button up=Ui.button(activity,"↑ 上移",false),down=Ui.button(activity,"↓ 下移",false),delete=Ui.button(activity,"删除",false);up.setTextSize(12);down.setTextSize(12);delete.setTextSize(12);delete.setTextColor(Ui.RED);up.setEnabled(i>0);down.setEnabled(i<entries.size()-1);delete.setEnabled(entries.size()>1);Ui.action(actions,up,1);Ui.action(actions,down,1);Ui.action(actions,delete,1);Ui.add(card,actions,6);
            up.setOnClickListener(v->move(pos,pos-1));down.setOnClickListener(v->move(pos,pos+1));delete.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle("从顺序表移除这道题？").setMessage("只移除本线路的安排，不删除历史记录或已有基线。").setNegativeButton("取消",null).setPositiveButton("移除",(d,w)->{if(entries.size()>1){entries.remove(pos);update();}}).show());
            View.OnLongClickListener start=v->{v.startDragAndDrop(ClipData.newPlainText("题目排序",entry.label),new View.DragShadowBuilder(card),Integer.valueOf(pos),0);return true;};drag.setOnLongClickListener(start);card.setOnLongClickListener(start);
            card.setOnDragListener((v,event)->{
                if(!(event.getLocalState() instanceof Integer))return false;
                switch(event.getAction()){
                    case DragEvent.ACTION_DRAG_STARTED:return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        android.view.ViewParent parent=getParent();while(parent!=null&&!(parent instanceof ScrollView))parent=parent.getParent();
                        if(parent instanceof ScrollView){ScrollView scroll=(ScrollView)parent;int[] cardLocation=new int[2],scrollLocation=new int[2];v.getLocationOnScreen(cardLocation);scroll.getLocationOnScreen(scrollLocation);float y=cardLocation[1]+event.getY()-scrollLocation[1];if(y<dp(70))scroll.smoothScrollBy(0,-dp(36));else if(y>scroll.getHeight()-dp(70))scroll.smoothScrollBy(0,dp(36));}return true;
                    case DragEvent.ACTION_DRAG_ENTERED:v.setAlpha(.55f);return true;
                    case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1);return true;
                    case DragEvent.ACTION_DROP:v.setAlpha(1);move((Integer)event.getLocalState(),pos);return true;
                    default:return true;
                }
            });Ui.add(this,card,8);
        }
        Button add=Ui.button(activity,"＋ 添加题目",false);Ui.add(this,add,10);add.setOnClickListener(v->add());
        Ui.add(this,Ui.text(activity,"长按 ☰ 拖动，或用上移／下移排序。至少保留一道；整轮请求全失败时停留本题。",11,Ui.MUTED),6);
    }
    private void add(){
        java.util.List<LineConfig.Prompt> available=new ArrayList<>();for(LineConfig.Prompt p:LineConfig.PROMPTS){if("custom".equals(p.id))continue;boolean exists=false;for(PromptSequence.Entry e:entries)if(p.id.equals(e.builtinId))exists=true;if(!exists)available.add(p);}
        String[] names=new String[available.size()+1];for(int i=0;i<available.size();i++)names[i]=available.get(i).label;names[names.length-1]="＋ 自定义题目";
        new AlertDialog.Builder(activity).setTitle("添加到本线路顺序表").setItems(names,(d,which)->{if(which<available.size()){entries.add(PromptSequence.builtin(available.get(which)));update();}else editCustom(-1);}).setNegativeButton("取消",null).show();
    }
    private void edit(int pos){editCustom(pos);}
    private void editCustom(int pos){
        boolean adding=pos<0;PromptSequence.Entry originalEntry=adding?null:entries.get(pos);
        LinearLayout body=Ui.column(activity);body.setPadding(dp(20),dp(6),dp(20),dp(10));body.addView(Ui.text(activity,"题目名称",12,Ui.MUTED));EditText label=Ui.input(activity,"给这道题起名",adding?"":originalEntry.label);Ui.add(body,label,5);Ui.add(body,Ui.text(activity,"原样发送的提示词",12,Ui.MUTED),10);EditText content=Ui.input(activity,"填写完整提示词",adding?"":originalEntry.text);content.setSingleLine(false);content.setMinLines(4);content.setMaxLines(10);content.setGravity(Gravity.TOP);Ui.add(body,content,5);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(adding?"添加自定义题目":"编辑题目").setView(scroll).setNegativeButton("取消",null).setPositiveButton("确定",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String name=label.getText().toString().trim(),text=content.getText().toString();if(name.isEmpty()){label.setError("名称不能为空");return;}if(text.trim().isEmpty()){content.setError("提示词不能为空");return;}
            if(adding)entries.add(new PromptSequence.Entry("custom_"+UUID.randomUUID(),name,text,""));else{PromptSequence.Entry e=entries.get(pos);if(!text.equals(e.text)&&e.builtinId!=null&&!e.builtinId.isEmpty()){e.id="custom_"+UUID.randomUUID();e.builtinId="";}else if(!text.equals(e.text)){e.id="custom_"+UUID.randomUUID();}e.label=name;e.text=text;}
            dialog.dismiss();update();}));dialog.show();
    }
}
