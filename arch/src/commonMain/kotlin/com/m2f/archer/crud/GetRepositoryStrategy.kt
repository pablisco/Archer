package com.m2f.archer.crud

import com.m2f.archer.crud.operation.*
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Get
import com.m2f.archer.repository.Repository
import com.m2f.archer.repository.mainSyncRepository
import com.m2f.archer.repository.storeSyncRepository
import com.m2f.archer.repository.toRepository

fun interface GetRepositoryStrategy<K, out A> {
    fun create(operation: Operation): GetRepository<K, A>
}

fun <K, A> cacheStrategy(
    mainDataSource: GetDataSource<K, A>,
    storeDataSource: StoreDataSource<K, A>,
    mainFallback: List<Failure> = mainAiraloFallbacks,
    storeFallback: List<Failure> = storageAiraloFallbacks,
): GetRepositoryStrategy<K, A> = GetRepositoryStrategy { operation ->
    when (operation) {
        is MainOperation -> mainDataSource.toRepository()
        is StoreOperation -> storeDataSource.toRepository()
        is MainSyncOperation -> mainSyncRepository(
            remote = mainDataSource,
            local = storeDataSource,
            recoverableFailures = mainFallback
        )

        is StoreSyncOperation -> storeSyncRepository(
            remote = mainDataSource,
            local = storeDataSource,
            recoverableFailures = storeFallback,
            storeRecoverableFailures = mainFallback,
        )
    }
}

infix fun <K, A> GetDataSource<K, A>.fallbackWith(store: StoreDataSource<K, A>): GetRepository<K, A> =
    cacheStrategy(this, store).create(MainSyncOperation)

/**
 *
 * [Uncurry](https://en.wikipedia.org/wiki/Currying) the [GetRepositoryStrategy] to create a [Repository] using the provided [operation]
 * and fetch the data using [q].
 *
 * @param K The generic key type used within the get operation.
 * @param A The generic type parameter representing some additional context or requirement for the repository.
 * @param operation The [Operation] to be performed.
 * @param q The query of type [Q] used to perform the get operation.
 * @return The result of the get operation, returned by invoking the created operation with the query [q].
 */
suspend fun <K, A> GetRepositoryStrategy<K, A>.get(
    operation: Operation,
    q: K,
) = create(operation).get(q)
