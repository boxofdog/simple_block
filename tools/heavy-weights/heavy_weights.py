# Computes the iron/gold/copper/diamond/netherite content per item from vanilla recipe JSON.
# See README.md in this folder.
# usage: heavy_weights.py <data/minecraft/recipe dir> <output.json>
import json, os, sys, collections
from fractions import Fraction as F
D=sys.argv[1]; TAGS=os.path.join(os.path.dirname(D),'tags','item')
def tag_items(tag, seen=None):
    seen=seen or set()
    p=os.path.join(TAGS, tag.split(':')[1]+'.json')
    if not os.path.exists(p): return []
    out=[]
    for v in json.load(open(p))['values']:
        v=v if isinstance(v,str) else v['id']
        if v.startswith('#'):
            if v not in seen: seen.add(v); out+=tag_items(v[1:],seen)
        else: out.append(v)
    return out
def alts(ing):
    if isinstance(ing,list):
        r=[]
        for x in ing: r+=alts(x)
        return r
    if isinstance(ing,dict): ing=ing.get('item') or ing.get('tag') and '#'+ing['tag'] or ing.get('id')
    if ing.startswith('#'): return tag_items(ing[1:])
    return [ing]
recipes=[]
for f in sorted(os.listdir(D)):
    p=os.path.join(D,f)
    if os.path.isdir(p): continue
    j=json.load(open(p)); t=j['type'].split(':')[1]
    res=j.get('result'); 
    if not res: continue
    rid=res if isinstance(res,str) else res.get('id'); n=1 if isinstance(res,str) else res.get('count',1)
    slots=[]
    if t=='crafting_shaped':
        for row in j['pattern']:
            for ch in row:
                if ch!=' ': slots.append(alts(j['key'][ch]))
    elif t=='crafting_shapeless': slots=[alts(i) for i in j['ingredients']]
    elif t in ('smelting','blasting','smoking','campfire_cooking','stonecutting'): slots=[alts(j['ingredient'])]
    elif t=='smithing_transform': slots=[alts(j['template']),alts(j['base']),alts(j['addition'])]
    elif t=='crafting_transmute': slots=[alts(j['input']),alts(j['material'])]
    else: continue
    recipes.append((f[:-5],t,rid,n,slots))
MATERIALS=('iron','gold','copper','diamond','netherite')
def vec(material, amount): return {material: F(amount)}
def total(v): return sum(v.values(), F(0))
def add(a, b):
    r=dict(a)
    for m,x in b.items(): r[m]=r.get(m,F(0))+x
    return r
def scale(v, k): return {m:x*k for m,x in v.items()}
BASE={}
for m in ('iron','gold','copper'):
    BASE[f'minecraft:{m}_ingot']=vec(m,1); BASE[f'minecraft:raw_{m}']=vec(m,1); BASE[f'minecraft:{m}_nugget']=vec(m,F(1,9))
    BASE[f'minecraft:{m}_ore']=vec(m,1); BASE[f'minecraft:deepslate_{m}_ore']=vec(m,1)
BASE['minecraft:nether_gold_ore']=vec('gold',1)
# Diamond: one diamond weighs like one ingot; its ores like the metal ores (one per block).
BASE['minecraft:diamond']=vec('diamond',1); BASE['minecraft:diamond_ore']=vec('diamond',1); BASE['minecraft:deepslate_diamond_ore']=vec('diamond',1)
# Netherite weighs like diamond: an ingot 1 (not the 4 gold it is made from).
BASE['minecraft:netherite_ingot']=vec('netherite',1)
produced={r[2] for r in recipes}
families="chiseled_copper copper copper_bars copper_bulb copper_chain copper_chest copper_door copper_golem_statue copper_grate copper_lantern copper_trapdoor cut_copper cut_copper_slab cut_copper_stairs lightning_rod".split()
for f in families:
    base='minecraft:'+('copper_block' if f=='copper' else f)
    variants=[f'{st}_{f}' for st in ('exposed','weathered','oxidized')]
    variants=variants+['waxed_'+v for v in variants]+['waxed_'+('copper_block' if f=='copper' else f)]
    for v in variants:
        recipes.append(('oxidation:'+v,'oxidation','minecraft:'+v,1,[[base]]))
# Items that are found, not made: same metal as the item they are a state of.
for b in "water lava milk powder_snow cod salmon pufferfish tropical_fish axolotl tadpole sulfur_cube".split():
    recipes.append(('filled:'+b,'manual','minecraft:'+b+'_bucket',1,[['minecraft:bucket']]))
for a in ('chipped_anvil','damaged_anvil'):
    recipes.append(('damage:'+a,'manual','minecraft:'+a,1,[['minecraft:anvil']]))
recipes.append(('use:filled_map','manual','minecraft:filled_map',1,[['minecraft:map']]))
recipes.append(('found:copper_golem_statue','manual','minecraft:copper_golem_statue',1,[['minecraft:copper_block']]))
recipes.append(('enchanted:enchanted_golden_apple','manual','minecraft:enchanted_golden_apple',1,[['minecraft:golden_apple']]))
# Loot-only metal gear: no recipe; smelts to one nugget, the weight it is given (exploit-safe minimum).
for item,nug in [('chainmail_helmet','iron'),('chainmail_chestplate','iron'),('chainmail_leggings','iron'),('chainmail_boots','iron'),
                 ('iron_horse_armor','iron'),('iron_nautilus_armor','iron'),('golden_horse_armor','gold'),('golden_nautilus_armor','gold'),
                 ('copper_horse_armor','copper'),('copper_nautilus_armor','copper')]:
    recipes.append(('loot:'+item,'manual','minecraft:'+item,1,[['minecraft:'+nug+'_nugget']]))
# Loot-only diamond gear cannot be smelted; it weighs one diamond.
for item in ('diamond_horse_armor','diamond_nautilus_armor'):
    recipes.append(('loot:'+item,'manual','minecraft:'+item,1,[['minecraft:diamond']]))
def netherite_upgrade(t, slots):
    """Smithing a diamond item into netherite: the result weighs what the diamond item did, as netherite."""
    return t=='smithing_transform' and len(slots)==3 and slots[2]==['minecraft:netherite_ingot']
# 1. Items that can contain metal: a base metal item, or made by a recipe with a slot whose
#    every alternative can contain metal.
metal=set(BASE)
grew=True
while grew:
    grew=False
    for name,t,rid,n,slots in recipes:
        if rid in metal: continue
        if any(s and all(x in metal for x in s) for s in slots):
            metal.add(rid); grew=True
# 2. Cheapest content among recipes (by total weight), everything else 0. Unknown (None) until resolved.
w={x:None for x in metal}
w.update(BASE)
def val_of(x):
    return w[x] if x in w else {}
def cheapest(slot):
    known=[v for v in (val_of(x) for x in slot) if v is not None]
    return min(known, key=total) if known else None
changed=True; it=0
while changed:
    changed=False; it+=1
    for name,t,rid,n,slots in recipes:
        if rid in BASE or rid not in metal: continue
        if netherite_upgrade(t, slots):
            base=cheapest(slots[1])
            if base is None: continue
            cost=vec('netherite', total(base))
        else:
            cost={}; unknown=False
            for s_ in slots:
                if not s_: continue
                c=cheapest(s_)
                if c is None: unknown=True; break
                cost=add(cost,c)
            if unknown: continue
        v={m:x/n for m,x in cost.items() if x>0}
        if w[rid] is None or total(v)<total(w[rid]):
            w[rid]=v; changed=True
print("iterations",it,"heavy-capable",len(metal),"unresolved",[k for k,v in w.items() if v is None])
w=collections.defaultdict(dict, {k:(v if v is not None else {}) for k,v in w.items()})
heavy={k:v for k,v in w.items() if total(v)>0}
print("heavy",len(heavy))
# check: no recipe's result weighs more than its cheapest inputs
bad=[]
for name,t,rid,n,slots in recipes:
    cin=sum((min(total(w[x]) for x in s_) for s_ in slots if s_), F(0)); cout=total(w[rid])*n
    if cout>cin: bad.append((name,float(cin),float(cout)))
print("recipes creating weight:",bad[:20])
mixed={k:v for k,v in heavy.items() if len(v)>1}
print("items of more than one material:",{k:{m:str(x) for m,x in v.items()} for k,v in mixed.items()})
for k,v in heavy.items():
    for m,x in v.items():
        assert (x*216).denominator==1, (k,m,x)
print("all weights are whole 1/216 units")
out={m:{k:str(v[m]) for k,v in sorted(heavy.items()) if v.get(m)} for m in MATERIALS}
json.dump(out, open(sys.argv[2],'w'), indent=1)
