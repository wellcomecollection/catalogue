package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.V2WorksIncludes
import uk.ac.wellcome.display.models.Implicits._
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.{
  ProductionEventGenerators,
  SubjectGenerators,
  WorksGenerators
}
import uk.ac.wellcome.models.work.internal._

class DisplayWorkV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with ProductionEventGenerators
    with SubjectGenerators
    with WorksGenerators {

  it("serialises a DisplayWorkV2") {
    val work = createIdentifiedWorkWith(
      workType = Some(
        WorkType(id = randomAlphanumeric(5), label = randomAlphanumeric(10))),
      description = Some(randomAlphanumeric(100)),
      lettering = Some(randomAlphanumeric(100)),
      createdDate = Some(Period("1901"))
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "description": "${work.data.description.get}",
      | "workType" : ${workType(work.data.workType.get)},
      | "lettering": "${work.data.lettering.get}",
      | "alternativeTitles": [],
      | "createdDate": ${period(work.data.createdDate.get)}
      |}
    """.stripMargin

    assertObjectMapsToJson(DisplayWorkV2(work), expectedJson = expectedJson)
  }

  it("renders an item if the items include is present") {
    val work = createIdentifiedWorkWith(
      items = createIdentifiedItems(count = 1) :+ createUnidentifiableItemWith()
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "items": [ ${items(work.data.items)} ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
  }

  it("includes 'items' if the items include is present, even with no items") {
    val work = createIdentifiedWorkWith(
      items = List()
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "items": [ ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(items = true)),
      expectedJson = expectedJson
    )
  }

  it("includes credit information in DisplayWorkV2 serialisation") {
    val location = DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = "",
      credit = Some("Wellcome Collection"),
      license = Some(License_CCBY)
    )
    val item = createIdentifiedItemWith(locations = List(location))
    val workWithCopyright = createIdentifiedWorkWith(
      items = List(item)
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithCopyright.canonicalId}",
      | "title": "${workWithCopyright.data.title.get}",
      | "alternativeTitles": [],
      | "items": [
      |   {
      |     "id": "${item.canonicalId}",
      |     "type": "${item.agent.ontologyType}",
      |     "locations": [
      |       {
      |         "type": "${location.ontologyType}",
      |         "url": "",
      |         "locationType": ${locationType(location.locationType)},
      |         "license": ${license(location.license.get)},
      |         "credit": "${location.credit.get}"
      |       }
      |     ]
      |   }
      | ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(
        workWithCopyright,
        includes = V2WorksIncludes(items = true)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes subject information in DisplayWorkV2 serialisation with the subjects include") {
    val workWithSubjects = createIdentifiedWorkWith(
      subjects = (1 to 3).map { _ =>
        createSubject
      }.toList
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithSubjects.canonicalId}",
      | "title": "${workWithSubjects.data.title.get}",
      | "alternativeTitles": [],
      | "subjects": [${subjects(workWithSubjects.data.subjects)}]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(
        workWithSubjects,
        includes = V2WorksIncludes(subjects = true)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes production information in DisplayWorkV2 serialisation with the production include") {
    val workWithProduction = createIdentifiedWorkWith(
      production = createProductionEventList(count = 3)
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${workWithProduction.canonicalId}",
      | "title": "${workWithProduction.data.title.get}",
      | "alternativeTitles": [],
      | "production": [${production(workWithProduction.data.production)}]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(
        workWithProduction,
        includes = V2WorksIncludes(production = true)
      ),
      expectedJson = expectedJson
    )
  }

  it(
    "includes the contributors in DisplayWorkV2 serialisation with the contribuotrs include") {
    val work = createIdentifiedWorkWith(
      workType = Some(
        WorkType(id = randomAlphanumeric(5), label = randomAlphanumeric(10))),
      description = Some(randomAlphanumeric(100)),
      lettering = Some(randomAlphanumeric(100)),
      createdDate = Some(Period("1901")),
      contributors = List(
        Contributor(Unidentifiable(Agent(randomAlphanumeric(25))))
      )
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "description": "${work.data.description.get}",
      | "alternativeTitles": [],
      | "workType" : ${workType(work.data.workType.get)},
      | "lettering": "${work.data.lettering.get}",
      | "createdDate": ${period(work.data.createdDate.get)},
      | "contributors": [${contributor(work.data.contributors.head)}]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(contributors = true)),
      expectedJson = expectedJson
    )
  }

  it(
    "includes genre information in DisplayWorkV2 serialisation with the genres include") {
    val work = createIdentifiedWorkWith(
      genres = List(
        Genre(
          label = "genre",
          concepts = List(
            Unidentifiable(Concept("woodwork")),
            Unidentifiable(Concept("etching"))
          )
        )
      )
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "genres": [ ${genres(work.data.genres)} ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(genres = true)),
      expectedJson = expectedJson
    )
  }

  it(
    "includes 'notes' if the notes include is present, with similar notes grouped together") {
    val work = createIdentifiedWorkWith(
      notes = List(GeneralNote("A"), FundingInformation("B"), GeneralNote("C"))
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "notes": [
      |   {
      |     "noteType": {
      |       "id": "funding-info",
      |       "label": "Funding information",
      |       "type": "NoteType"
      |     },
      |     "contents": ["B"],
      |     "type": "Note"
      |   },
      |   {
      |     "noteType": {
      |       "id": "general-note",
      |       "label": "General note",
      |       "type": "NoteType"
      |     },
      |     "contents": ["A", "C"],
      |     "type": "Note"
      |   }
      | ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(notes = true)),
      expectedJson = expectedJson
    )
  }

  it("includes a list of identifiers on DisplayWorkV2") {
    val otherIdentifier = createSourceIdentifier
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List(otherIdentifier)
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "identifiers": [
      |   ${identifier(work.sourceIdentifier)},
      |   ${identifier(otherIdentifier)}
      | ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(identifiers = true)),
      expectedJson = expectedJson
    )
  }

  it("always includes 'identifiers' with the identifiers include") {
    val work = createIdentifiedWorkWith(
      otherIdentifiers = List()
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "identifiers": [ ${identifier(work.sourceIdentifier)} ]
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes(identifiers = true)),
      expectedJson = expectedJson
    )
  }

  it("shows the thumbnail field if available") {
    val work = createIdentifiedWorkWith(
      thumbnail = Some(
        DigitalLocation(
          locationType = LocationType("thumbnail-image"),
          url = "https://iiif.example.org/1234/default.jpg",
          license = Some(License_CCBY)
        ))
    )

    val expectedJson = s"""
      |{
      | "type": "Work",
      | "id": "${work.canonicalId}",
      | "title": "${work.data.title.get}",
      | "alternativeTitles": [],
      | "thumbnail": ${location(work.data.thumbnail.get)}
      |}
    """.stripMargin

    assertObjectMapsToJson(
      DisplayWorkV2(work, includes = V2WorksIncludes()),
      expectedJson = expectedJson
    )
  }
}
