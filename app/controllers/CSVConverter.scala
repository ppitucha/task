package controllers

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext
import models.{GeoPosition, Position}
import play.api.libs.json._

import javax.inject._

import gallia._


@Singleton
class CSVConverter @Inject()(ws: WSClient, ec: ExecutionContext, val controllerComponents: ControllerComponents)
  extends BaseController {
  implicit val geoPositionReads: Reads[GeoPosition] = Json.reads[GeoPosition]
  implicit val positionReads: Reads[Position] = Json.reads[Position]

  val allowedColumns = List("_type", "_id", "key", "name", "fullName", "iata_airport_code", "country", "latitude",
    "longitude", "location_id", "inEurope", "countryCode", "coreCountry", "distscala.tools.reflect.ToolBoxance", "type", "id")

  val wrapRPathW: String => RPathW = (str: String) => RPathW(RPath.from(KPathW(KPath.from(KeyW(Symbol(str))))))
  val wrapKeyW: String => KeyW = (str: String) => KeyW(Symbol(str))
  val wrapKPathW: String => KPathW = (str: String) => KPathW(KPath.from(KeyW(Symbol(str))))

 //curl -v localhost:9000/csv/endpoint1/4
  def endpoint1(size: Int): play.api.mvc.Action[AnyContent] = {
    val futureResult = getFrame(size).map {frame =>
      frame
        .retain("_type", "_id", "name", "type", "latitude", "longitude")
        .reorderAsFirstKeys("_type", "_id", "name", "type", "latitude", "longitude")
        .format(_.csv)
        .linesWithSeparators
        .drop(1)
        .mkString
    }(ec)
    Action.async {
      futureResult.map(csv => Ok(csv))(ec)
    }
  }

  //curl -v -d '_type,_id,key,name,fullName,iata_airport_code,country,location_id,inEurope,countryCode,coreCountry,distance,latitude,longitude,type,id' -H 'Content-Type: text/plain' -X POST localhost:9000/csv/endpoint2/5
  def endpoint2(size: Int): play.api.mvc.Action[AnyContent] = {
    Action.async { implicit request =>
      val content = request.body
      val text = content.asText.getOrElse("")
      val columns = text.split(",").map(_.trim)

      //TODO
      val notSuportedColumns = columns.filter(!allowedColumns.contains(_))

      val toRetain = columns.map(wrapRPathW(_)).toSeq
      val toReorder = columns.map(wrapKeyW(_)).toSeq

      val futureResult = getFrame(size).map {frame =>
        frame
          .retain(RPathWz(toRetain))
          .reorderAsFirstKeys(KeyWz(toReorder))
          .format(_.csv)
          .linesWithSeparators
          .drop(1)
          .mkString
      }(ec)
      futureResult.map(csv => Ok(csv))(ec)
    }
  }

  //curl -v -d 'latitude+longitude,sqrt(distance),abs(latitude),round(distance),distance' -H 'Content-Type: text/plain' -X POST localhost:9000/csv/endpoint3/3
  def endpoint3(size: Int): play.api.mvc.Action[AnyContent] = {
    Action.async { implicit request =>
      val content = request.body
      val text = content.asText.getOrElse("")
      val columns = text.split(",").map(_.trim)
      val expressions = columns.map(parseExpression)

      val toRetain = columns.map(wrapRPathW(_)).toSeq
      val toReorder = columns.map(wrapKeyW(_)).toSeq

      val futureResult = getFrame(size).map {frame =>
        expressions.foldLeft(frame) { (frame, trans) =>
          trans match {
            case ex: BinaryExpression => frame
              .generate(wrapKPathW(ex.name))
              .from(_.double(ex.first), _.double(ex.second))
              .using(ex.function)
            case ex: UnaryExpression => frame
              .generate(wrapKPathW(ex.name))
              .from(_.double(ex.first))
              .using(ex.function)
            case _ => frame
          }
        }
          .retain(RPathWz(toRetain))
          .reorderAsFirstKeys(KeyWz(toReorder))
          .format(_.csv)
          .linesWithSeparators
          .drop(1)
          .mkString
      }(ec)
      futureResult.map(csv => Ok(csv))(ec)
    }
  }


  def getFrame(size: Int) = {
    callGenerateJsonEndpoint(size).map { list =>
      gallia.aobjsFromDataClasses(list)
        .unnestAllFrom("geo_position")
        .generate("type").from(_.string("_type")).using{ v => v}
        .generate("id").from(_.long("_id")).using{ v => v}
    }(ec)
  }

  def callGenerateJsonEndpoint(size: Int): Future[List[Position]] = {
    val endpointUrl: WSRequest = ws.url(s"http://localhost:9000/generate/json/$size")
    val jsonRequest: WSRequest =
      endpointUrl
        .addHttpHeaders("Accept" -> "application/json")
        .withRequestTimeout(10000.millis)
    jsonRequest.get().map { response =>
      response.json.as[List[Position]]
    }(ec)
  }


  //TODO only simple expression supported
  def parseExpression(expression: String) = {
    import scala.util.matching.Regex


    def matchOperator(operator: String): (Double, Double) => Double = {
      operator match {
        case "*" => (a: Double, b: Double) => a * b
        case "+" => (a: Double, b: Double) => a + b
        case "/" => (a: Double, b: Double) => a / b
        case "-" => (a: Double, b: Double) => a - b
      }
    }

    //TODO more functions
    def matchFunction(function: String): Double => Double = {
      function match {
        case "sqrt" => a: Double => math.sqrt(a)
        case "abs" => a: Double => math.abs(a)
        case "round" => a: Double => math.round(a).toDouble
      }
    }

    val twoArguments: Regex = """^(\w+)([+*/\-])(\w+)$""".r
    val oneArgument: Regex = """^(sqrt|abs|round)\((\w+)\)$""".r

    expression match {
      case twoArguments(first, operator, second) =>
        BinaryExpression(expression, first, second, matchOperator(operator))
      case oneArgument(function, argument) =>
        UnaryExpression(expression, argument, matchFunction(function))
      case _ =>
        None
    }
  }
  case class BinaryExpression(name: String, first: String, second: String, function :(Double, Double) => Double)
  case class UnaryExpression(name: String, first: String, function: Double => Double)

}
