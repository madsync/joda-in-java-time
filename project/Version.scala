object Version {

  import scala.sys.process._

  val ScalaVersionToUse = "2.12.7"

  //
  //
  def commit: String = ("git rev-parse --short HEAD" !!).trim

  def branch: String = ("git rev-parse --abbrev-ref HEAD" !!).trim

  def hasUnCommitted: Boolean = ("git diff-index --quiet HEAD --" !) != 0

  lazy val libraryDateVersioning: String = {
    val major: Int = sys.env.getOrElse("MAJOR_VERSION", "4").trim.toInt
    val minor: Int = sys.env.getOrElse("MINOR_VERSION", "0").trim.toInt
    val patchFromDate: String = new java.text.SimpleDateFormat("yyMMdd.HH").format(new java.util.Date())
    val versionFile = "version.txt"

    val value = s"$major.$minor.$patchFromDate"

    // write to a file
    import java.io._
    val pw = new PrintWriter(new File(versionFile))
    pw.write(value)
    pw.close()

    // return the value
    value
  }

}