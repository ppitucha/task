package controllers

import models.{GeoPosition, Position}
import play.api.mvc._

import javax.inject._
import play.api.libs.json._

@Singleton
class JsonGenerator @Inject()(val controllerComponents: ControllerComponents)
  extends BaseController {
  implicit val geoPositionJson: OFormat[GeoPosition] = Json.format[GeoPosition]
  implicit val positionJsonList: OFormat[Position] = Json.format[Position]

  //curl -v localhost:9000/generate/json/7
  def getPositions(size: Int): Action[AnyContent] = Action {
    val list = generatePositions(size)
    Ok(Json.toJson(list))
  }

  def generatePositions(size: Int): List[Position] = {
    Position.positionListGen(size).sample.get
  }
}
