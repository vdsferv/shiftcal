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
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;

public class CalendarFragment extends Fragment implements OnMonthChangedListener {
    private static final int MAX_CACHE_SIZE = 1000; // 캐시 크기 제한
    private MaterialCalendarView calendarView;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_PATTERNS = "work_patterns";
    private static final String KEY_SHIFT_TYPES = "shift_types";

    private List<String> workPatterns;
    private List<ShiftType> shiftTypes;
    private SharedViewModel sharedViewModel;
    private LocalDate startDate;
    private Map<String, Integer> shiftColors;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private List<DayViewDecorator> decorators = new ArrayList<>();
    
    // 데코레이터 캐시 추가 (크기 제한)
    private final Map<LocalDate, ShiftDecorator> decoratorCache = Collections.synchronizedMap(
        new LinkedHashMap<LocalDate, ShiftDecorator>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<LocalDate, ShiftDecorator> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        }
    );

    // 날짜 계산 캐시
    private final Map<LocalDate, Integer> patternIndexCache = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendar_view);
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_NONE);
        calendarView.setTileHeightDp(60);
        calendarView.setTileWidthDp(45);

        // 날짜 범위 설정
        calendarView.state().edit()
                .setMinimumDate(CalendarDay.from(2025, 1, 1))
                .setMaximumDate(CalendarDay.from(2025, 12, 31))
                .commit();

        // 제목 포맷 설정
        calendarView.setTitleFormatter(new TitleFormatter() {
            @Override
            public CharSequence format(CalendarDay day) {
                LocalDate date = LocalDate.of(day.getYear(), day.getMonth() + 1, day.getDay());
                return date.format(DateTimeFormatter.ofPattern("yyyy년 MM월"));
            }
        });

        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        workPatterns = new ArrayList<>();
        shiftTypes = new ArrayList<>();
        shiftColors = new HashMap<>();
        startDate = LocalDate.of(2025, 1, 1);

        loadShiftTypes();
        loadPatterns();
        updateShiftColors();

        // 현재 날짜 기준 달력 갱신
        updateCalendarForMonth(LocalDate.now());

        calendarView.setOnMonthChangedListener(this);

        // 패턴 변경 감지
        sharedViewModel.getPatternUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                loadShiftTypes();
                loadPatterns();
                updateShiftColors();
                decoratorCache.clear(); // 캐시 초기화
                updateCalendarForMonth(LocalDate.now());
                Toast.makeText(getContext(), "패턴이 변경되어 캘린더가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        int monthValue = date.getMonth() + 1;
        LocalDate currentMonth = LocalDate.of(date.getYear(), monthValue, 1);
        
        // 이전 작업 취소
        executorService.submit(() -> {
            try {
                updateCalendarForMonth(currentMonth);
            } catch (Exception e) {
                Log.e("CalendarFragment", "Error updating calendar", e);
            }
        });
    }

    private void updateCalendarForMonth(LocalDate month) {
        long startTime = System.currentTimeMillis();
        Log.d("CalendarFragment", "Updating calendar for " + month);

        LocalDate startOfMonth = month.withDayOfMonth(1);
        LocalDate endOfMonth = month.withDayOfMonth(month.lengthOfMonth());
        LocalDate startLimit = startOfMonth.minusMonths(1);
        LocalDate endLimit = endOfMonth.plusMonths(1);

        if (workPatterns.isEmpty()) {
            return;
        }

        // 데코레이터 데이터 준비
        List<ShiftDecorator> newDecorators = new ArrayList<>();
        final int patternSize = workPatterns.size();
        
        // 시작 날짜의 패턴 인덱스 미리 계산
        long startDaysBetween = ChronoUnit.DAYS.between(startDate, startLimit);
        final int startPatternIndex = (int) (startDaysBetween % patternSize);
        final int adjustedStartPatternIndex = startPatternIndex < 0 ? startPatternIndex + patternSize : startPatternIndex;

        // 날짜별 패턴 인덱스 계산 최적화
        for (LocalDate date = startLimit; !date.isAfter(endLimit); date = date.plusDays(1)) {
            final LocalDate currentDate = date; // 람다에서 사용할 final 변수
            ShiftDecorator decorator = decoratorCache.computeIfAbsent(currentDate, k -> {
                int patternIndex = (adjustedStartPatternIndex + (int)ChronoUnit.DAYS.between(startLimit, k)) % patternSize;
                String shift = workPatterns.get(patternIndex);
                int color = shiftColors.getOrDefault(shift, Color.GRAY);
                return new ShiftDecorator(k, shift, color);
            });
            newDecorators.add(decorator);
        }

        // UI 업데이트는 메인 스레드에서 실행
        if (isAdded() && !isDetached()) {
            requireActivity().runOnUiThread(() -> {
                try {
                    if (calendarView != null) {
                        // 기존 데코레이터 제거
                        for (DayViewDecorator decorator : decorators) {
                            calendarView.removeDecorator(decorator);
                        }
                        decorators.clear();

                        // 새로운 데코레이터 추가
                        for (ShiftDecorator decorator : newDecorators) {
                            calendarView.addDecorator(decorator);
                            decorators.add(decorator);
                        }

                        calendarView.invalidateDecorators();
                        long endTime = System.currentTimeMillis();
                        Log.d("CalendarFragment", "Update done in " + (endTime - startTime) + "ms");
                    }
                } catch (Exception e) {
                    Log.e("CalendarFragment", "Error updating UI", e);
                }
            });
        }
    }

    private void loadShiftTypes() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_SHIFT_TYPES, null);
        Log.d("KEY_SHIFT_TYPES:com.example.shiftcal", (json != null ? json : "null"));
        try {
            Type type = new TypeToken<List<ShiftType>>() {}.getType();
            shiftTypes = gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            sharedPreferences.edit().remove(KEY_SHIFT_TYPES).apply();
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
        sharedPreferences.edit()
                .putString(KEY_SHIFT_TYPES, new Gson().toJson(shiftTypes))
                .apply();
    }

    private void loadPatterns() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_PATTERNS, null);
        Log.d("KEY_PATTERNS:com.example.shiftcal", (json != null ? json : "null"));
        Type type = new TypeToken<List<String>>() {}.getType();
        workPatterns = gson.fromJson(json, type);

        if (workPatterns == null || workPatterns.isEmpty()) {
            workPatterns = new ArrayList<>();
            workPatterns.add("주간");
            workPatterns.add("야간");
            workPatterns.add("휴식");
            sharedPreferences.edit()
                    .putString(KEY_PATTERNS, gson.toJson(workPatterns))
                    .apply();
        }
        Log.d("CalendarFragment", "WorkPatterns loaded: " + workPatterns);
    }

    private void updateShiftColors() {
        shiftColors.clear();
        for (ShiftType shiftType : shiftTypes) {
            shiftColors.put(shiftType.getName(), shiftType.getColor());
        }
        Log.d("CalendarFragment", "ShiftColors updated: " + shiftColors);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // executorService 종료 전에 진행 중인 작업 완료 대기
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        calendarView = null;
        decoratorCache.clear(); // 캐시 정리
    }
}
