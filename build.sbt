name := "Gimd"

organization := "com.google.gimd"

version := "1.0"

scalaVersion := "2.8.0"

mainClass in (Compile, run) := Some("Sample")

libraryDependencies ++= Seq("org.eclipse.jgit" % "org.eclipse.jgit" % "0.7.1",
                            "org.eclipse.jgit" % "org.eclipse.jgit" % "0.7.1" % "test",
                            "org.scala-tools.testing" % "scalacheck_2.8.0" % "1.7" % "test",
                            "com.novocode" % "junit-interface" % "0.4.0" % "test",
                            "commons-io" % "commons-io" % "1.3.2" % "test",
                            "org.apache.lucene" % "lucene-core" % "3.0.0",
                            "junit" % "junit" % "4.10" % "test")
