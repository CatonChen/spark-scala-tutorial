import sbt._
import sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object BuildSettings {

  val Name = "activator-spark"
  val Version = "2.1.0"
  val ScalaVersion = "2.10.4"

  lazy val buildSettings = Defaults.defaultSettings ++ Seq (
    name          := Name,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.typesafe",
    description   := "Activator Spark Template",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )
}


object Resolvers {
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatype = "Sonatype Release" at "https://oss.sonatype.org/content/repositories/releases"
  val mvnrepository = "MVN Repo" at "http://mvnrepository.com/artifact"

  val allResolvers = Seq(typesafe, sonatype, mvnrepository)

}

// We don't actually use all these dependencies, but they are shown for the
// examples that explicitly use Hadoop.
object Dependency {
  object Version {
    val Spark        = "1.1.0"
    val HadoopClient = "1.0.4" // "2.4.0"
    val ScalaTest    = "2.1.4"
    val ScalaCheck   = "1.11.3"
  }

  val sparkCore      = "org.apache.spark"  %% "spark-core"      % Version.Spark
  val sparkStreaming = "org.apache.spark"  %% "spark-streaming" % Version.Spark
  val sparkSQL       = "org.apache.spark"  %% "spark-sql"       % Version.Spark
  val sparkRepl      = "org.apache.spark"  %% "spark-repl"      % Version.Spark
  // Hack: explicitly add this dependency to workaround an Avro related bug
  // SPARK-1121? Appears to work! Otherwise, a java.lang.IncompatibleClassChangeError
  // is thrown in the call to saveAsParquetFile in SparkSQL9.scala.
  val hadoopClient   = "org.apache.hadoop"  % "hadoop-client"   % Version.HadoopClient

  val scalaTest      = "org.scalatest"     %% "scalatest"       % Version.ScalaTest  % "test"
  val scalaCheck     = "org.scalacheck"    %% "scalacheck"      % Version.ScalaCheck % "test"
}

object Dependencies {
  import Dependency._

  val activatorspark =
    Seq(sparkCore, sparkStreaming, sparkSQL, // sparkRepl, hadoopClient,
      scalaTest, scalaCheck)
}

object ActivatorSparkBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  val excludeSigFilesRE = """META-INF/.*\.(SF|DSA|RSA)""".r
  lazy val activatorspark = Project(
    id = "Activator-Spark",
    base = file("."),
    settings = buildSettings ++ assemblySettings ++ Seq(
      // runScriptSetting,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.activatorspark,
      excludeFilter in unmanagedSources := (HiddenFileFilter || "Intro1*" || "HiveSQL*" || "SparkSQLParquet*"),
      unmanagedResourceDirectories in Compile += baseDirectory.value / "conf",
      mainClass := Some("run"),
      // Must run Spark tests sequentially because they compete for port 4040!
      parallelExecution in Test := false,
      mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
        {
          // Trips up loading due to security errors:
          case excludeSigFilesRE(toss) => MergeStrategy.discard
          case "META-INF/MANIFEST.MF" => MergeStrategy.discard
          case x => MergeStrategy.first
        }
      }
      ))
}



