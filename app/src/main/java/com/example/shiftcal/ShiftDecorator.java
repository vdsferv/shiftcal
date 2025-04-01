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
import android.util.LruCache;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.time.LocalDate;
import java.time.DateTimeException;

public class ShiftDecorator implements DayViewDecorator {
    private static final String TAG = "ShiftDecorator";
    private final LocalDate date;
    private final String shiftType;
    private final int color;
    private final Paint circlePaint;
    private final Paint textPaint;
    private final Paint dayPaint;

    public ShiftDecorator(LocalDate date, String shiftType, int color) {
        this.date = date;
        this.shiftType = shiftType;
        this.color = color;

        this.circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.circlePaint.setStyle(Paint.Style.FILL);
        this.circlePaint.setColor(color);

        this.textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(Color.WHITE);
        this.textPaint.setTextSize(28f);
        this.textPaint.setTextAlign(Paint.Align.CENTER);

        this.dayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.dayPaint.setColor(Color.BLACK);
        this.dayPaint.setTextSize(28f);
        this.dayPaint.setTextAlign(Paint.Align.LEFT);

        Log.d(TAG, String.format("Created decorator for date: %s, shift: %s, color: #%06X", 
            date, shiftType, color));
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        try {
            boolean shouldDecorate = day.getYear() == date.getYear() &&
                    day.getMonth() == date.getMonthValue() - 1 &&
                    day.getDay() == date.getDayOfMonth();
            
            Log.d(TAG, String.format("shouldDecorate called for %d-%d-%d: %b (target: %s)",
                    day.getYear(), day.getMonth() + 1, day.getDay(),
                    shouldDecorate, date));
            
            if (shouldDecorate) {
                Log.d(TAG, String.format("Decorator will be applied for date: %s", date));
            }
            
            return shouldDecorate;
        } catch (DateTimeException e) {
            Log.e(TAG, "Invalid date: " + day.getYear() + "-" + (day.getMonth() + 1) + "-" + day.getDay() + ", " + e.getMessage());
            return false;
        }
    }

    @Override
    public void decorate(DayViewFacade view) {
        Log.d(TAG, String.format("decorate called for date: %s, shift: %s", date, shiftType));
        view.addSpan(new HideDefaultNumberSpan());
        view.setBackgroundDrawable(new CircleDrawable(date.getDayOfMonth(), color, shiftType));
        Log.d(TAG, String.format("Decoration applied for date: %s", date));
    }

    private static class HideDefaultNumberSpan extends CharacterStyle {
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(Color.TRANSPARENT);
        }
    }

    private class CircleDrawable extends android.graphics.drawable.Drawable {
        private final int dayNumber;
        private final int color;
        private final String shiftText;

        public CircleDrawable(int dayNumber, int color, String shiftText) {
            this.dayNumber = dayNumber;
            this.color = color;
            this.shiftText = shiftText;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;

            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY() + 10f;
            float radius = Math.min(bounds.width(), bounds.height()) * 0.6f / 2f;

            // 배경 원
            canvas.drawCircle(centerX, centerY, radius, circlePaint);

            // 날짜 텍스트
            float dayTextX = bounds.left + 5f;
            float dayTextY = bounds.top + 24f;
            canvas.drawText(String.valueOf(dayNumber), dayTextX, dayTextY, dayPaint);

            // 시프트 텍스트
            float textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2;
            canvas.drawText(shiftText, centerX, textY, textPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            circlePaint.setAlpha(alpha);
            textPaint.setAlpha(alpha);
            dayPaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            circlePaint.setColorFilter(colorFilter);
            textPaint.setColorFilter(colorFilter);
            dayPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
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