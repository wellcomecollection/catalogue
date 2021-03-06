package weco.pipeline.transformer.miro.generators

import org.apache.commons.lang3.StringUtils
import weco.fixtures.RandomGenerators
import weco.pipeline.transformer.miro.source.MiroRecord

trait MiroRecordGenerators extends RandomGenerators {
  def createImageNumber: String = {
    val prefix = chooseFrom("A", "B", "L", "M")
    val number = randomInt(from = 1, to = 9999999)

    s"$prefix${StringUtils.leftPad(number.toString, 7, "0")}"
  }

  def createMiroRecordWith(
    title: Option[String] = Some("Auto-created title in MiroRecordGenerators"),
    creator: Option[List[Option[String]]] = None,
    description: Option[String] = None,
    academicDescription: Option[String] = None,
    secondaryCreator: Option[List[String]] = None,
    artworkDate: Option[String] = None,
    copyrightCleared: Option[String] = Some("Y"),
    keywords: Option[List[String]] = None,
    keywordsUnauth: Option[List[Option[String]]] = None,
    physFormat: Option[String] = None,
    lcGenre: Option[String] = None,
    useRestrictions: Option[String] = Some("CC-BY"),
    suppLettering: Option[String] = None,
    innopacID: Option[String] = None,
    creditLine: Option[String] = None,
    sourceCode: Option[String] = None,
    libraryRefDepartment: List[Option[String]] = Nil,
    libraryRefId: List[Option[String]] = Nil,
    award: List[Option[String]] = Nil,
    awardDate: List[Option[String]] = Nil,
    imageNumber: String = createImageNumber
  ): MiroRecord =
    MiroRecord(
      title = title,
      creator = creator,
      description = description,
      academicDescription = academicDescription,
      secondaryCreator = secondaryCreator,
      artworkDate = artworkDate,
      copyrightCleared = copyrightCleared,
      keywords = keywords,
      keywordsUnauth = keywordsUnauth,
      physFormat = physFormat,
      lcGenre = lcGenre,
      useRestrictions = useRestrictions,
      suppLettering = suppLettering,
      innopacID = innopacID,
      creditLine = creditLine,
      sourceCode = sourceCode,
      libraryRefDepartment = libraryRefDepartment,
      libraryRefId = libraryRefId,
      award = award,
      awardDate = awardDate,
      imageNumber = imageNumber
    )

  def createMiroRecord: MiroRecord =
    createMiroRecordWith()
}
