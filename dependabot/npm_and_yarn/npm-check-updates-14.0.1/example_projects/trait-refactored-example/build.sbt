scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "4.0.0",
  "org.mindrot"     %  "jbcrypt"     % "0.4",
  "org.scalatest"   %% "scalatest"   % "3.2.12" % "test"
)
