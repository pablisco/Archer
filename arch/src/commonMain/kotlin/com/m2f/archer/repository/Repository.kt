package com.m2f.archer.repository

import arrow.core.Either

fun interface Repository<out F, in Q, out A> {
    suspend operator fun invoke(q: Q): Either<F, A>
}
