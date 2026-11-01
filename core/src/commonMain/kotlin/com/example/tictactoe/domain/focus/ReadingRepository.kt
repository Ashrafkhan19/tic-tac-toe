package com.example.tictactoe.domain.focus

data class ReadingArticle(
    val id: String,
    val title: String,
    val author: String,
    val estimatedMinutes: Int,
    val pages: List<String>,
    val questions: List<RecallQuestion>
)

data class RecallQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

class ReadingRepository {
    private val curatedArticles = listOf(
        ReadingArticle(
            id = "deep-work-intro",
            title = "The Power of Deep Work",
            author = "Cal Newport & Cognitive Science",
            estimatedMinutes = 4,
            pages = listOf(
                "Deep work is the ability to focus without distraction on a cognitively demanding task. It is a skill that allows you to quickly master complicated information and produce better results in less time. In a hyper-connected world of notifications, deep focus is becoming exceptionally rare and valuable.",
                "When you switch from Task A to Task B, your attention does not follow immediately. A residue of your attention remains stuck thinking about the previous task. This attention residue severely reduces your cognitive bandwidth, making subsequent problem-solving sluggish and shallow.",
                "To counter attention residue, deliberate blocks of unbroken concentration are essential. By eliminating digital novelties and training the mind to tolerate low stimulation, neural pathways for sustained executive control are fortified once again."
            ),
            questions = listOf(
                RecallQuestion(
                    prompt = "What is 'attention residue'?",
                    options = listOf(
                        "Sleepiness after long work",
                        "Attention remaining stuck on a previous task after switching",
                        "Physical eye fatigue from screens"
                    ),
                    correctIndex = 1
                ),
                RecallQuestion(
                    prompt = "Why is deep work becoming more valuable?",
                    options = listOf(
                        "It is rare due to constant digital distractions",
                        "Computers cannot run without humans",
                        "It eliminates the need for sleep"
                    ),
                    correctIndex = 0
                )
            )
        ),
        ReadingArticle(
            id = "dopamine-reset",
            title = "The Biology of Dopamine & Cravings",
            author = "Neurobiology Insights",
            estimatedMinutes = 3,
            pages = listOf(
                "Dopamine is not the molecule of pleasure; it is the molecule of motivation, desire, and anticipation. When you anticipate a novel notification or entertaining reel, dopamine spikes, driving you to reach and scroll.",
                "However, frequent unnatural spikes lead to dopamine receptor downregulation. When your receptors desensitize, ordinary real-world activities feel dull, joyless, and impossible to concentrate on.",
                "The remedy is conscious friction. When you resist an urge for just 60 to 90 seconds, the dopamine anticipation wave naturally crests and subsides, allowing your prefrontal cortex to regain command."
            ),
            questions = listOf(
                RecallQuestion(
                    prompt = "What is dopamine primarily responsible for?",
                    options = listOf(
                        "Muscle recovery",
                        "Motivation, anticipation, and pursuit",
                        "Digestive regulation"
                    ),
                    correctIndex = 1
                ),
                RecallQuestion(
                    prompt = "How long does an urge peak typically last?",
                    options = listOf(
                        "60 to 90 seconds",
                        "Several hours continuously",
                        "Less than 2 seconds"
                    ),
                    correctIndex = 0
                )
            )
        )
    )

    fun getAllArticles(): List<ReadingArticle> = curatedArticles
    fun getArticleById(id: String): ReadingArticle? = curatedArticles.find { it.id == id }

    fun evaluateQuiz(articleId: String, userAnswers: List<Int>): Int {
        val article = getArticleById(articleId) ?: return 0
        if (article.questions.isEmpty() || userAnswers.isEmpty()) return 0
        var correct = 0
        article.questions.forEachIndexed { i, q ->
            if (i < userAnswers.size && userAnswers[i] == q.correctIndex) {
                correct++
            }
        }
        return (correct * 100) / article.questions.size
    }
}
