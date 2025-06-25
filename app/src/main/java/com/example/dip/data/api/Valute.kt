package com.example.dip.data.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "Valute", strict = false)
class Valute {

    @field:Element(name = "CharCode")
    var charCode: String = ""

    @field:Element(name = "Name")
    var name: String = ""

    @field:Element(name = "Nominal")
    var nominal: Int = 1

    @field:Element(name = "Value")
    var value: String = ""

    @field:Element(name = "Previous")
    var previous: String = ""

    val valueDouble: Double
        get() = value.replace(",", ".").toDouble()

    val previousDouble: Double
        get() = previous.replace(",", ".").toDouble()
}