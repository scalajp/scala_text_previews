scalaVersion := "2.13.15"

crossScalaVersions += "3.6.2"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "4.3.2",
  "org.mindrot"     %  "jbcrypt"     % "0.4"
)
