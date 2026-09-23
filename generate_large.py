exec(open('level_core.py').read())
import argparse, time

def large_level(lid,diff,n,rng):
    for attempt in range(100):
        p=random_perm(n,rng); g=grow_regions(n,p,rng)
        if not validate(g,p): continue
        # Start with a substantial but still playable set of fixed starter clues.
        rows=list(range(n)); rng.shuffle(rows)
        for k in range(max(6,n-6), n+1):
            clues=[(r,p[r]) for r in rows[:k]]
            if alternate(g,p,clues) is None:
                return {'id':lid,'size':n,'difficulty':diff,'colors':n,'grid':g,'solution':[[r,p[r]] for r in range(n)],'startingClues':[[r,c] for r,c in clues],'maxMistakes':3,'hints':{'itemFinder':True,'logicHint':True,'threeCellReveal':True},'metadata':{'patternFamily':f'F{((lid-1)//2)+1:03d}','estimatedDifficulty':score(n,g,clues)}}
    raise RuntimeError('large fail')

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('start',type=int); ap.add_argument('end',type=int); ap.add_argument('diff'); ap.add_argument('size',type=int); ap.add_argument('seed',type=int); a=ap.parse_args()
    rng=random.Random(a.seed); out=[]; t=time.time()
    for lid in range(a.start,a.end+1):
        out.append(large_level(lid,a.diff,a.size,rng))
        if lid%10==0: print(lid,round(time.time()-t,1),flush=True)
    name=f'levels_{a.start:03d}_{a.end:03d}.json'; (OUT/name).write_text(json.dumps(out,separators=(',',':'))); print('WROTE',name)
if __name__=='__main__':main()
