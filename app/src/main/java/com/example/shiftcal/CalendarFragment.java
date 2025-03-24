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
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarFragment extends Fragment implements OnMonthChangedListener {
    private MaterialCalendarView calendarView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_PATTERNS = "work_patterns";
    private static final String KEY_SHIFT_TYPES = "shift_types";
    private List<String> workPatterns;
    private List<ShiftType> shiftTypes;
    private SharedViewModel sharedViewModel;
    private LocalDate startDate;
    private Map<LocalDate, String> shiftMap;
    private Map<String, Integer> shiftColors;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
//        calendarView.state().edit()
//                .setMinimumDate(CalendarDay.from(2025, 0, 1))
//                .setMaximumDate(CalendarDay.from(2025, 11, 31))
//                .commit();

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        workPatterns = new ArrayList<>();
        shiftTypes = new ArrayList<>();
        shiftMap = new HashMap<>();
        shiftColors = new HashMap<>();
        startDate = LocalDate.of(2025, 1, 1);

        loadShiftTypes();
        loadPatterns();
        updateShiftColors();

        updateCalendarForMonth(LocalDate.now());
        calendarView.setOnMonthChangedListener(this);

        sharedViewModel.getPatternUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                loadShiftTypes();
                loadPatterns();
                updateShiftColors();
                updateCalendarForMonth(LocalDate.now());
                Toast.makeText(getContext(), "패턴이 변경되어 캘린더가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        sharedViewModel.getAlarmUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                Toast.makeText(getContext(), "알람이 설정되어 캘린더가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        int monthValue = date.getMonth() + 1;
        Log.d("CalendarFragment", "onMonthChanged: " + date.getYear() + "-" + monthValue + "-" + date.getDay());
        if (monthValue < 1 || monthValue > 12) {
            Log.e("CalendarFragment", "Invalid month value in onMonthChanged: " + monthValue + ", using current month");
            LocalDate now = LocalDate.now();
            monthValue = now.getMonthValue();
        }
        LocalDate currentMonth = LocalDate.of(date.getYear(), monthValue, 1);
        Log.d("CalendarFragment", "Month changed: " + currentMonth);
        updateCalendarForMonth(currentMonth);
    }

    private void loadShiftTypes() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_SHIFT_TYPES, null);
        Log.d("KEY_SHIFT_TYPES:com.example.shiftcal", json != null ? json : "null");
        try {
            Type type = new TypeToken<List<ShiftType>>() {}.getType();
            shiftTypes = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_SHIFT_TYPES).apply();
            shiftTypes = new ArrayList<>();
            shiftTypes.add(new ShiftType("주간", Color.parseColor("#FF6200EE")));
            shiftTypes.add(new ShiftType("야간", Color.parseColor("#FF03DAC5")));
            saveShiftTypes();
        }
        if (shiftTypes == null) {
            shiftTypes = new ArrayList<>();
        }
        Log.d("CalendarFragment", "ShiftTypes loaded: " + shiftTypes.size());
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
        Log.d("KEY_PATTERNS:com.example.shiftcal", json != null ? json : "null");
        Type type = new TypeToken<List<String>>() {}.getType();
        workPatterns = gson.fromJson(json, type);
        if (workPatterns == null || workPatterns.isEmpty()) {
            workPatterns = new ArrayList<>();
            workPatterns.add("주간");
            workPatterns.add("야간");
            workPatterns.add("휴식");
            Log.d("CalendarFragment", "Added test workPatterns: " + workPatterns.toString());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String testJson = gson.toJson(workPatterns);
            editor.putString(KEY_PATTERNS, testJson);
            editor.apply();
        }
        Log.d("CalendarFragment", "WorkPatterns loaded: " + workPatterns.toString());
    }

    private void updateShiftColors() {
        shiftColors.clear();
        for (ShiftType shiftType : shiftTypes) {
            shiftColors.put(shiftType.getName(), shiftType.getColor());
        }
        Log.d("CalendarFragment", "ShiftColors updated: " + shiftColors.toString());
    }

    private void updateCalendarForMonth(LocalDate month) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            Log.d("CalendarFragment", "Updating calendar for month: " + month);

            LocalDate startOfMonth = month.withDayOfMonth(1);
            LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());
            LocalDate startLimit = startOfMonth.minusMonths(1);
            LocalDate endLimit = endOfMonth.plusMonths(1);
            shiftMap.entrySet().removeIf(entry -> entry.getKey().isBefore(startLimit) || entry.getKey().isAfter(endLimit));
            Log.d("CalendarFragment", "ShiftMap after cleanup: " + shiftMap.size() + " entries");

            if (workPatterns.isEmpty()) {
                Log.d("CalendarFragment", "WorkPatterns is empty, clearing shiftMap");
                shiftMap.clear();
                requireActivity().runOnUiThread(() -> {
                    calendarView.removeDecorators();
                    calendarView.invalidateDecorators();
                });
                return;
            }

            LocalDate currentDate = startOfMonth;
            while (!currentDate.isAfter(endOfMonth)) {
                if (!shiftMap.containsKey(currentDate)) {
                    String shiftType = getShiftTypeForDate(currentDate, startDate, workPatterns);
                    shiftMap.put(currentDate, shiftType);
                    Log.d("CalendarFragment", "Added shift for " + currentDate + ": " + shiftType);
                }
                currentDate = currentDate.plusDays(1);
            }

            requireActivity().runOnUiThread(() -> {
                calendarView.removeDecorators();

                // 날짜별로 개별 ShiftDecorator 생성
                for (Map.Entry<LocalDate, String> entry : shiftMap.entrySet()) {
                    LocalDate date = entry.getKey();
                    String shift = entry.getValue();
                    if (shift == null) continue;

                    CalendarDay calendarDay = CalendarDay.from(
                            date.getYear(),
                            date.getMonthValue() - 1,
                            date.getDayOfMonth()
                    );
                    int color = shiftColors.getOrDefault(shift, Color.GRAY);
                    ShiftDecorator decorator = new ShiftDecorator(calendarDay, shift, color);
                    calendarView.addDecorator(decorator);
                    Log.d("CalendarFragment", "Added decorator for " + calendarDay + " with shift: " + shift);
                }

                Log.d("CalendarFragment", "ShiftMap updated for " + month + ": " + shiftMap.size() + " entries");
                Log.d("CalendarFragment", "ShiftMap contents: " + shiftMap.toString());
                calendarView.invalidateDecorators();
            });

            long endTime = System.currentTimeMillis();
            Log.d("CalendarFragment", "updateCalendarForMonth took " + (endTime - startTime) + " ms");
        }).start();
    }

    private String getShiftTypeForDate(LocalDate date, LocalDate startDate, List<String> workPatterns) {
        if (workPatterns == null || workPatterns.isEmpty()) {
            return null;
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, date);
        int patternLength = workPatterns.size();
        int patternIndex = (int) (daysBetween % patternLength);
        if (patternIndex < 0) {
            patternIndex += patternLength;
        }
        String shiftType = workPatterns.get(patternIndex);
        Log.d("CalendarFragment", "ShiftType for " + date + ": " + shiftType);
        return shiftType;
    }
}