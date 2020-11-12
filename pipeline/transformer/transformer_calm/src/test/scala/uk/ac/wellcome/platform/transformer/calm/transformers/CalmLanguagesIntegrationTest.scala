package uk.ac.wellcome.platform.transformer.calm.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class CalmLanguagesIntegrationTest extends AnyFunSpec with Matchers {
  it("parses everything") {
    // This is a list of all the unique values seen in the public Calm works,
    // taken from the snapshot on 10 November 2020.  It serves as a useful test
    // of the parser -- every one of these values should return *something*.
    //
    // The distinct cases are distilled into examples in the tests above.
    val testCases = Seq(
      "English",
      "French",
      "Spanish",
      "Portuguese\nSpanish",
      "Italian",
      "Latin",
      "French\nEnglish",
      "German",
      "English\nFrench",
      "Italian\nLatin",
      "French\nLatin",
      "Dutch",
      "Danish",
      "English.",
      "Dayak",
      "German; French",
      "Spanish, English",
      "Russian",
      "English, Spanish",
      "Portuguese",
      "Ukrainian",
      "Various",
      "Lugandan",
      "English`",
      "Swedish",
      "Luganda",
      "Greek",
      "Turkish",
      "<language>French</language>",
      "Arabic",
      "Potuguese",
      "Norwegian",
      "Finnish",
      "English, German and French.",
      "English/French",
      "Hungarian",
      "<language langcode=\"fre\">French</language>",
      "Czech",
      "Partly in German, partly in English.",
      "English and Portugese",
      "<language langcode=\"ita\">Italian </language>",
      "Welsh",
      "Catalan",
      "Portugese",
      "Serbian",
      "Polish",
      "German.",
      "Hebrew",
      "English; Japanese",
      "Japanese",
      "<language langcode=\"ger\">German </language>",
      "Language",
      "English and Latin",
      "Mainly in English, one article in Italian",
      "Italian.",
      "Partly in German, partly in English",
      "Burmese",
      "Flemish",
      "Arabic, English",
      "English, and translated books in Polish, French, German, Spanish, Serbo-Croat.",
      "Mainly English, one article in Russian.",
      "Chinese",
      "Swedish; English",
      "EnglishFrench (1949 conference programme)",
      "Lingala",
      "Hausa",
      "Portugese and English",
      "Serbo-Croat",
      "Spanish and English",
      "German, French",
      "English (with some annotations in Dutch)",
      "Bengali",
      "<language langcode=\"fre\">French, </language><language langcode=\"jap\">Japanese, </language>",
      "Partly in German, but mainly in English",
      "English and French.",
      "English, French.",
      "Old Guaran\u00ed",
      "Scots, English and Latin",
      "Smaller parts in German, mainly in English.",
      "<language langcode=\"ice\">Icelandic, </language><language langcode=\"dut\">Dutch </language>",
      "<language langcode=\"swe\">Swedish </language>",
      "English, Russian, German",
      "English/Serbo-Croat",
      "English translation.",
      "English, French, German",
      "English, a small amount of material in French, Russian and German.",
      "Mainly in German, smaller parts in English.",
      "English; French",
      "Swiss-German",
      "English, German and French",
      "Portguese",
      "Anglo-Saxon",
      "French and English",
      "Eng",
      "English and French",
      "English, German, French, Spanish, Dutch",
      "Mixed",
      "Smaller parts in German, but mainly in English.",
      "Austrian German",
      "<language langcode=\"ger\">German, </language>",
      "English and Norweigan",
      "Middle English",
      "Mostly English, some French.",
      "To a smaller extend in German, one article in French, but mainly in English.",
      "Mainly in English, one article in Russian.",
      "Nigerian",
      "English, Serbo-Croat, Spanish.",
      "<language langcode=\"ger\">German, </language><language langcode=\"fre\">French, </language>",
      "Mandarin",
      "Smaller parts in German, mainly in English",
      "Partly in German, partly in English, some articles in French.",
      "Mostly English",
      "English and Spanish.",
      "English and Russian",
      "English, Portugese, French and Spanish",
      "The majority of this collection is in English, however Kitzinger recieved letters from around the world and travelled widely for conferences so some material is not.",
      "English and Chinese",
      "English, Chinese",
      "English and Spanish"
    )

    var errorCount = 0

    testCases.foreach { languageField =>
      val (languages, languageNote) = CalmLanguages(Some(languageField))
      if (languages.isEmpty && languageNote.isEmpty) {
        println(s"Unable to parse: <<$languageField>>")
        errorCount += 1
      }
    }

    if (errorCount > 0) {
      println(s"There ${if (errorCount > 1) "were" else "was"} $errorCount error${if (errorCount > 1) "s" else ""}")
    }

    errorCount shouldBe 0
  }
}
