package app.pelican.check;
import java.util.*;

/** App-defined budgets are explicit and are not claims of model-to-model equivalence. */
public final class ReasoningPolicy {
    private ReasoningPolicy(){}
    public static final String[] VALUES={"auto","none","low","medium","high","xhigh","max"};
    public static final String[] LABELS={"服务端默认（auto）","关闭（none）","低（low）","中（medium）","高（high）","超高（xhigh）","最大（max）"};
    public static int budget(LineConfig line){
        if(!"anthropic".equals(line.protocol))return 0;
        switch(line.effort){case "low":return 4000;case "medium":return 10000;case "high":return 24000;case "xhigh":return 32000;case "max":return 64000;default:return 0;}
    }
    public static void validate(LineConfig line){
        if(!Arrays.asList(VALUES).contains(line.effort))throw new IllegalArgumentException("请选择有效思考档位");
        int budget=budget(line);
        if(budget>0&&line.maxTokens<=budget)throw new IllegalArgumentException("输出上限 "+line.maxTokens+" 必须大于思考预算 "+budget+" Token；请自行调整，应用不会自动提高上限");
    }
    public static String description(LineConfig line){
        if("auto".equals(line.effort))return "实际发送：不包含思考强度或思考预算参数，使用服务端默认值";
        if("anthropic".equals(line.protocol))return "none".equals(line.effort)?"实际发送：thinking.type = disabled":"实际发送：thinking.type = enabled，budget_tokens = "+budget(line)+" Token（APP 档位约定，并非跨模型完全等效）";
        return "实际发送："+("responses".equals(line.protocol)?"reasoning.effort":"reasoning_effort")+" = "+line.effort+"；协议未提供固定预算，不推测隐藏预算";
    }
}
