package robodqn


data class FeedbackMessage(
    val observation: Observation,
    val reward: Double,
    val isDone: Boolean
)

