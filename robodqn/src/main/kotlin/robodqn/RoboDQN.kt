package robodqn

import robocode.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread


data class Events(
    var energy: Double,
    var bulletHit: Boolean = false,
    var bulletMissed: Boolean = false,
    var hitByBullet: Boolean = false,
    var hitWall: Boolean = false,
    var hitRobot: Boolean = false,
    var death: Boolean = false
)

const val pi2 = Math.PI * 2;

fun normalizeRadianAngle(angle: Double): Double {
    return (((angle) % pi2) + pi2) % pi2
}

data class Position(val x: Double = 0.0, val y: Double = 0.0) {

    fun distance(pos: Position) = Math.sqrt(Math.pow(pos.x - x, 2.0) + Math.pow(pos.y - y, 2.0))

    fun headingLikeAngle(pos: Position): Double {
        val a = Position(0.0, 1.0)
        val b = Position(pos.y - y, pos.x - x)

        return normalizeRadianAngle(a.angle(b) - 1.5 * Math.PI)
    }

    fun angle(pos: Position): Double {

        val x1 = x
        val x2 = pos.x

        val y1 = y
        val y2 = pos.y

        val dot = x1 * x2 + y1 * y2
        val det = x1 * y2 - y1 * x2
        return Math.atan2(det, dot)
    }
}

class RoboDQN : AdvancedRobot() {
    companion object {
        private val actionChannel = ArrayBlockingQueue<Action>(1)
        private val feedbackChannel = ArrayBlockingQueue<FeedbackMessage>(1)

        init {
            println("Creating RobotLearning thread")
            thread(name = "RobotLearning") {
                try {
                    RobotLearning(actionChannel, feedbackChannel).start()
                } catch (e: Throwable) {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    val exceptionAsString = sw.toString()
                    File("/home/s/log").writeText(exceptionAsString)
                }
            }
            println("Created RobotLearning thread")
        }

    }

    init {
        println("RoboDQN constructor")
    }

    private var currentEvents = Events(0.0)

    private var enemyPosition = Position()

    override fun run() {
        while (true) {
            logic()
        }
    }

    private fun logic() {
        val a = myPosition().headingLikeAngle(enemyPosition)
        val h = Math.toRadians(gunHeading)
        val angleBetweenRobotAndEnemy = if (Math.abs(h - a) <= Math.PI) {
            h - a
        } else {
            a - h
        }

        val action = actionChannel.take()
//        val action = Action.values().random()
        doAction(action)


        val newObservation = Observation(
            doubleArrayOf(
                Action.values().indexOf(action).toDouble(),

                x,
                y,
                energy,
                velocity,

                myPosition().distance(enemyPosition),
                angleBetweenRobotAndEnemy,

                if (currentEvents.hitWall) 1.0 else 0.0,
                if (currentEvents.hitRobot) 1.0 else 0.0
            )
        )

//        println(Arrays.toString(newObservation.array))

        val reward =
            (if (currentEvents.bulletHit) 10.0 else 0.0) +
                    (if (currentEvents.bulletMissed) -1.0 else 0.0) +
                    (if (currentEvents.hitByBullet) -3.0 else 0.0) +
                    (if (currentEvents.hitRobot) -1.0 else 0.0) +
                    (if (currentEvents.hitWall) -1.0 else 0.0) +
                    (if (currentEvents.death) -10.0 else 0.0) +
                    (energy - currentEvents.energy) * 0.02

        val feedbackMessage =
            FeedbackMessage(
                observation = newObservation,
                reward = reward,
                isDone = currentEvents.death
            )

        feedbackChannel.put(feedbackMessage)


        this.currentEvents = Events(energy)

    }

    private fun myPosition(): Position = Position(x, y)

    private fun doAction(action: Action) =
        when (action) {
            Action.SPIN_LEFT -> {
                setTurnLeft(10000.0)
                setMaxVelocity(5.0)
                ahead(40.0)
            }
            Action.SPIN_RIGHT -> {
                setTurnRight(10000.0)
                setMaxVelocity(5.0)
                ahead(40.0)
            }
        }

    override fun onScannedRobot(e: ScannedRobotEvent?) {
        if (e == null) return

        fire(5.0)


        val angleToEnemy = e.bearing;
        val angle = Math.toRadians(heading + angleToEnemy % 360)

        enemyPosition = Position(
            x = (x + Math.sin(angle) * e.distance),
            y = (y + Math.cos(angle) * e.distance)
        )
    }

    override fun onBulletHit(event: BulletHitEvent?) {
        currentEvents.bulletHit = true
    }

    override fun onBulletMissed(event: BulletMissedEvent?) {
        currentEvents.bulletMissed = true
    }

    override fun onHitByBullet(event: HitByBulletEvent?) {
        currentEvents.hitByBullet = true
    }

    override fun onHitWall(event: HitWallEvent?) {
        currentEvents.hitWall = true
    }

    override fun onHitRobot(event: HitRobotEvent?) {
        currentEvents.hitRobot = true

        if (event == null) return

        if (event.bearing > -10 && event.bearing < 10) {
            fire(3.0);
        }
        if (event.isMyFault) {
            turnRight(10.0)
        }
    }

    override fun onDeath(event: DeathEvent?) {
        currentEvents.death = true
    }
}

