package com.amphi.server.configs

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnection

object ServerPostgresDatabase {

  lateinit var connection: SqlConnection

  fun init(vertx: Vertx) {

    val connectOptions = PgConnectOptions()
      .setPort(5432)
      .setHost("hte-host")
      .setDatabase("the-db")
      .setUser("user")
      .setPassword("134")

    val poolOptions = PoolOptions().setMaxSize(5)

    val pool = PgBuilder
      .pool()
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx)
      .build()

    pool.connection.onComplete { asyncResult ->
        connection = asyncResult.result()
    }
  }

    fun close() {

    }
}
