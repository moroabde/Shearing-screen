package com.example.model

data class SocialPostSample(
    val id: String,
    val platform: String,
    val author: String,
    val handle: String,
    val content: String,
    val engagement: String,
    val tags: List<String>
)

object SampleSocialPosts {
    val samples = listOf(
        SocialPostSample(
            id = "tech_launch",
            platform = "X / Twitter",
            author = "Alex Rivera",
            handle = "@arivera_dev",
            content = "Just open-sourced our real-time AI local inference engine for Android & Kotlin. Zero cloud latency, sub-50ms token generation, and fully private on-device processing. What do you prioritize most in mobile AI apps: battery efficiency, speed, or accuracy?",
            engagement = "1.2k Likes • 340 Reposts",
            tags = listOf("AndroidDev", "AI", "OpenSource")
        ),
        SocialPostSample(
            id = "linkedin_career",
            platform = "LinkedIn",
            author = "Sarah Chen, VP Product",
            handle = "sarahchen-product",
            content = "Hot take: The best product managers don't write 50-page specs. They build rapid prototypes, talk directly with 5 angry users every week, and empower engineering to make architectural decisions. Simplicity always beats bureaucratic process. Thoughts?",
            engagement = "890 Reactions • 145 Comments",
            tags = listOf("ProductManagement", "Leadership", "Startups")
        ),
        SocialPostSample(
            id = "instagram_travel",
            platform = "Instagram",
            author = "Wanderlust Chronicles",
            handle = "@wanderlust.journey",
            content = "Sunset over the Moroccan Atlas mountains after a 6-hour trek through ancient cedar forests. Moments like these remind us to disconnect from notifications and be truly present. 🌄 Where is your ultimate dream destination this year?",
            engagement = "4.5k Likes • 210 Comments",
            tags = listOf("Travel", "Photography", "Nature")
        ),
        SocialPostSample(
            id = "reddit_discussion",
            platform = "Reddit (r/AndroidDev)",
            author = "u/KotlinWizard",
            handle = "r/AndroidDev",
            content = "Are you migrating your legacy XML views to Jetpack Compose completely, or keeping a hybrid codebase? We just finished refactoring 80k lines of XML and our build times dropped by 30%, while UI state bugs decreased noticeably.",
            engagement = "420 Upvotes • 98 Comments",
            tags = listOf("JetpackCompose", "Kotlin", "Architecture")
        ),
        SocialPostSample(
            id = "arabic_social",
            platform = "X / Twitter",
            author = "طارق المحمودي",
            handle = "@tariq_tech",
            content = "الذكاء الاصطناعي التوليدي يغير طريقة تفاعلنا مع التطبيقات اليومية بشكل غير مسبوق. هل تفضلون الأدوات المساعدة العائمة (Floating Overlay) أم التطبيقات المباشرة؟ شاركونا آراءكم!",
            engagement = "680 إعجاب • 120 إعادة نشر",
            tags = listOf("تقنية", "ذكاء_اصطناعي", "تطبيقات")
        )
    )
}
