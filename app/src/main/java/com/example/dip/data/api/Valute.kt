package com.example.dip.data.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root


@Parcelize
@Root(name = "Valute", strict = false)
data class Valute(
    @field:Element(name = "CharCode", required = false)
    var charCode: String = "",

    @field:Element(name = "Name", required = false)
    var name: String = "",

    @field:Element(name = "Nominal", required = false)
    var nominal: Int = 1,

    @field:Element(name = "Value", required = false)
    var value: String = "",

    @field:Element(name = "Previous", required = false)
    var previous: String = ""
) : Parcelable {
    val valueDouble: Double
        get() = value.replace(",", ".").toDoubleOrNull() ?: 0.0

    val previousDouble: Double
        get() = previous.replace(",", ".").toDoubleOrNull() ?: 0.0
}