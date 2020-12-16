package com.memory.monitor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

/**
 * 模拟内存泄露，用到的类。
 */
class TestLeakActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_leak)
        ActivityManage.get().add(this)
        findViewById<Button>(R.id.btnClose).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //ActivityManage.get().remove(this)
    }
}