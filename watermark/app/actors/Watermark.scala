package actors

import java.util.UUID

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.json.JsValue.jsValueToJsLookup

import models.Document
import models.Ticket
import models.TicketStatus
import models._

/**
  * Water mark should be a stateless class in order not to be the bottleneck
  * but for simplicity we use here an object.
  * Otherwise we would have to store tickets and documents in a separate store
  */
object Watermark {
  implicit val timeout = 1 seconds

  //  Construct the ActorSystem we will use in our application
  implicit lazy val system: ActorSystem = ActorSystem("Watermark")

  // Ensure that the constructed ActorSystem is shut down when the JVM shuts down
  sys.addShutdownHook(system.shutdown())

  def props(): Props = Props(new Watermark())
  private val watermarkActor = system.actorOf(Watermark.props)

  // separate Maps for documents and tickets
  // ticket could contain document, but when just wanting to retrieve the status,
  // one does'not want to retrieve the complete document, if it is large
  private val tickets = new mutable.HashMap[UUID, Ticket]
  private val documents = new mutable.HashMap[UUID, Document]

  val processingTime = 1 seconds

  def accept(contentDocument: JsValue): Future[Ticket] = synchronized {
    val promise = Promise[Ticket]

    try {
      val id = UUID.randomUUID()
      val ticket = Ticket(id, TicketStatus.accepted)
      val document = Document(contentDocument)

      tickets += id -> ticket
      documents += ticket.id -> Document(contentDocument)

      watermarkActor ! Process(ticket, contentDocument)

      promise.success(ticket)
    } catch {
      case e: Throwable => promise.failure(e)
    }
    promise.future
  }

  def status(id: UUID): Future[Ticket] = synchronized {
    val promise = Promise[Ticket]
    val updatedTicket = tickets.get(id)

    updatedTicket match {
      case Some(t) => promise.success(t)
      case _       => promise.failure(new ServerException(play.api.mvc.Results.NotFound(s"""Invalid ticket with id (${id}) not found""")))
    }
    promise.future
  }

  def retrieve(id: UUID): Future[JsValue] = synchronized {
    val promise = Promise[JsValue]
    val document = documents.get(id)
    document match {
      case Some(d) => promise.success(d)
      case _       => promise.failure(new ServerException(play.api.mvc.Results.NotFound(s"""Document for ticket with id (${id}) not found""")))
    }
    promise.future
  }
}

// Actor messages
case class Process(ticket: Ticket, contentDocument: JsValue)
case class Processed(ticket: Ticket, document: Document)
case class Error(ticket: Ticket)

/**
  * The watermarkActor is stateless and could be replaced by a routing pool of routees
  * @see [[http://doc.akka.io/docs/akka/2.4.0/scala/routing.html Akka routing]]
  */
class Watermark extends Actor {
  import context._
  import Watermark._

  import Watermark._

  def receive = {
    case Process(ticket, contentDocument) =>
      // delayed processing end, simulating longer processing time
      context.system.scheduler.scheduleOnce(processingTime) {
        try {
          val watermarkedDocument = documents.get(ticket.id).map { d => d.watermarked(watermark(contentDocument)) }
          if (watermarkedDocument.isDefined)
            self ! Processed(ticket, watermarkedDocument.get)
          else
            self ! Error(ticket)
        } catch {
          case e: Throwable => self ! Error(ticket)
        }
      }
    case Processed(ticket, document) => {
      documents.update(ticket.id, document)
      tickets.update(ticket.id, ticket.copy(status = TicketStatus.processed))
    }
    case Error(ticket) =>
      tickets.update(ticket.id, ticket.copy(status = TicketStatus.error))
  }

  // process of watermarking
  private def watermark(contentDocument: JsValue) = contentDocument.toString()
}
