// git path: app/src/main/java/com/favpixel/nexus/PiperEngine.kt
package com.favpixel.nexus

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import org.json.JSONObject
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Loads a bundled Piper voice (an ONNX model + its .onnx.json config) and
 * runs inference directly on-device — no network call, no API key, works
 * with the phone in airplane mode.
 *
 * DELIBERATELY INCOMPLETE — this only proves the "easy half" works. It
 * takes phoneme IDs as input, not raw text. Turning text into phoneme IDs
 * (the actual hard part of Piper on Android — espeak-ng needs native
 * compilation, or a lighter dictionary-based alternative trades accuracy
 * for a pure-Kotlin build) is a separate, still-undecided piece. Calling
 * synthesizeTestTone() below does NOT produce an intelligible word — it
 * feeds the model nothing but its begin/end-of-sentence tokens, on
 * purpose. The only thing it's meant to prove is that the model loads and
 * ONNX Runtime inference completes on a real phone without crashing —
 * that's the actual technical risk worth retiring before committing to a
 * phonemizer approach, and it's a materially different question from
 * "does the audio sound right."
 *
 * REQUIRED BEFORE THIS COMPILES/RUNS — model files aren't included in this
 * change; .onnx files are tens of MB, too large to hand-write. Download a
 * voice from Hugging Face (huggingface.co/rhasspy/piper-voices) and place
 * both files at:
 *   app/src/main/assets/piper/voice.onnx
 *   app/src/main/assets/piper/voice.onnx.json
 * Start with en_US-lessac-low (~20MB) rather than -medium (~63MB) or
 * -high — GitHub's mobile web upload has roughly a 25MB per-file limit,
 * so -medium/-high likely can't even be uploaded the way everything else
 * in this project has been. Swap to a higher-quality voice later, once
 * this is confirmed working end to end and file transfer needs a
 * different approach anyway.
 */
class PiperEngine(private val context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    private var sampleRate: Int = 22050

    // Stable Piper convention across virtually every voice's phoneme map:
    // 0 = pad ("_"), 1 = beginning-of-sentence ("^"), 2 = end-of-sentence
    // ("$"). Not something invented here — this is Piper's own convention.
    companion object {
        private const val PAD_ID = 0L
        private const val BOS_ID = 1L
        private const val EOS_ID = 2L
    }

    fun loadIfNeeded() {
        if (session != null) return

        val modelBytes = context.assets.open("piper/voice.onnx").use { it.readBytes() }
        session = env.createSession(modelBytes)

        val configJson = context.assets.open("piper/voice.onnx.json")
            .bufferedReader().use { it.readText() }
        val config = JSONObject(configJson)
        sampleRate = config.optJSONObject("audio")?.optInt("sample_rate", 22050) ?: 22050
    }

    /**
     * BOS + pad + EOS only — no real phonemes. Exists purely to prove the
     * model loads and inference runs; expect silence or a short
     * meaningless blip, not a word. If session.run() throws complaining
     * about an unknown input name, that means Piper's actual ONNX export
     * uses different tensor names than "input"/"input_lengths"/"scales"
     * assumed here (they're the standard names across Piper's own export
     * script, but unverifiable from here without a live model to inspect)
     * — logging ortSession.inputNames right before the run() call below
     * is the fastest way to find the real names if that happens.
     */
    fun synthesizeTestTone(): ShortArray {
        loadIfNeeded()
        val ortSession = session ?: throw IllegalStateException("Model not loaded")

        val phonemeIds = longArrayOf(BOS_ID, PAD_ID, EOS_ID)
        val inputIds = OnnxTensor.createTensor(
            env, LongBuffer.wrap(phonemeIds), longArrayOf(1, phonemeIds.size.toLong())
        )
        val inputLengths = OnnxTensor.createTensor(
            env, LongBuffer.wrap(longArrayOf(phonemeIds.size.toLong())), longArrayOf(1)
        )
        // [noise_scale, length_scale, noise_w] — Piper's standard defaults.
        // These control expressiveness/speed, not correctness — safe to
        // leave as-is regardless of voice.
        val scales = OnnxTensor.createTensor(
            env, FloatBuffer.wrap(floatArrayOf(0.667f, 1.0f, 0.8f)), longArrayOf(3)
        )

        val inputs = mapOf(
            "input" to inputIds,
            "input_lengths" to inputLengths,
            "scales" to scales
        )

        ortSession.run(inputs).use { results ->
            val samples = flattenAudio(results[0].value)
            // Piper outputs float32 samples in [-1, 1] — AudioTrack wants
            // 16-bit PCM, so convert here.
            return ShortArray(samples.size) { i ->
                (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }

    // ONNX Runtime's Java/Kotlin bindings return nested Java arrays whose
    // exact depth depends on the model's declared output shape — could be
    // [samples], [batch][samples], or [batch][channel][samples]. Rather
    // than guess the depth and risk a ClassCastException on a real phone
    // with no way to inspect it live, this just flattens whatever comes
    // back, however deep it is.
    private fun flattenAudio(value: Any?): FloatArray {
        return when (value) {
            is FloatArray -> value
            is Array<*> -> value.flatMap { flattenAudio(it).toList() }.toFloatArray()
            else -> throw IllegalStateException(
                "Unexpected output shape from Piper model: ${value?.javaClass}"
            )
        }
    }

    fun playTestTone() {
        val pcm = synthesizeTestTone()
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
    }
}
