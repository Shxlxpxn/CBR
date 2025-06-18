package com.example.dip.data

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ValCurs", strict = false)
class ValCurs {

    @field:ElementList(inline = true, name = "Valute")
    var valute: List<Valute> = mutableListOf()
}

@Root(name = "Valute", strict = false)
class Valute {

    @field:Element(name = "CharCode")
    var charCode: String = ""

    @field:Element(name = "Value")
    var value: String = ""

    val valueDouble: Double
        get() = value.replace(",", ".").toDouble()
}