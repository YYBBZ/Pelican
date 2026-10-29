package app.pelican.check;

import java.util.*;
import okhttp3.HttpUrl;

/** Validate user input explicitly. Never invent credentials or route around authentication. */
public final class RequestInputs {
    private RequestInputs(){}
    public static String key(String raw){
        String s=raw==null?"":raw;
        int a=0,b=s.length();while(a<b&&edge(s.charAt(a)))a++;while(b>a&&edge(s.charAt(b-1)))b--;s=s.substring(a,b);
        if(s.regionMatches(true,0,"Bearer ",0,7))s=s.substring(7).trim();
        if(s.length()>1&&((s.startsWith("\"")&&s.endsWith("\""))||(s.startsWith("'")&&s.endsWith("'"))))s=s.substring(1,s.length()-1).trim();
        if(s.isEmpty())throw new IllegalArgumentException("请填写 API 密钥");
        for(int i=0;i<s.length();i++)if(s.charAt(i)<=32||s.charAt(i)>126)throw new IllegalArgumentException("密钥中含空格、换行或非英文字符，请重新粘贴；不会擅自修改中间内容");
        return s;
    }
    private static boolean edge(char c){return Character.isWhitespace(c)||Character.isSpaceChar(c)||Character.getType(c)==Character.FORMAT;}
    public static String base(String value){
        String s=value==null?"":value.trim();HttpUrl url=HttpUrl.parse(s);
        if(url==null)throw new IllegalArgumentException("接口地址须以 https:// 或 http:// 开头");
        if(!url.username().isEmpty()||!url.password().isEmpty()||url.query()!=null||url.fragment()!=null)
            throw new IllegalArgumentException("接口地址不要包含账号、密钥、查询参数或 # 后缀");
        s=url.toString();while(s.endsWith("/"))s=s.substring(0,s.length()-1);
        return s.replaceFirst("/(responses/input_tokens|messages/count_tokens|chat/completions|messages|models|responses)$","");
    }
    public static List<String> endpoints(String value,String suffix){
        String base=base(value);HttpUrl url=HttpUrl.parse(base);LinkedHashSet<String> out=new LinkedHashSet<>();
        if(url.encodedPath().equals("/")||url.encodedPath().isEmpty()){
            out.add(base+"/v1"+suffix);out.add(base+suffix);
        }else{
            out.add(base+suffix);
            if(base.endsWith("/v1"))out.add(base.substring(0,base.length()-3)+suffix);else out.add(base+"/v1"+suffix);
        }
        return new ArrayList<>(out);
    }
    public static String safeError(String message,String rawKey){
        String s=message==null?"":message;
        if(rawKey!=null&&!rawKey.isEmpty())s=s.replace(rawKey,"[密钥已隐藏]");
        try{s=s.replace(key(rawKey),"[密钥已隐藏]");}catch(Exception ignored){}
        return s.replaceAll("(?i)sk-[a-z0-9_-]{8,}","[密钥已隐藏]");
    }
    public static String generationEndpoint(LineConfig config,String path){
        String normalized=base(config.baseUrl);
        return normalized.equals(config.resolvedApiBase)?normalized+path:endpoints(normalized,path).get(0);
    }
}
