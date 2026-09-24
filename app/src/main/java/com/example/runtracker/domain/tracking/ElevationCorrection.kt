package com.example.runtracker.domain.tracking

/**
 * Nội suy độ cao cho toàn bộ trace từ một tập mẫu đã tra được ở dịch vụ DEM (dùng khi máy không có
 * barometer — xem `CorrectElevationUseCase`). Chỉ tra độ cao thật tại tối đa [MAX_SAMPLE_POINTS]
 * điểm cách đều (giới hạn số toạ độ gửi lên Elevation API mỗi buổi tập, kể cả trace hàng nghìn
 * điểm), rồi nội suy tuyến tính theo VỊ TRÍ TRONG DANH SÁCH (không theo khoảng cách thật) — đủ
 * chính xác vì `GpsTrackFilter` đã lọc điểm khá đều nhau theo thời gian, và đơn giản hơn nhiều so
 * với nội suy theo quãng đường tích luỹ kiểu `LapCalculator`. Thuần JVM.
 */
object ElevationCorrection {

    const val MAX_SAMPLE_POINTS = 100

    /** Chỉ số các điểm sẽ tra độ cao thật — cách đều, luôn có điểm đầu và điểm cuối. */
    fun sampleIndices(size: Int, maxSamples: Int = MAX_SAMPLE_POINTS): List<Int> {
        if (size <= 0) return emptyList()
        if (size <= maxSamples || maxSamples <= 1) return (0 until size).toList()
        val step = (size - 1).toDouble() / (maxSamples - 1)
        val indices = (0 until maxSamples - 1).map { (it * step).toInt() } + (size - 1)
        return indices.distinct()
    }

    /**
     * Độ cao cho MỌI điểm (chỉ số 0 cho tới [size] - 1), nội suy tuyến tính giữa các
     * [sampleIndices] đã biết [sampleElevations] tương ứng (cùng kích thước, cùng thứ tự).
     */
    fun interpolate(size: Int, sampleIndices: List<Int>, sampleElevations: List<Double>): List<Double> {
        require(sampleIndices.size == sampleElevations.size)
        if (size == 0 || sampleIndices.isEmpty()) return List(size) { 0.0 }

        val result = DoubleArray(size)
        sampleIndices.indices.forEach { result[sampleIndices[it]] = sampleElevations[it] }

        for (segment in 0 until sampleIndices.size - 1) {
            val i0 = sampleIndices[segment]
            val i1 = sampleIndices[segment + 1]
            val span = i1 - i0
            if (span > 1) {
                val e0 = sampleElevations[segment]
                val e1 = sampleElevations[segment + 1]
                for (i in (i0 + 1) until i1) {
                    result[i] = e0 + (e1 - e0) * (i - i0).toDouble() / span
                }
            }
        }
        for (i in 0 until sampleIndices.first()) result[i] = sampleElevations.first()
        for (i in (sampleIndices.last() + 1) until size) result[i] = sampleElevations.last()
        return result.toList()
    }
}
