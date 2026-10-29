package app.pelican.check;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

/** Mature chart engine for bounded X zoom/pan; UI and grouping are specific to Pelican. */
public class TrendActivity extends Activity {
    private Store store;
    private final List<RunData> data=new ArrayList<>();
    private LinkedHashSet<String> selectedGroups=new LinkedHashSet<>();
    private LineChart chart;
    private TextView summary,latestValue,minValue,maxValue,detailTitle,detailBody,zoomHint;
    private Button groupButton,axisButton,open;
    private LinearLayout legend;
    private final List<Button> metricButtons=new ArrayList<>(),rangeButtons=new ArrayList<>();
    private int metric,range,generation;
    private boolean timeAxis,wide;
    private int renderGeneration;
    private final Map<String,Integer> groupColors=new HashMap<>();
    private String lineId="",model="";
    private RunData selected;
    private long origin;
    private static final int[] LIMITS={30,100,500,10000};
    private static final int[] COLORS={0xFF246953,0xFF4B6CB7,0xFF9560A4,0xFFAF7432,0xFF267D86,0xFFAF5D73};

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);Ui.systemBars(this);store=Store.get(this);
        Bundle args=state==null?getIntent().getExtras():state;
        if(args!=null){metric=args.getInt("metric",0);range=args.getInt("range",0);timeAxis=args.getBoolean("time",false);lineId=args.getString("line","");model=args.getString("model","");ArrayList<String> groups=args.getStringArrayList("groups");if(groups!=null)selectedGroups.addAll(groups);}
        wide=getIntent().getBooleanExtra("wide",false);
        if(wide)setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        build();reload();
    }
    private int dp(float n){return Ui.dp(this,n);}
    private LinearLayout row(){return Ui.row(this);}
    private Button button(String text,boolean primary){return Ui.button(this,text,primary);}
    private void build(){
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);
        LinearLayout bar=row();bar.setPadding(dp(16),dp(8),dp(16),dp(8));bar.setBackgroundColor(Color.WHITE);
        Button back=button("‹",false);back.setContentDescription("返回历史");bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));back.setOnClickListener(v->finish());
        TextView title=Ui.heading(this,wide?"趋势 · 横屏查看":"历史趋势",20);title.setPadding(dp(12),0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button filter=button(wide?"关闭":"筛选",false);filter.setOnClickListener(v->{if(wide)finish();else filters();});bar.addView(filter);root.addView(bar);
        LinearLayout body=Ui.column(this);body.setPadding(dp(16),dp(wide?6:14),dp(16),dp(wide?8:28));
        if(wide)root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        else{ScrollView scroll=new ScrollView(this);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));}
        setContentView(root);
        if(!wide){
            LinearLayout controls=Ui.card(this);LinearLayout controlRow=row();groupButton=button("选择数据组",false);axisButton=button(timeAxis?"真实时间":"检测等距",false);
            Ui.action(controlRow,groupButton,1.3f);Ui.action(controlRow,axisButton,1);controls.addView(controlRow);
            groupButton.setOnClickListener(v->groups());axisButton.setOnClickListener(v->axis());
            LinearLayout ranges=row();String[] labels={"30 次","100 次","500 次","全部"};
            for(int i=0;i<labels.length;i++){final int ix=i;Button b=button(labels[i],false);b.setTextSize(12);Ui.action(ranges,b,1);rangeButtons.add(b);b.setOnClickListener(v->{range=ix;reload();});}
            Ui.add(controls,ranges,10);body.addView(controls);
        }
        HorizontalScrollView metrics=new HorizontalScrollView(this);metrics.setHorizontalScrollBarEnabled(false);LinearLayout tabs=row();String[] names={"复杂度","耗时","输入 Token","输出 Token","思考 Token"};
        for(int i=0;i<names.length;i++){final int ix=i;Button b=button(names[i],false);b.setTextSize(12);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(i<2?72:98),dp(48));p.rightMargin=dp(6);tabs.addView(b,p);metricButtons.add(b);b.setOnClickListener(v->{metric=ix;render();});}
        metrics.addView(tabs);Ui.add(body,metrics,wide?0:12);
        if(!wide){LinearLayout stats=row();latestValue=stat(stats,"最近");minValue=stat(stats,"最低");maxValue=stat(stats,"最高");Ui.add(body,stats,12);}
        LinearLayout graph=Ui.card(this);if(wide)graph.setPadding(dp(10),dp(8),dp(10),dp(8));summary=Ui.text(this,"正在读取…",11,Ui.MUTED);graph.addView(summary);
        chart=new LineChart(this);setupChart();
        if(wide)graph.addView(chart,new LinearLayout.LayoutParams(-1,0,1));else Ui.add(graph,chart,8);
        if(!wide)chart.getLayoutParams().height=dp(278);
        LinearLayout zoom=row();Button minus=button("−",false),plus=button("＋",false),reset=button("看全部",false),expand=button(wide?"分组":"横屏",false);
        minus.setContentDescription("缩小横轴");plus.setContentDescription("放大横轴");Ui.action(zoom,minus,.7f);Ui.action(zoom,plus,.7f);Ui.action(zoom,reset,1.2f);Ui.action(zoom,expand,1);
        Ui.add(graph,zoom,6);minus.setOnClickListener(v->{chart.zoom(.67f,1,chart.getWidth()/2f,chart.getHeight()/2f);updateZoom();});plus.setOnClickListener(v->{chart.zoom(1.5f,1,chart.getWidth()/2f,chart.getHeight()/2f);updateZoom();});reset.setOnClickListener(v->{chart.fitScreen();updateZoom();});expand.setOnClickListener(v->{if(wide)groups();else fullscreen();});
        legend=Ui.column(this);
        if(wide){legend.setOrientation(LinearLayout.HORIZONTAL);HorizontalScrollView legendScroll=new HorizontalScrollView(this);legendScroll.setHorizontalScrollBarEnabled(false);legendScroll.addView(legend);Ui.add(graph,legendScroll,4);summary.setOnClickListener(v->openSelected());}
        else Ui.add(graph,legend,8);
        zoomHint=Ui.text(this,"双指横向放大 · 左右拖动 · 点选查看",10,Ui.MUTED);Ui.add(graph,zoomHint,5);
        if(wide)Ui.add(body,graph,0);else Ui.add(body,graph,12);
        if(wide)graph.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
        if(!wide){
            LinearLayout details=Ui.card(this);detailTitle=Ui.heading(this,"点选一条记录",15);details.addView(detailTitle);detailBody=Ui.text(this,"显示准确时间、所属分组和指标。",12,Ui.MUTED);detailBody.setTextIsSelectable(true);Ui.add(details,detailBody,8);
            open=button("打开完整检测记录 →",false);open.setEnabled(false);Ui.add(details,open,10);open.setOnClickListener(v->openSelected());Ui.add(body,details,12);
            Ui.add(body,Ui.text(this,"缺失指标留空、断开连线；Token 指标按样本取中位数。颜色区分分组，橙／红色点提示该次检测的异常。",11,Ui.MUTED),10);
        }
    }
    private void setupChart(){
        chart.getDescription().setEnabled(false);chart.getLegend().setEnabled(false);chart.setNoDataText("该范围暂时没有有效指标");chart.setNoDataTextColor(Ui.MUTED);
        chart.setTouchEnabled(true);chart.setDragEnabled(true);chart.setScaleXEnabled(true);chart.setScaleYEnabled(false);chart.setPinchZoom(false);chart.setDoubleTapToZoomEnabled(true);
        chart.setDrawGridBackground(false);chart.setDrawBorders(false);chart.setExtraOffsets(3,10,12,5);chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(Ui.MUTED);chart.getAxisLeft().setTextSize(10);chart.getAxisLeft().setGridColor(Ui.BORDER);chart.getAxisLeft().setDrawAxisLine(false);chart.getAxisLeft().setLabelCount(5,false);
        XAxis x=chart.getXAxis();x.setPosition(XAxis.XAxisPosition.BOTTOM);x.setDrawGridLines(false);x.setDrawAxisLine(false);x.setTextColor(Ui.MUTED);x.setTextSize(10);x.setLabelCount(4,false);x.setAvoidFirstLastClipping(true);
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener(){public void onValueSelected(Entry e,Highlight h){if(e.getData() instanceof RunData)record((RunData)e.getData());updateZoom();}public void onNothingSelected(){}});
        chart.setContentDescription("可双指横向缩放并左右拖动的检测趋势图，也可使用下方放大缩小按钮");
    }
    private TextView stat(LinearLayout parent,String label){LinearLayout box=Ui.card(this);box.addView(Ui.text(this,label,11,Ui.MUTED));TextView value=Ui.heading(this,"—",20);value.setSingleLine(true);value.setAutoSizeTextTypeUniformWithConfiguration(11,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);Ui.add(box,value,4);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.rightMargin=dp(5);parent.addView(box,p);return value;}
    private void style(Button b,boolean active){b.setTextColor(active?Color.WHITE:Ui.GREEN);b.setBackground(Ui.round(this,active?Ui.GREEN:Ui.PALE_GREEN,12));}
    private void reload(){
        final int ticket=++generation;summary.setText("正在读取历史…");Store.RunFilter f=new Store.RunFilter();f.completedOnly=true;f.lineId=lineId;f.model=model;
        final int limit=LIMITS[Math.max(0,Math.min(3,range))];for(int i=0;i<rangeButtons.size();i++)style(rangeButtons.get(i),i==range);
        new Thread(()->{try{List<RunData> found=store.getRuns(f,limit+1,0);boolean capped=found.size()>limit;if(capped)found.remove(found.size()-1);Collections.reverse(found);runOnUiThread(()->{if(isFinishing()||isDestroyed()||ticket!=generation)return;data.clear();data.addAll(found);render();if(capped&&limit==10000)summary.append(" · 最近 10,000 条");});}catch(Exception e){runOnUiThread(()->{if(ticket==generation)summary.setText("读取失败，请重新选择范围");});}},"pelican-chart").start();
    }
    private void render(){
        final int renderTicket=++renderGeneration;
        selectedGroups=TrendSeries.select(data,selectedGroups,metric);
        groupColors.putAll(TrendSeries.colors(selectedGroups,groupColors,COLORS));if(groupButton!=null)groupButton.setText("对比 · "+selectedGroups.size()+" 组");
        for(int i=0;i<metricButtons.size();i++)style(metricButtons.get(i),metric==i);
        List<RunData> visible=TrendSeries.visible(data,selectedGroups);origin=TrendSeries.origin(visible,metric);
        LinkedHashMap<String,List<RunData>> groups=TrendSeries.groups(visible);ArrayList<ILineDataSet> sets=new ArrayList<>();legend.removeAllViews();
        double last=Double.NaN,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;RunData lastRun=null;float maxX=0,minX=Float.POSITIVE_INFINITY;int valid=0,groupIndex=0;
        for(Map.Entry<String,List<RunData>> group:groups.entrySet()){
            int color=colorFor(group.getKey());List<Entry> segment=new ArrayList<>();List<Integer> circles=new ArrayList<>();RunData example=group.getValue().get(0);
            TextView label=Ui.text(this,"● "+TrendSeries.label(example).replace('\n',' ')+configurationLabel(example),11,color);label.setMaxLines(wide?1:2);if(wide)label.setPadding(0,0,dp(14),0);Ui.add(legend,label,3);
            for(int i=0;i<visible.size();i++){
                RunData run=visible.get(i);if(!TrendMetrics.group(run).equals(group.getKey()))continue;double value=TrendMetrics.value(run,metric);
                if(!Double.isFinite(value)){addSet(sets,segment,circles,color);segment=new ArrayList<>();circles=new ArrayList<>();continue;}
                float x=TrendSeries.x(run,i,timeAxis,origin);Entry entry=new Entry(x,(float)value,run);segment.add(entry);circles.add(run.inputTokenAlert||"ERROR".equals(run.status)?Ui.RED:"SUSPECT".equals(run.status)?Ui.ORANGE:color);
                valid++;min=Math.min(min,value);max=Math.max(max,value);minX=Math.min(minX,x);maxX=Math.max(maxX,x);if(lastRun==null||run.startedAt>=lastRun.startedAt){last=value;lastRun=run;}
            }
            addSet(sets,segment,circles,color);
        }
        if(latestValue!=null){latestValue.setText(TrendMetrics.format(last,metric,true));minValue.setText(TrendMetrics.format(min,metric,true));maxValue.setText(TrendMetrics.format(max,metric,true));}
        summary.setText(TrendMetrics.TITLES[metric]+" · "+TrendMetrics.unit(metric)+"   "+valid+" 点 / "+groups.size()+" 组");
        chart.clear();chart.getAxisLeft().resetAxisMinimum();chart.getAxisLeft().resetAxisMaximum();
        chart.getAxisLeft().setValueFormatter(new ValueFormatter(){@Override public String getFormattedValue(float v){return TrendMetrics.format(v,metric,true);}});
        final long start=origin;chart.getXAxis().setGranularity(timeAxis?.0167f:1f);
        chart.getXAxis().setValueFormatter(new ValueFormatter(){@Override public String getFormattedValue(float v){if(!timeAxis)return "#"+((int)Math.round(v)+1);long t=start+(long)(v*60000);return new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.getDefault()).format(new Date(t));}});
        if(sets.isEmpty()){selected=null;if(open!=null)open.setEnabled(false);chart.invalidate();if(detailBody!=null)detailBody.setText("所选分组暂未回传这个指标，可切换指标或数据组。");return;}
        chart.getXAxis().setAxisMinimum(minX-.3f);chart.getXAxis().setAxisMaximum(Math.max(minX+1,maxX)+.3f);
        chart.setData(new LineData(sets));chart.notifyDataSetChanged();chart.fitScreen();
        float span=Math.max(1,maxX-minX);chart.setVisibleXRangeMinimum(timeAxis?Math.max(.01f,span/1000):Math.min(2,span));
        final float end=maxX;final float desired=timeAxis?span:Math.min(span,11);
        chart.post(()->{if(isFinishing()||isDestroyed()||renderTicket!=renderGeneration)return;chart.setVisibleXRangeMaximum(span+1);if(desired>0&&span>desired)chart.zoom(span/desired,1,chart.getWidth()/2f,chart.getHeight()/2f);chart.moveViewToX(Math.max(minXValue(),end-desired));chart.invalidate();updateZoom();});
        if(lastRun!=null)record(lastRun);
    }
    private float minXValue(){return chart.getXAxis().getAxisMinimum();}
    private void addSet(List<ILineDataSet> all,List<Entry> entries,List<Integer> circles,int color){if(entries.isEmpty())return;LineDataSet set=new LineDataSet(entries,"");set.setColor(color);set.setLineWidth(2);set.setCircleRadius(3);set.setCircleHoleRadius(1.2f);set.setCircleColors(circles);set.setDrawValues(false);set.setHighLightColor(Ui.BLUEGRAY);set.setDrawHorizontalHighlightIndicator(false);set.setHighlightLineWidth(1);set.setMode(LineDataSet.Mode.LINEAR);set.setDrawFilled(false);all.add(set);}
    private void updateZoom(){if(zoomHint!=null)zoomHint.setText("横轴 "+String.format(Locale.CHINA,"%.1f",chart.getScaleX())+"× · 双指放大／左右拖动 · 点击数据点");}
    private void record(RunData run){selected=run;if(wide){summary.setText(Ui.time(run.startedAt)+" · "+run.lineName+" · "+TrendMetrics.TITLES[metric]+" "+TrendMetrics.format(TrendMetrics.value(run,metric),metric,false));return;}open.setEnabled(true);detailTitle.setText(Ui.dateTime(run.startedAt)+" · "+Ui.statusText(run.status));detailBody.setText(TrendSeries.label(run)+"\n"+TrendMetrics.TITLES[metric]+"："+TrendMetrics.format(TrendMetrics.value(run,metric),metric,false)+" "+TrendMetrics.unit(metric)+(run.inputTokenNote==null||run.inputTokenNote.isEmpty()?"":"\n"+run.inputTokenNote));detailBody.setTextColor(run.inputTokenAlert?Ui.RED:Ui.TEXT);}
    private void openSelected(){if(selected!=null)startActivity(new Intent(this,RunDetailActivity.class).putExtra(MonitorService.EXTRA_RUN_ID,selected.id));}
    private void groups(){
        LinkedHashMap<String,List<RunData>> groups=TrendSeries.groups(data);if(groups.isEmpty()){Ui.toast(this,"当前范围还没有数据组");return;}
        ArrayList<String> keys=new ArrayList<>(groups.keySet());String[] labels=new String[keys.size()];boolean[] checks=new boolean[keys.size()];
        for(int i=0;i<keys.size();i++){RunData example=groups.get(keys.get(i)).get(0);labels[i]=TrendSeries.label(example)+configurationLabel(example)+" · "+groups.get(keys.get(i)).size()+" 条";checks[i]=selectedGroups.contains(keys.get(i));}
        new AlertDialog.Builder(this).setTitle("选择对比组（最多 6 组）").setMultiChoiceItems(labels,checks,(d,i,on)->checks[i]=on).setNegativeButton("取消",null).setPositiveButton("显示",(d,w)->{LinkedHashSet<String> next=new LinkedHashSet<>();for(int i=0;i<checks.length;i++)if(checks[i])next.add(keys.get(i));if(next.isEmpty()||next.size()>6){Ui.toast(this,"请选择 1～6 组");return;}selectedGroups=next;render();}).show();
    }
    private void axis(){new AlertDialog.Builder(this).setTitle("横轴排列").setSingleChoiceItems(new String[]{"按检测次数等距（推荐）","按真实时间间隔"},timeAxis?1:0,(d,i)->{timeAxis=i==1;axisButton.setText(timeAxis?"真实时间":"检测等距");d.dismiss();render();}).setNegativeButton("取消",null).show();}
    private void filters(){
        LinearLayout form=Ui.column(this);form.setPadding(dp(22),dp(8),dp(22),0);List<LineConfig> lines=store.getLines();List<String> labels=new ArrayList<>();labels.add("全部线路");for(LineConfig l:lines)labels.add(l.displayName());Spinner line=Ui.choiceField(form,"线路",labels);for(int i=0;i<lines.size();i++)if(lines.get(i).id.equals(lineId))line.setSelection(i+1);
        List<String> models=new ArrayList<>();models.add("全部模型");models.addAll(store.getModels());Spinner mdl=Ui.choiceField(form,"模型",models);mdl.setSelection(Math.max(0,models.indexOf(model)));
        new AlertDialog.Builder(this).setTitle("筛选趋势数据").setView(form).setNegativeButton("取消",null).setPositiveButton("应用",(d,w)->{lineId=line.getSelectedItemPosition()==0?"":lines.get(line.getSelectedItemPosition()-1).id;model=mdl.getSelectedItemPosition()==0?"":String.valueOf(mdl.getSelectedItem());selectedGroups.clear();reload();}).show();
    }
    private void fullscreen(){Intent i=new Intent(this,TrendActivity.class);i.putExtra("wide",true);i.putExtra("metric",metric);i.putExtra("range",range);i.putExtra("time",timeAxis);i.putExtra("line",lineId);i.putExtra("model",model);i.putStringArrayListExtra("groups",new ArrayList<>(selectedGroups));startActivity(i);}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("metric",metric);b.putInt("range",range);b.putBoolean("time",timeAxis);b.putString("line",lineId);b.putString("model",model);b.putStringArrayList("groups",new ArrayList<>(selectedGroups));}
    @Override protected void onDestroy(){generation++;super.onDestroy();}
    private int colorFor(String key){Integer color=groupColors.get(key);return color==null?COLORS[0]:color;}
    private String configurationLabel(RunData example){
        String baseLabel=TrendSeries.label(example);Set<String> fingerprints=new HashSet<>();
        for(RunData run:data)if(TrendSeries.label(run).equals(baseLabel))fingerprints.add(run.inputTokenSignature==null?"":run.inputTokenSignature);
        if(fingerprints.size()<2)return "";String sig=example.inputTokenSignature;return sig==null||sig.isEmpty()?" · 旧配置":" · 配置 "+sig.substring(0,Math.min(6,sig.length()));
    }
}
