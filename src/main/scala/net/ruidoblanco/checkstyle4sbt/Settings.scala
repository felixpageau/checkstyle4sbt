package net.ruidoblanco.checkstyle4sbt

import sbt._
import Keys._
import Project.Initialize

import ReportFormat._

import java.io.File

private[checkstyle4sbt] case class PathSettings(targetPath: File, reportName: String, configurationFile: File, propertiesFile: Option[File], sourcePath: File)

private[checkstyle4sbt] case class MiscSettings(reportFormat: ReportFormat)

private[checkstyle4sbt] trait Settings extends Plugin {

  val checkstyle = TaskKey[Unit]("checkstyle")

  val checkstyleCommandLine = TaskKey[List[String]]("checkstyle-command-line")

  val checkstyleClasspath = TaskKey[Classpath]("checkstyle-classpath")
  val checkstylePathSettings = TaskKey[PathSettings]("checkstyle-path-settings")
  val checkstyleMiscSettings = TaskKey[MiscSettings]("checkstyle-misc-settings")
  
  /** Output path for CheckStyle reports. Defaults to <code>target / "checkStyle"</code>. */
  val checkstyleTargetPath = SettingKey[File]("checkstyle-target-path")
  
  /** Name of the report file to generate. Defaults to <code>"checkstyle.xml"</code> */
  val checkstyleReportName = SettingKey[String]("checkstyle-report-name")

  /** Name of the configuration file to use. Defaults to <code>project / "checkstyle-config.xml"</code> */
  val checkstyleConfigurationFile = SettingKey[File]("checkstyle-configuration-file")

  /** Name of the properties file to use. Defaults to None */
  val checkstylePropertiesFile = SettingKey[Option[File]]("checkstyle-properties-file")

  /** The path to the source to be analyzed. Defaults to <code>javaSource</code>. */
  val checkstyleSourcePath = TaskKey[File]("checkstyle-source-path")

  /** Type of report to create. Defaults to <code>ReportFormat.Xml</code>. */
  val checkstyleReportFormat = SettingKey[ReportFormat]("checkstyle-report-format")

  protected def checkstyleTask(commandLine: List[String], streams: TaskStreams): Unit

  protected def checkstyleCommandLineTask(checkstyleClasspath: Classpath, paths: PathSettings, misc: MiscSettings, streams: TaskStreams): List[String]
  
  private val checkstyleConfig = config("checkstyle") hide
  
  val checkstyleSettings = Seq(
    ivyConfigurations += checkstyleConfig,
    libraryDependencies ++= Seq(
      "com.puppycrawl.tools" % "checkstyle" % "6.1.1" % "checkstyle->default"
    ),

    checkstyle <<= (checkstyleCommandLine, streams) map checkstyleTask,

    checkstyleCommandLine <<= (managedClasspath in checkstyleCommandLine, checkstylePathSettings, checkstyleMiscSettings, streams) map checkstyleCommandLineTask,
    checkstylePathSettings <<= (checkstyleTargetPath, checkstyleReportName, checkstyleConfigurationFile, checkstylePropertiesFile, checkstyleSourcePath) map PathSettings dependsOn (compile in Compile),
    checkstyleMiscSettings <<= (checkstyleReportFormat) map MiscSettings,

    managedClasspath in checkstyleCommandLine <<= (classpathTypes, update) map { 
      (ct, updateReport) => Classpaths.managedJars(checkstyleConfig, ct, updateReport)
    },

    checkstyleTargetPath <<= crossTarget(_ / "checkstyle"),
    checkstyleReportName := "checkstyle.xml",
    checkstyleReportFormat := ReportFormat.Xml,
    checkstyleConfigurationFile <<= baseDirectory(_ / "project" / "checkstyle-config.xml"),
    checkstylePropertiesFile := None,
    checkstyleSourcePath <<= javaSource in Compile map { (js) => js }
  )
}
