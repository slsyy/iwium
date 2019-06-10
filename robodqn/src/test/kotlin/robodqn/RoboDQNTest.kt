package robodqn

import org.junit.Assert.*
import org.junit.Test

class RoboDQNTest {

    @Test
    fun test() {
        val a = Position(x=292.6640488579855, y=124.9580209350075)
        val b = Position(x=165.158122931891, y=403.3956204621136)


        val x = Position(0.0, 1.0)
        val y = Position(b.y - a.y, b.x - a.x)

        val pi2 = Math.PI * 2;
        val res = ((x.angle(y) % pi2) + pi2 ) % pi2

        println(Math.toDegrees(res))
    }

}