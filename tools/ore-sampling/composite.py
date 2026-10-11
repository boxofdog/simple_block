import csv, glob, os, sys
from PIL import Image, ImageDraw, ImageFont
# usage: composite.py <mod ore-sampler dir> <control ore-sampler dir> <output image dir (deposits.csv in its parent)>
M=sys.argv[1]; C=sys.argv[2]; E=sys.argv[3]
SCALE=3; TOP=319
font=ImageFont.load_default()
def find(d, seed, label, region, kind):
    g=glob.glob(os.path.join(d, f'{seed}-{label}-{region}-{kind}*.png'))
    return g[0] if g else None
def crop_y(im, ytop, ybot, top=TOP):
    return im.crop((0,(top-ytop)*SCALE, im.width,(top-ybot+1)*SCALE))
def titled(im, title):
    out=Image.new('RGB',(im.width, im.height+16),'white'); out.paste(im,(0,16))
    ImageDraw.Draw(out).text((2,2),title,fill='black',font=font); return out
def side(images, gap=8):
    w=sum(i.width for i in images)+gap*(len(images)-1); h=max(i.height for i in images)
    out=Image.new('RGB',(w,h),'white'); x=0
    for i in images: out.paste(i,(x,0)); x+=i.width+gap
    return out
def axis(im, ytop, ybot, step=16):
    # left ruler with y labels
    out=Image.new('RGB',(im.width+34, im.height),'white'); out.paste(im,(34,0)); d=ImageDraw.Draw(out)
    off=16 if im.height>(ytop-ybot+1)*SCALE else 0
    for y in range(ybot - ybot%step, ytop+1, step):
        if y<ybot: continue
        py=off+(ytop-y)*SCALE
        d.line((28,py,34,py),fill='black'); d.text((0,py-5),str(y),fill='black',font=font)
    return out
def pair(seed, region, kind, ytop, ybot, name, title):
    a=find(C,seed,'control',region,kind); b=find(M,seed,'mod',region,kind)
    if not a or not b: print('missing',seed,region,kind); return
    ia=titled(crop_y(Image.open(a),ytop,ybot),'control (pre-change)'); ib=titled(crop_y(Image.open(b),ytop,ybot),'Simple Block')
    img=axis(side([ia,ib]),ytop,ybot)
    out=Image.new('RGB',(img.width,img.height+18),'white'); out.paste(img,(0,18)); ImageDraw.Draw(out).text((2,2),title,fill='black',font=font)
    out.save(os.path.join(E,name)); print('wrote',name,out.size)
def single(path, ytop, ybot, name, title, top=TOP):
    im=Image.open(path); im=crop_y(im,ytop,ybot,top); im=axis(titled(im,title),ytop,ybot); im.save(os.path.join(E,name)); print('wrote',name,im.size)
def maps(seed, region, name, title):
    a=find(C,seed,'control',region,'ironmap'); b=find(M,seed,'mod',region,'ironmap')
    ia=titled(Image.open(a),'control'); ib=titled(Image.open(b),'Simple Block')
    img=side([ia,ib]); out=Image.new('RGB',(img.width,img.height+18),'white'); out.paste(img,(0,18)); ImageDraw.Draw(out).text((2,2),title,fill='black',font=font)
    out.save(os.path.join(E,name)); print('wrote',name,out.size)
legend=[('stone',0x7F7F7F),('deepslate',0x3C3C44),('granite',0xC77B5F),('diorite',0xE8E4D8),('andesite',0x9AA0A6),('tuff',0x6D6B4E),
        ('iron ore',0xD81B60),('raw iron block',0x8E0038),('diamond ore',0x00E5FF),('copper ore/raw',0x2E7D32),('water',0x3F76E4),('lava',0xFF6A00),('other',0xB59A6A),('air','FFFFFF')]
lg=Image.new('RGB',(150,len(legend)*14+4),'white'); d=ImageDraw.Draw(lg)
for i,(n,c) in enumerate(legend):
    col=c if isinstance(c,int) else int(c,16)
    d.rectangle((2,2+i*14,14,13+i*14),fill=((col>>16)&255,(col>>8)&255,col&255),outline='black'); d.text((20,2+i*14),n,fill='black',font=font)
lg.save(os.path.join(E,'legend.png'))
# choose regions
dep=list(csv.DictReader(open(os.path.join(os.path.dirname(E),'deposits.csv'))))
def best(kind):
    r=max((r for r in dep if r['label']=='mod' and r['kind']==kind), key=lambda r:int(r['best_chunk_count']))
    return r['seed'], r['region']
pair('12345','ordinary_1','slice',100,-64,'ordinary-terrain-slice.png','Seed 12345, taiga region, x-y slice through the centre (y 100 to -64): granite/diorite/andesite/tuff blobs gone; iron in stone and deepslate')
pair('12345','deep_ocean_0','slice',70,-64,'deep-ocean-slice.png','Seed 12345, deep ocean region, centre slice: richer iron in the deepslate under deep water')
s_,r_=best('mountain_iron_y64_plus'); mod_dep=find(M,s_,'mod',r_,'deposit'); single(mod_dep,200,30,'mountain-banded-iron-slice.png',f'Seed {s_} {r_}: slice through the richest banded iron row (y 200 to 30)')
maps(s_,r_,'mountain-banded-iron-map.png',f'Seed {s_} {r_}: iron ore at y>=64 per column (80x80 blocks; brighter = more)')
s2,r2=best('placer_shallow_iron'); single(find(M,s2,'mod',r2,'deposit'),90,20,'placer-iron-slice.png',f'Seed {s2} {r2}: slice through the richest alluvial row (y 90 to 20)')
maps(s2,r2,'placer-iron-map.png',f'Seed {s2} {r2}: iron ore within 8 blocks of the surface per column')
s3,r3=best('lava_adjacent_iron'); single(find(M,s3,'mod',r3,'slice'),-20,-64,'geothermal-region-slice.png',f'Seed {s3} {r3}: centre slice of the region with the most lava-adjacent iron (y -20 to -64)')
for seed in ('12345','777777'):
    for vein,top,bot in (('iron',-4,-64),('copper',54,-4)):
        a=glob.glob(os.path.join(C,f'{seed}-control-vein_{vein}-slice-z*.png')); b=glob.glob(os.path.join(M,f'{seed}-mod-vein_{vein}-slice-z*.png'))
        if a and b:
            ia=titled(Image.open(a[0]),'control'); ib=titled(Image.open(b[0]),'Simple Block'); img=axis(side([ia,ib]),top,bot)
            out=Image.new('RGB',(img.width,img.height+18),'white'); out.paste(img,(0,18)); ImageDraw.Draw(out).text((2,2),f'Seed {seed}: strongest {vein} vein near spawn, filler removed, ore and raw blocks kept',fill='black',font=font)
            out.save(os.path.join(E,f'{vein}-vein-{seed}.png')); print('wrote',vein,seed,out.size)
