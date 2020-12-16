package com.memory.monitor;
import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟内存泄露，用到的类。
 */
public class ActivityManage {
    private static ActivityManage activityManage;
    private static List<Activity> list = new ArrayList<>();
    private ActivityManage() {
    }
    public static ActivityManage get() {
        if (activityManage == null) {
            activityManage = new ActivityManage();
        }
        return activityManage;
    }

    public void add(Activity activity) {
        list.add(activity);
    }
    public void remove(Activity activity){
        list.remove(activity);
    }
    public int getSize() {
        return list.size();
    }
    public List<Activity> getList() {
        return list;
    }
}
