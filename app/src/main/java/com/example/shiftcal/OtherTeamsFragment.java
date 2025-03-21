package com.example.shiftcal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OtherTeamsFragment extends Fragment {
    private TextView otherTeamsText;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "WorkPatternPrefs";
    private static final String KEY_OTHER_TEAMS = "other_teams";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other_teams, container, false);

        otherTeamsText = view.findViewById(R.id.other_teams_text);
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, requireContext().MODE_PRIVATE);

        List<String> otherTeams = loadOtherTeams();
        otherTeamsText.setText(otherTeams.toString());

        return view;
    }

    private List<String> loadOtherTeams() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_OTHER_TEAMS, null);
        System.out.println("KEY_OTHER_TEAMS JSON: " + json);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> otherTeams = gson.fromJson(json, type);
        if (otherTeams == null) {
            otherTeams = new ArrayList<>();
            otherTeams.add("팀 A: D-N-OFF");
            otherTeams.add("팀 B: N-OFF-D");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_OTHER_TEAMS, gson.toJson(otherTeams));
            editor.apply();
        }
        return otherTeams;
    }
}