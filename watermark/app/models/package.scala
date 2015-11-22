import models._
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.JsPath
import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import java.util.UUID

package object models {
  import play.api.libs.json.Reads.uuidReads
  import play.api.libs.json.Writes.UuidWrites

  implicit val readTicketStatus: Reads[TicketStatus.Value] = new Reads[TicketStatus.Value] {
    def reads(json: JsValue) = json match {
      case JsNumber(id) => {
        try {
          JsSuccess(TicketStatus(id.toInt))
        } catch {
          case e: java.util.NoSuchElementException => JsError("Enumeration expected")
        }
      }
      case _ => JsError("String expected")
    }
  }

  implicit val writeTicketStatus: Writes[TicketStatus.Value] = new Writes[TicketStatus.Value] {
    def writes(status: TicketStatus.Value) = JsNumber(status.id)
  }

  implicit val bookReads: Reads[Book] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "author").read[String] and
    (JsPath \ "topic").read[String] and
    (JsPath \ "watermark").readNullable[String]
  )(Book.apply _)

  implicit val bookWrites: Writes[Book] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "author").write[String] and
    (JsPath \ "topic").write[String] and
    (JsPath \ "watermark").writeNullable[String]
  )(unlift(Book.unapply))

  implicit val journalReads: Reads[Journal] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "author").read[String] and
    (JsPath \ "watermark").readNullable[String]
  )(Journal.apply _)

  implicit val journalWrites: Writes[Journal] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "author").write[String] and
    (JsPath \ "watermark").writeNullable[String]
  )(unlift(Journal.unapply))

  implicit val ticketReads: Reads[Ticket] = (
    (JsPath \ "id").read[UUID] and
    (JsPath \ "status").read[TicketStatus.Value]
  )(Ticket.apply _)

  implicit val ticketWrites: Writes[Ticket] = (
    (JsPath \ "id").write[UUID] and
    (JsPath \ "status").write[TicketStatus.Value]
  )(unlift(Ticket.unapply))
}