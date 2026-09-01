package com.example.model

enum class ReplyTone(
    val title: String,
    val description: String,
    val iconName: String,
    val promptInstruction: String
) {
    ENGAGING(
        title = "Engaging & Friendly",
        description = "Warm, conversational, and sparks community engagement",
        iconName = "Sparkles",
        promptInstruction = "Make comments engaging, warm, friendly, and community-oriented."
    ),
    WITTY(
        title = "Witty & Casual",
        description = "Clever, humorous, and lightweight social banter",
        iconName = "SentimentSatisfied",
        promptInstruction = "Make comments witty, humorous, fun, clever, and casual."
    ),
    PROFESSIONAL(
        title = "Professional & Insightful",
        description = "Thoughtful, high-value, and constructive perspective",
        iconName = "Work",
        promptInstruction = "Make comments professional, insightful, value-adding, and thoughtful."
    ),
    INQUIRING(
        title = "Inquiring & Discussion",
        description = "Asks open-ended follow-up questions to drive discussion",
        iconName = "QuestionAnswer",
        promptInstruction = "Make comments ask an intriguing, relevant question to encourage discussion."
    ),
    SUPPORTIVE(
        title = "Supportive & Encouraging",
        description = "Celebrates achievements, offers genuine encouragement",
        iconName = "Favorite",
        promptInstruction = "Make comments supportive, uplifting, empathetic, and encouraging."
    )
}
