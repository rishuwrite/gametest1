import json, random, collections, hashlib, sys
from pathlib import Path
import numpy as np
from scipy.optimize import milp, LinearConstraint, Bounds

ROOT=Path(__file__).resolve().parent
OUT=ROOT/'app/src/main/assets/levels'
OUT.mkdir(parents=True, exist_ok=True)

DIFFS=[('very_easy',1,8),('easy',2,8),('normal',3,9),('medium',4,10),('hard',5,11),('very_hard',6,12),('expert',7,14),('master',8,15)]
COUNTS=[50,100,150,200,200,150,100,50]
SIZES=[8,8,9,10,11,12,14,15]

DIRS=((1,0),(-1,0),(0,1),(0,-1))

def random_perm(n,rng):
    for _ in range(10000):
        p=list(range(n)); rng.shuffle(p)
        if all(abs(p[i]-p[i+1])!=1 for i in range(n-1)):
            return p
    raise RuntimeError('Could not create non-touching permutation')

def grow_regions(n,p,rng):
    g=[[-1]*n for _ in range(n)]
    front=[]
    # Seed each region at the official solution cell.
    for rid,c in enumerate(p):
        r=rid; g[r][c]=rid
    for r,c in [(r,p[r]) for r in range(n)]:
        for dr,dc in DIRS:
            rr,cc=r+dr,c+dc
            if 0<=rr<n and 0<=cc<n and g[rr][cc]<0:
                front.append((rr,cc,r,c))
    while front:
        # Bias toward compact growth while keeping organic variation.
        i=rng.randrange(len(front))
        rr,cc,pr,pc=front.pop(i)
        if g[rr][cc]>=0: continue
        g[rr][cc]=g[pr][pc]
        for dr,dc in DIRS:
            r2,c2=rr+dr,cc+dc
            if 0<=r2<n and 0<=c2<n and g[r2][c2]<0:
                front.append((r2,c2,rr,cc))
    return g

def connected(g,rid):
    n=len(g)
    cells=[(r,c) for r in range(n) for c in range(n) if g[r][c]==rid]
    if not cells: return False
    seen={cells[0]}; q=[cells[0]]
    for r,c in q:
        for dr,dc in DIRS:
            rr,cc=r+dr,c+dc
            if 0<=rr<n and 0<=cc<n and g[rr][cc]==rid and (rr,cc) not in seen:
                seen.add((rr,cc)); q.append((rr,cc))
    return len(seen)==len(cells)

def validate_regions(g,p):
    n=len(g)
    if sorted({x for row in g for x in row}) != list(range(n)): return False
    for rid in range(n):
        if not connected(g,rid): return False
        if sum(1 for r in range(n) for c in range(n) if g[r][c]==rid and c==p[r]) != 1:
            return False
    return True

def build_constraints(g, clues=(), exclude=None):
    n=len(g); V=n*n
    rows=[]; lo=[]; hi=[]
    for r in range(n):
        a=np.zeros(V); a[r*n:(r+1)*n]=1; rows.append(a); lo.append(1); hi.append(1)
    for c in range(n):
        a=np.zeros(V); a[c::n]=1; rows.append(a); lo.append(1); hi.append(1)
    for k in range(n):
        a=np.zeros(V)
        for r in range(n):
            for c in range(n):
                if g[r][c]==k: a[r*n+c]=1
        rows.append(a); lo.append(1); hi.append(1)
    for r in range(n):
        for c in range(n):
            for dr,dc in ((1,0),(0,1)):
                rr,cc=r+dr,c+dc
                if rr<n and cc<n:
                    a=np.zeros(V); a[r*n+c]=1; a[rr*n+cc]=1
                    rows.append(a); lo.append(-np.inf); hi.append(1)
    for r,c in clues:
        a=np.zeros(V); a[r*n+c]=1; rows.append(a); lo.append(1); hi.append(1)
    if exclude is not None:
        a=np.zeros(V)
        for r,c in exclude: a[r*n+c]=1
        rows.append(a); lo.append(-np.inf); hi.append(n-1)
    return np.vstack(rows), np.array(lo), np.array(hi)

def find_solution(g, clues=(), exclude=None, time_limit=2):
    A,lo,hi=build_constraints(g,clues,exclude)
    n=len(g); V=n*n
    res=milp(np.zeros(V),integrality=np.ones(V),bounds=Bounds(0,1),
             constraints=LinearConstraint(A,lo,hi),options={'time_limit':time_limit})
    if not res.success: return None
    return [(i//n,i%n) for i,x in enumerate(res.x) if x>.5]

def clues_for_unique(g,p,max_clues=None):
    n=len(g); max_clues=max_clues or n
    clues=[]
    while len(clues)<max_clues:
        alt=find_solution(g,clues,exclude=[(r,p[r]) for r in range(n)],time_limit=2)
        if alt is None:
            return clues
        aset=set(alt)
        candidates=[(r,p[r]) for r in range(n) if (r,p[r]) not in aset]
        if not candidates: return None
        # Choose a clue that also improves row/region distribution.
        candidates.sort(key=lambda x: (g[x[0]][x[1]], x[0]))
        clues.append(candidates[0])
    return None

def final_unique(g,clues,p):
    alt=find_solution(g,clues,exclude=[(r,p[r]) for r in range(len(p))],time_limit=5)
    return alt is None

def difficulty_score(n,g,clues):
    sizes=collections.Counter(x for row in g for x in row)
    irregular=sum(abs(v-n) for v in sizes.values())/n
    return round(max(1,min(10, n*0.55 + irregular*0.6 - len(clues)*0.08)),2)

def make_level(level_id,diff,size,rng,family):
    # Try many boards, prefer fewer starter clues.
    best=None
    for attempt in range(250):
        p=random_perm(size,rng)
        g=grow_regions(size,p,rng)
        if not validate_regions(g,p): continue
        clues=clues_for_unique(g,p,max_clues=size)
        if clues is None or not final_unique(g,clues,p): continue
        key=(len(clues),attempt)
        if best is None or key<best[0]: best=(key,p,g,clues)
        if len(clues)<=max(3,size-4): break
    if best is None:
        raise RuntimeError(f'Could not generate level {level_id} n={size}')
    _,p,g,clues=best
    score=difficulty_score(size,g,clues)
    return {
        'id':level_id,'size':size,'difficulty':diff,'colors':size,
        'grid':g,'solution':[[r,p[r]] for r in range(size)],
        'startingClues':[[r,c] for r,c in clues],
        'maxMistakes':3,
        'hints':{'itemFinder':True,'logicHint':True,'threeCellReveal':True},
        'metadata':{'patternFamily':family,'estimatedDifficulty':score}
    }

def main():
    rng=random.Random(20260923)
    levels=[]; seen=set(); lid=1
    for di,(diff,rank,size) in enumerate(DIFFS):
        for j in range(COUNTS[di]):
            family=f'F{((lid-1)//2)+1:03d}'
            # Paired levels share family label but remain independently generated.
            for _ in range(20):
                lv=make_level(lid,diff,size,rng,family)
                h=hashlib.sha256(json.dumps([lv['grid'],lv['solution']],separators=(',',':')).encode()).hexdigest()
                if h not in seen:
                    seen.add(h); levels.append(lv); lid+=1; break
            else:
                raise RuntimeError('Duplicate generation loop')
            if lid%25==0: print('generated',lid-1,flush=True)
    assert len(levels)==1000
    # Split into ten deterministic files.
    for p in OUT.glob('levels_*.json'): p.unlink()
    for i in range(10):
        chunk=levels[i*100:(i+1)*100]
        (OUT/f'levels_{i*100+1:03d}_{(i+1)*100:03d}.json').write_text(json.dumps(chunk,separators=(',',':')),encoding='utf-8')
    (ROOT/'level-generator/generated_summary.json').write_text(json.dumps({'count':len(levels),'seed':20260923},indent=2),encoding='utf-8')
    print('DONE',len(levels))

if __name__=='__main__': main()
