package models

import play.api.libs.json.JsValue

//object Document {
//  def apply(contentDocument: JsValue): Document = ???
//}

case class Document(title: String, author: String, watermark: Option[String] = None)