package com.example.simpleschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.simpleschedule.ui.screens.schedule.ScheduleScreen
import com.example.simpleschedule.ui.theme.SimpleScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleScheduleTheme {
                ScheduleScreen()
            }
        }
    }
}
