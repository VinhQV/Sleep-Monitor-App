"""Save the current HTTPS endpoint over USB without erasing the board's Wi-Fi settings."""
import argparse,json,pathlib,time
import serial
parser=argparse.ArgumentParser()
parser.add_argument('--port',required=True)
parser.add_argument('--wifi-from-secrets',action='store_true',help='Apply the Wi-Fi already configured in private secrets.h')
args=parser.parse_args()
root=pathlib.Path(__file__).resolve().parent.parent
access=json.loads((root/'backend/data/remote-access.json').read_text(encoding='utf-8-sig'))
assert access['url'].startswith('https://') and len(access['deviceKey'])>=32
payload={'server':access['url'],'key':access['deviceKey']}
if args.wifi_from_secrets:
    import re
    source=(root/'firmware/include/secrets.h').read_text(encoding='utf-8-sig')
    for name,target in [('VOICE_WIFI_SSID','ssid'),('VOICE_WIFI_PASSWORD','password')]:
        match=re.search(r'\b'+name+r'\b[^\r\n]*?"((?:\\.|[^"\\])*)"',source)
        if not match:raise SystemExit('Missing Wi-Fi configuration in secrets.h')
        payload[target]=json.loads('"'+match.group(1)+'"')
port=serial.Serial();port.port=args.port;port.baudrate=115200;port.timeout=.5;port.dtr=False;port.rts=False
try:
    port.open();port.write((json.dumps(payload)+'\n').encode());port.flush()
    until=time.monotonic()+35;saved=False
    while time.monotonic()<until:
        line=port.readline().decode('utf-8',errors='replace').strip()
        if 'Remote HTTPS configuration saved' in line:
            saved=True;print('HTTPS configuration saved; XIAO is restarting to reconnect.');break
        if line and 'password' not in line.lower():print(line,flush=True)
        if saved and 'Sensors synced' in line:break
    if not saved:raise SystemExit('Board did not confirm configuration. Check the serial port and firmware version.')
    print('HTTPS address saved. Wi-Fi applied from secrets.h.' if args.wifi_from_secrets else 'HTTPS address saved. Wi-Fi settings preserved.')
finally:
    if port.is_open:port.close()
