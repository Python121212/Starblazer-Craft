"""Generate the original 16x16 core texture using only the Python standard library."""
from pathlib import Path
from generate_texture_examples import write_png

ROOT = Path(__file__).resolve().parents[1]

def generate():
    pixels=[]
    for y in range(16):
        row=[]
        for x in range(16):
            edge=min(x,y,15-x,15-y)
            if edge==0: c=(27,34,44)
            elif edge==1: c=(121,139,151) if x+y<16 else (62,76,90)
            elif (x,y) in ((2,2),(13,2),(2,13),(13,13)): c=(193,210,214)
            elif 5<=x<=10 and 5<=y<=10:
                if x in (5,10) or y in (5,10): c=(22,91,105)
                else: c=(154,255,245) if x+y<16 else (37,198,211)
            elif (x in (3,12) and 4<=y<=11) or (y in (3,12) and 4<=x<=11): c=(37,119,137)
            elif (x in (7,8) and edge>1) or (y in (7,8) and edge>1): c=(35,191,198)
            else: c=(51,65,80) if (x+y)%2 else (59,73,88)
            row.append(c+(255,))
        pixels.append(row)
    write_png(ROOT/'src/main/resources/assets/starblazercraft/textures/block/core.png',pixels)

if __name__=='__main__': generate()
