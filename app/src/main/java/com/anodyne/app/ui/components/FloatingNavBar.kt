package com.anodyne.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anodyne.app.util.AnodyneMotion

data class FloatingNavItem(
    val tab: Any,
    val icon: ImageVector,
    val label: String
)

@Composable
fun FloatingNavBar(
    items: List<FloatingNavItem>,
    selectedItem: Any,
    onItemSelected: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val bgColor = colorScheme.surfaceVariant.copy(alpha = 0.95f)

    val springSpec = AnodyneMotion.spatialSpring<Float>()

    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(50),
                ambientColor = colorScheme.primary.copy(alpha = 0.25f),
                spotColor = colorScheme.primary.copy(alpha = 0.35f)
            )
            .border(
                width = 1.dp,
                color = colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50)
            )
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedItem == item.tab

                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.primary else Color.Transparent,
                    animationSpec = AnodyneMotion.effectsSpring(),
                    label = "pillBg"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    animationSpec = AnodyneMotion.effectsSpring(),
                    label = "iconTint"
                )
                val textAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = springSpec,
                    label = "textAlpha"
                )
                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 0.dp else 0.dp,
                    animationSpec = AnodyneMotion.effectsSpring(),
                    label = "pillWidth"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillBg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onItemSelected(item.tab) }
                        .padding(
                            horizontal = if (isSelected) 16.dp else 14.dp,
                            vertical = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )

                    if (textAlpha > 0.01f) {
                        Spacer(Modifier.width((6 * textAlpha).dp))
                        Text(
                            text = item.label,
                            color = colorScheme.onPrimary.copy(alpha = textAlpha),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
