package io.shiftleft.joern.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import javax.script.ScriptEngineManager
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import io.shiftleft.cpgserver.config.ServerConfiguration
import io.shiftleft.cpgserver.query.DefaultCpgQueryExecutor
import io.shiftleft.cpgserver.route.{CpgRoute, HttpErrorHandler, SwaggerRoute}
import io.shiftleft.joern.server.cpg.JoernCpgProvider

object JoernServer extends IOApp {

  private val cpgProvider: JoernCpgProvider =
    new JoernCpgProvider

  private val cpgQueryExecutor: DefaultCpgQueryExecutor =
    new DefaultCpgQueryExecutor(new ScriptEngineManager)

  private implicit val httpErrorHandler: HttpErrorHandler =
    CpgRoute.CpgHttpErrorHandler

  private val serverConfig: ServerConfiguration =
    ServerConfiguration.config.getOrElse(ServerConfiguration("127.0.0.1", 8080))

  private val httpRoutes: HttpRoutes[IO] =
    CpgRoute[String](cpgProvider, cpgQueryExecutor).routes <+> SwaggerRoute().routes

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(serverConfig.port, serverConfig.host)
      .withHttpApp(httpRoutes.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
