package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.serialization.ValidationException
import org.jetbrains.annotations.Contract
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun require(condition: Boolean, message: String) {
    contract {
        returns() implies condition
    }

    if(!condition) {
        throw IllegalArgumentException(message)
    }
}

fun validate(condition: Boolean, message: String) {
    contract {
        returns() implies condition
    }

    if (!condition) {
        throw ValidationException(message)
    }
}