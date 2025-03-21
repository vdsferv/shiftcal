package com.example.shiftcal;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment {
    private MaterialCalendarView calendarView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_PATTERNS = "work_patterns";
    private static final String KEY_SHIFT_TYPES = "shift_types";
    private List<String> workPatterns;
    private List<ShiftType> shiftTypes; // PatternEditFragment.ShiftType → ShiftType
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        loadShiftTypes();
        loadPatterns();
        updateCalendar();

        // 패턴 변경 관찰
        sharedViewModel.getPatternUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                loadShiftTypes();
                loadPatterns();
                updateCalendar();
                Toast.makeText(getContext(), "패턴이 변경되어 캘린더가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 알람 변경 관찰
        sharedViewModel.getAlarmUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                Toast.makeText(getContext(), "알람이 설정되어 캘린더가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void loadShiftTypes() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_SHIFT_TYPES, null);
        Log.d("KEY_SHIFT_TYPES:com.example.shiftcal", json);
        try {
            Type type = new TypeToken<List<ShiftType>>() {}.getType();
            shiftTypes = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            // 잘못된 데이터 초기화
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_SHIFT_TYPES).apply();
            shiftTypes = new ArrayList<>();
            // 초기 데이터 설정 (필요 시)
            shiftTypes.add(new ShiftType("주간", Color.parseColor("#FF6200EE")));
            shiftTypes.add(new ShiftType("야간", Color.parseColor("#FF03DAC5")));
            saveShiftTypes();
        }
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

    private void loadPatterns() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_PATTERNS, null);
        Type type = new TypeToken<List<String>>() {}.getType();
        workPatterns = gson.fromJson(json, type);
        if (workPatterns == null) {
            workPatterns = new ArrayList<>();
        }
    }

    private void updateCalendar() {
        calendarView.removeDecorators();
        CalendarDay today = CalendarDay.today();
        int daysInMonth = today.getDate().lengthOfMonth();
        for (int i = 0; i < daysInMonth; i++) {
            CalendarDay day = CalendarDay.from(today.getYear(), today.getMonth(), i + 1);
            if (!workPatterns.isEmpty()) {
                String shift = workPatterns.get(i % workPatterns.size());
                int color = getColorForShift(shift);
                ShiftDecorator decorator = new ShiftDecorator(day, shift, color);
                calendarView.addDecorator(decorator);
            }
        }
        calendarView.invalidateDecorators();
    }

    private int getColorForShift(String shift) {
        for (ShiftType shiftType : shiftTypes) {
            if (shiftType.getName().equalsIgnoreCase(shift)) {
                return shiftType.getColor();
            }
        }
        return android.graphics.Color.GRAY;
    }
}