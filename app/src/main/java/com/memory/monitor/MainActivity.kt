package com.memory.monitor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val monitorMemory=MonitorMemory()
        monitorMemory.initRegisterActivityLifecycleCallbacks(this.application)
        findViewById<View>(R.id.TestLeakMemory).setOnClickListener {
            startActivity(Intent(this,TestLeakActivity::class.java))
        }
    }
}