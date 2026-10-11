# usage: analyze.py <mod ore-sampler dir> <control ore-sampler dir>  (prints the Markdown summary)
import csv, glob, os, sys, statistics as st, collections, math
mod_dir, ctl_dir = sys.argv[1], sys.argv[2]
def load(d, kind):
    rows=[]
    for f in sorted(glob.glob(os.path.join(d, f'*-{kind}.csv'))):
        rows += list(csv.DictReader(open(f)))
    return rows
M=[r for r in load(mod_dir,'counts') if not r['region'].startswith('vein_')]; C=[r for r in load(ctl_dir,'counts') if not r['region'].startswith('vein_')]
seeds=sorted({r['seed'] for r in M} & {r['seed'] for r in C}, key=int)
def agg(rows, seed=None, cat=None, host=None):
    t=collections.Counter()
    for r in rows:
        if seed and r['seed']!=seed: continue
        if cat and r['category'] not in (cat if isinstance(cat,tuple) else (cat,)): continue
        if host and r['host']!=host: continue
        for k in ('rock','iron_ore','raw_iron_block','iron_exposed','diamond_ore','diamond_exposed','copper_ore','raw_copper_block','removed_rock'):
            t[k]+=int(r[k])
    return t
def per100k(t, k): return 1e5*t[k]/t['rock'] if t['rock'] else float('nan')
out=[]
p=out.append
p(f"Seeds: {', '.join(seeds)} ({len(seeds)})\n")
# ---- iron by category/host
cats=['ordinary','deep_ocean','mountain','placer','ocean']
p("## Iron ore per 100,000 blocks of eligible rock (all seeds pooled)\n")
p("| Surface category | Host rock | Rock sampled (mod) | Control iron/100k | Mod iron/100k | Mod / control | Per-seed mod/control (min–max) |")
p("|---|---|---:|---:|---:|---:|---|")
for cat in cats:
    for host in ('stone','deepslate'):
        tm=agg(M,cat=cat,host=host); tc=agg(C,cat=cat,host=host)
        if not tm['rock']: continue
        ratios=[]
        for s in seeds:
            a=agg(M,s,cat,host); b=agg(C,s,cat,host)
            if a['rock']>20000 and b['iron_ore']>0: ratios.append(per100k(a,'iron_ore')/per100k(b,'iron_ore'))
        rng=f"{min(ratios):.2f}–{max(ratios):.2f} (n={len(ratios)})" if ratios else "n/a"
        p(f"| {cat} | {host} | {tm['rock']:,} | {per100k(tc,'iron_ore'):.0f} | {per100k(tm,'iron_ore'):.0f} | {per100k(tm,'iron_ore')/per100k(tc,'iron_ore'):.2f} | {rng} |")
p("")
# ordering checks per seed
p("## ORE-01 ordering per seed (mod): iron per 100k rock\n")
p("| Seed | Ordinary stone | Ordinary deepslate | Deep-ocean deepslate | deepslate/stone | deep-ocean/deepslate |")
p("|---|---:|---:|---:|---:|---:|")
for s in seeds:
    a=per100k(agg(M,s,'ordinary','stone'),'iron_ore'); b=per100k(agg(M,s,'ordinary','deepslate'),'iron_ore'); c=per100k(agg(M,s,'deep_ocean','deepslate'),'iron_ore')
    p(f"| {s} | {a:.0f} | {b:.0f} | {c:.0f} | {b/a:.2f} | {c/b:.2f} |")
p("")
# ---- diamonds
p("## Diamond ore (all sampled columns, both hosts)\n")
p("| Seed | Rock (mod) | Control diamonds | Mod diamonds | Control /100k | Mod /100k | Mod / control |")
p("|---|---:|---:|---:|---:|---:|---:|")
ratios=[]
for s in seeds:
    a=agg(M,s); b=agg(C,s)
    r=per100k(a,'diamond_ore')/per100k(b,'diamond_ore'); ratios.append(r)
    p(f"| {s} | {a['rock']:,} | {b['diamond_ore']} | {a['diamond_ore']} | {per100k(b,'diamond_ore'):.1f} | {per100k(a,'diamond_ore'):.1f} | {r:.3f} |")
a=agg(M); b=agg(C)
pooled=per100k(a,'diamond_ore')/per100k(b,'diamond_ore')
# Poisson approx CI for ratio of counts (rock nearly equal)
se=pooled*math.sqrt(1/max(a['diamond_ore'],1)+1/max(b['diamond_ore'],1))
p(f"| **pooled** | {a['rock']:,} | {b['diamond_ore']} | {a['diamond_ore']} | {per100k(b,'diamond_ore'):.1f} | {per100k(a,'diamond_ore'):.1f} | **{pooled:.3f}** |")
p(f"\nPer-seed ratio mean {st.mean(ratios):.3f}, median {st.median(ratios):.3f}, sd {st.stdev(ratios) if len(ratios)>1 else 0:.3f}, range {min(ratios):.3f}–{max(ratios):.3f}. Pooled ratio {pooled:.3f} ± {1.96*se:.3f} (95%, Poisson counting error).")
p(f"Exposed (touching air or fluid): control {b['diamond_exposed']/b['diamond_ore']*100:.1f}% of diamonds, mod {a['diamond_exposed']/a['diamond_ore']*100:.1f}%.\n")
# ---- removed rocks
p("## Removed rocks (granite, diorite, andesite, tuff) in natural terrain\n")
p("| Seed | Control blocks | Control /100k rock | Mod blocks |")
p("|---|---:|---:|---:|")
for s in seeds:
    a=agg(M,s); b=agg(C,s)
    p(f"| {s} | {b['removed_rock']:,} | {per100k(b,'removed_rock'):.0f} | {a['removed_rock']} |")
p("")
# ---- iron exposure & total iron
p("## Iron totals and exposure (all categories)\n")
p("| Label | Rock | Iron ore | Raw iron blocks | Iron/100k | Exposed iron % |")
p("|---|---:|---:|---:|---:|---:|")
for lab,t in (('control',agg(C)),('mod',agg(M))):
    p(f"| {lab} | {t['rock']:,} | {t['iron_ore']:,} | {t['raw_iron_block']:,} | {per100k(t,'iron_ore'):.0f} | {100*t['iron_exposed']/t['iron_ore']:.1f} |")
p("")
# ---- deposits
D=load(mod_dir,'deposits'); DC=load(ctl_dir,'deposits')
p("## Deposit evidence (sum over regions; best chunk per seed)\n")
p("| Kind | Seed | Control count | Mod count | Mod best chunk (count) | Mod example block |")
p("|---|---|---:|---:|---|---|")
for kind in ('mountain_iron_y64_plus','placer_shallow_iron','lava_adjacent_iron'):
    for s in seeds:
        dm=[r for r in D if r['seed']==s and r['kind']==kind]; dc=[r for r in DC if r['seed']==s and r['kind']==kind]
        best=max(dm,key=lambda r:int(r['best_chunk_count']))
        ex=next((f"{r['example_x']} {r['example_y']} {r['example_z']}" for r in sorted(dm,key=lambda r:-int(r['count'])) if r['example_x']), '')
        p(f"| {kind} | {s} | {sum(int(r['count']) for r in dc)} | {sum(int(r['count']) for r in dm)} | chunk {best['best_chunk_x']},{best['best_chunk_z']} ({best['best_chunk_count']}) | {ex} |")
p("")
# ---- trial chambers
T=load(mod_dir,'trial-chambers'); TC=load(ctl_dir,'trial-chambers')
p("## Trial chambers nearest the origin (blocks inside structure pieces)\n")
for s in seeds:
    tm={r['block']:int(r['count']) for r in T if r['seed']==s}; tc={r['block']:int(r['count']) for r in TC if r['seed']==s}
    keys=sorted(set(tm)|set(tc))
    p(f"Seed {s}: " + "; ".join(f"{k} {tc.get(k,0)}→{tm.get(k,0)}" for k in keys))
p("")

V=load(mod_dir,'veins'); VC=load(ctl_dir,'veins')
p("## Large ore veins (GEO-03): strongest vein found near spawn, its 5x5 chunks, vein y range\n")
p("| Seed | Vein | Chunk | Label | Ore | Raw blocks | Tuff | Granite | Stone | Deepslate |")
p("|---|---|---|---|---:|---:|---:|---:|---:|---:|")
for s_ in seeds:
    for kind in ('iron','copper'):
        for lab,rows in (('control',VC),('mod',V)):
            r=next((r for r in rows if r['seed']==s_ and r['vein']==kind),None)
            if r: p(f"| {s_} | {kind} | {r['chunk_x']},{r['chunk_z']} | {lab} | {int(r['ore']):,} | {int(r['raw_block']):,} | {int(r['tuff']):,} | {int(r['granite']):,} | {int(r['stone']):,} | {int(r['deepslate']):,} |")
p("")
print("\n".join(out))
