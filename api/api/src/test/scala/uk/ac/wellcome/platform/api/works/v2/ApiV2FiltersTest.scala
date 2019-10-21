package uk.ac.wellcome.platform.api.works.v2

import uk.ac.wellcome.models.work.internal._

import scala.util.Random

class ApiV2FiltersTest extends ApiV2WorksTestBase {

  describe("listing works") {
    it("ignores works with no workType") {
      withApi {
        case (indexV2, routes) =>
          val noWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(workType = None)
          }
          val matchingWork = createIdentifiedWorkWith(
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = noWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(routes, s"/$apiPrefix/works?workType=b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork.canonicalId}",
                    "title": "${matchingWork.title}",
                    "workType": ${workType(matchingWork.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("filters out works with a different workType") {
      withApi {
        case (indexV2, routes) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork = createIdentifiedWorkWith(
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = wrongWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(routes, s"/$apiPrefix/works?workType=b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork.canonicalId}",
                    "title": "${matchingWork.title}",
                    "workType": ${workType(matchingWork.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("can filter by multiple workTypes") {
      withApi {
        case (indexV2, routes) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork1 = createIdentifiedWorkWith(
            canonicalId = "001",
            workType = Some(WorkType(id = "b", label = "Books")))
          val matchingWork2 = createIdentifiedWorkWith(
            canonicalId = "002",
            workType = Some(WorkType(id = "a", label = "Archives")))

          val works = wrongWorkTypeWorks :+ matchingWork1 :+ matchingWork2
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(routes, s"/$apiPrefix/works?workType=a,b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork1.canonicalId}",
                    "title": "${matchingWork1.title}",
                    "workType": ${workType(matchingWork1.workType.get)}
                  },
                  {
                    "type": "Work",
                    "id": "${matchingWork2.canonicalId}",
                    "title": "${matchingWork2.title}",
                    "workType": ${workType(matchingWork2.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("filters by item LocationType") {
      withApi {
        case (indexV2, routes) =>
          val noItemWorks = createIdentifiedWorks(count = 3)
          val matchingWork1 = createIdentifiedWorkWith(
            canonicalId = "001",
            items = List(
              createItemWithLocationType(LocationType("iiif-image"))
            )
          )
          val matchingWork2 = createIdentifiedWorkWith(
            canonicalId = "002",
            items = List(
              createItemWithLocationType(LocationType("digit")),
              createItemWithLocationType(LocationType("dimgs"))
            )
          )
          val wrongLocationTypeWork = createIdentifiedWorkWith(
            items = List(
              createItemWithLocationType(LocationType("dpoaa"))
            )
          )

          val works = noItemWorks :+ matchingWork1 :+ matchingWork2 :+ wrongLocationTypeWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?items.locations.locationType=iiif-image,digit&include=items") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork1.canonicalId}",
                    "title": "${matchingWork1.title}",
                    "items": [${items(matchingWork1.items)}]
                  },
                  {
                    "type": "Work",
                    "id": "${matchingWork2.canonicalId}",
                    "title": "${matchingWork2.title}",
                    "items": [${items(matchingWork2.items)}]
                  }
                ]
              }
            """
          }
      }
    }
  }

  describe("filtering works by date") {

    val (work1, work2, work3) = (
      createDatedWork("1709", canonicalId = "a"),
      createDatedWork("1950", canonicalId = "b"),
      createDatedWork("2000", canonicalId = "c")
    )

    it("filters by date range") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, work1, work2, work3)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?production.dates.from=1900-01-01&production.dates.to=1960-01-01") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${work2.canonicalId}",
                    "title": "${work2.title}"
                  }
                ]
              }
            """
          }
      }
    }

    it("filters by from date") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, work1, work2, work3)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?production.dates.from=1900-01-01") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${work2.canonicalId}",
                    "title": "${work2.title}"
                  },
                  {
                    "type": "Work",
                    "id": "${work3.canonicalId}",
                    "title": "${work3.title}"
                  }
                ]
              }
            """
          }
      }
    }

    it("filters by to date") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, work1, work2, work3)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?production.dates.to=1960-01-01") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${work1.canonicalId}",
                    "title": "${work1.title}"
                  },
                  {
                    "type": "Work",
                    "id": "${work2.canonicalId}",
                    "title": "${work2.title}"
                  }
                ]
              }
            """
          }
      }
    }

    it("errors on invalid date") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, work1, work2, work3)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?production.dates.from=1900-01-01&production.dates.to=INVALID") {
            Status.BadRequest ->
              badRequest(
                apiPrefix,
                "production.dates.to: Invalid date encoding. Expected YYYY-MM-DD"
              )
          }
      }
    }
  }

  describe("searching works") {

    it("ignores works with no workType") {
      withApi {
        case (indexV2, routes) =>
          val noWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              title = "Amazing aubergines",
              workType = None)
          }
          val matchingWork = createIdentifiedWorkWith(
            title = "Amazing aubergines",
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = noWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?query=aubergines&workType=b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork.canonicalId}",
                    "title": "${matchingWork.title}",
                    "workType": ${workType(matchingWork.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("filters out works with a different workType") {
      withApi {
        case (indexV2, routes) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              title = "Bouncing bananas",
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork = createIdentifiedWorkWith(
            title = "Bouncing bananas",
            workType = Some(WorkType(id = "b", label = "Books")))

          val works = wrongWorkTypeWorks :+ matchingWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?query=bananas&workType=b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork.canonicalId}",
                    "title": "${matchingWork.title}",
                    "workType": ${workType(matchingWork.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("can filter by multiple workTypes") {
      withApi {
        case (indexV2, routes) =>
          val wrongWorkTypeWorks = (1 to 3).map { _ =>
            createIdentifiedWorkWith(
              title = "Bouncing bananas",
              workType = Some(WorkType(id = "m", label = "Manuscripts")))
          }
          val matchingWork1 = createIdentifiedWorkWith(
            canonicalId = "001",
            title = "Bouncing bananas",
            workType = Some(WorkType(id = "b", label = "Books")))
          val matchingWork2 = createIdentifiedWorkWith(
            canonicalId = "002",
            title = "Bouncing bananas",
            workType = Some(WorkType(id = "a", label = "Archives")))

          val works = wrongWorkTypeWorks :+ matchingWork1 :+ matchingWork2
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?query=bananas&workType=a,b") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork1.canonicalId}",
                    "title": "${matchingWork1.title}",
                    "workType": ${workType(matchingWork1.workType.get)}
                  },
                  {
                    "type": "Work",
                    "id": "${matchingWork2.canonicalId}",
                    "title": "${matchingWork2.title}",
                    "workType": ${workType(matchingWork2.workType.get)}
                  }
                ]
              }
            """
          }
      }
    }

    it("filters by item LocationType") {
      withApi {
        case (indexV2, routes) =>
          val noItemWorks = createIdentifiedWorks(count = 3)
          val matchingWork1 = createIdentifiedWorkWith(
            canonicalId = "001",
            title = "Crumbling carrots",
            items = List(
              createItemWithLocationType(LocationType("iiif-image"))
            )
          )
          val matchingWork2 = createIdentifiedWorkWith(
            canonicalId = "002",
            title = "Crumbling carrots",
            items = List(
              createItemWithLocationType(LocationType("digit")),
              createItemWithLocationType(LocationType("dimgs"))
            )
          )
          val wrongLocationTypeWork = createIdentifiedWorkWith(
            items = List(
              createItemWithLocationType(LocationType("dpoaa"))
            )
          )

          val works = noItemWorks :+ matchingWork1 :+ matchingWork2 :+ wrongLocationTypeWork
          insertIntoElasticsearch(indexV2, works: _*)

          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?query=carrots&items.locations.locationType=iiif-image,digit&include=items") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${matchingWork1.canonicalId}",
                    "title": "${matchingWork1.title}",
                    "items": [${items(matchingWork1.items)}]
                  },
                  {
                    "type": "Work",
                    "id": "${matchingWork2.canonicalId}",
                    "title": "${matchingWork2.title}",
                    "items": [${items(matchingWork2.items)}]
                  }
                ]
              }
            """
          }
      }
    }
  }

  describe("filtering works by language") {

    val englishWork = createIdentifiedWorkWith(
      canonicalId = "1",
      title = "Caterpiller",
      language = Some(Language("eng", "English"))
    )
    val germanWork = createIdentifiedWorkWith(
      canonicalId = "2",
      title = "Ubergang",
      language = Some(Language("ger", "German"))
    )
    val noLanguageWork = createIdentifiedWorkWith(title = "£@@!&$")
    val works = List(englishWork, germanWork, noLanguageWork)

    it("filters by language") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(routes, s"/$apiPrefix/works?language=eng") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 1)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${englishWork.canonicalId}",
                    "title": "${englishWork.title}",
                    "language": {
                      "id": "eng",
                      "label": "English",
                      "type": "Language"
                    }
                  }
                ]
              }
            """
          }
      }
    }

    it("filters by multiple comma seperated languages") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(routes, s"/$apiPrefix/works?language=eng,ger") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${englishWork.canonicalId}",
                    "title": "${englishWork.title}",
                    "language": {
                      "id": "eng",
                      "label": "English",
                      "type": "Language"
                    }
                  },
                  {
                    "type": "Work",
                    "id": "${germanWork.canonicalId}",
                    "title": "${germanWork.title}",
                    "language": {
                      "id": "ger",
                      "label": "German",
                      "type": "Language"
                    }
                  }
                ]
              }
            """
          }
      }
    }
  }

  describe("filtering works by genre") {

    val horror = createGenreWith("horrible stuff")
    val romcom = createGenreWith("heartwarming stuff")

    val horrorWork = createIdentifiedWorkWith(
      title = "horror",
      canonicalId = "1",
      genres = List(horror)
    )
    val romcomWork = createIdentifiedWorkWith(
      title = "romcom",
      canonicalId = "2",
      genres = List(romcom)
    )
    val romcomHorrorWork = createIdentifiedWorkWith(
      title = "romcom horror",
      canonicalId = "3",
      genres = List(romcom, horror)
    )
    val noGenreWork = createIdentifiedWorkWith(
      title = "no genre",
      canonicalId = "4"
    )

    val works = List(horrorWork, romcomWork, romcomHorrorWork, noGenreWork)

    def workResponse(work: IdentifiedWork): String =
      s"""
        | {
        |   "type": "Work",
        |   "id": "${work.canonicalId}",
        |   "title": "${work.title}"
        | }
      """.stripMargin

    it("filters by genre with partial match") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(routes, s"/$apiPrefix/works?genres.label=horrible") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  ${workResponse(horrorWork)},
                  ${workResponse(romcomHorrorWork)}
                ]
              }
            """
          }
      }
    }

    it("filters by genre using multiple terms") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?genres.label=horrible+heartwarming") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 3)},
                "results": [
                  ${workResponse(horrorWork)},
                  ${workResponse(romcomWork)},
                  ${workResponse(romcomHorrorWork)}
                ]
              }
            """
          }
      }
    }
  }

  describe("filtering works by subject") {

    val nineteenthCentury = createSubjectWith("19th Century")
    val paris = createSubjectWith("Paris")

    val nineteenthCenturyWork = createIdentifiedWorkWith(
      title = "19th century",
      canonicalId = "1",
      subjects = List(nineteenthCentury)
    )
    val parisWork = createIdentifiedWorkWith(
      title = "paris",
      canonicalId = "2",
      subjects = List(paris)
    )
    val nineteenthCenturyParisWork = createIdentifiedWorkWith(
      title = "19th century paris",
      canonicalId = "3",
      subjects = List(nineteenthCentury, paris)
    )
    val noSubjectWork = createIdentifiedWorkWith(
      title = "no subject",
      canonicalId = "4"
    )

    val works = List(
      nineteenthCenturyWork,
      parisWork,
      nineteenthCenturyParisWork,
      noSubjectWork)

    def workResponse(work: IdentifiedWork): String =
      s"""
        | {
        |   "type": "Work",
        |   "id": "${work.canonicalId}",
        |   "title": "${work.title}"
        | }
      """.stripMargin

    it("filters by genre") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(routes, s"/$apiPrefix/works?subjects.label=paris") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 2)},
                "results": [
                  ${workResponse(parisWork)},
                  ${workResponse(nineteenthCenturyParisWork)}
                ]
              }
            """
          }
      }
    }

    it("filters by genre using multiple terms") {
      withApi {
        case (indexV2, routes) =>
          insertIntoElasticsearch(indexV2, works: _*)
          assertJsonResponse(
            routes,
            s"/$apiPrefix/works?subjects.label=19th+century+paris") {
            Status.OK -> s"""
              {
                ${resultList(apiPrefix, totalResults = 3)},
                "results": [
                  ${workResponse(nineteenthCenturyWork)},
                  ${workResponse(parisWork)},
                  ${workResponse(nineteenthCenturyParisWork)}
                ]
              }
            """
          }
      }
    }
  }

  private def createItemWithLocationType(
    locationType: LocationType): Identified[Item] =
    createIdentifiedItemWith(
      locations = List(
        // This test really shouldn't be affected by physical/digital locations;
        // we just pick randomly here to ensure we get a good mixture.
        Random
          .shuffle(
            List(
              createPhysicalLocationWith(locationType = locationType),
              createDigitalLocationWith(locationType = locationType)
            ))
          .head
      )
    )
}
