package com.example.shiftcal;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private Map<LocalDate, ShiftDecorator> decoratorCache;
    private List<ShiftDecorator> shiftDecorators;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 500;
    private LocalDate lastUpdatedMonth;
    private final Set<LocalDate> decoratedDates = new HashSet<>();
    private volatile boolean isUpdating = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
        try {
            calendarView.state().edit()
                    .setMinimumDate(CalendarDay.from(2025, 1, 1))
                    .setMaximumDate(CalendarDay.from(2025, 12, 31))
                    .commit();
        } catch (Exception e) {
            Log.e("CalendarFragment", "Error setting calendar date range: " + e.getMessage());
            calendarView.state().edit()
                    .setMinimumDate(CalendarDay.from(2025, 1, 1))
                    .setMaximumDate(CalendarDay.from(2025, 12, 31))
                    .commit();
        }

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        workPatterns = new ArrayList<>();
        shiftTypes = new ArrayList<>();
        shiftMap = new HashMap<>();
        shiftColors = new HashMap<>();
        decoratorCache = new HashMap<>();
        shiftDecorators = new ArrayList<>();
        startDate = LocalDate.of(2025, 1, 1);

        loadShiftTypes();
        loadPatterns();
        updateShiftColors();

        updateCalendarForMonth(LocalDate.now(), true);

        calendarView.setOnMonthChangedListener(this);

        sharedViewModel.getPatternUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                loadShiftTypes();
                loadPatterns();
                updateShiftColors();
                shiftMap.clear();
                decoratorCache.clear();
                shiftDecorators.clear();
                decoratedDates.clear();
                updateCalendarForMonth(LocalDate.now(), true);
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
        LocalDate currentMonth;
        try {
            currentMonth = LocalDate.of(date.getYear(), date.getMonth(), 1);
        } catch (Exception e) {
            Log.e("CalendarFragment", "Invalid date in onMonthChanged: " + date.getYear() + "-" + date.getMonth() + "-" + date.getDay() + ", using current month");
            currentMonth = LocalDate.now().withDayOfMonth(1);
        }

        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        LocalDate finalCurrentMonth = currentMonth;
        debounceRunnable = () -> {
            Log.d("CalendarFragment", "Month changed (debounced): " + finalCurrentMonth);
            updateCalendarForMonth(finalCurrentMonth, false);
        };
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
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
        Log.d("KEY_PATTERNS:com.example.shiftcal", "Loaded JSON: " + (json != null ? json : "null"));
        Type type = new TypeToken<List<String>>() {}.getType();
        workPatterns = gson.fromJson(json, type);

        boolean resetPatterns = false;
        if (workPatterns == null || workPatterns.isEmpty()) {
            Log.w("CalendarFragment", "WorkPatterns is null or empty, resetting to default");
            resetPatterns = true;
        } else {
            int originalSize = workPatterns.size();
            workPatterns.removeIf(Objects::isNull);
            if (workPatterns.isEmpty()) {
                Log.w("CalendarFragment", "WorkPatterns contains only null elements, resetting to default");
                resetPatterns = true;
            } else if (workPatterns.size() < originalSize) {
                Log.w("CalendarFragment", "Removed " + (originalSize - workPatterns.size()) + " null elements from workPatterns");
                resetPatterns = true;
            }
        }

        if (resetPatterns) {
            workPatterns = new ArrayList<>();
            workPatterns.add("주간");
            workPatterns.add("야간");
            workPatterns.add("휴식");
            workPatterns.add("호호");
            workPatterns.add("호");
            Log.d("CalendarFragment", "Reset workPatterns to default: " + workPatterns.toString());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String testJson = gson.toJson(workPatterns);
            editor.putString(KEY_PATTERNS, testJson);
            editor.apply();
            Toast.makeText(getContext(), "근무 패턴 데이터가 초기화되었습니다.", Toast.LENGTH_LONG).show();
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

    private void updateCalendarForMonth(LocalDate month, boolean initialLoad) {
        if (isUpdating) {
            Log.d("CalendarFragment", "Update already in progress for month: " + month + ", skipping");
            return;
        }

        isUpdating = true;
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();
                Log.d("CalendarFragment", "Updating calendar for month: " + month + " on thread: " + Thread.currentThread().getName());

                LocalDate startOfMonth = month.withDayOfMonth(1);
                LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());
                LocalDate startLimit = initialLoad ? startOfMonth : startOfMonth.minusMonths(1);
                LocalDate endLimit = initialLoad ? endOfMonth : endOfMonth.plusMonths(1);

                // shiftMap에서 범위 밖의 데이터 제거
                shiftMap.entrySet().removeIf(entry -> entry.getKey().isBefore(startLimit) || entry.getKey().isAfter(endLimit) || entry.getValue() == null);
                Log.d("CalendarFragment", "ShiftMap after cleanup: " + shiftMap.size() + " entries");

                if (workPatterns.isEmpty()) {
                    Log.d("CalendarFragment", "WorkPatterns is empty, clearing shiftMap");
                    shiftMap.clear();
                    requireActivity().runOnUiThread(() -> {
                        calendarView.removeDecorators();
                        shiftDecorators.clear();
                        decoratedDates.clear();
                        decoratorCache.clear();
                        calendarView.invalidateDecorators();
                    });
                    return;
                }

                // 현재 달 데이터 추가
                LocalDate currentDate = startOfMonth;
                while (!currentDate.isAfter(endOfMonth)) {
                    if (!shiftMap.containsKey(currentDate)) {
                        String shiftType = getShiftTypeForDate(currentDate, startDate, workPatterns);
                        shiftMap.put(currentDate, shiftType);
                        if (currentDate.equals(startOfMonth) || currentDate.equals(LocalDate.of(2025, 7, 12))) {
                            Log.d("CalendarFragment", "Added to shiftMap: " + currentDate + " -> " + shiftType);
                        }
                    }
                    currentDate = currentDate.plusDays(1);
                }

                // 초기 로드가 아닌 경우에만 이전 달과 다음 달 데이터 추가
                if (!initialLoad) {
                    // 이전 달 데이터 추가
                    LocalDate prevMonthStart = startLimit.withDayOfMonth(1);
                    LocalDate prevMonthEnd = startOfMonth.minusDays(1);
                    currentDate = prevMonthStart;
                    while (!currentDate.isAfter(prevMonthEnd)) {
                        if (!shiftMap.containsKey(currentDate)) {
                            String shiftType = getShiftTypeForDate(currentDate, startDate, workPatterns);
                            shiftMap.put(currentDate, shiftType);
                            if (currentDate.equals(prevMonthStart)) {
                                Log.d("CalendarFragment", "Added to shiftMap (prev month): " + currentDate + " -> " + shiftType);
                            }
                        }
                        currentDate = currentDate.plusDays(1);
                    }

                    // 다음 달 데이터 추가
                    LocalDate nextMonthStart = endOfMonth.plusDays(1);
                    LocalDate nextMonth = endOfMonth.plusMonths(1).withDayOfMonth(1);
                    LocalDate nextMonthEnd = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
                    currentDate = nextMonthStart;
                    while (!currentDate.isAfter(nextMonthEnd)) {
                        if (!shiftMap.containsKey(currentDate)) {
                            String shiftType = getShiftTypeForDate(currentDate, startDate, workPatterns);
                            shiftMap.put(currentDate, shiftType);
                            if (currentDate.equals(nextMonthStart)) {
                                Log.d("CalendarFragment", "Added to shiftMap (next month): " + currentDate + " -> " + shiftType);
                            }
                        }
                        currentDate = currentDate.plusDays(1);
                    }
                }

                // shiftMap 데이터 검증
                Map<LocalDate, String> tempShiftMap = new HashMap<>();
                for (Map.Entry<LocalDate, String> entry : shiftMap.entrySet()) {
                    LocalDate date = entry.getKey();
                    String shiftType = entry.getValue();
                    if (tempShiftMap.containsKey(date)) {
                        Log.w("CalendarFragment", "Duplicate shiftMap entry for " + date + ": existing=" + tempShiftMap.get(date) + ", new=" + shiftType);
                    }
                    tempShiftMap.put(date, shiftType);
                }
                shiftMap.clear();
                shiftMap.putAll(tempShiftMap);
                Log.d("CalendarFragment", "ShiftMap after validation: " + shiftMap.toString());

                // 데코레이터 캐싱 및 생성
                List<ShiftDecorator> newDecorators = new ArrayList<>();
                Set<LocalDate> newDecoratedDates = new HashSet<>();

                for (Map.Entry<LocalDate, String> entry : shiftMap.entrySet()) {
                    LocalDate date = entry.getKey();
                    if (date.isBefore(startLimit) || date.isAfter(endLimit)) {
                        continue;
                    }
                    if (newDecoratedDates.contains(date)) {
                        Log.w("CalendarFragment", "Duplicate decorator for " + date + " skipped");
                        continue;
                    }
                    String shiftType = entry.getValue();
                    int color = shiftColors.getOrDefault(shiftType, Color.GRAY);
                    ShiftDecorator decorator = decoratorCache.computeIfAbsent(date, k -> new ShiftDecorator(date, shiftType, color));
                    newDecorators.add(decorator);
                    newDecoratedDates.add(date);
                }

                // UI 업데이트
                requireActivity().runOnUiThread(() -> {
                    long uiStartTime = System.currentTimeMillis();
                    Log.d("CalendarFragment", "invalidateDecorators called on thread: " + Thread.currentThread().getName());

                    calendarView.removeDecorators();
                    shiftDecorators.clear();
                    decoratedDates.clear();

                    shiftDecorators.addAll(newDecorators);
                    decoratedDates.addAll(newDecoratedDates);
                    calendarView.addDecorators(newDecorators);
                    calendarView.invalidateDecorators();

                    Log.d("CalendarFragment", "ShiftDecorators size: " + shiftDecorators.size());
                    Log.d("CalendarFragment", "DecoratorCache size: " + decoratorCache.size());
                    long uiEndTime = System.currentTimeMillis();
                    Log.d("CalendarFragment", "UI update took " + (uiEndTime - uiStartTime) + " ms");
                    Log.d("CalendarFragment", "ShiftMap updated for " + month + ": " + shiftMap.size() + " entries");
                });

                lastUpdatedMonth = month;
                long endTime = System.currentTimeMillis();
                Log.d("CalendarFragment", "updateCalendarForMonth took " + (endTime - startTime) + " ms");
            } finally {
                isUpdating = false;
            }
        }).start();
    }

    private String getShiftTypeForDate(LocalDate date, LocalDate startDate, List<String> workPatterns) {
        if (workPatterns == null || workPatterns.isEmpty()) {
            throw new IllegalStateException("WorkPatterns is null or empty for date: " + date);
        }
        if (workPatterns.contains(null)) {
            throw new IllegalStateException("WorkPatterns contains null elements: " + workPatterns);
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, date);
        int patternLength = workPatterns.size();
        int patternIndex = (int) (daysBetween % patternLength);
        if (patternIndex < 0) {
            patternIndex += patternLength;
        }
        String shiftType = workPatterns.get(patternIndex);
        Log.d("CalendarFragment", "getShiftTypeForDate: date=" + date + ", daysBetween=" + daysBetween + ", patternLength=" + patternLength + ", patternIndex=" + patternIndex + ", shiftType=" + shiftType);
        return shiftType;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        calendarView.removeDecorators();
        shiftDecorators.clear();
        decoratedDates.clear();
        decoratorCache.clear();
        calendarView = null;
    }
}