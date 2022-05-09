package com.example.diashield;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.Spinner;

public class SymptomActivity extends MainActivity {

    SQLiteDatabase database;
    Spinner tempVar;
    RatingBar ratingBar;

    String[] databaseSymptoms = new String[] {"nausea", "headache", "diarrhea", "sorethroat", "fever", "muscleache", "lossofsmellortaste", "cough", "shortnessofbreath", "feelingtired"};

    @Override
    protected void onCreate(Bundle finalState) {
        super.onCreate(finalState);
        setContentView(R.layout.activity_symptom);
        String[] symptomList = new String[] {"Nausea", "Headache", "diarrhea", "Sore Throat", "Fever", "Muscle Ache", "Loss of Smell or Taste", "Cough", "Shortness of Breath", "Feeling tired"};
        tempVar = (Spinner) findViewById(R.id.spinnerSymptomList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, symptomList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tempVar.setAdapter(adapter);
        ratingBar = (RatingBar)findViewById(R.id.ratingBar);
    }

    public void onClickUploadSymptoms(View view) {
        try {
            String symData = databaseSymptoms[tempVar.getSelectedItemPosition()];
            float ratingOfSelectedSymptom = ratingBar.getRating();
            ratingBar.setRating(0.0f);
            database = openOrCreateDatabase("utkarshDatabase.db", Context.MODE_PRIVATE, null);
            database.beginTransaction();
            try {
                database.execSQL("" + "Update User_Database " + "Set "+symData+" = "+ratingOfSelectedSymptom+" " + " Where record_id in (select record_id from User_Database order by record_id desc LIMIT 1);");
                database.setTransactionSuccessful();
            }catch (SQLiteException e){
                e.printStackTrace();
            }finally {
                database.endTransaction();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}