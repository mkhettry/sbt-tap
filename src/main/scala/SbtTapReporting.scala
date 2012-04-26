import java.io.{PrintWriter, StringWriter, File, FileWriter}
import sbt._
import org.scalatools.testing.{Event => TEvent, Result => TResult}

import java.util.concurrent.atomic.AtomicInteger

object SbtTapReporting extends Plugin {
  lazy val tapListener = new SbtTapListener
}

/**
 * Listens to sbt test listener events and writes them to a tap compatible file. Results for all groups
 * go to a single file although it might be desirable to generate one tap file per group.
 * <p>
 * sbt runs tests in parallel and the protocol does not seem to provide a way to match a group to a test event. It
 * does look line one thread calls startGroup/testEvent/endGroup sequentially and using thread local to keep
 * the current active group might be one way to go.
 */
class SbtTapListener extends TestsListener {
  var testId = new AtomicInteger(0)
  var fileWriter: FileWriter = _

  override def doInit {
    new File("test-results").mkdirs()

    fileWriter = new FileWriter("test-results/test.tap")
  }

  def startGroup(name: String) {}

  def testEvent(event: TestEvent) {
    event.detail.foreach { e: TEvent =>
      e.result match {
        case TResult.Success => writeTapFields("ok", testId.incrementAndGet(), "-", e.testName())
        case TResult.Error | TResult.Failure =>
          writeTapFields("not ok", testId.incrementAndGet(), "-", e.testName())
          // According to the TAP spec, as long as there is any kind of whitespace, this output should belong to the
          // the test that failed and it should get displayed in the UI.
          // TODO:It would be nice if we could report the exact line in the test where this happened.
          writeTapFields(" ", stackTraceForError(e.error()))
        case TResult.Skipped =>
          // it doesn't look like this framework distinguishes between pending and ignored.
          writeTapFields("ok", testId.incrementAndGet(), e.testName(), "#", "skip", e.testName())
      }
    }
  }

  override def doComplete(finalResult: TestResult.Value) {
    writeTapFields("1.." + testId.get)
    fileWriter.close()
  }

  private def writeTapFields(s: Any*) { fileWriter.write(s.mkString("",  " ", "\n")) }

  private def stackTraceForError(t: Throwable): String = {
    val sw = new StringWriter()
    val printWriter = new PrintWriter(sw)
    t.printStackTrace(printWriter)
    sw.toString
  }
  def endGroup(name: String, t: Throwable) { }

  def endGroup(name: String, result: TestResult.Value) { }
}
