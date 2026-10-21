package com.example.lenote.data

/**
 * Utilitário puro em Kotlin para serializar e deserializar caixas delimitadoras de palavras em JSON
 * sem depender do stub do Android `org.json.JSONObject` (permitindo execução em testes unitários JVM puros).
 */
object WordBoxJsonSerializer {

    fun serialize(boxes: List<RecognizedWordBox>): String {
        if (boxes.isEmpty()) return "[]"
        val sb = StringBuilder()
        sb.append("[")
        for (i in boxes.indices) {
            val b = boxes[i]
            val escapedWord = b.word.replace("\\", "\\\\").replace("\"", "\\\"")
            sb.append("{\"word\":\"").append(escapedWord).append("\",")
            sb.append("\"left\":").append(b.left).append(",")
            sb.append("\"top\":").append(b.top).append(",")
            sb.append("\"right\":").append(b.right).append(",")
            sb.append("\"bottom\":").append(b.bottom).append("}")
            if (i < boxes.size - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserialize(json: String?): List<RecognizedWordBox> {
        if (json.isNullOrBlank()) return emptyList()
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()

        val list = mutableListOf<RecognizedWordBox>()
        // Expressão regular para casar cada objeto individual: {"word":"...","left":...,"top":...,"right":...,"bottom":...}
        val objectRegex = Regex("""\{"word":"(.*?)","left":([\d.-]+),"top":([\d.-]+),"right":([\d.-]+),"bottom":([\d.-]+)\}""")
        for (match in objectRegex.findAll(trimmed)) {
            val word = match.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\")
            val left = match.groupValues[2].toFloatOrNull() ?: 0f
            val top = match.groupValues[3].toFloatOrNull() ?: 0f
            val right = match.groupValues[4].toFloatOrNull() ?: 0f
            val bottom = match.groupValues[5].toFloatOrNull() ?: 0f
            list.add(RecognizedWordBox(word, left, top, right, bottom))
        }
        return list
    }
}
