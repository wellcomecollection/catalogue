import java.io.File

import scala.util.parsing.json.{JSONArray, JSONObject}

def setupProject(
  project: Project,
  folder: String,
  localDependencies: Seq[Project] = Seq(),
  externalDependencies: Seq[ModuleID] = Seq()
): Project = {

  // Here we write a bit of metadata about the project, and the other
  // local projects it depends on.  This can be used to determine whether
  // to run tests based on the up-to-date project graph.
  // See https://www.scala-sbt.org/release/docs/Howto-Generating-Files.html
  val file = new File(s".sbt_metadata/${project.id}.json")
  val dependencyIds: List[String] = localDependencies
    .map { p: Project => p.id }
    .toList

  val metadata = Map(
    "id" -> project.id,
    "folder" -> folder,
    "dependencyIds" -> JSONArray(dependencyIds)
  )

  IO.write(file, JSONObject(metadata).toString())

  val dependsOn = localDependencies
    .map { project: Project =>
      ClasspathDependency(
        project = project,
        configuration = Some("compile->compile;test->test")
      )
    }

  project
    .in(new File(folder))
    .settings(Common.settings: _*)
    .settings(DockerCompose.settings: _*)
    .enablePlugins(DockerComposePlugin)
    .enablePlugins(JavaAppPackaging)
    .dependsOn(dependsOn: _*)
    .settings(libraryDependencies ++= externalDependencies)
}

lazy val internal_model = setupProject(project, "common/internal_model",
  externalDependencies = CatalogueDependencies.internalModelDependencies
)

lazy val display = setupProject(project, "common/display",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.displayModelDependencies
)

lazy val elasticsearch = setupProject(project, "common/elasticsearch",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.elasticsearchDependencies
)

lazy val elasticsearch_typesafe = setupProject(project, "common/elasticsearch_typesafe",
  localDependencies = Seq(elasticsearch),
  externalDependencies = CatalogueDependencies.elasticsearchTypesafeDependencies
)

lazy val big_messaging = setupProject(project, "common/big_messaging",
  externalDependencies = CatalogueDependencies.bigMessagingDependencies
)

lazy val big_messaging_typesafe = setupProject(project, "common/big_messaging_typesafe",
  localDependencies = Seq(big_messaging),
  externalDependencies = CatalogueDependencies.bigMessagingTypesafeDependencies
)

lazy val api = setupProject(project, "api/api",
  localDependencies = Seq(internal_model, display, elasticsearch),
  externalDependencies = CatalogueDependencies.apiDependencies
)

lazy val goobi_reader = setupProject(project, "goobi_adapter/goobi_reader",
  externalDependencies = CatalogueDependencies.goobiReaderDependencies
)

lazy val id_minter = setupProject(project, "pipeline/id_minter",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.idminterDependencies
)

lazy val ingestor = setupProject(project, "pipeline/ingestor",
  localDependencies = Seq(elasticsearch_typesafe, big_messaging_typesafe),
  externalDependencies = CatalogueDependencies.ingestorDependencies
)

lazy val matcher = setupProject(project, "pipeline/matcher",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.matcherDependencies
)

lazy val merger = setupProject(project, "pipeline/merger",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.mergerDependencies
)

lazy val recorder = setupProject(project, "pipeline/recorder",
  localDependencies = Seq(internal_model, big_messaging_typesafe),
  externalDependencies = CatalogueDependencies.recorderDependencies
)

lazy val reindex_worker = setupProject(project, "reindexer/reindex_worker",
  externalDependencies = CatalogueDependencies.reindexWorkerDependencies
)

lazy val transformer_miro = setupProject(project,
  folder = "pipeline/transformer/transformer_miro",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.miroTransformerDependencies
)

lazy val transformer_sierra = setupProject(project,
  folder = "pipeline/transformer/transformer_sierra",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.sierraTransformerDependencies
)

// Sierra adapter

lazy val sierra_adapter_common = setupProject(project, "sierra_adapter/common",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.sierraAdapterCommonDependencies
)

lazy val sierra_reader = setupProject(project, "sierra_adapter/sierra_reader",
  localDependencies = Seq(sierra_adapter_common),
  externalDependencies = CatalogueDependencies.sierraReaderDependencies
)

lazy val sierra_bib_merger = setupProject(project, "sierra_adapter/sierra_bib_merger",
  localDependencies = Seq(sierra_adapter_common)
)

lazy val sierra_item_merger = setupProject(project, "sierra_adapter/sierra_item_merger",
  localDependencies = Seq(sierra_adapter_common)
)

lazy val sierra_items_to_dynamo = setupProject(project,
  folder = "sierra_adapter/sierra_items_to_dynamo",
  localDependencies = Seq(sierra_adapter_common)
)

// Snapshots

lazy val snapshot_generator = setupProject(project, "snapshots/snapshot_generator",
  localDependencies = Seq(internal_model, display, elasticsearch_typesafe),
  externalDependencies = CatalogueDependencies.snapshotGeneratorDependencies
)
