package actors

/**
  * @author Jörg
  */
import play.api.mvc.Result

case class ServerException(error: Result) extends RuntimeException
