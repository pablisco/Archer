package com.m2f.archer.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.recover
import com.m2f.archer.crud.*
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get

fun <K, A> storeSyncRepository(
    remote: GetDataSource<K, A>,
    local: StoreDataSource<K, A>,
    recoverableFailures: List<Failure> = emptyList(),
    storeRecoverableFailures: List<Failure> = emptyList(),
): GetRepository<K, A> = GetRepository { query: Get<K> ->
    local.get(query).recoverWhenLeftIn(recoverableFailures) {
        remote.get(query)
            .flatMap { local.put(query.key, it) }
            .recoverWhenLeftIn(storeRecoverableFailures) {
                local.get(query)
            }
    }
}

fun <K, A> mainSyncRepository(
    remote: GetDataSource<K, A>,
    local: StoreDataSource<K, A>,
    recoverableFailures: List<Failure> = emptyList(),
): GetRepository<K, A> = GetRepository { query ->
    remote.get(query)
        .flatMap { local.put(query.key, it) }
        .recoverWhenLeftIn(recoverableFailures) { local.get(query) }
}

private inline fun <A> Either<Failure, A>.recoverWhenLeftIn(
    fallbackChecks: List<Failure>,
    block: () -> Either<Failure, A>
): Either<Failure, A> =
    recover {
        if (it in fallbackChecks) block().bind() else raise(it)
    }
