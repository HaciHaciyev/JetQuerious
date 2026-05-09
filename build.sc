package build

import mill.*
import mill.scalalib.*
import mill.scalalib.publish.*

object jetquerious extends ScalaModule with PublishModule {

    def pomSettings                 = PomSettings(
        description     = "JetQuerious is a lightweight, high-performance, and developer-friendly library for working with JDBC and SQL in Java.",
        organization    = "io.github.hacihaciyev",
        url             = "https://github.com/HaciHaciyev/JetQuerious",
        licenses        = Seq(License.MIT),
        versionControl  = VersionControl.github("HaciHaciyev", "JetQuerious"),
        developers      = Seq(
            Developer("hadzhy", "Hadzhyiev Hadzhy", "https://github.com/HaciHaciyev")
        )
    )

    def publishVersion              = "1.0.7"
  
    def scalaVersion                = "3.8.3"

    def javacOptions                = Seq("--release", "25")

    def scalacOptions               = Seq("-Werror")

    def mvnDeps                     = Seq(mvn"org.scala-lang::scala3-library:3.8.3")

    override def sources            = Task.Sources(
        moduleDir / os.up / "src" / "main" / "java",
        moduleDir / os.up / "src" / "main" / "scala"
    )
  
    def metaGen                     = Task {
        val mainClasses = compile().classes.path
        val testClasses = this.test.compile().classes.path
        val deps        = compileClasspath().map(_.path) ++ this.test.compileClasspath().map(_.path)
        val cp          = (deps :+ mainClasses :+ testClasses).mkString(java.io.File.pathSeparator)
    
        os.proc(
            sys.props("java.home") + "/bin/java",
            "-Djetquerious.packages=io.github.hacihaciyev.types",
            s"-Djetquerious.output_dir=${mainClasses}",
            "-cp", cp,
            "io.github.hacihaciyev.types.internal.MetaGen",
            mainClasses.toString,
            testClasses.toString,
            os.pwd.toString
        ).call()
    }
    
    def build                       = Task {
        metaGen()
        test.testCached()
        ()
    }
    
    def testWithGen(args: String*)  = Task.Command {
        metaGen()
        test.testOnly(args*)()
        ()
    }
    
    def testOnly(args: String*)     = Task.Command {
        test.testOnly(args*)()
        ()
    }
    
    private object test extends ScalaTests {
        
        override def forkArgs       = Task {
            Seq(
                s"-Djetquerious.output_dir=${jetquerious.compile().classes.path}",
                "-Djetquerious.packages=io.github.hacihaciyev.types"
            )
        }

        override def sources        = Task.Sources(
            moduleDir / os.up / os.up / "src" / "test" / "java",
            moduleDir / os.up / os.up / "src" / "test" / "scala"
        )

        def mvnDeps = Seq(
          mvn"org.junit.jupiter:junit-jupiter-api:6.0.3",
          mvn"org.junit.jupiter:junit-jupiter-params:6.0.3",
          mvn"com.github.sbt.junit:jupiter-interface:0.18.0",
          mvn"net.jqwik:jqwik:1.9.3",        
          mvn"org.assertj:assertj-core:4.0.0-M1",
          mvn"org.mockito:mockito-core:5.23.0",
          mvn"org.postgresql:postgresql:42.7.11",
          mvn"org.testcontainers:testcontainers:2.0.5",
          mvn"org.testcontainers:postgresql:1.21.4"
        )

        def testFramework           = "com.github.sbt.junit.jupiter.api.JupiterFramework"
    }
}