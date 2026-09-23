#!/usr/bin/env python3
import json, glob, sys
DIRS=((1,0),(-1,0),(0,1),(0,-1))

def connected(g,rid):
    n=len(g); cells=[(r,c) for r in range(n) for c in range(n) if g[r][c]==rid]
    if not cells:return False
    seen={cells[0]}; stack=[cells[0]]
    for r,c in stack:
        for dr,dc in DIRS:
            rr,cc=r+dr,c+dc
            if 0<=rr<n and 0<=cc<n and g[rr][cc]==rid and (rr,cc) not in seen:
                seen.add((rr,cc)); stack.append((rr,cc))
    return len(seen)==len(cells)

def has_alternate(g,solution,clues):
    n=len(g); sol_cols=[c for r,c in solution]
    assigned=[None]*n; used_c=used_reg=0
    for r,c in clues:
        assigned[r]=c; used_c|=1<<c; used_reg|=1<<g[r][c]
    rows=[r for r in range(n) if assigned[r] is None]
    found=False
    def rec(idx,uc,ur):
        nonlocal found
        if found:return
        if idx==len(rows):
            if any(abs(assigned[r]-assigned[r+1])==1 for r in range(n-1)):return
            if all(assigned[r]==sol_cols[r] for r in range(n)):return
            found=True; return
        best_i=idx; best=None
        for ii in range(idx,len(rows)):
            r=rows[ii]; cand=[]
            for c in range(n):
                if uc>>c&1 or ur>>g[r][c]&1:continue
                if r>0 and assigned[r-1] is not None and abs(c-assigned[r-1])==1:continue
                if r<n-1 and assigned[r+1] is not None and abs(c-assigned[r+1])==1:continue
                cand.append(c)
            if best is None or len(cand)<len(best):best_i,best=ii,cand
            if len(best)<=1:break
        if not best:return
        rows[idx],rows[best_i]=rows[best_i],rows[idx]
        r=rows[idx]
        for c in best:
            assigned[r]=c;rec(idx+1,uc|1<<c,ur|1<<g[r][c]);assigned[r]=None
            if found:break
        rows[idx],rows[best_i]=rows[best_i],rows[idx]
    rec(0,used_c,used_reg)
    return found

def validate(lv):
    n=lv['size']; g=lv['grid']; sol=[tuple(x) for x in lv['solution']]; clues=[tuple(x) for x in lv.get('startingClues',[])]
    assert 8<=n<=15
    assert len(g)==n and all(len(row)==n for row in g)
    assert sorted({v for row in g for v in row})==list(range(n))
    assert len(sol)==n and {r for r,c in sol}==set(range(n)) and len({c for r,c in sol})==n
    assert all(0<=r<n and 0<=c<n for r,c in sol)
    assert all(abs(sol[i][1]-sol[i+1][1])!=1 for i in range(n-1))
    for rid in range(n):
        assert connected(g,rid)
        assert sum(g[r][c]==rid for r,c in sol)==1
    assert len(clues)==len(set(clues))
    assert len({r for r,c in clues})==len(clues)
    assert all(x in sol for x in clues)
    assert not has_alternate(g,sol,clues)

files=sorted(glob.glob('app/src/main/assets/levels/levels_*.json'))
levels=[]
for f in files: levels.extend(json.load(open(f,encoding='utf-8')))
assert len(files)==10, f'expected 10 level files, got {len(files)}'
assert len(levels)==1000, f'expected 1000 levels, got {len(levels)}'
ids=[x['id'] for x in levels]
assert ids==list(range(1,1001)), 'level IDs are not exactly 1..1000'
for lv in levels: validate(lv)
print('1000 / 1000 LEVELS VALID')
print('Color Connectivity: PASS')
print('Region Item Count: PASS')
print('Row Constraint: PASS')
print('Column Constraint: PASS')
print('No-Touch Constraint: PASS')
print('Solution Validity: PASS')
print('Solution Uniqueness (including fixed starting clues): PASS')
print('Data Integrity: PASS')
