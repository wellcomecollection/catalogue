package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.platform.transformer.sierra.source.SierraItemData

sealed trait RulesForRequestingResult

case class NotRequestable(message: Option[String] = None) extends RulesForRequestingResult

case object NotRequestable {
  def apply(message: String): NotRequestable =
    NotRequestable(message = Some(message))
}

case object Requestable extends RulesForRequestingResult

/** The Rules for Requesting are a set of rules in Sierra that can block an item
  * from being requested, and if so, optionally explain to the user why an item
  * can't be requested.
  *
  * This object translates the rules from the MARC-like syntax into Scala.
  * The original rules are included for reference and to help apply updates,
  * along with explanations of the syntax.
  *
  * Relevant Sierra docs:
  *
  *   - Rules for Requesting syntax
  *     https://documentation.iii.com/sierrahelp/Content/sgasaa/sgasaa_requestrl.html
  *   - Fixed fields on items
  *     https://documentation.iii.com/sierrahelp/Content/sril/sril_records_fixed_field_types_item.html
  *   - Variable length fields on items
  *     https://documentation.iii.com/sierrahelp/Content/sril/sril_records_varfld_types_item.html
  *
  */
object SierraRulesForRequesting {
  def apply(itemData: SierraItemData): RulesForRequestingResult =
    itemData match {

      // This is the line:
      //
      //    q|i||97||=|x||This item belongs in the Strongroom
      //
      // This rule means "if fixed field 97 on the item has the value 'x'".
      case i if i.imessage.contains("x") =>
        NotRequestable(message = "This item belongs in the Strongroom")

      // These cases cover the lines:
      //
      //    q|i||88||=|m||This item is missing.
      //    q|i||88||=|s||This item is on search.
      //    q|i||88||=|x||This item is withdrawn.
      //    q|i||88||=|r||This item is unavailable.
      //    q|i||88||=|z||
      //    q|i||88||=|v||This item is with conservation.
      //    q|i||88||=|h||This item is closed.
      //
      // These rules mean "if fixed field 88 on the item has a given value,
      // show this message".
      case i if i.status.contains("m") =>
        NotRequestable(message = "This item is missing.")
      case i if i.status.contains("s") =>
        NotRequestable(message = "This item is on search.")
      case i if i.status.contains("x") =>
        NotRequestable(message = "This item is withdrawn.")
      case i if i.status.contains("r") =>
        NotRequestable(message = "This item is unavailable.")
      case i if i.status.contains("z") => NotRequestable()
      case i if i.status.contains("v") =>
        NotRequestable(message = "This item is with conservation.")
      case i if i.status.contains("h") =>
        NotRequestable(message = "This item is closed.")

      // These cases cover the lines:
      //
      //    v|i||88||=|b||
      //    q|i||88||=|c||Please request top item.
      //
      // These are similar to the rules above; the difference is that the 'v' means
      // "if this line or the next line matches".  The 'q' means 'end of rule'.
      case i if i.status.contains("b") || i.status.contains("c") =>
        NotRequestable(message = "Please request top item.")

      // These cases cover the lines:
      //
      //    q|i||88||=|d||On new books display.
      //    q|i||88||=|e||On exhibition. Please ask at Enquiry Desk.
      //    q|i||88||=|y||
      //
      // These are the same as the checks above.
      case i if i.status.contains("d") =>
        NotRequestable(message = "On new books display.")
      case i if i.status.contains("e") =>
        NotRequestable(message = "On exhibition. Please ask at Enquiry Desk.")
      case i if i.status.contains("y") =>
        NotRequestable()

      // These cases cover the lines:
      //
      //    v|i||87||~|0||
      //    v|i|8|||e|||
      //    q|i||88||=|!||Item is in use by another reader. Please ask at Enquiry Desk.
      //
      // How they work:
      //
      //    v|i||87||~|0||      # If fixed field 87 (loan rule) is not-equal to zero OR
      //    v|i|8|||e|||        # If variable field with tag 8 exists OR
      //    q|i||88||=|!||      # If fixed field 88 (status) equals '!'
      //
      // Notes:
      //    - Some items are missing fixed field 87 but are requestable using Encore.
      //      The Sierra API docs suggest the default loan rule is '0', so I'm assuming
      //      a missing FF87 doesn't block requesting.
      //    - I haven't found an example of an item with tag 8, so I'm skipping that rule
      //      for now.  TODO: Find an example of this.
      //
      case i if i.loanRule.getOrElse("0") != "0" || i.status.contains("!") =>
        NotRequestable(message = "Item is in use by another reader. Please ask at Enquiry Desk.")

      // These cases cover the lines:
      //
      //    v|i||79||=|mfgmc||
      //    v|i||79||=|mfinc||
      //    v|i||79||=|mfwcm||
      //    v|i||79||=|hmfac||
      //    q|i||79||=|mfulc||Item cannot be requested online. Please contact Medical Film & Audio Library.   Email: mfac@wellcome.ac.uk. Telephone: +44 (0)20 76118596/97.
      //
      case i if i.locationCode.containsAnyOf("mfgmc", "mfinc", "mfwcm", "hmfac", "mfulc") =>
        NotRequestable(message = "Item cannot be requested online. Please contact Medical Film & Audio Library.   Email: mfac@wellcome.ac.uk. Telephone: +44 (0)20 76118596/97.")

      // These cases cover the lines:
      //
      //    v|i||79||=|dbiaa||
      //    v|i||79||=|dcoaa||
      //    v|i||79||=|dinad||
      //    v|i||79||=|dinop||
      //    v|i||79||=|dinsd||
      //    v|i||79||=|dints||
      //    v|i||79||=|dpoaa||
      //    v|i||79||=|dimgs||
      //    v|i||79||=|dhuaa||
      //    v|i||79||=|dimgs||
      //    v|i||79||=|dingo||
      //    v|i||79||=|dpleg||
      //    v|i||79||=|dpuih||
      //    v|i||79||=|gblip||
      //    q|i||79||=|ofvds||This item cannot be requested online. Please place a manual request.
      //
      case i if i.locationCode.containsAnyOf("dbiaa", "dcoaa", "dinad", "dinop", "dinsd", "dints", "dpoaa", "dimgs", "dhuaa", "dimgs", "dingo", "dpleg", "dpuih", "gblip", "ofvds") =>
        NotRequestable(message = "This item cannot be requested online. Please place a manual request.")

      // These cases cover the lines:
      //
      //    v|i||79||=|isvid||
      //    q|i||79||=|iscdr||Item cannot be requested online. Please ask at Information Service desk, email: infoserv@wellcome.ac.uk or telephone +44 (0)20 7611 8722.
      //
      case i if i.locationCode.containsAnyOf("isvid", "iscdr") =>
        NotRequestable(message = "Item cannot be requested online. Please ask at Information Service desk, email: infoserv@wellcome.ac.uk or telephone +44 (0)20 7611 8722.")

      // These cases cover the lines:
      //
      //    v|i||79||=|isope||
      //    v|i||79||=|isref||
      //    v|i||79||=|gblip||
      //    v|i||79||=|wghib||
      //    v|i||79||=|wghig||
      //    v|i||79||=|wghip||
      //    v|i||79||=|wghir||
      //    v|i||79||=|wghxb||
      //    v|i||79||=|wghxg||
      //    v|i||79||=|wghxp||
      //    v|i||79||=|wghxr||
      //    v|i||79||=|wgmem||
      //    v|i||79||=|wgmxm||
      //    v|i||79||=|wgpvm||
      //    v|i||79||=|wgsee||
      //    v|i||79||=|wgsem||
      //    v|i||79||=|wgser||
      //    v|i||79||=|wqrfc||
      //    v|i||79||=|wqrfd||
      //    v|i||79||=|wqrfe||
      //    v|i||79||=|wqrfp||
      //    v|i||79||=|wqrfr||
      //    v|i||79||=|wslob||
      //    v|i||79||=|wslom||
      //    v|i||79||=|wslor||
      //    v|i||79||=|wslox||
      //    v|i||79||=|wsref||
      //    v|i||79||=|hgslr||
      //    q|i||79||=|wsrex||Item is on open shelves.  Check Location and Shelfmark for location details.
      //
      case i if i.locationCode.containsAnyOf("isope", "isref", "gblip", "wghib", "wghig", "wghip", "wghir", "wghxb", "wghxg", "wghxp", "wghxr", "wgmem", "wgmxm", "wgpvm", "wgsee", "wgsem", "wgser", "wqrfc", "wqrfd", "wqrfe", "wqrfp", "wqrfr", "wslob", "wslom", "wslor", "wslox", "wsref", "hgslr", "wsrex") =>
        NotRequestable(message = "Item is on open shelves.  Check Location and Shelfmark for location details.")

      case _ => Requestable
    }

  private implicit class ItemDataOps(itemData: SierraItemData) {
    def imessage: Option[String] =
      itemData.fixedFields.get("97").map { _.value.trim }

    def status: Option[String] =
      itemData.fixedFields.get("88").map { _.value.trim }

    def loanRule: Option[String] =
      itemData.fixedFields.get("87").map { _.value.trim }

    def locationCode: Option[String] =
      itemData.fixedFields.get("79").map { _.value.trim }
  }

  private implicit class OptionalStringOps(s: Option[String]) {
    def containsAnyOf(substrings: String*): Boolean =
      substrings.exists(s.contains(_))
  }
}
