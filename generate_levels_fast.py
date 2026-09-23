import json, random, collections, hashlib, sys, time
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/'app/src/main/assets/levels'
OUT.mkdir(parents=True, exist_ok=True)
DIFFS=[('very_easy',8,50),('easy',8,100),('normal',9,150),('medium',10,200),('hard',11,200),('very_hard',12,150),('expert',14,100),('master',15,50)]
DIRS=((1,0),(-1,0),(0,1),(0,-1))

def random_perm(n,rng):
    p=list(range(n))
    for _ in range(1000):
        rng.shuffle(p)
        if all(abs(p[i]-p[i+1])!=1 for i in range(n-1)): return p[:]
    # deterministic fallback
    for p in __import__('itertools').permutations(range(n)):
        if all(abs(p[i]-p[i+1])!=1 for i in range(n-1)): return list(p)
    raise RuntimeError('no permutation')

def grow_regions(n,p,rng):
    g=[[-1]*n for _ in range(n)]
    front=[]
    for r,c in enumerate(p): g[r][c]=r
    for r,c in enumerate(p):
        for dr,dc in DIRS:
            rr,cc=r+dr,c+dc
            if 0<=rr<n and 0<=cc<n and g[rr][cc]<0: front.append((rr,cc,r,c))
    while front:
        # Favor compactness by picking among a small random sample.
        k=min(8,len(front)); best=None
        for _ in range(k):
            i=rng.randrange(len(front)); item=front[i]
            rr,cc,pr,pc=item
            score=sum(1 for dr,dc in DIRS if 0<=rr+dr<n and 0<=cc+dc<n and g[rr+dr][cc+dc]==g[pr][pc])
            if best is None or score>best[0]: best=(score,i,item)
        _,i,(rr,cc,pr,pc)=best
        front.pop(i)
        if g[rr][cc]>=0: continue
        g[rr][cc]=g[pr][pc]
        for dr,dc in DIRS:
            r2,c2=rr+dr,cc+dc
            if 0<=r2<n and 0<=c2<n and g[r2][c2]<0: front.append((r2,c2,rr,cc))
    return g

def connected(g,rid):
    n=len(g); cells=[]
    for r in range(n):
        for c in range(n):
            if g[r][c]==rid: cells.append((r,c))
    if not cells:return False
    seen={cells[0]}; stack=[cells[0]]
    for r,c in stack:
        for dr,dc in DIRS:
            rr,cc=r+dr,c+dc
            if 0<=rr<n and 0<=cc<n and g[rr][cc]==rid and (rr,cc) not in seen:
                seen.add((rr,cc)); stack.append((rr,cc))
    return len(seen)==len(cells)

def validate(g,p):
    n=len(g)
    if sorted({x for row in g for x in row})!=list(range(n)): return False
    for k in range(n):
        if not connected(g,k): return False
        cnt=sum(1 for r in range(n) for c in range(n) if g[r][c]==k and c==p[r])
        if cnt!=1:return False
    return True

def alternate(g,p,clues):
    n=len(g); clue_by_row={r:c for r,c in clues}; usedc=0; usedreg=0
    for r,c in clues: usedc|=1<<c; usedreg|=1<<g[r][c]
    # Candidate masks by row.
    rows=[r for r in range(n) if r not in clue_by_row]
    # Search most constrained dynamic row. But adjacency only depends on neighboring rows, so use natural order with skip known rows.
    assigned=[None]*n
    for r,c in clues: assigned[r]=c
    count=0; found=None
    def rec(idx,uc,ur):
        nonlocal count,found
        if count>=2:return
        if idx==len(rows):
            # Validate adjacency across all rows and ignore the official solution.
            for r in range(n-1):
                if abs(assigned[r]-assigned[r+1])==1:return
            if all(assigned[r]==p[r] for r in range(n)): return
            count+=1; found=[(r,assigned[r]) for r in range(n)]; return
        # Choose next row with MRV among unassigned; only adjacency to assigned neighbors matters.
        best_i=None; best=[]
        for ii,r in enumerate(rows[idx:],start=idx):
            cand=[]
            for c in range(n):
                if uc>>c&1 or ur>>g[r][c]&1: continue
                if r>0 and assigned[r-1] is not None and abs(c-assigned[r-1])==1: continue
                if r<n-1 and assigned[r+1] is not None and abs(c-assigned[r+1])==1: continue
                cand.append(c)
            if best_i is None or len(cand)<len(best): best_i,best=ii,cand
            if len(best)<=1: break
        if best_i is None:return
        rows[idx],rows[best_i]=rows[best_i],rows[idx]
        r=rows[idx]
        for c in best:
            assigned[r]=c
            rec(idx+1,uc|1<<c,ur|1<<g[r][c])
            assigned[r]=None
            if count>=2:break
        rows[idx],rows[best_i]=rows[best_i],rows[idx]
    rec(0,usedc,usedreg)
    return found if count else None

def unique_with_clues(g,p,clues):
    return alternate(g,p,clues) is None

def make_clues(g,p,max_clues):
    clues=[]
    while len(clues)<max_clues:
        alt=alternate(g,p,clues)
        if alt is None:return clues
        aset=set(alt)
        candidates=[(r,p[r]) for r in range(len(p)) if (r,p[r]) not in aset]
        if not candidates:return None
        # Choose clue with highest local blocking degree.
        candidates.sort(key=lambda rc: sum(1 for dr,dc in DIRS if 0<=rc[0]+dr<len(p) and 0<=rc[1]+dc<len(p) and (rc[0]+dr,rc[1]+dc) in aset), reverse=True)
        clues.append(candidates[0])
    return None

def score(n,g,clues):
    sizes=collections.Counter(v for row in g for v in row)
    variation=sum(abs(s-n) for s in sizes.values())/n
    return round(max(1,min(10,n*.52+variation*.45-len(clues)*.05)),2)

def make_level(lid,diff,n,rng,family):
    best=None
    for attempt in range(120):
        p=random_perm(n,rng); g=grow_regions(n,p,rng)
        if not validate(g,p):continue
        # Clue target rises with difficulty but we always minimize until unique.
        clues=make_clues(g,p,n)
        if clues is None:continue
        metric=len(clues)
        if best is None or metric<best[0]:best=(metric,p,g,clues)
        target=max(2,n-6) if diff in ('very_easy','easy') else max(2,n-4)
        if metric<=target:break
    if best is None:raise RuntimeError(f'failed {lid}')
    _,p,g,clues=best
    return {'id':lid,'size':n,'difficulty':diff,'colors':n,'grid':g,'solution':[[r,p[r]] for r in range(n)],'startingClues':[[r,c] for r,c in clues],'maxMistakes':3,'hints':{'itemFinder':True,'logicHint':True,'threeCellReveal':True},'metadata':{'patternFamily':family,'estimatedDifficulty':score(n,g,clues)}}

def main():
    rng=random.Random(20260923); levels=[]; seen=set(); lid=1; t=time.time()
    for diff,n,count in DIFFS:
        for _ in range(count):
            family=f'F{((lid-1)//2)+1:03d}'
            while True:
                lv=make_level(lid,diff,n,rng,family)
                h=hashlib.sha256(json.dumps([lv['grid'],lv['solution']],separators=(',',':')).encode()).hexdigest()
                if h not in seen:seen.add(h);break
            levels.append(lv); lid+=1
            if lid%25==0:print('generated',lid-1,'elapsed',round(time.time()-t,1),flush=True)
    for p in OUT.glob('levels_*.json'):p.unlink()
    for i in range(10):
        chunk=levels[i*100:(i+1)*100]
        (OUT/f'levels_{i*100+1:03d}_{(i+1)*100:03d}.json').write_text(json.dumps(chunk,separators=(',',':')),encoding='utf8')
    (ROOT/'level-generator/generated_summary.json').write_text(json.dumps({'count':1000,'seed':20260923},indent=2))
    print('DONE 1000 in',round(time.time()-t,1),'sec')
if __name__=='__main__':main()
