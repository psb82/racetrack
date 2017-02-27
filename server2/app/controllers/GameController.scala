package controllers

import com.google.inject.Inject
import model.{Car, CarData, Player, PlayerData}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

case class CarResponse(name: String, token: String, px: Int, py: Int, vx: Int, vy: Int)
object CarResponse {
  implicit val carFormat = Json.format[CarResponse]
}

case class MoveRequest(px: Int, py: Int)
object MoveRequest {
  implicit val moveFormat = Json.format[MoveRequest]
}

class GameController @Inject() extends Controller {

  def index = Action {
    Ok(views.html.game())
  }

  // GET /car
  def getAllCars = Action {
    val cars = CarData.all map (car => CarResponse(car.playerName, car.token, car.pos._1, car.pos._2, car.vel._1, car.vel._2))
    Ok(Json.toJson(cars))
  }

  // GET /car/:token
  def getOneCar(token: String) = Action {
    CarData.get(token) match {
      case Some(car) => Ok(Json.toJson(CarResponse(car.playerName, car.token, car.pos._1, car.pos._2, car.vel._1, car.vel._2)))
      case None => NotFound("No such car found.")
    }
  }

  // moving the car
  val moveRequestForm: Form[MoveRequest] = Form {
    mapping(
      "px" -> number,
      "py" -> number
    )(MoveRequest.apply)(MoveRequest.unapply)
  }

  def move(token: String) = Action { implicit request =>
    println("MOVE: " + token)
    val moveRequest = moveRequestForm.bindFromRequest.get
    CarData.get(token) match {
      case Some(car) => moveAndUpdate(car, (moveRequest.px, moveRequest.py))
      case None      => println("Error: no car with token " + token + " found.")
    }

    Redirect(routes.GameController.index())
  }

  type Vec = (Int, Int)

  // moves a car, returns new pos and whether an error occurred
  def moveAndUpdate(car: Car, newPos: Vec) = {
    val c = add(car.pos, car.vel)
    val validFields = for {
      x <- -1 to 1
      y <- -1 to 1
    } yield add(c, (x, y))

    println("car.token: " + car.token + " || car.pos: " + car.pos + " || new pos: " + newPos + " || car.vec: " + car.vel)

    if (validFields contains newPos) {
      CarData.update(car.playerName, car.token, newPos, sub(newPos, car.pos))
      /*
      PlayerData.get(car.token) match {
        case Some(p) => {
          val m = new UpdateMessage
          m.add("player", p.name)
          m.add("pos", newPos)
          game.addUpdate(m)
          (newPos, "Moved car to new position.")
        }
        case None => (car.pos, "Player does not exist.")
      }
      */
    }
  }

  def add(a: Vec, b: Vec): Vec = (a._1 + b._1, a._2 + b._2)

  def sub(a: Vec, b: Vec): Vec = (a._1 - b._1, a._2 - b._2)

  /*
  post("/car/move/:token") {
    def moveCar(car: Car) = {
      val req = parsedBody.extract[MoveRequest]
      val res = Controller.move(car, (req.x, req.y))
      MoveResponse(x = res._1._1, y = res._1._2, message = res._2)
    }

    println("DEBUG (route '/move/:token'): " + params("token"))

    CarData.get(params("token")) match {
      case Some(car) => Ok(moveCar(car))
      case None => NotFound(ErrorResponse("The token has not been found in the car list. Probably the game has not started yet."))
    }
  }
  */

  /**
    * starts a game with a selection of players and a track.
    * todo: assigns random start positions to the cars (on the start line)
    */
  def start = Action {
    // val req = parsedBody.extract[StartRequest]
    // println("DEBUG (route '/start'): " + req.player)
    // val info = startGame(req.player, req.track)

    for (p <- PlayerData.all) CarData.add(p.name, p.token, (0, 0))
    Redirect(routes.GameController.index())
  }

}
