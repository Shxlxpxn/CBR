package com.example.dip.data.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Parcelize
@Root(name = "Valute", strict = false)
class Valute(
    @field:Element(name = "CharCode")
    var charCode: String = "",

    @field:Element(name = "Name")
    var name: String = "",

    @field:Element(name = "Nominal")
    var nominal: Int = 1,

    @field:Element(name = "Value")
    var value: String = "",

    @field:Element(name = "Previous", required = false)
    var previous: String = ""
) : Parcelable {

    val valueDouble: Double
        get() = value.replace(",", ".").toDouble()

    val previousDouble: Double
        get() = previous.replace(",", ".").toDouble()
}