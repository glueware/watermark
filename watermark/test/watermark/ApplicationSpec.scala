package watermark

import play.api.test.WithApplication
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.FakeApplication
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.mvc.AcceptExtractors
import play.api.mvc.Results
import play.api.test.FakeApplication
import play.api.test.PlaySpecification
import play.api.test.Helpers
import play.api.libs.json._
import models.TicketStatus
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import actors.Watermark
import play.api.mvc.Result

/**
  * Add your spec here.
  * You can mock out a whole application including requests, plugins etc.
  * For more information, consult the wiki.
  */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification with Results {

  val controller = new controllers.Application()

  val contentDocument1 = """{"content":"book", "title":"The Dark Code", "author":"Bruce Wayne", "topic":"Science"}"""
  val contentDocument2 = """{"content":"book", "title":"How to make money", "author":"Dr. Evil", "topic":"Business"}"""
  val contentDocument3 = """{"content":"journal", "title":"Journal of human flight routes", "author":"Clark Kent"}"""

  "Application" should {

    var id = ""
    "accept content document and return ticket" in {
      implicit val app = FakeApplication()
      running(app) {

        val fakeRequest = FakeRequest(Helpers.POST, controllers.routes.Application.accept.url, FakeHeaders(), Json.parse(contentDocument1))
        val ticketResult = controller.accept().apply(fakeRequest)

        status(ticketResult) must equalTo(OK)
        contentType(ticketResult) must beSome("application/json")

        val ticketJson = contentAsJson(ticketResult)
        id = (ticketJson \ "id").as[String]
        (ticketJson \ "status").as[Int] must equalTo(TicketStatus.accepted.id)
      }
    }

    step(Thread.sleep((Watermark.processingTime / 5).toMillis))

    "use accepted ticket id and return ticket with status accepted or processing after half of processing time" in {
      implicit val app = FakeApplication()
      running(app) {

        val fakeRequest = FakeRequest(Helpers.GET, controllers.routes.Application.status(id).url, FakeHeaders(), null)
        val ticketResult = Future(play.api.test.Helpers.await(controller.status(id).apply(fakeRequest)))

        status(ticketResult) must equalTo(OK)
        contentType(ticketResult) must beSome("application/json")
        (contentAsJson(ticketResult) \ "status").as[Int] must equalTo(TicketStatus.accepted.id) or equalTo(TicketStatus.processing.id)
      }
    }

    step(Thread.sleep((Watermark.processingTime * 2).toMillis))

    "use accepted ticket id and return ticket with status processed after double of processing time" in {
      implicit val app = FakeApplication()
      running(app) {

        val fakeRequest = FakeRequest(Helpers.GET, controllers.routes.Application.status(id).url, FakeHeaders(), null)
        val ticketResult = Future(play.api.test.Helpers.await(controller.status(id).apply(fakeRequest)))

        status(ticketResult) must equalTo(OK)
        contentType(ticketResult) must beSome("application/json")
        (contentAsJson(ticketResult) \ "status").as[Int] must equalTo(TicketStatus.processed.id)
      }
    }

    "use accepted ticket id and retrieve document" in {
      implicit val app = FakeApplication()
      running(app) {

        val fakeRequest = FakeRequest(Helpers.GET, controllers.routes.Application.retrieve(id).url, FakeHeaders(), null)
        val documentResult = Future(play.api.test.Helpers.await(controller.retrieve(id).apply(fakeRequest)))

        status(documentResult) must equalTo(OK)
        contentType(documentResult) must beSome("application/json")

        val documentJson = contentAsJson(documentResult)
        (documentJson \ "author").as[String] must equalTo("Bruce Wayne")
        (documentJson \ "title").as[String] must equalTo("The Dark Code")
        Json.parse((documentJson \ "watermark").as[String]) must equalTo(Json.parse(contentDocument1))
      }
    }
  }
}
