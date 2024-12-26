import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.0.1"
ThisBuild / organization     := "stoufexis"

val shapeless = "2.3.10"
val cats      = "2.9.0"

githubOwner       := "stoufexis"
githubRepository  := "envy"
githubTokenSource := TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")

lazy val root = (project in file("."))
  .settings( name := "envy"
           , libraryDependencies ++= Seq( scalaTest                      % Test
                                        , "com.chuusai"   %% "shapeless" % shapeless
                                        , "org.typelevel" %% "cats-core" % cats
                                        ),
           )
