enablePlugins(JavaAppPackaging)

enablePlugins(ClasspathJarPlugin)

val packageZip = taskKey[File]("package-zip")

packageZip := (baseDirectory in Compile).value / "target" / "universal" / (name.value + "-" + version.value + ".zip")

artifact in (Universal, packageZip) ~= { (art:Artifact) => art.copy(`type` = "zip", extension = "zip") }

addArtifact(artifact in (Universal, packageZip), packageZip in Universal)

publish := ((publish) dependsOn (packageBin in Universal)).value

publishM2 := ((publishM2) dependsOn (packageBin in Universal)).value

publishLocal := ((publishLocal) dependsOn (packageBin in Universal)).value

PgpKeys.publishSigned := ((PgpKeys.publishSigned) dependsOn (packageBin in Universal)).value

mappings in Universal += file("README.md") -> "README.txt"

mappings in Universal += file("LICENSE") -> "LICENSE.txt"

mappings in Universal += file("NOTICE") -> "NOTICE.txt"

mappings in Universal += file("LICENSE-THIRDPARTY") -> "LICENSE-THIRDPARTY.txt"

mappings in Universal += file("CHANGELOG") -> "CHANGELOG.txt"

mappings in Universal <++= (packageBin in Compile, target ) map { (_, target) =>
  val dir = file("./features")
  (dir.***) pair relativeTo(dir.getParentFile)
}

mappings in Universal <++= (packageBin in Compile, target ) map { (_, target) =>
  val dir = file("./features-data")
  (dir.***) pair relativeTo(dir.getParentFile)
}

val bashClasspathPattern = "declare -r app_classpath=\"(.*)\"\n".r

bashScriptDefines := bashScriptDefines.value.map {
  case bashClasspathPattern(classpath) => "declare -r app_classpath=\"$GWEN_CLASSPATH:$SELENIUM_HOME/*:$SELENIUM_HOME/libs/*:" + classpath + "\"\n"
  case _@entry => entry
}

batScriptExtraDefines += """set "APP_CLASSPATH=%GWEN_CLASSPATH%;%SELENIUM_HOME%\*;%SELENIUM_HOME%\libs\*;%APP_CLASSPATH%""""
