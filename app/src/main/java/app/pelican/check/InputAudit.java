package app.pelican.check;

import com.google.gson.*;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Audits the immutable serialized bytes used by the actual request, not a UI reconstruction. */
public final class InputAudit {
    private static EncodingRegistry registry;
    private static String activeModel="";
    private InputAudit(){}
    public static class Evidence {
        public String bodyJson="",bodySha256="",promptSha256="",encoding="",countSource="",countNote="";
        public int bodyBytes,promptBytes,promptCharacters,messageCount,estimatedInputTokens;
        public Integer expectedInputTokens=null;
        public boolean contentVerified,wireBytesVerified,countReliable;
    }
    public static class Frozen {
        private final byte[] bytes;
        public final Evidence evidence;
        private Frozen(byte[] value,Evidence e){bytes=value.clone();evidence=e;}
        public byte[] bytes(){return bytes.clone();}
        public boolean matches(byte[] other){return Arrays.equals(bytes,other)&&evidence.bodySha256.equals(sha256(other));}
    }
    public static String sha256(byte[] bytes){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.US,"%02x",b&255));return out.toString();}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static Frozen freeze(LineConfig config,String prompt,JsonObject payload){
        if(prompt==null||prompt.isEmpty())throw new IllegalArgumentException("检测提示词为空，未发送请求");
        if(!config.model.equals(payload.get("model").getAsString()))throw new IllegalStateException("APP 自检：模型字段与所选模型不一致，未发送");
        for(String key:new String[]{"system","instructions","tools","functions","previous_response_id","conversation"})
            if(payload.has(key))throw new IllegalStateException("APP 自检：存在未配置的输入字段 "+key+"，未发送");
        Set<String> allowed=new HashSet<>(Arrays.asList("model","stream","messages","max_tokens","temperature"));
        if("responses".equals(config.protocol))allowed=new HashSet<>(Arrays.asList("model","stream","input","max_output_tokens","reasoning"));
        else if("anthropic".equals(config.protocol))allowed.add("thinking");
        else{allowed.add("stream_options");allowed.add("reasoning_effort");}
        for(String k:payload.keySet())if(!allowed.contains(k))throw new IllegalStateException("APP 自检：出现未声明的请求字段 "+k+"，未发送");
        String field="responses".equals(config.protocol)?"input":"messages";
        JsonArray messages=payload.getAsJsonArray(field);
        if(messages==null||messages.size()!=1)throw new IllegalStateException("APP 自检：必须恰好发送一条检测消息，未发送");
        JsonObject message=messages.get(0).getAsJsonObject();
        if(message.size()!=2||!"user".equals(message.get("role").getAsString())||!message.get("content").isJsonPrimitive()
                ||!prompt.equals(message.get("content").getAsString()))
            throw new IllegalStateException("APP 自检：提示词发生遗漏、重复或额外拼接，未发送");
        String cap="responses".equals(config.protocol)?"max_output_tokens":"max_tokens";
        if(!payload.has(cap)||payload.get(cap).getAsInt()!=config.maxTokens)throw new IllegalStateException("APP 自检：输出上限与设置不一致，未发送");
        String json=Json.gson().toJson(payload);byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        JsonObject roundtrip=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
        if(!payload.equals(roundtrip)||!prompt.equals(roundtrip.getAsJsonArray(field).get(0).getAsJsonObject().get("content").getAsString()))
            throw new IllegalStateException("APP 自检：JSON/UTF-8 往返校验失败，未发送");
        Evidence e=new Evidence();e.bodyJson=json;e.bodyBytes=bytes.length;e.bodySha256=sha256(bytes);
        byte[] text=prompt.getBytes(StandardCharsets.UTF_8);e.promptBytes=text.length;e.promptCharacters=prompt.codePointCount(0,prompt.length());e.promptSha256=sha256(text);e.messageCount=1;e.contentVerified=true;
        estimate(e,config,prompt);return new Frozen(bytes,e);
    }
    private static synchronized void estimate(Evidence e,LineConfig config,String prompt){
        try{
            // Keep only one registry alive to bound vocabulary memory on phones.
            if(registry==null||!activeModel.equals(config.model)){registry=Encodings.newLazyEncodingRegistry();activeModel=config.model;}
            Optional<Encoding> mapped=registry.getEncodingForModel(config.model);
            Encoding encoding=mapped.orElseGet(()->registry.getEncoding(EncodingType.O200K_BASE));
            int textTokens=encoding.countTokensOrdinary(prompt);
            e.estimatedInputTokens=Math.addExact(textTokens,encoding.countTokensOrdinary("user")+6);
            e.encoding=encoding.getName();
            e.countSource="本地 JTokkit / "+e.encoding;
            e.countNote=mapped.isPresent()?"本地分词含消息封装估算；用于输入异常筛查，不单凭差额确认注入":"未确认该中转模型的真实分词器；仅用 "+e.encoding+" 估算；命中规则仅表示疑似注入";
        }catch(Exception|LinkageError error){e.estimatedInputTokens=0;e.countNote="本地分词暂不可用；按请求正文字节数补充规则筛查输入异常";}
        // A local tokenizer alone cannot attest model-specific chat framing. Never promote it to exact.
        e.countReliable=false;e.expectedInputTokens=null;
    }
}
