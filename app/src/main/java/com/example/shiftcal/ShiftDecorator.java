package com.example.shiftcal;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.CharacterStyle;
import android.text.TextPaint;
import android.util.Log;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.time.LocalDate;
import java.time.DateTimeException;

public class ShiftDecorator implements DayViewDecorator {
    private final LocalDate date;
    private final String shiftType;
    private final int color;

    public ShiftDecorator(LocalDate date, String shiftType, int color) {
        this.date = date;
        this.shiftType = shiftType;
        this.color = color;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        try {
            LocalDate localDate = LocalDate.of(day.getYear(), day.getMonth(), day.getDay());
            boolean shouldDecorate = localDate.equals(date);
            Log.d("ShiftDecorator", "shouldDecorate for " + localDate + ": " + shouldDecorate + ", shift: " + shiftType);
            return shouldDecorate;
        } catch (DateTimeException e) {
            Log.e("ShiftDecorator", "Invalid date: " + day.getYear() + "-" + day.getMonth() + "-" + day.getDay() + ", " + e.getMessage());
            return false;
        }
    }

    @Override
    public void decorate(DayViewFacade view) {
        Log.d("ShiftDecorator", "decorate called for " + date + " with shift: " + shiftType);
        view.addSpan(new HideDefaultNumberSpan());
        CircleDrawable circleDrawable = new CircleDrawable(date.getDayOfMonth(), color, shiftType);
        view.setBackgroundDrawable(circleDrawable);
    }

    private static class HideDefaultNumberSpan extends CharacterStyle {
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(Color.TRANSPARENT);
        }
    }

    public class CircleDrawable extends Drawable {
        private final int dayNumber;
        private final int color;
        private final String shiftText;
        private final Paint paint;
        private final Paint textPaint;
        private final Paint dayPaint;

        public CircleDrawable(int dayNumber, int color, String shiftText) {
            this.dayNumber = dayNumber;
            this.color = color;
            this.shiftText = shiftText;
            this.paint = new Paint();
            this.paint.setAntiAlias(true);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setColor(color);
            this.textPaint = new Paint();
            this.textPaint.setColor(Color.WHITE);
            this.textPaint.setTextSize(28f);
            this.textPaint.setAntiAlias(true);
            this.textPaint.setTextAlign(Paint.Align.CENTER);
            this.dayPaint = new Paint();
            this.dayPaint.setColor(Color.BLACK);
            this.dayPaint.setTextSize(28f);
            this.dayPaint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;

            Rect clipBounds = canvas.getClipBounds();
            int left = clipBounds.left;
            int top = clipBounds.top;

            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY() + 10f;
            float radius = Math.min(bounds.width(), bounds.height()) / 2f * 0.6f;

            canvas.drawCircle(centerX, centerY, radius, paint);

            float dayTextY = top + 24f;
            canvas.drawText(String.valueOf(dayNumber), left + 5f, dayTextY, dayPaint);

            if (!shiftText.trim().isEmpty()) {
                float textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2;
                canvas.drawText(shiftText, centerX, textY, textPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            textPaint.setAlpha(alpha);
            dayPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            textPaint.setColorFilter(colorFilter);
            dayPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        @Override
        public int getIntrinsicWidth() {
            return 60;
        }

        @Override
        public int getIntrinsicHeight() {
            return 60;
        }

        @Override
        public ConstantState getConstantState() {
            return new CircleDrawableState();
        }

        private class CircleDrawableState extends ConstantState {
            @Override
            public Drawable newDrawable() {
                return new CircleDrawable(dayNumber, color, shiftText);
            }

            @Override
            public Drawable newDrawable(Resources res) {
                return new CircleDrawable(dayNumber, color, shiftText);
            }

            @Override
            public int getChangingConfigurations() {
                return 0;
            }
        }
    }
}