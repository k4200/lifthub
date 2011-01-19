package net.lifthub {
package lib {

import org.specs._

import java.io._

object FileUtilsSpec extends Specification {
  import FileUtils._

  "FileUtils" can {
    val fileName = "/tmp/foo.txt"
    val file = new File(fileName)

    "write to a file" in {
      FileUtils.printToFile(file)(writer => {
	writer.println("Hello World!")
      })
      file.exists mustBe true
    }
  }
  "FileUtils" should {
    val writeProtectedFile = new File("/root/foo.txt")
    "fail when passed an invalid file" in {
      FileUtils.printToFile(writeProtectedFile)(writer => {
	writer.println("Hello World!")
      }) mustBe false
    }
  }

}

}
}
