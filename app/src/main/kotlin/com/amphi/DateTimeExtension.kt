package com.amphi

import java.time.LocalDateTime

fun parsedDate(data: String) : LocalDateTime {

    val split = data.split(";")

    return LocalDateTime.of(
        Integer.parseInt(split[0]),
        Integer.parseInt(split[1]),
        Integer.parseInt(split[2]),
        Integer.parseInt(split[3]),
        Integer.parseInt(split[4]),
        Integer.parseInt(split[5])
    )
}

fun LocalDateTime.stringify() : String {
    return "$year;$monthValue;$dayOfMonth;$hour;$minute;$second"
}