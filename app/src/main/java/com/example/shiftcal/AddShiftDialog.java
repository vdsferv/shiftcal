package com.example.shiftcal;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AddShiftDialog extends Dialog {
    private EditText shiftNameInput;
    private RadioGroup colorGroup;
    private RadioButton colorBlue, colorRed, colorGreen, colorPurple;
    private Button confirmButton;
    private TextView colorPreview; // 색상 미리보기 뷰
    private OnShiftAddedListener listener;

    public AddShiftDialog(Context context, OnShiftAddedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_shift);
        setTitle("새 근무 추가");

        // 뷰 초기화
        shiftNameInput = findViewById(R.id.shift_name_input);
        colorGroup = findViewById(R.id.color_group);
        colorBlue = findViewById(R.id.color_blue);
        colorRed = findViewById(R.id.color_red);
        colorGreen = findViewById(R.id.color_green);
        colorPurple = findViewById(R.id.color_purple);
        confirmButton = findViewById(R.id.confirm_button);
        colorPreview = findViewById(R.id.color_preview);

        // 색상 선택 시 미리보기 업데이트
        colorGroup.setOnCheckedChangeListener((group, checkedId) -> updateColorPreview(getSelectedColor(checkedId)));

        // 확인 버튼 클릭 리스너
        confirmButton.setOnClickListener(v -> {
            String name = shiftNameInput.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(getContext(), "근무 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedColor = getSelectedColor(colorGroup.getCheckedRadioButtonId());
            if (selectedColor == Color.BLACK) { // 기본값 체크
                Toast.makeText(getContext(), "색상을 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // ShiftType 객체로 전달
            ShiftType newShift = new ShiftType(name, selectedColor);
            listener.onShiftAdded(newShift);
            dismiss();
        });

        // 초기 미리보기 설정
        updateColorPreview(Color.BLACK);
    }

    private int getSelectedColor(int selectedId) {
        if (selectedId == R.id.color_blue) {
            return Color.BLUE;
        } else if (selectedId == R.id.color_red) {
            return Color.RED;
        } else if (selectedId == R.id.color_green) {
            return Color.GREEN;
        } else if (selectedId == R.id.color_purple) {
            return Color.parseColor("#800080"); // 보라색
        } else {
            return Color.BLACK; // 기본값
        }
    }

    private void updateColorPreview(int color) {
        colorPreview.setBackgroundColor(color);
        colorPreview.setText("선택된 색상");
        colorPreview.setTextColor(color == Color.BLACK ? Color.WHITE : Color.BLACK); // 가독성 조정
    }

    public interface OnShiftAddedListener {
        void onShiftAdded(ShiftType shift);
    }
}

//package com.example.shiftcal;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//
//public class AddShiftDialog extends Dialog {
//    private EditText shiftNameInput;
//    private RadioGroup colorGroup;
//    private Button confirmButton;
//    private OnShiftAddedListener listener;
//
//    public AddShiftDialog(Context context, OnShiftAddedListener listener) {
//        super(context);
//        this.listener = listener;
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.dialog_add_shift);
//
//        shiftNameInput = findViewById(R.id.shift_name_input);
//        colorGroup = findViewById(R.id.color_group);
//        confirmButton = findViewById(R.id.confirm_button);
//
//        confirmButton.setOnClickListener(v -> {
//            String name = shiftNameInput.getText().toString().trim();
//            if (name.isEmpty()) return;
//
//            int selectedId = colorGroup.getCheckedRadioButtonId();
//            String color;
//            switch (selectedId) {
//                case R.id.color_blue: color = "#0000FF"; break;
//                case R.id.color_red: color = "#FF0000"; break;
//                case R.id.color_green: color = "#00FF00"; break;
//                case R.id.color_purple: color = "#800080"; break;
//                default: color = "#000000"; // 기본 검정
//            }
//
//            listener.onShiftAdded(name, color);
//            dismiss();
//        });
//    }
//
//    public interface OnShiftAddedListener {
//        void onShiftAdded(String name, String color);
//    }
//}