package com.shapeshed.aerial.data

object NumberNormalizer {

    private val wordToDigit: Map<String, String> = mapOf(
        // 1
        "one" to "1", "un" to "1", "une" to "1", "uno" to "1", "una" to "1",
        "ein" to "1", "eine" to "1", "um" to "1", "uma" to "1",
        // 2
        "two" to "2", "deux" to "2", "zwei" to "2", "dos" to "2",
        "due" to "2", "dois" to "2", "duas" to "2",
        // 3
        "three" to "3", "trois" to "3", "drei" to "3", "tres" to "3",
        "tre" to "3", "três" to "3",
        // 4
        "four" to "4", "quatre" to "4", "vier" to "4", "cuatro" to "4",
        "quattro" to "4", "quatro" to "4",
        // 5
        "five" to "5", "cinq" to "5", "fünf" to "5", "cinco" to "5", "cinque" to "5",
        // 6
        "six" to "6", "sechs" to "6", "seis" to "6", "sei" to "6",
        // 7
        "seven" to "7", "sept" to "7", "sieben" to "7", "siete" to "7",
        "sette" to "7", "sete" to "7",
        // 8
        "eight" to "8", "huit" to "8", "acht" to "8", "ocho" to "8",
        "otto" to "8", "oito" to "8",
        // 9
        "nine" to "9", "neuf" to "9", "neun" to "9", "nueve" to "9", "nove" to "9",
        // 10
        "ten" to "10", "dix" to "10", "zehn" to "10", "diez" to "10",
        "dieci" to "10", "dez" to "10",
        // 11
        "eleven" to "11", "onze" to "11", "elf" to "11", "once" to "11",
        "undici" to "11",
        // 12
        "twelve" to "12", "douze" to "12", "zwölf" to "12", "doce" to "12",
        "dodici" to "12", "doze" to "12",
        // 13
        "thirteen" to "13", "treize" to "13", "dreizehn" to "13", "trece" to "13",
        "tredici" to "13", "treze" to "13",
        // 14
        "fourteen" to "14", "quatorze" to "14", "vierzehn" to "14", "catorce" to "14",
        "quattordici" to "14", "catorze" to "14",
        // 15
        "fifteen" to "15", "quinze" to "15", "fünfzehn" to "15", "quince" to "15",
        "quindici" to "15",
        // 16
        "sixteen" to "16", "seize" to "16", "sechzehn" to "16", "sedici" to "16",
        "dezasseis" to "16", "dezesseis" to "16",
        // 17
        "seventeen" to "17", "dix-sept" to "17", "siebzehn" to "17",
        "diciassette" to "17", "dezassete" to "17", "dezessete" to "17",
        // 18
        "eighteen" to "18", "dix-huit" to "18", "achtzehn" to "18", "dieciocho" to "18",
        "diciotto" to "18", "dezoito" to "18",
        // 19
        "nineteen" to "19", "dix-neuf" to "19", "neunzehn" to "19", "diecinueve" to "19",
        "diciannove" to "19", "dezenove" to "19",
        // 20
        "twenty" to "20", "vingt" to "20", "zwanzig" to "20", "veinte" to "20",
        "venti" to "20", "vinte" to "20",
    )

    fun normalize(text: String): String =
        text.split(" ").joinToString(" ") { word ->
            val stripped = word.trimEnd(',', '.', '-', ';', ':', '!')
            val punctuation = word.substring(stripped.length)
            wordToDigit[stripped.lowercase()] ?.let { it + punctuation } ?: word
        }
}
