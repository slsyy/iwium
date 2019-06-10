package robodqn

import org.deeplearning4j.rl4j.space.Encodable

data class Observation(val array: DoubleArray = DoubleArray(ARRAY_SIZE)) : Encodable {
    companion object {
        const val ARRAY_SIZE = 9
    }

    override fun toArray(): DoubleArray = array

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Observation

        if (!array.contentEquals(other.array)) return false

        return true
    }

    override fun hashCode(): Int {
        return array.contentHashCode()
    }
}
