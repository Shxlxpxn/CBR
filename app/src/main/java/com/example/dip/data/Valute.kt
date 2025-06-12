package com.example.dip.data

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ValCurs", strict = false)
data class ValCurs(
    @field:ElementList(name = "Valute", inline = true)
    var valuteList: List<Valute>? = null
)

@Root(name = "Valute", strict = false)
data class Valute(
    @field:Element(name = "CharCode")
    var charCode: String = "",

    @field:Element(name = "Value")
    var value: String = ""
)