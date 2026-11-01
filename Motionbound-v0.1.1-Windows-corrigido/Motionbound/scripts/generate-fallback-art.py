"""Generate original procedural 16x16 prototype sprites. Requires Pillow.
These are fallbacks and NOT part of Kenney's Tiny Dungeon pack.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import random
ROOT=Path(__file__).resolve().parents[1] / 'assets'
T=16
random.seed(103)
C={'outline':'#30263c','stone':'#67738c','stone2':'#94a2b9','sand':'#ba7e56','sand2':'#d89b63','dark':'#4b3345','gold':'#ffc56d','light':'#f1d6a5','red':'#d04f60','teal':'#61bfbd','blue':'#4a92c4','steel':'#c6d5d9'}

def tile(index):
    im=Image.new('RGBA',(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
    if index==0:
        d.rectangle([0,0,15,15],fill='#775451')
        d.rectangle([0,0,15,3],fill='#825954')
        for px,py in [(1,2),(9,6),(12,12),(4,13),(14,3),(3,8)]:d.point((px,py),fill='#986459')
        for px,py in [(5,5),(12,1),(8,14)]:d.point((px,py),fill='#573e4c')
    elif index==49:
        d.rectangle([0,0,15,15],fill='#805950')
        for i in range(11):
            x=random.randrange(16);y=random.randrange(16);d.point((x,y),fill='#ad725a')
    elif index==40:
        d.rectangle([0,0,15,15],fill='#2c2b40');d.rectangle([0,0,15,1],fill='#a3b2c2')
        d.rectangle([1,2,14,14],fill='#66758b');d.line([1,9,14,9],fill='#334454',width=2)
        d.line([8,2,8,8],fill='#334454',width=2);d.line([4,10,4,15],fill='#334454',width=2)
        d.line([1,2,14,2],fill='#8d9aac');d.line([1,10,14,10],fill='#7686a0')
    elif index==14:
        for x,y in [(3,5),(9,12),(13,2)]: d.rectangle([x,y,x+2,y+1],fill='#513b4b')
    elif index==66:
        d.rectangle([2,3,13,14],fill='#443244');d.rectangle([3,4,12,13],fill='#98613e');
        d.line([4,5,11,12],fill='#d0a067',width=2);d.line([11,5,4,12],fill='#d0a067',width=2)
        d.rectangle([2,3,13,4],fill='#e2a95f')
    elif index==82:
        d.ellipse([3,2,12,14],fill='#4e3042');d.rectangle([4,4,11,12],fill='#9c684d');d.line([4,6,11,6],fill='#c3a17a',width=2);d.line([4,11,11,11],fill='#d8b083')
    elif index==89:
        d.rectangle([2,5,13,14],fill='#3d2d40');d.rectangle([3,6,12,12],fill='#aa693d');d.rectangle([3,5,12,7],fill='#e1a85b');d.rectangle([7,6,8,10],fill='#f3d589')
    else:
        d.rectangle([0,0,15,15],fill='#72504c')
    return im
sheet=Image.new('RGBA',(12*T,11*T),(0,0,0,0))
for idx in range(132):sheet.paste(tile(idx),(idx%12*T,idx//12*T))
sheet.save(ROOT/'fallback'/'tiles.png')

def hero(direction,frame):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    step=[0,1,0,-1][frame]
    # legs, cloak, boots
    d.rectangle([4,11+max(0,step),6,14+max(0,step)], fill='#354657')
    d.rectangle([9,11+max(0,-step),11,14+max(0,-step)], fill='#354657')
    d.rectangle([4,6,11,12], fill='#2d516d')
    d.rectangle([3,7,12,11], fill='#4484a0')
    d.rectangle([5,3,10,7], fill='#e7d4ab')
    d.rectangle([4,1,11,4], fill='#536b8b')
    d.rectangle([5,0,10,2], fill='#b5c4cf')
    d.rectangle([6,5,9,5], fill='#5c4e58')
    d.rectangle([6,10,9,10],fill='#d8ad66')
    if direction==0: # front/down
        d.point((6,5),fill='#241f31');d.point((9,5),fill='#241f31')
    elif direction==1: # back/up
        d.rectangle([5,3,10,7], fill='#637993');d.rectangle([6,3,9,5],fill='#3b506c')
    elif direction==2: # left
        d.point((5,5),fill='#241f31');d.rectangle([3,6,5,9],fill='#8c9fb2')
    else: # right
        d.point((10,5),fill='#241f31');d.rectangle([10,6,12,9],fill='#8c9fb2')
    # sword hilt at side
    if direction in (0,1):
        d.line([12,5+step,14,10+step],fill='#d8e5d3',width=2)
        d.line([11,8+step,15,7+step],fill='#e2ba64',width=2)
    elif direction==2:
        d.line([1,5+step,3,10+step],fill='#d8e5d3',width=2)
    else:
        d.line([13,5+step,15,10+step],fill='#d8e5d3',width=2)
    return im
im=Image.new('RGBA',(64,64))
for r in range(4):
    for c in range(4):im.paste(hero(r,c),(c*16,r*16))
im.save(ROOT/'characters'/'hero.png')

def slime(frame):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    bob=(frame%2)
    d.ellipse([1,6+bob,14,14],fill='#284a44');d.ellipse([2,5+bob,13,13],fill='#58bca0')
    d.rectangle([4,6+bob,10,7+bob],fill='#7dd3ad');d.rectangle([5,8+bob,6,9+bob],fill='#263445');d.rectangle([10,8+bob,11,9+bob],fill='#263445')
    d.point((8,11+bob),fill='#3d6b69');return im
im=Image.new('RGBA',(64,16))
for c in range(4):im.paste(slime(c),(c*16,0))
im.save(ROOT/'characters'/'slime.png')

def skeleton(frame):
    im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
    bob=(frame%2)
    d.rectangle([3,6+bob,12,13+bob],fill='#3e3e53');d.rectangle([5,6+bob,10,11+bob],fill='#d7c9ad')
    d.rectangle([4,2+bob,11,7+bob],fill='#e7dcc4')
    d.rectangle([5,4+bob,6,5+bob],fill='#282435');d.rectangle([9,4+bob,10,5+bob],fill='#282435')
    d.rectangle([7,6+bob,8,7+bob],fill='#6c5a5f')
    d.rectangle([4,13,6,15],fill='#a6a29e');d.rectangle([9,13,11,15],fill='#a6a29e')
    d.line([13,7+bob,15,12+bob],fill='#c8af92',width=2)
    return im
im=Image.new('RGBA',(64,16))
for c in range(4):im.paste(skeleton(c),(c*16,0))
im.save(ROOT/'characters'/'skeleton.png')
icon=hero(0,0);icon.resize((64,64),Image.Resampling.NEAREST).save(ROOT/'icon.png')
print('Created original procedural fallback sprite sheets in',ROOT)
