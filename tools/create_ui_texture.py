from PIL import Image
from pathlib import Path
import random
p=Path(__file__).resolve().parents[1]/'src/main/resources/assets/civitatum_inventory/textures/gui'
p.mkdir(parents=True,exist_ok=True)
rng=random.Random(1701)
im=Image.new('RGBA',(32,32))
for y in range(32):
 for x in range(32):
  n=rng.randint(-8,8);vein=3 if ((x+y*3)%23)<3 else 0
  im.putpixel((x,y),(max(0,39+n+vein),max(0,35+n+vein),max(0,29+n),102))
im.save(p/'stone.png')
assert set(im.getchannel('A').get_flattened_data())=={102}
