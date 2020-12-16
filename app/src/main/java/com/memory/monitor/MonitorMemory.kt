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
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 核心代码
 */
class MonitorMemory {
    private val queue = ReferenceQueue<Any>()
    private val retainedKeys: MutableSet<String> = CopyOnWriteArraySet()

    private fun watchActivity(application: Application, weakReference: KeyedWeakReference) {
        removeWeaklyReachableObjects()
        //运行GC
        runGC()
        removeWeaklyReachableObjects()
        if (retainedKeys.contains(weakReference.key)) {
            //activity 泄漏
            Log.d(TAG, "-----activity leak----" + weakReference.description)
            Log.d(TAG, "MyWeakReference Activity:  --" + weakReference.get())
            Log.d(TAG, "retainedKeys size:" + retainedKeys.size)
            val storageDirectory = File(application.cacheDir.toString() + "/watchActivity")
            if (!storageDirectory.exists()) {
                storageDirectory.mkdir()
            }
            val heapDumpFile =
                File(storageDirectory, UUID.randomUUID().toString() + ".hprof")
            try {
                //dump 一份内存快照，这里比较慢。
                Debug.dumpHprofData(heapDumpFile.absolutePath)
                Log.d(TAG, "-----heapDumpFile size----" + heapDumpFile.totalSpace)
                val heapAnalyzer = HeapAnalyzer(OnAnalysisProgressListener.NO_OP)

                val leakingObjectFilter = object :
                    FilteringLeakingObjectFinder.LeakingObjectFilter {
                    override fun isLeakingObject(heapObject: HeapObject): Boolean {

                        if(heapObject is HeapObject.HeapInstance){
                            return when {
                                heapObject instanceOf "com.memory.monitor.MyWeakReference" -> {
                                    Log.d(TAG, "-----heapObject1 查找到泄漏对象 ----" + heapObject.toString())
                                    //   val instance = heapObject as HeapObject.HeapInstance
                                    //  val destroyedField = instance["com.memory.monitor.MyWeakReference", "destroyed"]
                                    //  return destroyedField?.value?.asBoolean!!
                                    true
                                }
                                heapObject instanceOf "leakcanary.KeyedWeakReference" -> {
                                    Log.d(TAG, "-----heapObject2 ----" + heapObject.toString())
                                    true
                                }
                                heapObject instanceOf "com.squareup.leakcanary.KeyedWeakReference" -> {
                                    Log.d(TAG, "-----heapObject3 ----" + heapObject.toString())
                                    true
                                }
                                else -> {
                                    false
                                }
                            }
                        }

              /*
*/
                    //   val keyedWeakReferenceClass=  heapObject.graph.findClassByName("com.memory.monitor.MyWeakReference");

                        return false
                    }
                }

                val leakingObjectFinder = FilteringLeakingObjectFinder(listOf(leakingObjectFilter))
                val keyedWeakReferenceFinder=KeyedWeakReferenceFinder
                val analysis = heapAnalyzer.analyze(
                    heapDumpFile = heapDumpFile,
                    leakingObjectFinder = keyedWeakReferenceFinder,
                    referenceMatchers = AndroidReferenceMatchers.appDefaults,
                    computeRetainedHeapSize = true,
                    objectInspectors =  AndroidObjectInspectors.appDefaults,
                    metadataExtractor = AndroidMetadataExtractor
                )
                Log.d(TAG, "analysis:" + analysis.toString());

                // Log.d(TAG, "-----heapDumpFile size----" + heapDumpFile.getTotalSpace());
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Log.d(
            TAG,
            "Activity size:" + ActivityManage.get().size
        )
    }

    private fun enqueueReferences() {
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            throw AssertionError()
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
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                Log.d(TAG, "Destroyed Activity:" + activity.javaClass.name)
                val key = UUID.randomUUID().toString()
                retainedKeys.add(key)
                val weakReference = KeyedWeakReference(activity,key,"描述:"+activity.localClassName ,SystemClock.uptimeMillis(), queue)
                //Log.d(TAG, "MyWeakReference Activity1:    --" + weakReference.get());
                //五秒后去观察，让 gc 飞一会
                Handler().postDelayed(
                    { watchActivity(application, weakReference) },
                    5000
                )
            }
        })
    }

    companion object {
        private const val TAG = "MonitorMemory"
    }

}