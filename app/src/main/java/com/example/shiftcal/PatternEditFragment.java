package com.example.shiftcal;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PatternEditFragment extends Fragment {
    private Button addShiftTypeButton;
    private LinearLayout shiftButtonsContainer;
    private TextView patternDisplay;
    private Button savePatternButton;
    private Button clearPatternButton;
    private List<ShiftType> shiftTypes;
    private List<String> workPattern;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_SHIFT_TYPES = "shift_types";
    private static final String KEY_PATTERNS = "work_patterns";
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pattern_edit, container, false);

        addShiftTypeButton = view.findViewById(R.id.add_shift_type_button);
        shiftButtonsContainer = view.findViewById(R.id.shift_buttons_container);
        patternDisplay = view.findViewById(R.id.pattern_display);
        savePatternButton = view.findViewById(R.id.save_pattern_button);
        clearPatternButton = view.findViewById(R.id.clear_pattern_button);

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // 초기화
        shiftTypes = new ArrayList<>();
        workPattern = new ArrayList<>();
        loadShiftTypes();
        loadWorkPattern();
        displayShiftButtons();
        updatePatternDisplay();

        // 새로운 근무 형태 추가 버튼 클릭 리스너
        addShiftTypeButton.setOnClickListener(v -> showAddShiftTypeDialog());

        // 패턴 저장 버튼 클릭 리스너
        savePatternButton.setOnClickListener(v -> {
            if (workPattern.isEmpty()) {
                Toast.makeText(getContext(), "패턴을 조합하세요.", Toast.LENGTH_SHORT).show();
            } else {
                saveWorkPattern();
                sharedViewModel.notifyPatternUpdated();
                Toast.makeText(getContext(), "패턴이 저장되었습니다: " + workPattern.toString(), Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // 패턴 초기화 버튼 클릭 리스너
        clearPatternButton.setOnClickListener(v -> {
            workPattern.clear();
            updatePatternDisplay();
            Toast.makeText(getContext(), "패턴이 초기화되었습니다.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void showAddShiftTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_shift_type, null);
        builder.setView(dialogView);

        EditText shiftTypeInput = dialogView.findViewById(R.id.dialog_shift_type_input);
        Spinner colorSpinner = dialogView.findViewById(R.id.color_spinner);
        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        Button addButton = dialogView.findViewById(R.id.dialog_add_button);

        Resources res = getResources();
        String[] colorNames = res.getStringArray(R.array.color_names);
        int[] colorValues = res.getIntArray(R.array.color_values);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, colorNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        addButton.setOnClickListener(v -> {
            String shiftType = shiftTypeInput.getText().toString().trim();
            if (!shiftType.isEmpty()) {
                if (!containsShiftType(shiftType)) {
                    int selectedColor = colorValues[colorSpinner.getSelectedItemPosition()];
                    shiftTypes.add(new ShiftType(shiftType, selectedColor));
                    saveShiftTypes();
                    displayShiftButtons();
                    Toast.makeText(getContext(), "근무 형태 추가: " + shiftType, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "이미 존재하는 근무 형태입니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "근무 형태를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private boolean containsShiftType(String shiftType) {
        for (ShiftType type : shiftTypes) {
            if (type.getName().equalsIgnoreCase(shiftType)) {
                return true;
            }
        }
        return false;
    }

    private void loadShiftTypes() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_SHIFT_TYPES, null);
        System.out.println("KEY_SHIFT_TYPES JSON: " + json);
        Type type = new TypeToken<List<ShiftType>>() {}.getType();
        shiftTypes = gson.fromJson(json, type);
        if (shiftTypes == null) {
            shiftTypes = new ArrayList<>();
        }
    }

    private void saveShiftTypes() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(shiftTypes);
        editor.putString(KEY_SHIFT_TYPES, json);
        editor.apply();
    }

    private void loadWorkPattern() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_PATTERNS, null);
        System.out.println("KEY_PATTERNS JSON: " + json);
        Type type = new TypeToken<List<String>>() {}.getType();
        workPattern = gson.fromJson(json, type);
        if (workPattern == null) {
            workPattern = new ArrayList<>();
        }
    }

    private void saveWorkPattern() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(workPattern);
        editor.putString(KEY_PATTERNS, json);
        editor.apply();
    }

    private void displayShiftButtons() {
        shiftButtonsContainer.removeAllViews();
        for (ShiftType shiftType : shiftTypes) {
            Button button = createShiftButton(shiftType);
            shiftButtonsContainer.addView(button);
        }
    }

    private Button createShiftButton(ShiftType shiftType) {
        Button button = new Button(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(60),
                dpToPx(60)
        );
        params.setMargins(dpToPx(8), 0, dpToPx(8), 0);
        button.setLayoutParams(params);
        button.setText(shiftType.getName());
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setBackground(createCircleDrawable(shiftType.getColor()));
        button.setOnClickListener(v -> {
            workPattern.add(shiftType.getName());
            updatePatternDisplay();
            Toast.makeText(getContext(), shiftType.getName() + " 추가됨", Toast.LENGTH_SHORT).show();
        });
        return button;
    }

    private void updatePatternDisplay() {
        if (workPattern.isEmpty()) {
            patternDisplay.setText("조합된 패턴: 없음");
        } else {
            patternDisplay.setText("조합된 패턴: " + String.join("-", workPattern));
        }
    }

    private android.graphics.drawable.Drawable createCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}