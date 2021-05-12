# Catalogue pipeline

## Structure

All data comes from an external data source, e.g. [Sierra and it's adapter](sierra_adapter.md), onto a queue for entering the pipeline, and finally makes it into out query index, [Elasticsearch](https://www.elastic.co/products/elasticsearch).

The flow of data is as follows:

![Pipeline diagram](https://user-images.githubusercontent.com/4429247/80081224-914c6b00-854a-11ea-94e4-5d2618b7b6b1.png)

Each service in the pipeline has an input of an SNS topic that it subscribes to and after it has worked on that message, pushes its result to a SQS queue.

We use AWS SNS / SQS for this, there are talks of abstracting that out.

```text
SNS => Service => SQS
```

### [Transformer](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/transformer/README.md)

Each data source will have their own transformer e.g.

* [Miro](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/transformer/transformer_miro/README.md)
* [Sierra](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/transformer/transformer_sierra/README.md)

These take the original source data from an adapter, and transform them into a Work \(Unidentified\).

### [Matcher](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/matcher/README.md)

Searches for potential merge candidates, and records them on the Work.

### [Merger](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/merger/README.md)

Runs some [rules](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/merger/src/test/scala/uk/ac/wellcome/platform/merger/rules/README.md) on the merge candidates and decides if it is a valid merge. Extracts images from works and sends them to the images branch of the pipeline.

### [ID Minters](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/id_minter/README.md)

Each Unidentified Work or Image has an ID minted for it, using a source ID and avoiding dupes.

### [Inferrer](pipeline/inferrer)

Infers data from the image (currently, this data is a feature vector) and attaches that to the image. Composed of 2 coupled services as detailed in [this RFC](https://github.com/wellcomecollection/docs/tree/main/rfcs/021-data_science_in_the_pipeline).

### [Ingestors](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/ingestor/README.md)

Inserts a work or image into our query index - Elasticsearch.

## Releasing

1. Create a new release with the [Wellcome Release Tool](https://github.com/wellcometrust/dockerfiles/tree/master/release_tooling)

   to `stage`

   ```bash
      wrt prepare
      wrt deploy stage
   ```

2. Create a new stack by copying the current one in

   [`./terraform/main.tf`](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/pipeline/%60./terraform/main.tf%60).

3. Change the namespace and index name
4. Make sure the reindex topics are uncommented. You probably only need the Sierra one.

   ```text
      "${local.sierra_reindexer_topic_name}"
      "${local.miro_reindexer_topic_name}"
   ```

5. Run

   ```bash
      terraform init
      terraform apply
   ```

6. Run the [`reindexer`](https://github.com/wellcomecollection/catalogue/tree/864b998aae9ed3fe40515edfef061c7c7371f721/reindexer/README.md)

   ```text
    python3 ./start_reindex.py --src sierra --dst catalogue --reason "Great Good" --mode partial
   ```

7. Watch your new pipeline do it's magic and land up as expected in the new index 🔮

### If there were no model changes

1. Change the name of the index to the current index
2. Comment out the reindex topics
3. Remove the old stack
4. Remove the test index
5. `terraform apply`

### If there were model changes

1. Do a complete reindex into the new index
2. Once you're happy it's up to date, remove the old stack
3. Change the index that the API is pointing to on the staging API
4. Test
5. Make the stage API the prod api

