package app.noca

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val CARD_PATH =
    "M28.7,45.2 L49.6,38.4 C53.3,37.2 56.1,38.7 57.3,42.4 L62.6,58.6 C63.8,62.3 62.3,65.1 58.6,66.3 L37.7,73.1 C34,74.3 31.2,72.8 30,69.1 L24.7,52.9 C23.5,49.2 25,46.4 28.7,45.2 Z"
private const val NEAR_PATH = "M65.8,43.1 C73.8,50.7 75.3,60.9 69.5,69.8"
private const val FAR_PATH = "M74.8,34.7 C87.1,46.8 89,63.1 79.4,77.3"

object NocaIcons {
    val Contactless: ImageVector by lazy {
        ImageVector.Builder("Contactless", 24.dp, 24.dp, 72f, 72f)
            .apply {
                addGroup(translationX = -18f, translationY = -18f)
                addPath(
                    PathParser().parsePathString(CARD_PATH).toNodes(),
                    fill = SolidColor(Color.Black),
                )
                for (path in listOf(NEAR_PATH, FAR_PATH)) {
                    addPath(
                        PathParser().parsePathString(path).toNodes(),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 5.8f,
                        strokeLineCap = StrokeCap.Round,
                    )
                }
                clearGroup()
            }
            .build()
    }
}

@Composable
fun AnimatedContactlessIcon(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val transition = rememberInfiniteTransition(label = "contactless")
    val near by
        transition.animateFloat(
            initialValue = .15f,
            targetValue = .15f,
            animationSpec =
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 1600
                        .15f at 0
                        1f at 320
                        1f at 680
                        .15f at 1120
                        .15f at 1600
                    }
                ),
            label = "near-wave",
        )
    val far by
        transition.animateFloat(
            initialValue = .15f,
            targetValue = .15f,
            animationSpec =
                infiniteRepeatable(
                    keyframes {
                        durationMillis = 1600
                        .15f at 0
                        .15f at 320
                        1f at 680
                        1f at 1040
                        .15f at 1480
                        .15f at 1600
                    }
                ),
            label = "far-wave",
        )
    val paths = remember {
        listOf(CARD_PATH, NEAR_PATH, FAR_PATH).map { PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier.size(104.dp)) {
        withTransform({
            scale(
                size.width / 72f,
                size.height / 72f,
                pivot = androidx.compose.ui.geometry.Offset.Zero,
            )
            translate(-18f, -18f)
        }) {
            drawPath(paths[0], color)
            drawPath(
                paths[1],
                color.copy(alpha = color.alpha * near),
                style = Stroke(5.8f, cap = StrokeCap.Round),
            )
            drawPath(
                paths[2],
                color.copy(alpha = color.alpha * far),
                style = Stroke(5.8f, cap = StrokeCap.Round),
            )
        }
    }
}
