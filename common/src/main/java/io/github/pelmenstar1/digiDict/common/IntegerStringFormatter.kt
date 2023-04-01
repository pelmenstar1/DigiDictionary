package io.github.pelmenstar1.digiDict.common

object IntegerStringFormatter : StringFormatter<Int> {
    override fun format(value: Int): String = value.toString()
}