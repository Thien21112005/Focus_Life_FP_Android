package com.hcmute.edu.vn.focus_life.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.hcmute.edu.vn.focus_life.core.utils.Constants;
import com.hcmute.edu.vn.focus_life.data.local.dao.DailySummaryDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.HabitDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.NutritionDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.PomodoroDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.ProfileDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.StepDao;
import com.hcmute.edu.vn.focus_life.data.local.dao.WaterDao;
import com.hcmute.edu.vn.focus_life.data.local.entity.DailySummaryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.HabitCheckinEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.HabitEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.NutritionEntryEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.PomodoroSessionEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.ProfileEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.StepRecordEntity;
import com.hcmute.edu.vn.focus_life.data.local.entity.WaterEntryEntity;

@Database(
        entities = {
                StepRecordEntity.class,
                PomodoroSessionEntity.class,
                HabitEntity.class,
                HabitCheckinEntity.class,
                NutritionEntryEntity.class,
                WaterEntryEntity.class,
                DailySummaryEntity.class,
                ProfileEntity.class
        },
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract StepDao stepDao();
    public abstract PomodoroDao pomodoroDao();
    public abstract HabitDao habitDao();
    public abstract NutritionDao nutritionDao();
    public abstract WaterDao waterDao();
    public abstract DailySummaryDao dailySummaryDao();
    public abstract ProfileDao profileDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            Constants.DB_NAME
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
