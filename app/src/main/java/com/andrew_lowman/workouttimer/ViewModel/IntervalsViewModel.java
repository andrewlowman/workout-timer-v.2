package com.andrew_lowman.workouttimer.ViewModel;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.andrew_lowman.workouttimer.Database.Repository;
import com.andrew_lowman.workouttimer.Entities.IntervalsEntity;

import java.util.List;

public class IntervalsViewModel extends AndroidViewModel {
    private Repository mRepository;
    private LiveData<List<IntervalsEntity>> mAllIntervals;

    public IntervalsViewModel(@NonNull Application application) {
        super(application);
        mRepository = new Repository(application);
        mAllIntervals = mRepository.getAllIntervals();
    }

    public LiveData<List<IntervalsEntity>> getAllIntervals(){
        return mAllIntervals;
    }
    public void insert(IntervalsEntity intervalsEntity){
        mRepository.insert(intervalsEntity);
    }
    public void delete(int intervalID){
        mRepository.delete(intervalID);
    }
}
