package com.example.dip.data.api

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "ValCurs", strict = false)
class ValCurs {

    @field:ElementList(inline = true, name = "Valute")
    var valute: List<Valute> = mutableListOf()
}