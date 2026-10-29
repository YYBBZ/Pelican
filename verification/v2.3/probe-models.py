import urllib.request,urllib.error,json,re,time
from pathlib import Path
key=Path('/tmp/pelican-live-test/api-key').read_text().strip()
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args,**kwargs):return None
opener=urllib.request.build_opener(NoRedirect)
records=[]
for path in ['/v1/models','/models']:
 url='https://api.fluxapi.vip'+path;t=time.monotonic()
 req=urllib.request.Request(url,headers={'Authorization':'Bearer '+key,'Accept':'application/json','User-Agent':'Pelican/2.3 diagnostics'})
 try:
  with opener.open(req,timeout=30) as res:status=res.status;body=res.read(2000000).decode('utf-8','replace');kind=res.headers.get('Content-Type','')
 except urllib.error.HTTPError as e:status=e.code;body=e.read(10000).decode('utf-8','replace');kind=e.headers.get('Content-Type','')
 except Exception as e:records.append({'path':path,'error':type(e).__name__+': '+str(e),'seconds':round(time.monotonic()-t,2)});continue
 body=body.replace(key,'[REDACTED]');item={'path':path,'status':status,'contentType':kind,'seconds':round(time.monotonic()-t,2)}
 try:
  obj=json.loads(body);models=obj.get('data',obj.get('models',[])) if isinstance(obj,dict) else obj
  if isinstance(models,list):
   ids=[m.get('id',m.get('name','')) if isinstance(m,dict) else m for m in models];ids=[m for m in ids if isinstance(m,str)];item['modelCount']=len(ids);item['models']=ids
   if status==200 and ids:Path('/workspace/Pelican/verification/v2.3/relay-models.json').write_text(json.dumps(ids,ensure_ascii=False,indent=2))
  elif status!=200:item['body']=body[:1500]
 except Exception:item['body']=body[:500]
 records.append(item)
Path('/workspace/Pelican/verification/v2.3/models-probe.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
for r in records:
 visible={k:v for k,v in r.items() if k!='models'}
 print(json.dumps(visible,ensure_ascii=False))
 if r.get('models'):
  ids=r['models'];print('Target model available:', 'gpt-6.1-sol' in ids);print('Model examples:',ids[:18])
print('Generated requests used: 0 / 3. No request sent to previous provider.')
