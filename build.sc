package build

import mill.*
import mill.scalalib.*
import mill.scalalib.publish.*

object jetquerious extends ScalaModule with PublishModule {

  def pomSettings = PomSettings(
    description = "JetQuerious is a lightweight, high-performance, and developer-friendly library for working with JDBC and SQL in Java.",
    organization = "io.github.hacihaciyev",
    url = "https://github.com/HaciHaciyev/JetQuerious",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("HaciHaciyev", "JetQuerious"),
    developers = Seq(
      Developer("hadzhy", "Hadzhyiev Hadzhy", "https://github.com/HaciHaciyev")
    )
  )
  
  def publishVersion = "1.0.7"
  
  def scalaVersion = "3.6.4"

  def javacOptions = Seq("--release", "25")

  def scalacOptions = Seq("-Werror")

  def mvnDeps = Seq(mvn"org.scala-lang::scala3-library:3.6.4")
  
  override def sources = Task.Sources(
    moduleDir / os.up / "src" / "main" / "java",
    moduleDir / os.up / "src" / "main" / "scala"
  )

  override def compile = Task {
    val result = super.compile()
    val cp = (compileClasspath().map(_.path) :+ result.classes.path).mkString(java.io.File.pathSeparator)
    
    os.proc(
      sys.props("java.home") + "/bin/java",
      "-cp", cp,
      "-Djetquerious.packages=io.github.hacihaciyev.types",
      s"-Djetquerious.output_dir=${result.classes.path}",
      "io.github.hacihaciyev.types.internal.MetaGen"
    ).call(stdout = os.Inherit, stderr = os.Inherit)
    
    result
  }

  object test extends ScalaTests {

    override def sources = Task.Sources(
      moduleDir / os.up / os.up / "src" / "test" / "java",
      moduleDir / os.up / os.up / "src" / "test" / "scala"
    )

    def mvnDeps = Seq(
      mvn"org.assertj:assertj-core:4.0.0-M1",
      mvn"com.github.sbt.junit:jupiter-interface:0.18.0",
      mvn"org.junit.jupiter:junit-jupiter-api:5.13.0-M2",
      mvn"org.junit.jupiter:junit-jupiter-params:5.13.0-M2",
      mvn"org.mockito:mockito-core:5.18.0",
      mvn"org.postgresql:postgresql:42.7.5",
      mvn"org.testcontainers:testcontainers:1.21.3",
      mvn"org.testcontainers:postgresql:1.21.3",
      mvn"net.aichler:jupiter-interface:0.11.1"
    )

    def testFramework = "com.github.sbt.junit.jupiter.api.JupiterFramework"
  }
}