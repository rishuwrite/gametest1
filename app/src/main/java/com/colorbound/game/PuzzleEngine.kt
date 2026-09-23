package com.colorbound.game

object PuzzleEngine {
    fun isCorrect(level: Level, cell: Cell): Boolean = cell in level.solution

    fun isComplete(level: Level, selected: Set<Cell>): Boolean = level.solution.all { it in selected }

    fun hasOrthogonalConflict(cell: Cell, selected: Set<Cell>): Boolean =
        Cell(cell.row-1,cell.col) in selected || Cell(cell.row+1,cell.col) in selected ||
        Cell(cell.row,cell.col-1) in selected || Cell(cell.row,cell.col+1) in selected

    fun isLegal(level: Level, selected: Set<Cell>): Boolean {
        val n=level.size
        if (selected.size != n) return false
        if (selected.any { it.row !in 0 until n || it.col !in 0 until n }) return false
        if (selected.groupBy { it.row }.any { it.value.size != 1 }) return false
        if (selected.groupBy { it.col }.any { it.value.size != 1 }) return false
        if (selected.groupBy { level.grid[it.row][it.col] }.any { it.value.size != 1 }) return false
        return selected.none { hasOrthogonalConflict(it, selected - it) }
    }

    fun candidateCells(level: Level, selected: Set<Cell>, excluded: Set<Cell> = emptySet()): List<Cell> {
        val usedRows=selected.mapTo(HashSet()) { it.row }
        val usedCols=selected.mapTo(HashSet()) { it.col }
        val usedRegions=selected.mapTo(HashSet()) { level.grid[it.row][it.col] }
        return buildList {
            for (r in 0 until level.size) for (c in 0 until level.size) {
                val cell=Cell(r,c)
                if (cell in excluded || cell in selected) continue
                if (r in usedRows || c in usedCols || level.grid[r][c] in usedRegions) continue
                if (hasOrthogonalConflict(cell,selected)) continue
                add(cell)
            }
        }
    }

    fun logicHint(level: Level, selected: Set<Cell>, revealedEmpty: Set<Cell>): HintMessage? {
        val candidates=candidateCells(level,selected,revealedEmpty)
        val unresolved=level.solution - selected
        // Region -> exactly one candidate.
        for (region in 0 until level.colors) {
            val cells=candidates.filter { level.grid[it.row][it.col]==region }
            if (cells.size==1) return HintMessage("A region has only one option", "That connected color region has just one legal cell remaining.", cells)
        }
        // Row -> exactly one candidate.
        for (r in 0 until level.size) {
            if (selected.any { it.row==r }) continue
            val cells=candidates.filter { it.row==r }
            if (cells.size==1) return HintMessage("A row is forced", "Every other cell in this row is blocked by the current solution state.", cells)
        }
        // Column -> exactly one candidate.
        for (c in 0 until level.size) {
            if (selected.any { it.col==c }) continue
            val cells=candidates.filter { it.col==c }
            if (cells.size==1) return HintMessage("A column is forced", "Only one cell can satisfy this column without breaking the rules.", cells)
        }
        // Useful fallback: reveal a real constraint chain around an unresolved solution.
        val target=unresolved.firstOrNull { it in candidates }
        return target?.let { HintMessage("A safe next move", "This cell fits the row, column, region, and no-touch constraints in the current board.", listOf(it)) }
    }
}
