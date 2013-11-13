import java.io.{PrintWriter, StringWriter, FileWriter}
import sbt._
import sbt.testing.{Event => TEvent, Status => TStatus, OptionalThrowable}

import java.util.concurrent.atomic.AtomicInteger

object SbtTapReporting extends Plugin {
  def apply() = new SbtTapListener
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
      e.status match {
        case TStatus.Success => writeTapFields("ok", testId.incrementAndGet(), "-", e.fullyQualifiedName())
        case TStatus.Error | TStatus.Failure =>
          writeTapFields("not ok", testId.incrementAndGet(), "-", e.fullyQualifiedName())
          // According to the TAP spec, as long as there is any kind of whitespace, this output should belong to the
          // the test that failed and it should get displayed in the UI.
          // TODO:It would be nice if we could report the exact line in the test where this happened.
          writeTapFields(" ", stackTraceForError(e.throwable()))
        case TStatus.Skipped | TStatus.Ignored | TStatus.Canceled =>
          // it doesn't look like this framework distinguishes between pending and ignored.
          writeTapFields("ok", testId.incrementAndGet(), e.fullyQualifiedName(), "#", "skip", e.fullyQualifiedName())
        case TStatus.Pending =>
          // it doesn't look like this framework distinguishes between pending and ignored.
          writeTapFields("not ok", testId.incrementAndGet(), e.fullyQualifiedName(), "#", "TODO", e.fullyQualifiedName())

      }
    }
  }

  override def doComplete(finalResult: TestResult.Value) {
    writeTapFields("1.." + testId.get)
    fileWriter.close()
  }

  private def writeTapFields(s: Any*) { fileWriter.write(s.mkString("",  " ", "\n")) }

  private def stackTraceForError(t: OptionalThrowable): String = {
    if(!t.isEmpty) {
      val sw = new StringWriter()
      val printWriter = new PrintWriter(sw)
      t.get.printStackTrace(printWriter)
      sw.toString
    } else {
      ""
    }
  }
  def endGroup(name: String, t: Throwable) { }

  def endGroup(name: String, result: TestResult.Value) { }
}
