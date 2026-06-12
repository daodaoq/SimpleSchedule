package com.example.simpleschedule

import android.app.Application
import com.example.simpleschedule.data.local.CourseStore
import com.example.simpleschedule.data.local.preferences.PreferencesManager
import com.example.simpleschedule.data.repository.CourseRepository

/**
 * Application 子类 — 手动依赖注入容器
 *
 * 轻量化 DI 方案：不引入 Hilt/Koin 等 DI 框架，
 * 在 Application.onCreate() 中初始化所有单例依赖。
 *
 * 架构：CourseStore（JSON 文件） + PreferencesManager（DataStore） → CourseRepository
 *
 * 使用方式：
 *   (context.applicationContext as SimpleScheduleApp).container.repository
 */
class SimpleScheduleApp : Application() {

    /** 依赖注入容器，在 onCreate 后保证非空 */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        /** 全局 Application 实例引用，方便在非 Context 环境中获取 */
        lateinit var instance: SimpleScheduleApp
            private set
    }
}

/**
 * 依赖注入容器
 *
 * 集中管理全局单例：
 * - CourseStore（JSON 课程数据持久化）
 * - PreferencesManager（DataStore 偏好设置）
 * - CourseRepository（对外统一入口）
 */
class AppContainer(context: Application) {

    /** JSON 文件课程存储 */
    private val courseStore = CourseStore(context)

    /** DataStore 偏好管理 */
    private val preferencesManager = PreferencesManager(context)

    /** 数据仓库——ViewModel 层的唯一数据入口 */
    val repository: CourseRepository = CourseRepository(
        courseStore = courseStore,
        prefs = preferencesManager
    )
}
