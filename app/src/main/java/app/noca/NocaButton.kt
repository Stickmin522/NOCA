package app.noca

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.raisedFace(
    color: Color,
    shape: Shape,
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val depth by
        animateFloatAsState(
            if (pressed && enabled) 1f else 0f,
            tween(100),
            label = "button-depth",
        )
    val c = LocalNocaColors.current
    val base = if (enabled) color else lerp(color, c.surface, .65f)
    return graphicsLayer {
            scaleX = 1f - .015f * depth
            scaleY = 1f - .015f * depth
            translationY = 1.5.dp.toPx() * depth
        }
        .shadow(
            if (enabled) (5f - 4f * depth).dp else 0.dp,
            shape,
            clip = false,
            ambientColor = c.shadow,
            spotColor = c.shadow,
        )
        .background(
            Brush.verticalGradient(
                listOf(
                    lerp(base, Color.White, .14f - .08f * depth),
                    base,
                    lerp(base, Color.Black, .08f),
                )
            ),
            shape,
        )
        .border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = .24f), Color.Black.copy(alpha = .07f))
                ),
            ),
            shape,
        )
}

@Composable
fun NocaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val c = LocalNocaColors.current
    val interaction = remember { MutableInteractionSource() }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.raisedFace(c.accent, shape, interaction, enabled),
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Transparent,
                contentColor = c.ink,
                disabledContainerColor = Color.Transparent,
            ),
        content = content,
    )
}

@Composable
fun NocaIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalNocaColors.current
    val interaction = remember { MutableInteractionSource() }
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.raisedFace(c.accent, CircleShape, interaction),
        interactionSource = interaction,
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.Transparent,
                contentColor = c.ink,
            ),
        content = content,
    )
}
