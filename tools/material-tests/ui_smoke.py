import subprocess,time,xml.etree.ElementTree as E,re,json,os,tempfile,shutil
from pathlib import Path
ADB=os.environ.get('ADB',shutil.which('adb') or str(Path(os.environ.get('ANDROID_HOME',str(Path.home()/'Android/Sdk')))/'platform-tools/adb'));DEV=os.environ.get('ANDROID_SERIAL','emulator-5554');PKG='com.th3nekit.finegram.materialpreview';COMP=PKG+'/com.th3nekit.finegram.appearance.material.MaterialStudioActivity'
OUTPUT=Path(os.environ.get('FINEGRAM_QA_DIR',tempfile.mkdtemp(prefix='finegram-material-qa-')));OUTPUT.mkdir(parents=True,exist_ok=True)
if not DEV.startswith('emulator-'):raise SystemExit('This smoke test is restricted to an emulator, not a personal phone.')
def adb(*args):return subprocess.check_output([ADB,'-s',DEV,*args],text=True,stderr=subprocess.STDOUT)
def nodes():
 adb('shell','uiautomator','dump','/sdcard/finegram-ui.xml')
 return list(E.fromstring(adb('shell','cat','/sdcard/finegram-ui.xml')).iter('node'))
def find(text):return next((n for n in nodes() if n.get('text')==text or n.get('content-desc')==text),None)
def expect(text):
 n=find(text)
 assert n is not None,'Missing '+text
 print('PASS:',text,flush=True);return n
def tap(text):
 n=expect(text);x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.6)
def shot(name):
 with open(str(OUTPUT/(name+'.png')),'wb') as f:subprocess.run([ADB,'-s',DEV,'exec-out','screencap','-p'],stdout=f,check=True)
def scroll():adb('shell','input','swipe','540','1870','540','680','350');time.sleep(.4)
adb('shell','cmd','locale','set-app-locales',PKG,'--user','0','--locales','en');adb('shell','cmd','uimode','night','no');adb('shell','am','force-stop',PKG);adb('shell','am','start','-n',COMP,'--ez','guide','true');time.sleep(.8)
expect('Step 1 of 4');expect('Sound off · tap to enable');shot('01-welcome')
tap('Make it mine');expect('The next steps can play sound');shot('02-sound-consent');tap('Continue silently');expect('Step 2 of 4')
tap('Lilac');expect('#9A82DB');shot('03-accent')
tap('Custom colour · #RRGGBB')
edit=next(n for n in nodes() if n.get('class')=='android.widget.EditText'); x1,y1,x2,y2=map(int,re.findall(r'\d+',edit.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));adb('shell','input','keyevent','123');adb('shell','input','keyevent',*(['67']*8));adb('shell','input','text','12ZZZZ');tap('Apply');expect('Enter six hex digits, for example #526FB8');shot('04-invalid-colour')
adb('shell','input','keyevent','123');adb('shell','input','keyevent',*(['67']*8));adb('shell','input','text','368a91');tap('Apply');expect('#368A91')
tap('Next');expect('Step 3 of 4');shot('05-wallpaper-step')
tap('Next');expect('Step 4 of 4');tap('Let’s go');expect('Material Studio');shot('06-studio')
print('UI_SMOKE_COMPLETE; screenshots:',OUTPUT,flush=True)
