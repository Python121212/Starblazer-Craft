"""Generate the root Markdown catalog and software-rendered PNG model previews.

Requires requirements-dev.txt. Does not deploy or change the public site.
"""
import argparse
import io
import math
from pathlib import Path
import tempfile
from PIL import Image, ImageDraw
from asset_catalog import ROOT, collect

SIDES = {
    'north': lambda a,b: [(b[0],a[1],a[2]), (a[0],a[1],a[2]), (a[0],b[1],a[2]), (b[0],b[1],a[2])],
    'south': lambda a,b: [(a[0],a[1],b[2]), (b[0],a[1],b[2]), (b[0],b[1],b[2]), (a[0],b[1],b[2])],
    'west': lambda a,b: [(a[0],a[1],a[2]), (a[0],a[1],b[2]), (a[0],b[1],b[2]), (a[0],b[1],a[2])],
    'east': lambda a,b: [(b[0],a[1],b[2]), (b[0],a[1],a[2]), (b[0],b[1],a[2]), (b[0],b[1],b[2])],
    'up': lambda a,b: [(a[0],b[1],b[2]), (b[0],b[1],b[2]), (b[0],b[1],a[2]), (a[0],b[1],a[2])],
    'down': lambda a,b: [(a[0],a[1],a[2]), (b[0],a[1],a[2]), (b[0],a[1],b[2]), (a[0],a[1],b[2])],
}


def rotate(p, axis, degrees):
    p=list(p); i,j=(axis+1)%3,(axis+2)%3
    c,s=math.cos(math.radians(degrees)),math.sin(math.radians(degrees))
    p[i],p[j]=p[i]*c-p[j]*s,p[i]*s+p[j]*c
    return p


def default_uv(side,a,b):
    x,y,z=a; X,Y,Z=b
    return {'down':[x,16-Z,X,16-z], 'up':[x,z,X,Z], 'north':[16-X,16-Y,16-x,16-y],
            'south':[x,16-Y,X,16-y], 'west':[z,16-Y,Z,16-y], 'east':[16-Z,16-Y,16-z,16-y]}[side]


def render(model, textures, held=False):
    size=320
    image=Image.new('RGBA',(size,size),(19,30,46,255))
    if not model['elements']:
        ref=model['textures'].get('layer0')
        if ref not in textures:
            raise ValueError(f"{model['id']}: cannot render model; add support before updating index")
        icon=textures[ref].resize((224,224),Image.Resampling.NEAREST)
        image.alpha_composite(icon,(48,48))
        return image
    if model.get('warning') or model['source'].get('loader'):
        raise ValueError(f"Unsupported preview: {model['id']}")
    display=model['source'].get('display',{}).get('firstperson_righthand',{}) if held else {}
    angles=display.get('rotation',[0,0,0])
    scale=display.get('scale',[1,1,1]); translation=display.get('translation',[0,0,0])
    def project(p, element):
        if 'rotation' in element:
            r=element['rotation']; origin=r['origin']; axis='xyz'.index(r['axis'])
            p=rotate([p[i]-origin[i] for i in range(3)],axis,r['angle'])
            if r.get('rescale'):
                for i in range(3):
                    if i!=axis: p[i]/=math.cos(math.radians(r['angle']))
            p=[p[i]+origin[i] for i in range(3)]
        p=[(p[i]-8)*scale[i] for i in range(3)]
        for axis,angle in enumerate(angles): p=rotate(p,axis,angle)
        p=[p[i]+translation[i] for i in range(3)]
        p=rotate(p,1,30);p=rotate(p,0,-18)
        return (160+p[0]*14,170-p[1]*14,p[2])
    depth=[-float('inf')]*(size*size)
    pixels=image.load()
    for element in model['elements']:
        for side,face in element.get('faces',{}).items():
            points=[project(p,element) for p in SIDES[side](element['from'],element['to'])]
            tex=textures.get(face['texture'])
            if tex is None: raise ValueError('Missing PNG: '+face['texture'])
            u,v,U,V=face.get('uv',default_uv(side,element['from'],element['to']))
            uv=[(u,V),(U,V),(U,v),(u,v)]
            turns=(face.get('rotation',0)//90)%4
            if turns: uv=uv[-turns:]+uv[:-turns]
            shade={'up':1,'down':.55,'north':.68,'south':.94,'east':.78,'west':.66}[side]
            for ids in [(0,1,2),(0,2,3)]:
                a,b,c=[points[i] for i in ids]; t=[uv[i] for i in ids]
                det=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
                if abs(det)<1e-8: continue
                for y in range(max(0,int(min(a[1],b[1],c[1]))),min(size,int(max(a[1],b[1],c[1]))+1)):
                    for x in range(max(0,int(min(a[0],b[0],c[0]))),min(size,int(max(a[0],b[0],c[0]))+1)):
                        w0=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/det
                        w1=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/det
                        w=[w0,w1,1-w0-w1]
                        if min(w)<-1e-8: continue
                        z=sum(w[i]*[a,b,c][i][2] for i in range(3))
                        if z<=depth[y*size+x]: continue
                        tx=min(tex.width-1,max(0,int(sum(w[i]*t[i][0] for i in range(3))*tex.width/16)))
                        ty=min(tex.height-1,max(0,int(sum(w[i]*t[i][1] for i in range(3))*tex.height/16)))
                        color=tex.getpixel((tx,ty))
                        if not color[3]: continue
                        depth[y*size+x]=z
                        pixels[x,y]=tuple(int(ch*shade) for ch in color[:3])+(255,)
    return image


def png(image):
    out=io.BytesIO();image.save(out,format='PNG');return out.getvalue()


def creative_tab_preview(texture):
    """Documentation-only schematic, not a game screenshot or runtime GUI texture."""
    image = Image.new('RGBA', (360, 290), (28, 38, 52, 255))
    d = ImageDraw.Draw(image)
    d.text((16, 12), 'CREATIVE INVENTORY - LAYOUT SKETCH', fill='white')
    d.rectangle((16, 48, 344, 270), fill='#c6c6c6', outline='#ffffff', width=2)
    d.rectangle((22, 29, 62, 55), fill='#c6c6c6', outline='white', width=2)
    icon = texture.resize((24, 24), Image.Resampling.NEAREST)
    image.alpha_composite(icon, (30, 30))
    d.text((30, 65), 'Starblazer Craft', fill='#303030')
    for row in range(5):
        for col in range(9):
            x, y = 30 + col * 33, 88 + row * 31
            d.rectangle((x, y, x + 29, y + 27), fill='#8b8b8b', outline='#eeeeee')
    image.alpha_composite(icon, (33, 90))
    d.text((30, 249), 'Wrench / more mod items in future', fill='#303030')
    return image


def generate():
    models, paths, dimensions=collect([ROOT/'src/main/resources/assets',ROOT/'docs/examples/assets'])
    textures={key:Image.open(path).convert('RGBA') for key,path in paths.items()}
    outputs={}
    outputs['docs/images/catalog/ui/creative-tab.png']=png(creative_tab_preview(textures['starblazercraft:item/wrench']))
    lines=['# テクスチャ一覧', '', 'アイテム・ブロック・UI を追加／変更するたびに、この一覧と PNG 見本を更新します。',
           '', '**画像は JSON から生成した簡易見本で、ゲーム内スクリーンショットではありません。**',
           '手持ち画像は一人称右手の `display` 設定を適用したモデル単体の見本です（プレイヤーの手は描画しません）。',
           'ブロック画像は設置形状の見本です。陰影・透過・照明・カメラは Minecraft と完全には一致しません。',
           'サイトの公開とは独立して更新します。', '', '## テクスチャ', '', '| ID | 区分 | PNG（拡大） | 元画像 |', '|---|---|---|---|']
    def output_path(key,kind):
        ns,name=key.split(':');return 'docs/images/catalog/'+ns+'/'+name+'-'+kind+'.png'
    for key,path in sorted(paths.items()):
        target=output_path(key,'texture')
        outputs[target]=png(textures[key].resize((128,128),Image.Resampling.NEAREST))
        status='実装素材' if 'src/main/resources' in path.as_posix() else '説明用・未登録'
        if ':gui/' in key: status+=' / UI'
        lines.append(f'| `{key}` | {status} | ![{key}]({target}) | [{dimensions[key][0]}×{dimensions[key][1]}]({path.relative_to(ROOT).as_posix()}) |')
    lines+=['','## 3D・手持ち見本','','| モデル | 種別 | PNG 見本 | JSON |','|---|---|---|---|']
    for model in models:
        held=':item/' in model['id']; kind='held' if held else 'placed'
        target=output_path(model['id'],kind)
        outputs[target]=png(render(model,textures,held))
        relative='assets/'+model['id'].replace(':','/models/')+'.json'
        source=next(root/relative for root in [ROOT/'src/main/resources',ROOT/'docs/examples'] if (root/relative).is_file())
        status='手持ち' if held else '設置時'
        if 'docs/examples' in source.as_posix(): status+='（説明用・未登録）'
        lines.append(f"| `{model['id']}` | {status} | ![{status}]({target}) | [JSON]({source.relative_to(ROOT).as_posix()}) |")
    lines+=['', '## レンチの参考画像', '', '[IMG_0185.png](画像/IMG_0185.png) をもとに、銀色の本体・黒い輪郭・斜めの持ち手を再現しています。', '参考画像のサイズは自由です。本番は16×16、手持ちモデルは輪郭に沿って厚みを付けています。', '', '## UI','','クリエイティブ専用タブ：**Starblazer Craft**（ID: `starblazercraft:main`）。レンチをアイコンとし、登録済みの本MODのアイテムを一覧表示します。通常のサバイバル用インベントリを増やす機能ではありません。', '', '![専用タブの配置イメージ](docs/images/catalog/ui/creative-tab.png)', '', '上図は説明用の配置図です。実機スクリーンショットではなく、実際の寸法・タブ位置・アイコン描画は異なります。背景・タブ枠はバニラUIを使用し、新規のゲーム用UIテクスチャはありません。レンチは従来の「道具と実用品」タブからも取得できます。',
            '', '## 更新方法', '', '```sh', 'python3 -m pip install -r requirements-dev.txt',
            'python3 scripts/generate_texture_index.py', 'python3 scripts/generate_texture_index.py --check','```',
            '', '一覧とモデル見本は自動生成です。説明の変更は `scripts/generate_texture_index.py` に反映してください。',
            '未対応モデルは生成エラーにして、誤った見本を黙って掲載しない方針です。', 'UI の画面全体の見本・用途は追加時に生成処理も拡張します。','']
    outputs['テクスチャ一覧.md']='\n'.join(lines).encode()
    return outputs


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    outputs=generate()
    stale=[]
    for name,content in outputs.items():
        path=ROOT/name
        if args.check:
            if not path.exists() or path.read_bytes()!=content: stale.append(name)
        else:
            path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(content)
    if stale: raise SystemExit('Regenerate texture index: '+', '.join(stale))
    print('Texture index verified' if args.check else 'Texture index generated')
