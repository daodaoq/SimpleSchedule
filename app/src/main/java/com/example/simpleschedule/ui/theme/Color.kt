package com.example.simpleschedule.ui.theme

import androidx.compose.ui.graphics.Color

// ===================== 莫兰迪低饱和度色系 =====================
// 设计理念：
// - 低饱和度、低明度对比，视觉柔和护眼，适合长时间查看课表
// - 12 种颜色足够区分不同课程，信息层级清晰但不过度刺激
// - 深色模式与浅色模式切换时，卡片颜色保持不变（仅调整背景/文字）
// - 每个颜色具有良好可读性：白色文字在浅色系上难以阅读，
//   故卡片内使用深色文字（Color(0xFF333333)）

/** 莫兰迪灰粉 — 温柔内敛 */
val MorandiPink = Color(0xFFC4A9A2)

/** 莫兰迪鼠尾草绿 — 自然安静（默认课程颜色） */
val MorandiSage = Color(0xFFA3B5A6)

/** 莫兰迪雾蓝 — 沉稳冷静 */
val MorandiBlue = Color(0xFF8FA7B5)

/** 莫兰迪暖灰 — 中性百搭 */
val MorandiWarmGray = Color(0xFFB8B0A6)

/** 莫兰迪薰衣草 — 优雅淡紫 */
val MorandiLavender = Color(0xFFB5A8C4)

/** 莫兰迪赭石 — 温润土色 */
val MorandiOchre = Color(0xFFC4B08A)

/** 莫兰迪灰绿 — 知性清冷 */
val MorandiTeal = Color(0xFF8CADA2)

/** 莫兰迪灰粉玫瑰 — 低调浪漫 */
val MorandiRose = Color(0xFFC49DA6)

/** 莫兰迪岩板灰 — 坚实稳重 */
val MorandiSlate = Color(0xFF9EA8B0)

/** 莫兰迪橄榄 — 素雅淡定 */
val MorandiOlive = Color(0xFFB0B58A)

/** 莫兰迪淡紫灰 — 朦胧高级 */
val MorandiMauve = Color(0xFFBBA0B5)

/** 莫兰迪雾天蓝 — 宁静致远 */
val MorandiSky = Color(0xFFA4B5C2)

/**
 * 莫兰迪 12 色完整色板
 * 索引 0~11，ColorPicker 组件按 4 列 × 3 行网格展示
 */
val MorandiColorPalette = listOf(
    MorandiPink,
    MorandiSage,
    MorandiBlue,
    MorandiWarmGray,
    MorandiLavender,
    MorandiOchre,
    MorandiTeal,
    MorandiRose,
    MorandiSlate,
    MorandiOlive,
    MorandiMauve,
    MorandiSky
)

// ===================== 主题色定义 =====================

/** 浅色主题主色：莫兰迪雾蓝，干净典雅 */
val PrimaryLight = Color(0xFF6B8A9A)

/** 浅色主题表面色 */
val SurfaceLight = Color(0xFFFBFCFA)

/** 浅色主题背景色：偏暖白，比纯白柔和不刺眼 */
val BackgroundLight = Color(0xFFF5F3F0)

/** 深色主题主色 */
val PrimaryDark = Color(0xFFA4BBC8)

/** 深色主题表面色 */
val SurfaceDark = Color(0xFF1E1E1E)

/** 深色主题背景色 */
val BackgroundDark = Color(0xFF151515)
