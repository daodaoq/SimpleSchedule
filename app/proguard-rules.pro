# ===================== PDFBox ProGuard 规则 =====================
# JP2 (JPEG2000) 解码器在 PDFBox Android 移植版中不可用，忽略缺失警告
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.filter.JPXFilter
# 保留 PDFBox 核心类
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.pdfbox.**
