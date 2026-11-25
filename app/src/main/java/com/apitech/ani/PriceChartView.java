package com.apitech.ani;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** Draws the SVG price chart from Dashboard (viewBox 0 0 700 180). */
public class PriceChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path areaPath = new Path();

    public PriceChartView(Context c) {
        this(c, null);
    }

    public PriceChartView(Context c, AttributeSet a) {
        this(c, a, 0);
    }

    public PriceChartView(Context c, AttributeSet a, int defStyleAttr) {
        super(c, a, defStyleAttr);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 3));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(0xFF054721);
        dot.setColor(0xFF054721);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float sx = w / 700f, sy = h / 180f;
        linePath.reset();
        linePath.moveTo(0, 150 * sy);
        linePath.cubicTo(80 * sx, 132 * sy, 95 * sx, 142 * sy, 165 * sx, 115 * sy);
        linePath.cubicTo(235 * sx, 88 * sy, 250 * sx, 125 * sy, 320 * sx, 90 * sy);
        linePath.cubicTo(390 * sx, 55 * sy, 405 * sx, 101 * sy, 470 * sx, 66 * sy);
        linePath.cubicTo(535 * sx, 31 * sy, 565 * sx, 85 * sy, 700 * sx, 30 * sy);
        areaPath.set(linePath);
        areaPath.lineTo(w, h);
        areaPath.lineTo(0, h);
        areaPath.close();
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x739CAF88, 0x059CAF88, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(areaPath, fill);
        canvas.drawPath(linePath, line);
        canvas.drawCircle(getWidth() - Ui.dp(getContext(), 6), 30f * getHeight() / 180f, Ui.dp(getContext(), 5), dot);
    }
}
