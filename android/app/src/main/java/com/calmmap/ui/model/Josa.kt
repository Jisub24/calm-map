package com.calmmap.ui.model

/**
 * [word] 의 마지막 글자 받침에 맞는 조사. 받침이 있으면 [batchim], 없으면 [plain].
 *   josa("사람 많음", "이", "가") → "이"     josa("갑작스러운 소리", "을", "를") → "를"
 * '으로' 는 ㄹ 받침 뒤에서 '로' 가 된다 ("기차로")
 */
fun josa(word: String, batchim: String, plain: String): String {
    val c = word.lastOrNull() ?: return plain
    if (c !in '가'..'힣') return plain
    val final = (c - '가') % 28
    return when {
        final == 0 -> plain
        final == 8 && batchim == "으로" -> plain
        else -> batchim
    }
}
