package build

import mill.*
import mill.scalalib.*
import mill.scalalib.publish.*

object jetquerious extends ScalaModule with PublishModule {

  def scalaVersion = "3.6.4"

  def javacOptions = Seq("25")

  def scalacOptions = Seq("-Werror")

  def mvnDeps = Seq(
    mvn"org.scala-lang::scala3-library:3.6.4"
  )

  def publishVersion = "1.0.7"

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

  def runMetaGen = Task {
    val compiled = compile()

    val cp = (
      compileClasspath().map(_.path) :+
      compiled.classes.path
    ).mkString(java.io.File.pathSeparator)

    os.proc(
      "java",
      "-cp", cp,
      "-Djetquerious.packages=io.github.hacihaciyev.types",
      "io.github.hacihaciyev.types.internal.MetaGen"
    ).call(stdout = os.Inherit, stderr = os.Inherit)

    compiled
  }

  override def compile = super.compile

  def build = Task {
    runMetaGen()
  }

  object test extends ScalaTests {
  
    def mvnDeps = Seq(
      mvn"org.assertj:assertj-core:4.0.0-M1",
      mvn"org.junit.jupiter:junit-jupiter-api:5.13.0-M2",
      mvn"org.junit.jupiter:junit-jupiter-params:5.13.0-M2",
      mvn"org.mockito:mockito-core:5.18.0",
      mvn"org.postgresql:postgresql:42.7.5",
      mvn"org.testcontainers:testcontainers:1.21.3",
      mvn"org.testcontainers:postgresql:1.21.3",
      mvn"net.aichler:jupiter-interface:0.11.1"
    )
  
    def testFramework = "net.aichler.jupiter.api.JupiterFramework"
  
    def generateTestMeta = Task {
      val cp = runClasspath().map(_.path)
        .mkString(java.io.File.pathSeparator)
  
      os.proc(
        "java",
        "--enable-preview",
        "-cp", cp,
        "-Djetquerious.packages=io.github.hacihaciyev.types",
        "io.github.hacihaciyev.types.internal.MetaGen"
      ).call(stdout = os.Inherit, stderr = os.Inherit)
    }
  
    def runMeta = Task {
      generateTestMeta()
    }
  }
}