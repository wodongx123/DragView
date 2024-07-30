package com.wodongx123.dragviewtest.test

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wodongx123.dragview.AnimationConfig
import com.wodongx123.dragview.DragAlignConfig
import com.wodongx123.dragview.DragViewConfig
import com.wodongx123.dragview.databinding.ActivityDragViewBinding

class DragViewActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDragViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDragViewBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.dragContainer.post {
            val config = DragViewConfig()

            config.canOutOfEdge = true

            config.alignConfig = DragAlignConfig(alignToHorizonSide = true, alignToVerticalSide = true)
            config.alignConfig!!.animationConfig = AnimationConfig(timeMode = true, duration = 1000)

            binding.dragContainer.setConfig(config)
        }

        binding.btn.setOnClickListener {
            Toast.makeText(this, "click", Toast.LENGTH_SHORT).show()
        }
    }
}