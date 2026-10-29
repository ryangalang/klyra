"""Render platform assets from KLYRA's approved 03 Rhythm vector silhouette.

Requires Pillow. All outputs are inside this repository; no application code,
data directories, package identities or signing configuration are changed.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
BG = '#161616'
FG = '#FAFAF9'
# Faithful flat vector trace of the selected concept's K; no dot or extra symbol.
COMMANDS = [
    ('M', 0, 35), ('L', 71, 0), ('L', 71, 137),
    ('C', 108, 68, 199, 51, 251, 9), ('L', 251, 52),
    ('C', 251, 95, 211, 109, 146, 127),
    ('C', 105, 137, 78, 153, 71, 172), ('L', 71, 185),
    ('C', 95, 152, 125, 151, 156, 167),
    ('C', 192, 187, 233, 210, 264, 211), ('L', 264, 284),
    ('C', 213, 290, 183, 256, 154, 230),
    ('C', 113, 190, 91, 187, 71, 205),
    ('L', 71, 301), ('L', 0, 266), ('Z',),
]
PATH = ' '.join(command[0] + ' '.join(str(v) for v in command[1:]) for command in COMMANDS)

def write(relative, content):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding='utf-8')

def svg(background):
    backing = f'<rect width="420" height="420" rx="92" fill="{BG}"/>' if background else ''
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="420" height="420" viewBox="0 0 420 420">{backing}<path fill="{FG}" transform="translate(78 60)" d="{PATH}"/></svg>\n'

def vector(viewport, tx, ty, scale=1):
    return f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">
    <group android:translateX="{tx}" android:translateY="{ty}" android:scaleX="{scale}" android:scaleY="{scale}">
        <path android:fillColor="#FFFFFFFF" android:pathData="{PATH}"/>
    </group>
</vector>
'''

write('branding/rhythm-mark.svg', svg(False))
write('branding/rhythm-icon.svg', svg(True))
for asset in ['desktopApp/src/main/composeResources/drawable/logo.svg',
              'desktopApp/src/main/composeResources/drawable/logo_mark.svg',
              'app/src/main/assets/Logo.svg']:
    write(asset, svg(True))
write('app/src/main/assets/LogoTransparent.svg', svg(False))
for asset in ['app/src/main/res/drawable/ic_logo.xml',
              'app/src/main/res/drawable/ic_notification_logo.xml',
              'sharedUi/src/commonMain/composeResources/drawable/ic_logo.xml']:
    write(asset, vector(320, 28, 9.5))
# Keep the complete mark within the adaptive launcher's safe area, including circular masks.
write('app/src/main/res/drawable/ic_launcher_foreground.xml',
      vector(108, 31.56, 28.415, .17).replace('android:width="24dp" android:height="24dp"',
                                           'android:width="108dp" android:height="108dp"'))

def outline():
    points = []
    current = (0, 0)
    for command in COMMANDS:
        op, *v = command
        if op in ('M', 'L'):
            current = (v[0], v[1])
            points.append(current)
        elif op == 'C':
            start = current
            for step in range(1, 65):
                t = step / 64
                u = 1 - t
                points.append((u**3*start[0]+3*u*u*t*v[0]+3*u*t*t*v[2]+t**3*v[4],
                               u**3*start[1]+3*u*u*t*v[1]+3*u*t*t*v[3]+t**3*v[5]))
            current = (v[4], v[5])
    return points

POINTS = outline()

def render(size, circular=False):
    factor = size * 4 / 420
    canvas = Image.new('RGBA', (size*4, size*4), (0,0,0,0))
    draw = ImageDraw.Draw(canvas)
    bounds = (0, 0, size*4-1, size*4-1)
    if circular:
        draw.ellipse(bounds, fill=BG)
    else:
        draw.rounded_rectangle(bounds, radius=92*factor, fill=BG)
    draw.polygon([((x+78)*factor,(y+60)*factor) for x,y in POINTS], fill=FG)
    return canvas.resize((size,size), Image.Resampling.LANCZOS)

for density,size in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    folder = ROOT / f'app/src/main/res/mipmap-{density}'
    render(size).save(folder/'ic_launcher.webp', lossless=True)
    render(size, circular=True).save(folder/'ic_launcher_round.webp', lossless=True)
render(512).save(ROOT/'app/src/main/ic_launcher-playstore.png')
render(512).save(ROOT/'branding/rhythm-icon.png')
render(256).save(ROOT/'desktopApp/packaging/icons/AppIcon.png')
render(256).save(ROOT/'desktopApp/packaging/icons/AppIcon.ico', sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
print('Generated Rhythm SVG, adaptive/monochrome/notification icons, Android density assets and Windows PNG/ICO.')
