package com.example.dip.data

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ValCurs", strict = false)
data class ValCurs(
    @field:ElementList(inline = true, name = "Valute")
    val valute: List<Valute>
)

@Root(name = "Valute", strict = false)
data class Valute(
    @field:Element(name = "CharCode")
    val charCode: String,

    @field:Element(name = "Value")
    val value: String
) {
    val valueDouble: Double
        get() = value.replace(",", ".").toDouble()
}