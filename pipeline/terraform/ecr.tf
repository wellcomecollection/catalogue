resource "aws_ecr_repository" "ecr_repository_nginx_services" {
  name = "uk.ac.wellcome/nginx_services"
}

resource "aws_ecr_repository" "ecr_repository_transformer_miro" {
  name = "uk.ac.wellcome/transformer_miro"
}

resource "aws_ecr_repository" "ecr_repository_transformer_sierra" {
  name = "uk.ac.wellcome/transformer_sierra"
}

resource "aws_ecr_repository" "ecr_repository_transformer_mets" {
  name = "uk.ac.wellcome/transformer_mets"
}

resource "aws_ecr_repository" "ecr_repository_transformer_calm" {
  name = "uk.ac.wellcome/transformer_calm"
}
//The image id minter and the work id minter are deprecated.
// TODO remove them when the current prod pipeline (catalogue-2020-11-25) is updated
resource "aws_ecr_repository" "ecr_repository_id_minter_works" {
  name = "uk.ac.wellcome/id_minter_works"
}

resource "aws_ecr_repository" "ecr_repository_id_minter_images" {
  name = "uk.ac.wellcome/id_minter_images"
}

resource "aws_ecr_repository" "ecr_repository_id_minter" {
  name = "uk.ac.wellcome/id_minter"
}

resource "aws_ecr_repository" "ecr_repository_matcher" {
  name = "uk.ac.wellcome/matcher"
}

resource "aws_ecr_repository" "ecr_repository_merger" {
  name = "uk.ac.wellcome/merger"
}

resource "aws_ecr_repository" "ecr_repository_ingestor_works" {
  name = "uk.ac.wellcome/ingestor_works"
}

resource "aws_ecr_repository" "ecr_repository_inference_manager" {
  name = "uk.ac.wellcome/inference_manager"
}

resource "aws_ecr_repository" "ecr_repository_feature_inferrer" {
  name = "uk.ac.wellcome/feature_inferrer"
}

resource "aws_ecr_repository" "ecr_repository_feature_training" {
  name = "uk.ac.wellcome/feature_training"
}

resource "aws_ecr_repository" "ecr_repository_palette_inferrer" {
  name = "uk.ac.wellcome/palette_inferrer"
}

resource "aws_ecr_repository" "ecr_repository_aspect_ratio_inferrer" {
  name = "uk.ac.wellcome/aspect_ratio_inferrer"
}

resource "aws_ecr_repository" "ecr_repository_ingestor_images" {
  name = "uk.ac.wellcome/ingestor_images"
}

resource "aws_ecr_repository" "ecr_repository_elasticdump" {
  name = "uk.ac.wellcome/elasticdump"
}

resource "aws_ecr_repository" "ecr_repository_relation_embedder" {
  name = "uk.ac.wellcome/relation_embedder"
}

resource "aws_ecr_repository" "ecr_repository_router" {
  name = "uk.ac.wellcome/router"
}

resource "aws_ecr_repository" "ecr_repository_batcher" {
  name = "uk.ac.wellcome/batcher"
}
