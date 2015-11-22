package controllers

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.Action
import play.api.mvc.BodyParsers
import play.api.mvc.Controller
import play.api.mvc.Result

import actors.ServerException
import actors.Watermark
import models.Ticket
import models.ticketReads
import models.ticketWrites

class Application extends Controller {

  def accept() = Action.async(BodyParsers.parse.json) { implicit request =>
    request.body.validate[JsValue] match {
      case JsSuccess(contentDocument, _) =>
        getResult(Watermark.accept(contentDocument))
      case JsError(error) => Future(BadRequest(error.toString()))
    }
  }

  def status(id: String) = Action.async { implicit request =>
    getResult(Watermark.status(UUID.fromString(id)))
  }

  def retrieve(id: String) = Action.async { implicit request =>
    getResult(Watermark.retrieve(UUID.fromString(id)))
  }

  private def getResult[R](result: Future[R])(implicit tjs: Writes[R]): Future[Result] = {
    val promise = Promise[Result]()
    result onComplete {
      case Success(r) => {
        promise.success(Ok(Json.toJson(r)))
      }
      case Failure(exception) => {
        exception match {
          case e: ServerException => promise.success(e.error)
          case _                  => promise.success(InternalServerError(exception.getMessage))
        }
      }
    }
    promise.future
  }

}
