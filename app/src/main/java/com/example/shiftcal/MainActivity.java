package com.example.shiftcal;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar 설정
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // NavController 설정
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // BottomNavigationView와 NavController 연결
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // 디버깅 로그 추가
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            Log.d("MainActivity", "Destination changed: " + destination.getLabel());
        });

        // ActionBar와 NavController 연결
        NavigationUI.setupActionBarWithNavController(this, navController);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = NavHostFragment.findNavController(
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment));
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}

//
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import com.jakewharton.threetenabp.AndroidThreeTen;
//import com.prolificinteractive.materialcalendarview.CalendarDay;
//import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
//import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
//import org.threeten.bp.Instant;
//import org.threeten.bp.LocalDate;
//import org.threeten.bp.ZoneId;
//import org.threeten.bp.ZonedDateTime;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MainActivity extends AppCompatActivity {
//    private MaterialCalendarView calendarView;
//    private TextView shiftText;
//    private TextView shiftTimeText;
//    private Button savePatternButton;
//    private Button setAlarmButton;
//    private Button editPatternButton;
//    private FloatingActionButton fabSettings;
//    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//    private ShiftDatabaseHelper dbHelper;
//    private List<ShiftType> shiftTypes = new ArrayList<>();
//    private Map<String, String> shiftTimes = new HashMap<>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        AndroidThreeTen.init(this);
//        setContentView(R.layout.activity_main);
//
//        // Initialize views
//        calendarView = findViewById(R.id.calendarView);
//        shiftText = findViewById(R.id.shiftText);
//        shiftTimeText = findViewById(R.id.shiftTimeText);
//        savePatternButton = findViewById(R.id.savePatternButton);
//        setAlarmButton = findViewById(R.id.setAlarmButton);
//        editPatternButton = findViewById(R.id.editPatternButton);
//        //fabSettings = findViewById(R.id.fabSettings);
//        dbHelper = new ShiftDatabaseHelper(this);
//
//        // Initialize default shift types
//        initializeShiftTypes();
//
//        // Set listeners
//        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
//            @Override
//            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
//                showShiftDialog(date);
//            }
//        });
//
//        calendarView.setTileSizeDp(60 );
//
//        savePatternButton.setOnClickListener(v -> saveCurrentPattern());
//        setAlarmButton.setOnClickListener(v -> setShiftAlarm());
//        editPatternButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ShiftPatternActivity.class)));
//        fabSettings.setOnClickListener(v -> {
//            AddShiftDialog dialog = new AddShiftDialog(MainActivity.this, newShift -> {
//                shiftTypes.add(newShift);
//                calendarView.removeDecorators();
//                decorateCalendar();
//                Toast.makeText(MainActivity.this, newShift.getName() + " 추가됨", Toast.LENGTH_SHORT).show();
//            });
//            dialog.show();
//        });
//
//        decorateCalendar();
//    }
//
//    private void initializeShiftTypes() {
//        shiftTypes.add(new ShiftType("주간", android.graphics.Color.BLUE));
//        shiftTypes.add(new ShiftType("야간", android.graphics.Color.RED));
//        shiftTypes.add(new ShiftType("휴무", android.graphics.Color.GREEN));
//        shiftTimes.put("주간", "08:00-16:00");
//        shiftTimes.put("야간", "16:00-00:00");
//        shiftTimes.put("휴무", "없음");
//    }
//
//    private void decorateCalendar() {
//        calendarView.removeDecorators();
//        Calendar baseDate = Calendar.getInstance();
//        baseDate.set(2025, Calendar.JANUARY, 1);
//        Calendar current = Calendar.getInstance();
//        current.setTime(new Date());
//        current.add(Calendar.MONTH, -1);
//        Calendar end = Calendar.getInstance();
//        end.add(Calendar.MONTH, 1);
//
//        while (current.before(end)) {
//            String dateStr = dateFormat.format(current.getTime());
//            String shift = dbHelper.getShift(dateStr);
//
//            if (shift == null) {
//                long diffMillis = current.getTimeInMillis() - baseDate.getTimeInMillis();
//                int daysDiff = (int) (diffMillis / (1000 * 60 * 60 * 24));
//                int patternIndex = Math.abs(daysDiff) % shiftTypes.size();
//                shift = shiftTypes.get(patternIndex).getName();
//                dbHelper.saveShift(dateStr, shift);
//            }
//
//            Instant instant = Instant.ofEpochMilli(current.getTimeInMillis());
//            LocalDate localDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
//            CalendarDay day = CalendarDay.from(localDate);
//            ShiftType shiftType = getShiftTypeByName(shift);
//            calendarView.addDecorator(new ShiftDecorator(day, shift, shiftType.getColor()));
//            current.add(Calendar.DATE, 1);
//        }
//    }
//
//    private void showShiftDialog(CalendarDay date) {
//        String[] shiftNames = new String[shiftTypes.size()];
//        for (int i = 0; i < shiftTypes.size(); i++) {
//            shiftNames[i] = shiftTypes.get(i).getName();
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("근무 선택 - " + dateFormat.format(date.getDate()))
//                .setItems(shiftNames, (dialog, which) -> {
//                    String selectedShift = shiftNames[which];
//                    String dateStr = dateFormat.format(date.getDate());
//                    dbHelper.saveShift(dateStr, selectedShift);
//                    calendarView.removeDecorators();
//                    decorateCalendar();
//                    showShiftForDate(date.getDate());
//                })
//                .setNegativeButton("취소", null)
//                .show();
//    }
//
//    private void showShiftForDate(LocalDate date) {
//        Instant instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
//        String dateStr = dateFormat.format(Date.from(toJavaTimeInstant(instant)));
//        String shift = dbHelper.getShift(dateStr);
//        if (shift == null) shift = "미정";
//        String time = shiftTimes.getOrDefault(shift, "없음");
//        shiftText.setText(dateStr + " 근무: " + shift);
//        shiftTimeText.setText("시간: " + time);
//    }
//
//    private void saveCurrentPattern() {
//        Toast.makeText(this, "패턴 저장 완료", Toast.LENGTH_SHORT).show();
//    }
//
//    private void setShiftAlarm() {
//        CalendarDay selectedDate = calendarView.getSelectedDate();
//        if (selectedDate == null) {
//            Toast.makeText(this, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        Calendar alarmTime = Calendar.getInstance();
//        LocalDate localDate = selectedDate.getDate();
//        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
//        alarmTime.setTime(Date.from(toJavaTimeInstant(instant)));
//        alarmTime.add(Calendar.DATE, -1); // 하루 전
//        alarmTime.set(Calendar.HOUR_OF_DAY, 18); // 오후 6시
//        alarmTime.set(Calendar.MINUTE, 0);
//        alarmTime.set(Calendar.SECOND, 0);
//
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(this, AlarmReceiver.class);
//        intent.putExtra("date", dateFormat.format(selectedDate.getDate()));
//        intent.putExtra("shift", dbHelper.getShift(dateFormat.format(selectedDate.getDate())));
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
//        Toast.makeText(this, "알림 설정 완료: " + dateFormat.format(alarmTime.getTime()), Toast.LENGTH_SHORT).show();
//    }
//
//    private ShiftType getShiftTypeByName(String name) {
//        for (ShiftType type : shiftTypes) {
//            if (type.getName().equals(name)) return type;
//        }
//        return new ShiftType("미정", android.graphics.Color.GRAY);
//    }
//
//    // org.threeten.bp.Instant를 java.time.Instant로 변환하는 헬퍼 메서드
//    private java.time.Instant toJavaTimeInstant(org.threeten.bp.Instant threeTenInstant) {
//        return java.time.Instant.ofEpochMilli(threeTenInstant.toEpochMilli());
//    }
//}
//
//
////package com.example.shiftcal;
////
////import android.app.AlarmManager;
////import android.app.PendingIntent;
////import android.content.Context;
////import android.content.Intent;
////import android.os.Bundle;
////import android.view.View;
////import android.widget.Button;
////import android.widget.TextView;
////import android.widget.Toast;
////import androidx.appcompat.app.AlertDialog;
////import androidx.appcompat.app.AppCompatActivity;
////import com.google.android.material.floatingactionbutton.FloatingActionButton;
////import com.jakewharton.threetenabp.AndroidThreeTen;
////import com.prolificinteractive.materialcalendarview.CalendarDay;
////import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
////import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
////import org.threeten.bp.LocalDate;
////import org.threeten.bp.ZoneId;
////
////import java.text.SimpleDateFormat;
////import java.util.ArrayList;
////import java.util.Calendar;
////import java.util.Date;
////import java.util.HashMap;
////import java.util.List;
////import java.util.Map;
////
////public class MainActivity extends AppCompatActivity {
////    private MaterialCalendarView calendarView;
////    private TextView shiftText;
////    private TextView shiftTimeText;
////    private Button savePatternButton;
////    private Button setAlarmButton;
////    private Button editPatternButton;
////    private FloatingActionButton fabSettings;
////    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
////    private ShiftDatabaseHelper dbHelper;
////    private List<ShiftType> shiftTypes = new ArrayList<>(); // 사용자 정의 근무 유형
////    private Map<String, String> shiftTimes = new HashMap<>(); // 근무 시간 맵
////
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        AndroidThreeTen.init(this);
////        setContentView(R.layout.activity_main_upgraded);
////
////        // Initialize views
////        calendarView = findViewById(R.id.calendarView);
////        shiftText = findViewById(R.id.shiftText);
////        shiftTimeText = findViewById(R.id.shiftTimeText);
////        savePatternButton = findViewById(R.id.savePatternButton);
////        setAlarmButton = findViewById(R.id.setAlarmButton);
////        editPatternButton = findViewById(R.id.editPatternButton);
////        fabSettings = findViewById(R.id.fabSettings);
////        dbHelper = new ShiftDatabaseHelper(this);
////
////        // Initialize default shift types
////        initializeShiftTypes();
////
////        // MainActivity.java 내 onCreate 메서드에서
////        fabSettings.setOnClickListener(v -> {
////            AddShiftDialog dialog = new AddShiftDialog(MainActivity.this, newShift -> {
////                shiftTypes.add(newShift); // 새로운 근무 유형 추가
////                calendarView.removeDecorators();
////                decorateCalendar(); // 캘린더 갱신
////                Toast.makeText(MainActivity.this, newShift.getName() + " 추가됨", Toast.LENGTH_SHORT).show();
////            });
////            dialog.show();
////        });
////
////        // Set listeners
////        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
////            @Override
////            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
////                showShiftDialog(date);
////            }
////        });
////
////        savePatternButton.setOnClickListener(v -> saveCurrentPattern());
////        setAlarmButton.setOnClickListener(v -> setShiftAlarm());
////        editPatternButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ShiftPatternActivity.class)));
////        fabSettings.setOnClickListener(v -> Toast.makeText(MainActivity.this, "설정 기능 준비 중", Toast.LENGTH_SHORT).show());
////
////        decorateCalendar();
////    }
////
////    private void initializeShiftTypes() {
////        shiftTypes.add(new ShiftType("주간", android.graphics.Color.BLUE));
////        shiftTypes.add(new ShiftType("야간", android.graphics.Color.RED));
////        shiftTypes.add(new ShiftType("휴무", android.graphics.Color.GREEN));
////        shiftTimes.put("주간", "08:00-16:00");
////        shiftTimes.put("야간", "16:00-00:00");
////        shiftTimes.put("휴무", "없음");
////    }
////
////    private void decorateCalendar() {
////        calendarView.removeDecorators();
////        Calendar baseDate = Calendar.getInstance();
////        baseDate.set(2025, Calendar.JANUARY, 1);
////        Calendar current = Calendar.getInstance();
////        current.setTime(new Date());
////        current.add(Calendar.MONTH, -1);
////        Calendar end = Calendar.getInstance();
////        end.add(Calendar.MONTH, 1);
////
////        while (current.before(end)) {
////            String dateStr = dateFormat.format(current.getTime());
////            String shift = dbHelper.getShift(dateStr);
////
////            if (shift == null) {
////                long diffMillis = current.getTimeInMillis() - baseDate.getTimeInMillis();
////                int daysDiff = (int) (diffMillis / (1000 * 60 * 60 * 24));
////                int patternIndex = Math.abs(daysDiff) % shiftTypes.size();
////                shift = shiftTypes.get(patternIndex).getName();
////                dbHelper.saveShift(dateStr, shift);
////            }
////
////            LocalDate localDate = current.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
////            CalendarDay day = CalendarDay.from(localDate);
////            ShiftType shiftType = getShiftTypeByName(shift);
////            calendarView.addDecorator(new ShiftDecorator(day, shift, shiftType.getColor()));
////            current.add(Calendar.DATE, 1);
////        }
////    }
////
////    private void showShiftDialog(CalendarDay date) {
////        String[] shiftNames = new String[shiftTypes.size()];
////        for (int i = 0; i < shiftTypes.size(); i++) {
////            shiftNames[i] = shiftTypes.get(i).getName();
////        }
////
////        AlertDialog.Builder builder = new AlertDialog.Builder(this);
////        builder.setTitle("근무 선택 - " + dateFormat.format(date.getDate()))
////                .setItems(shiftNames, (dialog, which) -> {
////                    String selectedShift = shiftNames[which];
////                    String dateStr = dateFormat.format(date.getDate());
////                    dbHelper.saveShift(dateStr, selectedShift);
////                    calendarView.removeDecorators();
////                    decorateCalendar();
////                    showShiftForDate(date.getDate());
////                })
////                .setNegativeButton("취소", null)
////                .show();
////    }
////
////    private void showShiftForDate(LocalDate date) {
////        org.threeten.bp.Instant threeTenInstant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
////        java.time.Instant javaTimeInstant = java.time.Instant.ofEpochMilli(threeTenInstant.toEpochMilli());
////        String dateStr = dateFormat.format(Date.from(javaTimeInstant));
////        String shift = dbHelper.getShift(dateStr);
////        if (shift == null) shift = "미정";
////        String time = shiftTimes.getOrDefault(shift, "없음");
////        shiftText.setText(dateStr + " 근무: " + shift);
////        shiftTimeText.setText("시간: " + time);
////    }
////
////    private void saveCurrentPattern() {
////        Toast.makeText(this, "패턴 저장 완료", Toast.LENGTH_SHORT).show();
////        // SharedPreferences에 저장 로직 추가 가능
////    }
////
////    private void setShiftAlarm() {
////        CalendarDay selectedDate = calendarView.getSelectedDate();
////        if (selectedDate == null) {
////            Toast.makeText(this, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
////            return;
////        }
////
////        Calendar alarmTime = Calendar.getInstance();
////        alarmTime.setTime(selectedDate.getDate());
////        alarmTime.add(Calendar.DATE, -1); // 하루 전
////        alarmTime.set(Calendar.HOUR_OF_DAY, 18); // 오후 6시
////        alarmTime.set(Calendar.MINUTE, 0);
////        alarmTime.set(Calendar.SECOND, 0);
////
////        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
////        Intent intent = new Intent(this, AlarmReceiver.class);
////        intent.putExtra("date", dateFormat.format(selectedDate.getDate()));
////        intent.putExtra("shift", dbHelper.getShift(dateFormat.format(selectedDate.getDate())));
////        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
////
////        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
////        Toast.makeText(this, "알림 설정 완료: " + dateFormat.format(alarmTime.getTime()), Toast.LENGTH_SHORT).show();
////    }
////
////    private ShiftType getShiftTypeByName(String name) {
////        for (ShiftType type : shiftTypes) {
////            if (type.getName().equals(name)) return type;
////        }
////        return new ShiftType("미정", android.graphics.Color.GRAY);
////    }
////}
////
//////package com.example.shiftcal;
//////
//////import android.os.Bundle;
//////import android.widget.Button;
//////import android.widget.TextView;
//////import androidx.appcompat.app.AppCompatActivity;
//////import com.jakewharton.threetenabp.AndroidThreeTen;
//////import com.prolificinteractive.materialcalendarview.CalendarDay;
//////import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
//////import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
//////import org.threeten.bp.LocalDate;
//////import org.threeten.bp.ZoneId;
//////
//////import java.text.SimpleDateFormat;
//////import java.util.ArrayList;
//////import java.util.Calendar;
//////import java.util.Date;
//////import java.util.List;
//////
//////public class MainActivity extends AppCompatActivity {
//////    private MaterialCalendarView calendarView;
//////    private TextView shiftText;
//////    private TextView shiftTimeText;
//////    private Button savePatternButton;
//////    private Button setAlarmButton;
//////    private Button editPatternButton;
//////    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//////    private ShiftDatabaseHelper dbHelper;
//////    private List<String> shiftPattern = new ArrayList<>();
//////
//////    @Override
//////    protected void onCreate(Bundle savedInstanceState) {
//////        super.onCreate(savedInstanceState);
//////        AndroidThreeTen.init(this);
//////        setContentView(R.layout.activity_main);
//////
//////        // Initialize views
//////        calendarView = findViewById(R.id.calendarView);
//////        shiftText = findViewById(R.id.shiftText);
//////        shiftTimeText = findViewById(R.id.shiftTimeText);
//////        savePatternButton = findViewById(R.id.savePatternButton);
//////        setAlarmButton = findViewById(R.id.setAlarmButton);
//////        editPatternButton = findViewById(R.id.editPatternButton);
//////        dbHelper = new ShiftDatabaseHelper(this);
//////
//////        // Initialize shift pattern
//////        shiftPattern.add("주간");
//////        shiftPattern.add("야간");
//////        shiftPattern.add("휴무");
//////
//////        // Set listener for date selection
//////        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
//////            @Override
//////            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
//////                showShiftForDate(date.getDate());
//////            }
//////        });
//////
//////        decorateCalendar();
//////    }
//////
//////    private void decorateCalendar() {
//////        calendarView.removeDecorators();
//////        Calendar baseDate = Calendar.getInstance();
//////        baseDate.set(2025, Calendar.JANUARY, 1); // 0-based month
//////        Calendar current = Calendar.getInstance();
//////        current.setTime(new Date());
//////        current.add(Calendar.MONTH, -1);
//////        Calendar end = Calendar.getInstance();
//////        end.add(Calendar.MONTH, 1);
//////
//////        while (current.before(end)) {
//////            String dateStr = dateFormat.format(current.getTime());
//////            String shift = dbHelper.getShift(dateStr);
//////
//////            if (shift == null) {
//////                long diffMillis = current.getTimeInMillis() - baseDate.getTimeInMillis();
//////                int daysDiff = (int) (diffMillis / (1000 * 60 * 60 * 24));
//////                int patternIndex = (int) (Math.abs((double) daysDiff) % shiftPattern.size());
//////                shift = shiftPattern.get(patternIndex);
//////                dbHelper.saveShift(dateStr, shift);
//////            }
//////
//////            LocalDate localDate = current.toInstant()
//////                    .atZone(ZoneId.systemDefault())
//////                    .toLocalDate();
//////            CalendarDay day = CalendarDay.from(localDate);
//////            calendarView.addDecorator(new ShiftDecorator(day, shift != null ? shift : "미정"));
//////            current.add(Calendar.DATE, 1);
//////        }
//////    }
//////
//////    private void showShiftForDate(LocalDate date) {
//////        org.threeten.bp.Instant threeTenInstant = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
//////        java.time.Instant javaTimeInstant = java.time.Instant.ofEpochMilli(threeTenInstant.toEpochMilli());
//////        String dateStr = dateFormat.format(Date.from(javaTimeInstant));
//////        String shift = dbHelper.getShift(dateStr);
//////        if (shift == null) shift = "미정";
//////        shiftText.setText(dateStr + " 근무: " + shift);
//////    }
//////}