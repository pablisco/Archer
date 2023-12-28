package com.m2f.archer.crud.cache

import com.m2f.archer.crud.fallbackWith
import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainOperation
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.failure.DataNotFound
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RepositoryStrategyTest: FunSpec({

    test("Create a fallback strategy with infix function") {

        val mainGet = getDataSource<Int, String> { key -> if(key == 0) "main get" else raise(DataNotFound)}
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        /* FallbackWith creates a Repository using MainSyncOperation
         * this strategy always tries to get data form the main Data source and save the result
         * in the store. If the main one fails the it just tries to return the last saved data.
         */
        val repository = mainGet fallbackWith storeGet + storePut

        repository.get(0) shouldBeRight "store put: main get"
        repository.get(1) shouldBeRight "store get"
    }

    test("Uncurry a strategy") {
        /* A strategy is an objects that returns a Repository given an operation
         * but we also provide a get function that calls the get function of the
         * Repository without the need to get the instance of the Repository
         */

        val mainGet = getDataSource<Int, String> { key -> if(key == 0) "main get" else raise(DataNotFound)}
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cache(storeGet + storePut)

        strategy.get(MainOperation, 0) shouldBeRight "main get"
        strategy.get(StoreOperation, 0) shouldBeRight "store get"
        strategy.get(MainSyncOperation, 0) shouldBeRight "store put: main get"
        strategy.get(StoreSyncOperation, 0) shouldBeRight "store get"

        val expiredStore = (storeGet.validate { false } + storePut)
        val expiredStrategy = mainGet.cache(expiredStore)

        expiredStrategy.get(StoreSyncOperation, 0) shouldBeRight "store put: main get"
    }
})