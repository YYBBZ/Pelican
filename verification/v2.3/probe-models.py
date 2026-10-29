"""Manual model-list probe. No generation; requires an explicit authorized URL."""
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('Usage: python3 probe-models.py KEY_FILE; set PELICAN_TEST_BASE_URL first')
base = os.environ.get('PELICAN_TEST_BASE_URL', '').strip().rstrip('/')
parts = urllib.parse.urlsplit(base)
host = (parts.hostname or '').lower()
if (parts.scheme != 'https' or not host or parts.username is not None or parts.password is not None
        or parts.query or parts.fragment or host in ('invalid', 'example.com') or host.endswith('.invalid')):
    raise SystemExit('Set a real authorized HTTPS PELICAN_TEST_BASE_URL without embedded credentials, query or fragment')
key = Path(sys.argv[1]).read_text(encoding='utf-8').strip()
if not key:
    raise SystemExit('The key file is empty')
root = Path(__file__).resolve().parents[2]
output = Path(os.environ.get('PELICAN_TEST_OUTPUT_DIR') or root / 'verification/local/v2.3')
output.mkdir(parents=True, exist_ok=True)

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None

opener = urllib.request.build_opener(NoRedirect)
paths = ['/models'] if parts.path.rstrip('/').endswith('/v1') else ['/v1/models', '/models']
records = []
for path in paths:
    url = base + path
    started = time.monotonic()
    request = urllib.request.Request(url, headers={
        'Authorization': 'Bearer ' + key,
        'Accept': 'application/json',
        'User-Agent': 'Pelican manual model-list probe',
    })
    try:
        with opener.open(request, timeout=30) as response:
            status = response.status
            body = response.read(2000000).decode('utf-8', 'replace')
            kind = response.headers.get('Content-Type', '')
    except urllib.error.HTTPError as error:
        status = error.code
        body = error.read(10000).decode('utf-8', 'replace')
        kind = error.headers.get('Content-Type', '')
    except Exception as error:
        records.append({'path': path, 'error': type(error).__name__ + ': ' + str(error).replace(key, '[REDACTED]'),
                        'seconds': round(time.monotonic() - started, 2)})
        continue
    body = body.replace(key, '[REDACTED]')
    item = {'path': path, 'status': status, 'contentType': kind, 'seconds': round(time.monotonic() - started, 2)}
    try:
        obj = json.loads(body)
        models = obj.get('data', obj.get('models', [])) if isinstance(obj, dict) else obj
        if isinstance(models, list):
            ids = [model.get('id', model.get('name', '')) if isinstance(model, dict) else model for model in models]
            ids = [model for model in ids if isinstance(model, str)]
            item.update(modelCount=len(ids), models=ids)
            if status == 200 and ids:
                (output / 'relay-models.json').write_text(json.dumps(ids, ensure_ascii=False, indent=2), encoding='utf-8')
        elif status != 200:
            item['body'] = body[:1500]
    except (ValueError, TypeError):
        item['body'] = body[:500]
    records.append(item)
(output / 'models-probe.json').write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding='utf-8')
for record in records:
    print(json.dumps({k: v for k, v in record.items() if k != 'models'}, ensure_ascii=False))
    if record.get('models'):
        print('Model examples:', record['models'][:18])
print('Generation requests: 0. Only model-list endpoints were queried.')
