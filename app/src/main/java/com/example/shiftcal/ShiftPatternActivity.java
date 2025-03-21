package com.example.shiftcal;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ShiftPatternActivity extends AppCompatActivity {
    private LinearLayout patternContainer, selectedPatternContainer;
    private Button addButton, saveButton;
    private List<ShiftItem> shiftItems = new ArrayList<>(); // 기본 근무 패턴
    private List<ShiftItem> selectedPattern = new ArrayList<>(); // 선택된 패턴

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_pattern);

        patternContainer = findViewById(R.id.pattern_container);
        selectedPatternContainer = findViewById(R.id.selected_pattern_container);
        addButton = findViewById(R.id.add_shift_button);
        saveButton = findViewById(R.id.save_pattern_button);

        // 기본 근무 패턴 추가
        shiftItems.add(new ShiftItem("주", "#0000FF")); // 파랑
        shiftItems.add(new ShiftItem("야", "#FF0000")); // 빨강
        shiftItems.add(new ShiftItem("휴", "#00FF00")); // 초록
        updatePatternButtons();

        // + 버튼 클릭
        addButton.setOnClickListener(v -> {
            AddShiftDialog dialog = new AddShiftDialog(this, new AddShiftDialog.OnShiftAddedListener() {
                @Override
                public void onShiftAdded(ShiftType shift) {
                    // ShiftType의 int color를 HEX 문자열로 변환
                    String hexColor = String.format("#%06X", (0xFFFFFF & shift.getColor()));
                    shiftItems.add(new ShiftItem(shift.getName(), hexColor));
                    updatePatternButtons();
                }
            });
            dialog.show();
        });

        // 저장 버튼 클릭
        saveButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            String[] patternArray = new String[selectedPattern.size()];
            for (int i = 0; i < selectedPattern.size(); i++) {
                patternArray[i] = selectedPattern.get(i).name;
            }
            resultIntent.putExtra("shiftPattern", patternArray);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updatePatternButtons() {
        patternContainer.removeAllViews();
        for (ShiftItem item : shiftItems) {
            Button button = new Button(this);
            button.setText(item.name);
            button.setBackground(createCircleDrawable(item.color));
            button.setTextColor(getResources().getColor(android.R.color.white));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            button.setOnClickListener(v -> {
                selectedPattern.add(item);
                updateSelectedPattern();
            });
            patternContainer.addView(button);
        }
    }

    private void updateSelectedPattern() {
        selectedPatternContainer.removeAllViews();
        for (ShiftItem item : selectedPattern) {
            Button button = new Button(this);
            button.setText(item.name);
            button.setBackground(createCircleDrawable(item.color));
            button.setTextColor(getResources().getColor(android.R.color.white));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
            params.setMargins(4, 4, 4, 4);
            button.setLayoutParams(params);
            selectedPatternContainer.addView(button);
        }
    }

    private Drawable createCircleDrawable(String color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(android.graphics.Color.parseColor(color));
        return drawable;
    }

    // ShiftItem 클래스
    private static class ShiftItem {
        String name;
        String color;

        ShiftItem(String name, String color) {
            this.name = name;
            this.color = color;
        }
    }
}

//package com.example.shiftcal;
//
//import android.content.Intent;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import androidx.appcompat.app.AppCompatActivity;
//import java.util.ArrayList;
//import java.util.List;
//
//public class ShiftPatternActivity extends AppCompatActivity {
//    private LinearLayout patternContainer, selectedPatternContainer;
//    private Button addButton, saveButton;
//    private List<ShiftItem> shiftItems = new ArrayList<>(); // 기본 근무 패턴
//    private List<ShiftItem> selectedPattern = new ArrayList<>(); // 선택된 패턴
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_shift_pattern);
//
//        patternContainer = findViewById(R.id.pattern_container);
//        selectedPatternContainer = findViewById(R.id.selected_pattern_container);
//        addButton = findViewById(R.id.add_shift_button);
//        saveButton = findViewById(R.id.save_pattern_button);
//
//        // 기본 근무 패턴 추가
//        shiftItems.add(new ShiftItem("주", "#0000FF")); // 파랑
//        shiftItems.add(new ShiftItem("야", "#FF0000")); // 빨강
//        shiftItems.add(new ShiftItem("휴", "#00FF00")); // 초록
//        updatePatternButtons();
//
//        // + 버튼 클릭
//        addButton.setOnClickListener(v -> {
//            AddShiftDialog dialog = new AddShiftDialog(this, new AddShiftDialog.OnShiftAddedListener() {
//                @Override
//                public void onShiftAdded(String name, String color) {
//                    shiftItems.add(new ShiftItem(name, color));
//                    updatePatternButtons();
//                }
//            });
//            dialog.show();
//        });
//
//        // 저장 버튼 클릭
//        saveButton.setOnClickListener(v -> {
//            Intent resultIntent = new Intent();
//            String[] patternArray = new String[selectedPattern.size()];
//            for (int i = 0; i < selectedPattern.size(); i++) {
//                patternArray[i] = selectedPattern.get(i).name;
//            }
//            resultIntent.putExtra("shiftPattern", patternArray);
//            setResult(RESULT_OK, resultIntent);
//            finish();
//        });
//    }
//
//    private void updatePatternButtons() {
//        patternContainer.removeAllViews();
//        for (ShiftItem item : shiftItems) {
//            Button button = new Button(this);
//            button.setText(item.name);
//            button.setBackground(createCircleDrawable(item.color));
//            button.setTextColor(getResources().getColor(android.R.color.white));
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
//            params.setMargins(8, 8, 8, 8);
//            button.setLayoutParams(params);
//
//            button.setOnClickListener(v -> {
//                selectedPattern.add(item);
//                updateSelectedPattern();
//            });
//            patternContainer.addView(button);
//        }
//    }
//
//    private void updateSelectedPattern() {
//        selectedPatternContainer.removeAllViews();
//        for (ShiftItem item : selectedPattern) {
//            Button button = new Button(this);
//            button.setText(item.name);
//            button.setBackground(createCircleDrawable(item.color));
//            button.setTextColor(getResources().getColor(android.R.color.white));
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(80, 80);
//            params.setMargins(4, 4, 4, 4);
//            button.setLayoutParams(params);
//            selectedPatternContainer.addView(button);
//        }
//    }
//
//    private Drawable createCircleDrawable(String color) {
//        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
//        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
//        drawable.setColor(android.graphics.Color.parseColor(color));
//        return drawable;
//    }
//
//    // ShiftItem 클래스
//    private static class ShiftItem {
//        String name;
//        String color;
//
//        ShiftItem(String name, String color) {
//            this.name = name;
//            this.color = color;
//        }
//    }
//}