package com.colorbound.game

import org.junit.Assert.*
import org.junit.Test

class PuzzleEngineTest {
    private val level = Level(
        id=1,size=4,difficulty="test",colors=4,
        grid=listOf(
            listOf(0,0,1,1),
            listOf(0,2,2,1),
            listOf(3,2,2,1),
            listOf(3,3,3,1)
        ),
        solution=setOf(Cell(0,0),Cell(1,2),Cell(2,1),Cell(3,3)),
        startingClues=emptySet()
    )

    @Test fun correctCellBelongsToSolution(){ assertTrue(PuzzleEngine.isCorrect(level,Cell(1,2))); assertFalse(PuzzleEngine.isCorrect(level,Cell(1,1))) }
    @Test fun noTouchRejectsOrthogonalNeighbors(){
        val s=setOf(Cell(0,0),Cell(1,0),Cell(2,1),Cell(3,3))
        assertFalse(PuzzleEngine.isLegal(level,s))
    }
    @Test fun validSolutionPasses(){ assertTrue(PuzzleEngine.isLegal(level,level.solution)) }
    @Test fun completionRequiresAllSolutionCells(){ assertFalse(PuzzleEngine.isComplete(level,setOf(Cell(0,0)))); assertTrue(PuzzleEngine.isComplete(level,level.solution)) }
    @Test fun candidateBlocksUsedRowColumnAndRegion(){
        val selected=setOf(Cell(0,0))
        val candidates=PuzzleEngine.candidateCells(level,selected)
        assertTrue(candidates.none { it.row==0 || it.col==0 || level.grid[it.row][it.col]==0 })
    }
}
