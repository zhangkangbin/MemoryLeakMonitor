package com.memory.monitor
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import leakcanary.KeyedWeakReference
import shark.*
import java.io.File
import java.io.IOException
import java.lang.ref.ReferenceQueue
import java.util.*

/**
 * 核心代码
 */
class MonitorMemory {
    private val TAG = "MonitorMemory"
    private val queue = ReferenceQueue<Any>()
    private val retainedKeys = mutableMapOf<String, KeyedWeakReference>()
    private fun watchActivity(application: Application, weakReference: KeyedWeakReference) {
        removeWeaklyReachableObjects()
        //运行GC
        runGC()
        removeWeaklyReachableObjects()
        if (retainedKeys.contains(weakReference.key)) {
            //activity 泄漏
            Log.d(TAG, "-----泄漏activity leak----$weakReference.description::::: Activity${weakReference.get()}" )
            val storageDirectory = File(application.cacheDir.toString() + "/watchActivity")
            if (!storageDirectory.exists()) {
                storageDirectory.mkdir()
            }
            val heapDumpFile = File(storageDirectory, UUID.randomUUID().toString() + ".hprof")
            try {
                Debug.dumpHprofData(heapDumpFile.absolutePath) //dump 一份内存快照，这里比较慢。
                val heapAnalyzer = HeapAnalyzer(OnAnalysisProgressListener.NO_OP)
                val analysis = heapAnalyzer.analyze(
                    heapDumpFile = heapDumpFile,
                    leakingObjectFinder = KeyedWeakReferenceFinder,
                    referenceMatchers = AndroidReferenceMatchers.appDefaults,
                    computeRetainedHeapSize = true,
                    objectInspectors = AndroidObjectInspectors.appDefaults,
                    metadataExtractor = AndroidMetadataExtractor
                )
                Log.d(TAG, "\u200B\n分析结果:\u200B\n$analysis");
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun enqueueReferences() {
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
        }
    }

    private fun removeWeaklyReachableObjects() {
        var ref: KeyedWeakReference?
        do {
            ref = queue.poll() as KeyedWeakReference?
            if (ref != null) {
                retainedKeys.remove(ref.key)
            }
        } while (ref != null)
    }

    private fun runGC() {
        Runtime.getRuntime().gc()
        enqueueReferences()
        System.runFinalization()
    }

    fun initRegisterActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                Log.d(TAG, "Destroyed Activity:$activity.javaClass.name" )
                val key = UUID.randomUUID().toString()
                val weakReference = KeyedWeakReference(
                    activity,
                    key,
                    "描述:" + activity.localClassName,
                    SystemClock.uptimeMillis(),
                    queue
                )
                retainedKeys[key]=weakReference
                //五秒后去观察，让 gc 飞一会, 这块应该用线程池，简单起见，就没写了。
                Handler().postDelayed(
                    { watchActivity(application, weakReference) },
                    5000
                )
            }
        })
    }
}