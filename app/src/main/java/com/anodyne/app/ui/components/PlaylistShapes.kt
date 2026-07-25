package com.anodyne.app.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.anodyne.app.ui.screens.AppPlaylistShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object PlaylistShapes {

    fun getShape(appShape: AppPlaylistShape): Shape {
        return when (appShape) {
            AppPlaylistShape.Square -> RoundedCornerShape(0.dp)
            AppPlaylistShape.Rounded -> RoundedCornerShape(8.dp)
            AppPlaylistShape.Circle -> CircleShape
            AppPlaylistShape.Squircle -> RoundedCornerShape(24.dp) // A large rounded rect approximates squircle well enough for icons
            AppPlaylistShape.Clover -> CloverShape
            AppPlaylistShape.Hexagon -> HexagonShape
            AppPlaylistShape.Star -> StarShape
            AppPlaylistShape.Slanted -> SlantedOvalShape
        }
    }
}

val CloverShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = w / 3f
    moveTo(w / 2f, h / 2f)
    // Simplified clover using path building
    // A clover can be approximated by 4 circles on the corners of a central square
    addOval(Rect(0f, h/2f - r, r*2, h/2f + r)) // Left
    addOval(Rect(w/2f - r, 0f, w/2f + r, r*2)) // Top
    addOval(Rect(w - r*2, h/2f - r, w, h/2f + r)) // Right
    addOval(Rect(w/2f - r, h - r*2, w/2f + r, h)) // Bottom
    addRect(Rect(r, r, w-r, h-r)) // Center fill
}

val HexagonShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val radius = w / 2f
    val cx = w / 2f
    val cy = h / 2f
    
    for (i in 0 until 6) {
        val angle = PI / 3 * i - PI / 2
        val px = cx + radius * cos(angle).toFloat()
        val py = cy + radius * sin(angle).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

val StarShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val outerRadius = w / 2f
    val innerRadius = w / 3.5f
    val points = 8 // Scalloped / star shape with 8 points
    
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = PI / points * i - PI / 2
        val px = cx + r * cos(angle).toFloat()
        val py = cy + r * sin(angle).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

val SlantedOvalShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    // A slanted pill/oval.
    // We can draw a path that looks like a rounded diamond
    moveTo(w * 0.2f, 0f)
    lineTo(w * 0.8f, 0f)
    quadraticBezierTo(w, 0f, w, h * 0.2f)
    lineTo(w, h * 0.8f)
    quadraticBezierTo(w, h, w * 0.8f, h)
    lineTo(w * 0.2f, h)
    quadraticBezierTo(0f, h, 0f, h * 0.8f)
    lineTo(0f, h * 0.2f)
    quadraticBezierTo(0f, 0f, w * 0.2f, 0f)
    close()
}
