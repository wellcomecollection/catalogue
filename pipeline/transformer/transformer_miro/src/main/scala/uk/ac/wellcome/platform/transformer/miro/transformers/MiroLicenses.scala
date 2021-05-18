package uk.ac.wellcome.platform.transformer.miro.transformers

import uk.ac.wellcome.platform.transformer.miro.exceptions.{
  ShouldNotTransformException,
  ShouldSuppressException
}
import weco.catalogue.internal_model.locations.License

trait MiroLicenses {

  // In some cases, the license in the Miro data is incorrect.
  // We can't edit that data, so we provide a list of overrides that
  // replace the license.
  val licenseOverrides: Map[String, License]

  def chooseLicense(miroId: String,
                    maybeUseRestrictions: Option[String]): License =
    licenseOverrides.get(miroId) match {
      case Some(license) => license
      case None          => chooseLicenseFromUseRestrictions(maybeUseRestrictions)
    }

  /** If the image has a non-empty image_use_restrictions field, choose which
    *  license (if any) we're going to assign to the thumbnail for this work.
    *
    *  The mappings in this function are based on a document provided by
    *  Christy Henshaw (MIRO drop-downs.docx).  There are still some gaps in
    *  that, we'll have to come back and update this code later.
    *
    *  For now, this mapping only covers use restrictions seen in the
    *  V collection.  We'll need to extend this for other licenses later.
    *
    *  TODO: Expand this mapping to cover all of MIRO.
    *  TODO: Update these mappings based on the final version of Christy's
    *        document.
    */
  private def chooseLicenseFromUseRestrictions(
    maybeUseRestrictions: Option[String]): License =
    maybeUseRestrictions match {

      case None =>
        throw new ShouldNotTransformException(
          "Nothing in the image_use_restrictions field")

      case Some(useRestrictions) =>
        useRestrictions match {

          // Certain strings map directly onto license types
          case "CC-0"         => License.CC0
          case "CC-BY"        => License.CCBY
          case "CC-BY-NC"     => License.CCBYNC
          case "CC-BY-NC-ND"  => License.CCBYNCND
          case "PDM"          => License.PDM
          case "In copyright" => License.InCopyright

          // These mappings are defined in Christy's document
          case "Academics" => License.CCBYNC

          // These images should really be removed entirely and sent to something
          // like Tandem Vault, but we have seen some of these strings in the
          // catalogue data -- for now, explicitly mark these as "do not transform"
          // so they don't end up on the DLQ.
          case "Do not use" =>
            throw new ShouldSuppressException(
              "image_use_restrictions = 'Do not use'")
          case "Image withdrawn, see notes" =>
            throw new ShouldSuppressException(
              "image_use_restrictions = 'Image withdrawn, see notes'")
        }
    }
}
