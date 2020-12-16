package com.memory.monitor

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import leakcanary.LeakCanary


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //这里几行一般是写在Application里面的，简单起见，你懂的。
        //接入LeakCanary，但禁用，因为要使用到里面一些类。
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = false)
        val monitorMemory=MonitorMemory()
        monitorMemory.initRegisterActivityLifecycleCallbacks(this.application)


        findViewById<View>(R.id.TestLeakMemory).setOnClickListener {
            startActivity(Intent(this,TestLeakActivity::class.java))
        }
    }

}