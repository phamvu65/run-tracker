package com.example.runtracker.domain.tracking

/** DEM samples are spaced by travelled distance, never by GPS sampling frequency. */
object ElevationCorrection {
    const val MAX_SAMPLE_POINTS = 100

    fun sampleIndices(distances: List<Double>, maxSamples: Int = MAX_SAMPLE_POINTS): List<Int> {
        require(maxSamples >= 2)
        require(distances.all { it.isFinite() } && distances.zipWithNext().all { (a, b) -> b >= a })
        if (distances.size <= maxSamples) return distances.indices.toList()
        val total = distances.last() - distances.first()
        if (total == 0.0) return listOf(0, distances.lastIndex)
        val indices = linkedSetOf(0)
        var cursor = 0
        for (i in 1 until maxSamples - 1) {
            val target = distances.first() + total * i / (maxSamples - 1)
            while (cursor < distances.lastIndex && distances[cursor] < target) cursor++
            indices += cursor
        }
        indices += distances.lastIndex
        return indices.toList()
    }

    fun interpolate(distances: List<Double>, sampleIndices: List<Int>, sampleElevations: List<Double>): List<Double> {
        require(sampleIndices.size == sampleElevations.size)
        require(sampleElevations.all { it.isFinite() })
        require(distances.all { it.isFinite() } && distances.zipWithNext().all { (a, b) -> b >= a })
        require(sampleIndices.all { it in distances.indices } && sampleIndices.zipWithNext().all { (a, b) -> b > a })
        if (distances.isEmpty()) return emptyList()
        require(sampleIndices.isNotEmpty())
        val result = DoubleArray(distances.size)
        for (i in 0..sampleIndices.first()) result[i] = sampleElevations.first()
        for (segment in 0 until sampleIndices.lastIndex) {
            val first = sampleIndices[segment]
            val last = sampleIndices[segment + 1]
            val span = distances[last] - distances[first]
            for (i in first..last) {
                val ratio = if (span > 0) (distances[i] - distances[first]) / span else 0.0
                result[i] = sampleElevations[segment] + ratio * (sampleElevations[segment + 1] - sampleElevations[segment])
            }
        }
        for (i in sampleIndices.last()..distances.lastIndex) result[i] = sampleElevations.last()
        return result.toList()
    }
}