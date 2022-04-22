package example

import caliban.ZHttpAdapter
import example.CategoryModel.CategoryRepository
import example.ExpenseModel.ExpenseRepository
import example.TagModel.TagRepository
import zhttp.http.Middleware.cors
import zhttp.http._
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio._

object Backend extends App {

  lazy val corsConfig: CorsConfig =
    CorsConfig(
      allowedOrigins = _ == "http://127.0.0.1:3000",
      allowedMethods = Some(Set(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.OPTIONS))
    )

  lazy val program = for {
    interpreter <- GraphQLModel.api.interpreter
    _ <- Server
      .start(
        8088,
        Http.route[Request] { case _ -> !! / "api" / "graphql" =>
          ZHttpAdapter.makeHttpService(interpreter)
        } @@ cors(corsConfig)
      )
      .forever
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .provideLayer(TagRepository.test ++ CategoryRepository.test ++ ExpenseRepository.test)
      .exitCode
  }

}
