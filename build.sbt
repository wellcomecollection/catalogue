import java.io.File
import java.util.UUID
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

def setupProject(
  project: Project,
  folder: String,
  localDependencies: Seq[Project] = Seq(),
  externalDependencies: Seq[ModuleID] = Seq()
): Project = {

  Metadata.write(project, folder, localDependencies)

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

lazy val internal_model = setupProject(
  project,
  "common/internal_model",
  externalDependencies = CatalogueDependencies.internalModelDependencies)
  .settings(Publish.settings: _*)
  .enablePlugins(GitVersioning)
  .settings(
    git.baseVersion:= sys.env.getOrElse("BUILDKITE_BUILD_NUMBER","0"),
    git.formattedShaVersion := git.gitHeadCommit.value map { sha => s"${git.baseVersion.value}.$sha" })
  .enablePlugins(BuildInfoPlugin).settings(
  buildInfoKeys := Seq[BuildInfoKey](name, version))

lazy val flows = setupProject(
  project,
  "common/flows",
  externalDependencies = CatalogueDependencies.flowDependencies)

lazy val source_model = setupProject(
  project,
  folder = "common/source_model",
  externalDependencies = CatalogueDependencies.sourceModelDependencies
)

lazy val source_model_typesafe = setupProject(
  project,
  folder = "common/source_model_typesafe",
  localDependencies = Seq(source_model),
  externalDependencies = CatalogueDependencies.sourceModelTypesafeDependencies
)

lazy val pipeline_storage = setupProject(
  project,
  "common/pipeline_storage",
  localDependencies = Seq(internal_model),
  externalDependencies = CatalogueDependencies.pipelineStorageDependencies
)

lazy val pipeline_storage_typesafe = setupProject(
  project,
  "common/pipeline_storage_typesafe",
  localDependencies = Seq(pipeline_storage),
  externalDependencies =
    CatalogueDependencies.pipelineStorageTypesafeDependencies
)

lazy val id_minter = setupProject(
  project,
  "pipeline/id_minter",
  localDependencies = Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.idminterDependencies
)

lazy val ingestor_common = setupProject(
  project,
  "pipeline/ingestor/ingestor_common",
  localDependencies = Seq(pipeline_storage_typesafe),
  externalDependencies = WellcomeDependencies.elasticsearchTypesafeLibrary
)

lazy val ingestor_works = setupProject(
  project,
  "pipeline/ingestor/ingestor_works",
  localDependencies = Seq(ingestor_common)
)

lazy val ingestor_images = setupProject(
  project,
  "pipeline/ingestor/ingestor_images",
  localDependencies = Seq(ingestor_common)
)

lazy val matcher = setupProject(
  project,
  "pipeline/matcher",
  localDependencies = Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.matcherDependencies
)

lazy val merger = setupProject(
  project,
  "pipeline/merger",
  localDependencies = Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.mergerDependencies
)

lazy val relation_embedder = setupProject(
  project,
  "pipeline/relation_embedder/relation_embedder",
  localDependencies =
    Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.relationEmbedderDependencies
)

lazy val router = setupProject(
  project,
  "pipeline/relation_embedder/router",
  localDependencies =
    Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.routerDependencies
)

lazy val batcher = setupProject(
  project,
  "pipeline/relation_embedder/batcher",
  localDependencies = Seq(),
  externalDependencies = CatalogueDependencies.batcherDependencies
)

lazy val reindex_worker = setupProject(
  project,
  "reindexer/reindex_worker",
  localDependencies = Seq(source_model),
  externalDependencies = CatalogueDependencies.reindexWorkerDependencies)

lazy val transformer_common = setupProject(
  project,
  "pipeline/transformer/transformer_common",
  localDependencies =
    Seq(internal_model, source_model_typesafe, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.transformerCommonDependencies
)

lazy val transformer_miro = setupProject(
  project,
  folder = "pipeline/transformer/transformer_miro",
  localDependencies = Seq(transformer_common),
  externalDependencies = CatalogueDependencies.miroTransformerDependencies
)

lazy val transformer_sierra = setupProject(
  project,
  folder = "pipeline/transformer/transformer_sierra",
  localDependencies = Seq(transformer_common),
  externalDependencies = CatalogueDependencies.sierraTransformerDependencies
)

lazy val transformer_mets = setupProject(
  project,
  folder = "pipeline/transformer/transformer_mets",
  localDependencies = Seq(transformer_common, mets_adapter),
  externalDependencies = CatalogueDependencies.metsTransformerDependencies
)

lazy val transformer_calm = setupProject(
  project,
  folder = "pipeline/transformer/transformer_calm",
  localDependencies = Seq(transformer_common, calm_adapter),
  externalDependencies = CatalogueDependencies.calmTransformerDependencies
)

// Sierra adapter

lazy val sierra_reader = setupProject(
  project,
  "sierra_adapter/sierra_reader",
  localDependencies = Seq(source_model_typesafe),
  externalDependencies = CatalogueDependencies.sierraReaderDependencies
)

lazy val sierra_merger = setupProject(
  project,
  "sierra_adapter/sierra_merger",
  localDependencies = Seq(source_model_typesafe),
  externalDependencies = CatalogueDependencies.sierraMergerDependencies
)

lazy val sierra_linker = setupProject(
  project,
  folder = "sierra_adapter/sierra_linker",
  localDependencies = Seq(source_model_typesafe),
  externalDependencies = CatalogueDependencies.sierraLinkerDependencies
)

lazy val sierra_indexer = setupProject(
  project,
  folder = "sierra_adapter/sierra_indexer",
  localDependencies = Seq(source_model_typesafe, pipeline_storage_typesafe))

// METS adapter

lazy val mets_adapter = setupProject(
  project,
  folder = "mets_adapter/mets_adapter",
  localDependencies = Seq(internal_model, source_model, flows),
  externalDependencies = CatalogueDependencies.metsAdapterDependencies
)

// CALM adapter

lazy val calm_api_client = setupProject(
  project,
  folder = "calm_adapter/calm_api_client",
  localDependencies = Seq(source_model, flows),
  externalDependencies = CatalogueDependencies.calmApiClientDependencies
)

lazy val calm_adapter = setupProject(
  project,
  folder = "calm_adapter/calm_adapter",
  localDependencies =
    Seq(calm_api_client, internal_model, source_model_typesafe)
)

lazy val calm_deletion_checker = setupProject(
  project,
  folder = "calm_adapter/calm_deletion_checker",
  localDependencies = Seq(calm_api_client, source_model_typesafe),
  externalDependencies = ExternalDependencies.scalacheckDependencies
)

// Inference manager
lazy val inference_manager = setupProject(
  project,
  folder = "pipeline/inferrer/inference_manager",
  localDependencies = Seq(internal_model, pipeline_storage_typesafe),
  externalDependencies = CatalogueDependencies.inferenceManagerDependencies
)

// API Projects

lazy val display = setupProject(
  project,
  "api/common/display",
  externalDependencies = CatalogueDependencies.displayModelDependencies)

lazy val api = setupProject(
  project,
  "api/api",
  localDependencies = Seq(display),
  externalDependencies = CatalogueDependencies.apiDependencies
)

lazy val stacks_common = setupProject(
  project,
  "api/stacks/common",
  localDependencies = Seq(display),
  externalDependencies = CatalogueDependencies.stacksDependencies
)

lazy val items_api = setupProject(
  project,
  "api/stacks/items_api",
  localDependencies = Seq(stacks_common),
  externalDependencies = CatalogueDependencies.stacksDependencies
)

lazy val requests_api = setupProject(
  project,
  "api/stacks/requests_api",
  localDependencies = Seq(stacks_common),
  externalDependencies = CatalogueDependencies.stacksDependencies
)

lazy val snapshot_generator = setupProject(
  project,
  "api/snapshots/snapshot_generator",
  localDependencies = Seq(display),
  externalDependencies = CatalogueDependencies.snapshotGeneratorDependencies
)

// AWS Credentials to read from S3

s3CredentialsProvider := { _ =>
  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
    "arn:aws:iam::760097843905:role/platform-ci",
    UUID.randomUUID().toString
  )
  builder.build()
}
