package models

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import actors.ServerException

object Document {
  /**
    * Document is constructed from given content document
    * depending on the content
    */
  def apply(contentDocument: JsValue): Document = {

    try {
      val title = (contentDocument \ "title").as[String]
      val author = (contentDocument \ "author").as[String]

      val content = (contentDocument \ "content").as[String]
      content match {
        case "book" =>
          Book(title, author, (contentDocument \ "topic").as[String])
        case "journal" =>
          Journal(title, author)
        case _ =>
          throw new ServerException(play.api.mvc.Results.BadRequest(s"Unknown content: $content"))
      }
    } catch {
      case e: Throwable => throw new ServerException(play.api.mvc.Results.BadRequest(s"Bad content document"))
    }
  }

  implicit def documentToJson(document: Document) = document.toJson
}

sealed trait Document {
  val title: String
  val author: String
  val watermark: Option[String]

  // no unified implementation for Book and Journal since copy is generated for case classes
  def watermarked(watermark: String): Document

  def toJson: JsValue
}

case class Book(title: String, author: String, topic: String, watermark: Option[String] = None) extends Document {
  def watermarked(watermark: String): Document = copy(watermark = Some(watermark))
  def toJson: JsValue = Json.toJson(this)
}

case class Journal(title: String, author: String, watermark: Option[String] = None) extends Document {
  def watermarked(watermark: String): Document = copy(watermark = Some(watermark))
  def toJson: JsValue = Json.toJson(this)
}