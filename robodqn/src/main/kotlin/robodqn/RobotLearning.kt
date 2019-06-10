package robodqn

import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.deeplearning4j.rl4j.space.ArrayObservationSpace
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.deeplearning4j.rl4j.util.DataManager
import org.json.JSONObject
import org.nd4j.linalg.learning.config.Adam
import java.util.concurrent.ArrayBlockingQueue


class RobotMDP(
    private val actionChannel: ArrayBlockingQueue<Action>,
    private val feedbackChannel: ArrayBlockingQueue<FeedbackMessage>
) : MDP<Observation, Int, DiscreteSpace> {
    private val actionSpace = DiscreteSpace(Action.values().size)
    private val observationSpace = ArrayObservationSpace<Observation>(intArrayOf(Observation.ARRAY_SIZE))

    private var lastObservation = Observation()

    override fun getActionSpace(): DiscreteSpace = actionSpace
    override fun getObservationSpace(): ObservationSpace<Observation> = observationSpace
    override fun isDone(): Boolean = false

    override fun newInstance(): MDP<Observation, Int, DiscreteSpace> {
        TODO("not implemented")
    }

    override fun reset(): Observation {
        lastObservation = Observation()
        return lastObservation
    }

    override fun close() {}

    override fun step(action: Int): StepReply<Observation> {
        try {
            actionChannel.put(Action.values()[action])

            val (observation, reward, isDone) = feedbackChannel.take()
            lastObservation = observation

            return StepReply(observation, reward, isDone, JSONObject("{}"))
        } catch (e: InterruptedException) {
            actionChannel.clear()
            feedbackChannel.clear()
            return StepReply(lastObservation, -5.0, true, JSONObject("{}"))
        }
    }
}

class RobotLearning(
    private val actionChannel: ArrayBlockingQueue<Action>,
    private val feedbackChannel: ArrayBlockingQueue<FeedbackMessage>
) {

    private val qlConfiguration = QLearning.QLConfiguration(
        123,
        200,
        150000,
        150000,
        32,
        500,
        10,
        0.01,
        0.99,
        1.0,
        0.1f,
        1000,
        true
    )

    private val networkConfiguration: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
        .l2(0.00)
        .updater(Adam(0.01))
        .numHiddenNodes(30)
        .numLayer(3)
        .build()


    fun start() {

        val manager = DataManager(true)
        val mdp = RobotMDP(actionChannel, feedbackChannel)
        val dql = QLearningDiscreteDense(mdp, networkConfiguration, qlConfiguration, manager)
        dql.train()
    }
}