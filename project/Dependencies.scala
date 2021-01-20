import sbt._

object WellcomeDependencies {

  val defaultVersion = "26.0.2"

  lazy val versions = new {
    val typesafe = defaultVersion
    val fixtures = defaultVersion
    val json = defaultVersion
    val messaging = defaultVersion
    val monitoring = defaultVersion
    val storage = defaultVersion

    val sierraStreamsSource = "2.0.0"
  }

  val jsonLibrary: Seq[ModuleID] = library(
    name = "json",
    version = versions.json
  )

  val fixturesLibrary: Seq[ModuleID] = library(
    name = "fixtures",
    version = versions.fixtures
  )

  val messagingLibrary: Seq[ModuleID] = library(
    name = "messaging",
    version = versions.messaging
  )

  val monitoringLibrary: Seq[ModuleID] = library(
    name = "monitoring",
    version = versions.monitoring
  )

  val storageLibrary: Seq[ModuleID] = library(
    name = "storage",
    version = versions.storage
  )

  val typesafeLibrary: Seq[ModuleID] = library(
    name = "typesafe_app",
    version = versions.typesafe
  ) ++ fixturesLibrary

  val storageTypesafeLibrary: Seq[ModuleID] = storageLibrary ++ library(
    name = "storage_typesafe",
    version = versions.storage
  )

  val messagingTypesafeLibrary: Seq[ModuleID] = messagingLibrary ++ library(
    name = "messaging_typesafe",
    version = versions.messaging
  ) ++ monitoringLibrary

  val sierraStreamsSourceLibrary: Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% "sierra-streams-source" % versions.sierraStreamsSource
  )

  private def library(name: String, version: String): Seq[ModuleID] = Seq(
    "uk.ac.wellcome" %% name % version,
    "uk.ac.wellcome" %% name % version % "test" classifier "tests"
  )
}

object ExternalDependencies {
  lazy val versions = new {
    val akka = "2.6.10"
    val akkaHttp = "10.1.11"
    val akkaHttpCirce = "1.32.0"
    val akkaStreamAlpakka = "1.1.2"
    val apacheCommons = "3.7"
    val apacheLogging = "2.8.2"
    val aws = "1.11.504"
    val circe = "0.13.0"
    val elastic4s = "7.10.1"
    val fastparse = "2.3.0"
    val swagger = "2.0.10"
    val mockito = "1.9.5"
    val scalatest = "3.2.3"
    val scalatestplus = "3.1.2.0"
    val scalatestplusMockitoArtifactId = "mockito-3-2"
    val scalacheckShapeless = "1.1.6"
    val scalacsv = "1.3.5"
    val scalaGraph = "1.12.5"
    val apm = "1.12.0"
    val enumeratum = "1.5.13"
    val enumeratumScalacheck = "1.5.16"
    val jsoup = "1.13.1"
  }
  val enumeratumDependencies = Seq(
    "com.beachape" %% "enumeratum" % versions.enumeratum,
    "com.beachape" %% "enumeratum-scalacheck" % versions.enumeratumScalacheck % "test"
  )

  val scribeJavaDependencies = Seq(
    "com.github.dakatsuka" %% "akka-http-oauth2-client" % "0.2.0")

  val apmDependencies = Seq(
    "co.elastic.apm" % "apm-agent-attach" % versions.apm,
    "co.elastic.apm" % "apm-agent-api" % versions.apm
  )

  val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-testkit" % versions.akka % "test",
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
    "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttp % "test",
    "de.heikoseeberger" %% "akka-http-circe" % versions.akkaHttpCirce
  )

  val alpakkaS3Dependencies = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % versions.akkaStreamAlpakka
  )

  val apacheCommonsDependencies = Seq(
    "org.apache.commons" % "commons-lang3" % versions.apacheCommons
  )

  val circeOpticsDependencies = Seq(
    "io.circe" %% "circe-optics" % versions.circe
  )

  val elasticsearchDependencies = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
  )

  val mockitoDependencies: Seq[ModuleID] = Seq(
    "org.mockito" % "mockito-core" % versions.mockito % "test",
    "org.scalatestplus" %% versions.scalatestplusMockitoArtifactId % versions.scalatestplus % "test")

  val wireMockDependencies = Seq(
    "com.github.tomakehurst" % "wiremock" % "2.25.1" % Test
  )

  val mySqlDependencies = Seq(
    "com.amazonaws" % "aws-java-sdk-rds" % versions.aws,
    "org.flywaydb" % "flyway-core" % "4.2.0",
    "org.scalikejdbc" %% "scalikejdbc" % "3.4.0",
    "mysql" % "mysql-connector-java" % "6.0.6"
  )

  val scalacheckDependencies = Seq(
    "org.scalatestplus" %% "scalacheck-1-14" % versions.scalatestplus % "test",
    "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % versions.scalacheckShapeless % "test"
  )

  val scalacsvDependencies = Seq(
    "com.github.tototoshi" %% "scala-csv" % versions.scalacsv
  )

  val scalaGraphDependencies = Seq(
    "org.scala-graph" %% "graph-core" % versions.scalaGraph
  )

  val scalatestDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % "test"
  )

  val swaggerDependencies = Seq(
    "io.swagger.core.v3" % "swagger-core" % versions.swagger,
    "io.swagger.core.v3" % "swagger-annotations" % versions.swagger,
    "io.swagger.core.v3" % "swagger-models" % versions.swagger,
    "io.swagger.core.v3" % "swagger-integration" % versions.swagger,
    "io.swagger.core.v3" % "swagger-jaxrs2" % versions.swagger,
    "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.8"
  )

  val parseDependencies = Seq(
    "com.lihaoyi" %% "fastparse" % versions.fastparse
  )

  val javaxDependencies = Seq(
    "javax.xml.bind" % "jaxb-api" % "2.3.0",
    "com.sun.xml.bind" % "jaxb-ri" % "2.3.0"
  )

  val scalaXmlDependencies = Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
  )

  val jsoupDependencies = Seq(
    "org.jsoup" % "jsoup" % versions.jsoup
  )
}

object CatalogueDependencies {
  val internalModelDependencies =
    ExternalDependencies.scalacsvDependencies ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.jsonLibrary ++
      ExternalDependencies.parseDependencies ++
      ExternalDependencies.scalacheckDependencies ++
      ExternalDependencies.enumeratumDependencies ++
      ExternalDependencies.scalaXmlDependencies

  val displayModelDependencies =
    ExternalDependencies.swaggerDependencies ++
      ExternalDependencies.scalacheckDependencies

  val elasticsearchDependencies: Seq[ModuleID] =
    ExternalDependencies.elasticsearchDependencies ++
      ExternalDependencies.scalacheckDependencies ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.typesafeLibrary

  val flowDependencies: Seq[ModuleID] =
    WellcomeDependencies.typesafeLibrary

  val sourceModelDependencies: Seq[sbt.ModuleID] =
    WellcomeDependencies.storageLibrary ++
      WellcomeDependencies.fixturesLibrary ++
      ExternalDependencies.scalatestDependencies

  val sourceModelTypesafeDependencies: Seq[ModuleID] =
    WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.messagingTypesafeLibrary

  val pipelineStorageDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingLibrary

  val pipelineStorageTypesafeDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary

  val elasticsearchTypesafeDependencies: Seq[ModuleID] =
    WellcomeDependencies.typesafeLibrary

  val transformerCommonDependencies: Seq[ModuleID] =
      WellcomeDependencies.storageLibrary

  val apiDependencies: Seq[ModuleID] =
    ExternalDependencies.akkaHttpDependencies ++
      ExternalDependencies.apmDependencies ++
      ExternalDependencies.circeOpticsDependencies ++
      WellcomeDependencies.typesafeLibrary

  val idminterDependencies: Seq[ModuleID] =
    ExternalDependencies.mySqlDependencies ++
      ExternalDependencies.circeOpticsDependencies

  val matcherDependencies: Seq[ModuleID] =
    ExternalDependencies.mockitoDependencies ++
      ExternalDependencies.scalaGraphDependencies ++
      WellcomeDependencies.storageTypesafeLibrary

  val mergerDependencies: Seq[ModuleID] = Nil

  val relationEmbedderDependencies: Seq[ModuleID] =
    WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.messagingTypesafeLibrary

  val routerDependencies: Seq[ModuleID] =
      WellcomeDependencies.messagingTypesafeLibrary

  val batcherDependencies: Seq[ModuleID] =
    ExternalDependencies.scalatestDependencies ++
      WellcomeDependencies.typesafeLibrary ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.messagingTypesafeLibrary

  val miroTransformerDependencies: Seq[ModuleID] =
    ExternalDependencies.apacheCommonsDependencies ++
      WellcomeDependencies.storageTypesafeLibrary

  val reindexWorkerDependencies: Seq[ModuleID] =
    WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.fixturesLibrary ++
      WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.scalatestDependencies

  val sierraTransformerDependencies: Seq[ModuleID] =
    ExternalDependencies.apacheCommonsDependencies

  val metsTransformerDependencies: Seq[ModuleID] =
    ExternalDependencies.apacheCommonsDependencies

  val calmTransformerDependencies: Seq[ModuleID] =
    ExternalDependencies.apacheCommonsDependencies ++
      ExternalDependencies.jsoupDependencies ++
      ExternalDependencies.parseDependencies

  // METS adapter

  val metsAdapterDependencies: Seq[ModuleID] =
    ExternalDependencies.apacheCommonsDependencies ++
      ExternalDependencies.akkaHttpDependencies ++
      ExternalDependencies.mockitoDependencies ++
      ExternalDependencies.wireMockDependencies ++
      ExternalDependencies.scribeJavaDependencies ++
      WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.storageTypesafeLibrary

  // CALM adapter

  val calmAdapterDependencies: Seq[ModuleID] =
    ExternalDependencies.akkaHttpDependencies

  // Sierra adapter stack

  val sierraAdapterCommonDependencies: Seq[ModuleID] =
    WellcomeDependencies.storageTypesafeLibrary ++
      WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.jsonLibrary ++
      WellcomeDependencies.typesafeLibrary ++
      ExternalDependencies.javaxDependencies

  val sierraReaderDependencies: Seq[ModuleID] =
    WellcomeDependencies.sierraStreamsSourceLibrary ++
      ExternalDependencies.circeOpticsDependencies ++
      WellcomeDependencies.messagingTypesafeLibrary

  // Inference manager
  val inferenceManagerDependencies: Seq[ModuleID] =
    ExternalDependencies.akkaHttpDependencies ++
      ExternalDependencies.wireMockDependencies

  // Snapshots stack

  val snapshotGeneratorDependencies: Seq[ModuleID] =
    WellcomeDependencies.messagingTypesafeLibrary ++
      WellcomeDependencies.storageLibrary ++
      ExternalDependencies.alpakkaS3Dependencies
}
