package com.calmmap.data.sound

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10
import kotlin.math.sqrt

/** 약 1초 동안 들은 소리를 분석한 결과 */
data class SoundReading(
    /** 상대 소리 크기. 기기마다 달라서 같은 기기끼리만 비교한다 */
    val db: Int,
    /** YAMNet 원본 라벨별 점수 0~1 */
    val labelScores: Map<String, Float>,
    /** 이 1초 안에서 0.1초 단위로 가장 컸던 크기. 짧은 소리(그릇 부딪힘)가 1초 평균에 묻히지 않게 따로 본다 */
    val peakDb: Int,
)

/**
 * 마이크로 소리를 듣고 1초마다 크기와 종류를 알려준다.
 *
 * 원래 소리는 메모리에서 분석만 하고 바로 버린다. 파일로 저장하지도, 보내지도 않는다.
 * readings() 를 collect 하는 동안만 마이크가 켜지고, collect 를 멈추면 꺼진다.
 */
class SoundMeter(private val context: Context) {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun readings(): Flow<SoundReading> = flow {
        val classifier = AudioClassifier.createFromOptions(
            context,
            AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).build())
                .setRunningMode(RunningMode.AUDIO_CLIPS)
                .build(),
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            maxOf(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),
                CLIP_SAMPLES * Float.SIZE_BYTES,
            ),
        )
        val format = AudioData.AudioDataFormat.builder()
            .setNumOfChannels(1)
            .setSampleRate(SAMPLE_RATE.toFloat())
            .build()
        val buffer = FloatArray(CLIP_SAMPLES)

        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                // 1초 분량이 찰 때까지 기다린다
                var filled = 0
                while (filled < CLIP_SAMPLES) {
                    val read = record.read(buffer, filled, CLIP_SAMPLES - filled, AudioRecord.READ_BLOCKING)
                    if (read <= 0) break
                    filled += read
                }
                if (filled < CLIP_SAMPLES) continue

                val audio = AudioData.create(format, CLIP_SAMPLES).apply { load(buffer) }
                val categories = classifier.classify(audio)
                    .classificationResults().firstOrNull()
                    ?.classifications()?.firstOrNull()
                    ?.categories().orEmpty()

                emit(
                    SoundReading(
                        db = relativeDb(buffer),
                        labelScores = categories.associate { it.categoryName() to it.score() },
                        peakDb = peakDb(buffer),
                    ),
                )
            }
        } finally {
            // 화면을 나가면 마이크를 끄고 모델을 닫는다
            record.stop()
            record.release()
            classifier.close()
        }
    }.flowOn(Dispatchers.Default)

    // 소리 크기의 평균(RMS)을 dB 로. 마이크 최대치를 0 으로 보는 값(dBFS)에
    // 보기 쉬운 숫자가 되게 90 을 더했다. 보정하지 않은 상대값이다
    private fun relativeDb(samples: FloatArray): Int {
        val rms = sqrt(samples.sumOf { (it * it).toDouble() } / samples.size)
        val dbfs = 20 * log10(rms.coerceAtLeast(1e-6))
        return (dbfs + DB_OFFSET).toInt().coerceIn(0, 120)
    }

    // 0.1초 조각마다 relativeDb 를 내고 가장 큰 값. analysis/sound_analysis.py 의 peak_db 와 같다
    private fun peakDb(samples: FloatArray): Int {
        val window = SAMPLE_RATE / 10
        var best = 0
        var start = 0
        while (start + window <= samples.size) {
            best = maxOf(best, relativeDb(samples.copyOfRange(start, start + window)))
            start += window
        }
        return best
    }

    private companion object {
        const val MODEL = "yamnet.tflite"
        // YAMNet 은 16kHz 소리를 0.975초(15600개)씩 본다
        const val SAMPLE_RATE = 16_000
        const val CLIP_SAMPLES = 15_600
        const val DB_OFFSET = 90
    }
}
