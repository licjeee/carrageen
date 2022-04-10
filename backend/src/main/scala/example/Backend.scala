package example

import caliban.ZHttpAdapter
import example.CategoryModel.CategoryRepository
import example.ExpenseModel.ExpenseRepository
import example.TagModel.TagRepository
import zhttp.http._
import zio._
import zio.magic._

object Backend extends App {

  val app =
    Http.fromEffect {
      for {
        interpreter <- GraphQLModel.api.interpreter.orDie
      } yield Http.route[Request] { case _ -> Root / "api" / "graphql" =>
        ZHttpAdapter.makeHttpService(interpreter)
      }
    }.flatten

  val program = for {
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- zhttp.service.Server.start(port, CORS(app))
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .injectCustom(TagRepository.test, CategoryRepository.test, ExpenseRepository.test)
      .exitCode
  }

}
