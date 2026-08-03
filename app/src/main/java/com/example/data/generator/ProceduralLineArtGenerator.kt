package com.example.data.generator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ProceduralLineArtGenerator {

    fun generateLineArtBitmap(
        prompt: String,
        category: String,
        style: String,
        seed: Long = Random.nextLong(),
        width: Int = 1080,
        height: Int = 1080
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white paper background
        canvas.drawColor(Color.WHITE)

        val strokePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            setStyle(Paint.Style.STROKE)
            strokeWidth = when (style.lowercase()) {
                "bold outlines" -> 8f
                "fine zentangle" -> 2.5f
                "minimalist" -> 4f
                else -> 3.5f // Intricate Detail
            }
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val thinStrokePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            setStyle(Paint.Style.STROKE)
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
        }

        val random = Random(seed + prompt.hashCode().toLong())

        // Draw intricate outer margin border for adult coloring sheet
        val margin = 40f
        val borderPath = Path().apply {
            addRoundRect(
                RectF(margin, margin, width - margin, height - margin),
                20f, 20f, Path.Direction.CW
            )
        }
        canvas.drawPath(borderPath, strokePaint)

        val innerMargin = margin + 16f
        canvas.drawRoundRect(
            RectF(innerMargin, innerMargin, width - innerMargin, height - innerMargin),
            12f, 12f, thinStrokePaint
        )

        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadius = (width / 2f) - (margin + 40f)

        val catLower = category.lowercase()
        when {
            catLower.contains("neon") || catLower.contains("surveillance") -> {
                drawStainedGlass(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint)
                drawMandala(canvas, centerX, centerY, maxRadius * 0.7f, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("garage") || catLower.contains("chopper") -> {
                drawArchitectureLineArt(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("wasteland") || catLower.contains("scrapper") -> {
                drawAbstractZentangle(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("metal") || catLower.contains("horror") -> {
                drawAnimalLineArt(canvas, centerX, centerY, maxRadius, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("cyberpunk") || catLower.contains("dystopian") || catLower.contains("sci-fi") -> {
                drawStainedGlass(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint)
            }
            catLower.contains("weird") || catLower.contains("surreal") || catLower.contains("lowbrow") -> {
                drawAbstractZentangle(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("pinup") || catLower.contains("curves") || catLower.contains("ink & curves") -> {
                drawFloralZen(canvas, centerX, centerY, maxRadius, random, strokePaint, thinStrokePaint)
            }
            catLower.contains("colorful") || catLower.contains("language") -> {
                drawFloralZen(canvas, centerX, centerY, maxRadius, random, strokePaint, thinStrokePaint)
                drawMandala(canvas, centerX, centerY, maxRadius * 0.5f, random, strokePaint, thinStrokePaint)
            }
            else -> drawAbstractZentangle(canvas, width.toFloat(), height.toFloat(), margin, random, strokePaint, thinStrokePaint)
        }

        return bitmap
    }

    private fun drawMandala(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        random: Random,
        strokePaint: Paint,
        thinPaint: Paint
    ) {
        val petalsegments = listOf(8, 12, 16, 24).random(random)
        val numRings = random.nextInt(4, 8)
        val ringSpacing = maxRadius / numRings

        for (r in 1..numRings) {
            val radius = ringSpacing * r
            canvas.drawCircle(cx, cy, radius, thinPaint)

            val stepAngle = 360f / petalsegments
            for (i in 0 until petalsegments) {
                val angleRad = Math.toRadians((i * stepAngle).toDouble()).toFloat()
                val px = cx + radius * cos(angleRad)
                val py = cy + radius * sin(angleRad)

                // Draw petal loop
                val path = Path()
                val midAngleRad = Math.toRadians((i * stepAngle + stepAngle / 2).toDouble()).toFloat()
                val ctrlR = radius + ringSpacing * 0.4f
                val ctrlX = cx + ctrlR * cos(midAngleRad)
                val ctrlY = cy + ctrlR * sin(midAngleRad)

                val nextAngleRad = Math.toRadians(((i + 1) * stepAngle).toDouble()).toFloat()
                val npx = cx + radius * cos(nextAngleRad)
                val npy = cy + radius * sin(nextAngleRad)

                path.moveTo(px, py)
                path.quadTo(ctrlX, ctrlY, npx, npy)
                canvas.drawPath(path, strokePaint)

                // Inner detail line
                val innerPath = Path()
                innerPath.moveTo(cx + (radius - ringSpacing * 0.5f) * cos(midAngleRad), cy + (radius - ringSpacing * 0.5f) * sin(midAngleRad))
                innerPath.lineTo(ctrlX, ctrlY)
                canvas.drawPath(innerPath, thinPaint)
            }
        }
        // Center flower
        canvas.drawCircle(cx, cy, ringSpacing * 0.4f, strokePaint)
    }

    private fun drawFloralZen(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        random: Random,
        strokePaint: Paint,
        thinPaint: Paint
    ) {
        val numFlowers = random.nextInt(5, 9)
        for (f in 0 until numFlowers) {
            val fcx = if (f == 0) cx else cx + random.nextFloat() * maxRadius * 0.8f * (if (random.nextBoolean()) 1 else -1)
            val fcy = if (f == 0) cy else cy + random.nextFloat() * maxRadius * 0.8f * (if (random.nextBoolean()) 1 else -1)
            val flowerRadius = random.nextFloat() * 80f + 60f

            canvas.drawCircle(fcx, fcy, flowerRadius * 0.3f, strokePaint)
            val petals = random.nextInt(6, 12)
            for (p in 0 until petals) {
                val angle = (2 * PI * p / petals).toFloat()
                val p1x = fcx + (flowerRadius * 0.3f) * cos(angle)
                val p1y = fcy + (flowerRadius * 0.3f) * sin(angle)

                val tipR = flowerRadius * 1.2f
                val tipX = fcx + tipR * cos(angle + (PI / petals).toFloat())
                val tipY = fcy + tipR * sin(angle + (PI / petals).toFloat())

                val p2x = fcx + (flowerRadius * 0.3f) * cos(angle + (2 * PI / petals).toFloat())
                val p2y = fcy + (flowerRadius * 0.3f) * sin(angle + (2 * PI / petals).toFloat())

                val path = Path()
                path.moveTo(p1x, p1y)
                path.quadTo(tipX, tipY, p2x, p2y)
                canvas.drawPath(path, strokePaint)

                // Vein
                canvas.drawLine(fcx + flowerRadius * 0.3f * cos(angle + (PI/petals).toFloat()), fcy + flowerRadius * 0.3f * sin(angle + (PI/petals).toFloat()), tipX * 0.9f + fcx * 0.1f, tipY * 0.9f + fcy * 0.1f, thinPaint)
            }
        }
    }

    private fun drawStainedGlass(
        canvas: Canvas,
        w: Float,
        h: Float,
        margin: Float,
        random: Random,
        strokePaint: Paint
    ) {
        val points = mutableListOf<Pair<Float, Float>>()
        val count = random.nextInt(25, 40)
        for (i in 0 until count) {
            points.add(
                Pair(
                    margin + random.nextFloat() * (w - 2 * margin),
                    margin + random.nextFloat() * (h - 2 * margin)
                )
            )
        }

        // Connect nearby points to form polygonal stained glass facets
        for (i in 0 until points.size) {
            val p1 = points[i]
            val neighbors = points.mapIndexed { idx, p -> Pair(idx, (p.first - p1.first) * (p.first - p1.first) + (p.second - p1.second) * (p.second - p1.second)) }
                .sortedBy { it.second }
                .take(5)

            for (n in neighbors) {
                val p2 = points[n.first]
                canvas.drawLine(p1.first, p1.second, p2.first, p2.second, strokePaint)
            }
        }
    }

    private fun drawAnimalLineArt(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        maxRadius: Float,
        random: Random,
        strokePaint: Paint,
        thinPaint: Paint
    ) {
        // Draw stylized animal head silhouette + zentangle fill
        val headRadius = maxRadius * 0.65f
        canvas.drawCircle(cx, cy, headRadius, strokePaint)

        // Ears
        val earPath1 = Path().apply {
            moveTo(cx - headRadius * 0.5f, cy - headRadius * 0.7f)
            lineTo(cx - headRadius * 0.9f, cy - headRadius * 1.3f)
            lineTo(cx - headRadius * 0.1f, cy - headRadius * 0.85f)
        }
        val earPath2 = Path().apply {
            moveTo(cx + headRadius * 0.5f, cy - headRadius * 0.7f)
            lineTo(cx + headRadius * 0.9f, cy - headRadius * 1.3f)
            lineTo(cx + headRadius * 0.1f, cy - headRadius * 0.85f)
        }
        canvas.drawPath(earPath1, strokePaint)
        canvas.drawPath(earPath2, strokePaint)

        // Eyes & Muzzle
        canvas.drawCircle(cx - headRadius * 0.35f, cy - headRadius * 0.15f, 25f, strokePaint)
        canvas.drawCircle(cx + headRadius * 0.35f, cy - headRadius * 0.15f, 25f, strokePaint)

        val nosePath = Path().apply {
            moveTo(cx, cy + headRadius * 0.2f)
            lineTo(cx - 30f, cy + headRadius * 0.05f)
            lineTo(cx + 30f, cy + headRadius * 0.05f)
            close()
        }
        canvas.drawPath(nosePath, strokePaint)

        // Zentangle detail lines inside head
        for (i in 1..8) {
            canvas.drawCircle(cx, cy, headRadius * (i / 9f), thinPaint)
        }
    }

    private fun drawArchitectureLineArt(
        canvas: Canvas,
        w: Float,
        h: Float,
        margin: Float,
        random: Random,
        strokePaint: Paint,
        thinPaint: Paint
    ) {
        val baseH = h - margin - 40f
        val numTowers = random.nextInt(3, 6)
        val towerW = (w - 2 * margin) / numTowers

        for (i in 0 until numTowers) {
            val left = margin + i * towerW
            val right = left + towerW - 10f
            val towerHeight = random.nextFloat() * (h * 0.45f) + (h * 0.3f)
            val top = baseH - towerHeight

            canvas.drawRect(left, top, right, baseH, strokePaint)

            // Gothic Spire
            val spirePath = Path().apply {
                moveTo(left, top)
                lineTo((left + right) / 2f, top - random.nextFloat() * 120f - 80f)
                lineTo(right, top)
            }
            canvas.drawPath(spirePath, strokePaint)

            // Arched Windows
            val windows = random.nextInt(3, 6)
            val winH = (baseH - top) / (windows + 1)
            for (wIdx in 1..windows) {
                val winTop = top + wIdx * winH
                val winLeft = left + 15f
                val winRight = right - 15f
                val winRect = RectF(winLeft, winTop, winRight, winTop + winH * 0.6f)
                canvas.drawRoundRect(winRect, 15f, 15f, strokePaint)
                canvas.drawLine((winLeft + winRight) / 2f, winTop, (winLeft + winRight) / 2f, winTop + winH * 0.6f, thinPaint)
            }
        }
    }

    private fun drawAbstractZentangle(
        canvas: Canvas,
        w: Float,
        h: Float,
        margin: Float,
        random: Random,
        strokePaint: Paint,
        thinPaint: Paint
    ) {
        val numRibbons = random.nextInt(5, 9)
        for (i in 0 until numRibbons) {
            val path = Path()
            val startX = margin + random.nextFloat() * (w - 2 * margin)
            val startY = margin
            path.moveTo(startX, startY)

            val ctrl1X = random.nextFloat() * w
            val ctrl1Y = h * 0.33f
            val ctrl2X = random.nextFloat() * w
            val ctrl2Y = h * 0.66f
            val endX = margin + random.nextFloat() * (w - 2 * margin)
            val endY = h - margin

            path.cubicTo(ctrl1X, ctrl1Y, ctrl2X, ctrl2Y, endX, endY)
            canvas.drawPath(path, strokePaint)

            // Parallel zentangle lines
            val pathParallel = Path()
            pathParallel.moveTo(startX + 25f, startY)
            pathParallel.cubicTo(ctrl1X + 25f, ctrl1Y, ctrl2X + 25f, ctrl2Y, endX + 25f, endY)
            canvas.drawPath(pathParallel, thinPaint)
        }
    }

    fun saveBitmapToLocalFile(context: Context, bitmap: Bitmap, fileName: String): String {
        val directory = File(context.filesDir, "coloring_pages")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}
