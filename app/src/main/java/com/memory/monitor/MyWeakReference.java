package com.memory.monitor;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;



/**
 * @author myoffer
 */
public class MyWeakReference extends WeakReference<Object> {
    public final String key;
    public final String name;

    public MyWeakReference(String key,Object referent, ReferenceQueue<Object> referenceQueue) {
        super(referent, referenceQueue);
        this.key = key;
        this.name = "类名："+referent.getClass().getName();
    }


}
