package net.lifthub {
package lib {

import java.io._

object FileUtils {

  def printToFile(out: Writer)(op: PrintWriter => Unit): Boolean = {
    val p = new java.io.PrintWriter(out)
    //Methods in PrintWriter never throw I/O exceptions,
    //although some of its constructors may.
    op(p)
    p.close()
    !p.checkError
  }

  def printToFile(f: File)(op: PrintWriter => Unit): Boolean = {
    try {
      // This may throw FileNotFoundException
      val w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))
      printToFile(w)(op)
    } catch {
      case e: FileNotFoundException => { e.printStackTrace; false }
    }
  }
  implicit def string2file(s: String): File = new File(s)

}


}
}
