package com.example.tictactoe.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.tictactoe.domain.model.Outcome
import com.example.tictactoe.domain.model.UltimateBoard
import com.example.tictactoe.ui.theme.LocalGameColors

@Composable
fun UltimateBoardView(
    board: UltimateBoard,
    onCellClick: (macroIndex: Int, microIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalGameColors.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    val isGameOver = board.outcome() !is Outcome.InProgress
    val macroWon = board.outcome() as? Outcome.Won

    Box(modifier = modifier.aspectRatio(1f).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (macroR in 0 until 3) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (macroC in 0 until 3) {
                        val macroIndex = macroR * 3 + macroC
                        val subBoard = board.subBoards[macroIndex]
                        val isTarget = board.activeMacroIndex == null || board.activeMacroIndex == macroIndex
                        val isComplete = subBoard.outcome() !is Outcome.InProgress
                        val macroWinner = (subBoard.outcome() as? Outcome.Won)?.winner
                        val isPlayable = enabled && !isGameOver && isTarget && !isComplete

                        val subBoardShape = RoundedCornerShape(10.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(subBoardShape)
                                .background(
                                    if (isPlayable) colors.primary.copy(alpha = 0.08f * pulseAlpha)
                                    else colors.panel
                                )
                                .then(
                                    if (isPlayable) {
                                        Modifier.border(
                                            2.dp,
                                            colors.primary.copy(alpha = pulseAlpha),
                                            subBoardShape
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            colors.cellBorder.copy(alpha = 0.35f),
                                            subBoardShape
                                        )
                                    }
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AdaptiveGridBoard(
                                board = subBoard,
                                enabled = isPlayable,
                                showWinLine = false,
                                onCellClick = { microIndex -> onCellClick(macroIndex, microIndex) },
                                modifier = Modifier.fillMaxSize().alpha(if (isComplete) 0.25f else 1f)
                            )
                            if (macroWinner != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MarkSymbol(
                                        mark = macroWinner,
                                        modifier = Modifier.fillMaxSize().padding(6.dp).alpha(0.85f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (macroWon != null) {
            GridWinLine(won = macroWon, size = 3, gap = 8.dp, modifier = Modifier.matchParentSize())
        }
    }
}
