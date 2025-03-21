package com.example.shiftcal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class AlarmSettingsFragment extends Fragment {
    private TimePicker timePicker;
    private Button saveAlarmButton;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_ALARM_TIME = "alarm_time";
    private SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_settings, container, false);

        timePicker = view.findViewById(R.id.time_picker);
        saveAlarmButton = view.findViewById(R.id.save_alarm_button);
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        loadAlarmTime();

        saveAlarmButton.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            saveAlarmTime(hour, minute);
            Toast.makeText(getContext(), "알람이 설정되었습니다: " + hour + ":" + minute, Toast.LENGTH_SHORT).show();
            sharedViewModel.notifyAlarmUpdated(); // 캘린더 갱신 알림
            // Navigation으로 이동하지 않음. 사용자가 탭으로 이동하도록 유도
        });

        return view;
    }

    private void saveAlarmTime(int hour, int minute) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_ALARM_TIME + "_hour", hour);
        editor.putInt(KEY_ALARM_TIME + "_minute", minute);
        editor.apply();
    }

    private void loadAlarmTime() {
        int hour = sharedPreferences.getInt(KEY_ALARM_TIME + "_hour", 8);
        int minute = sharedPreferences.getInt(KEY_ALARM_TIME + "_minute", 0);
        timePicker.setHour(hour);
        timePicker.setMinute(minute);
    }
}