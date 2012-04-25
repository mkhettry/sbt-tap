import sbt._
import org.scalatools.testing.{Event => TEvent, Result => TResult}

import java.util.concurrent.atomic.AtomicInteger
import java.io.{File, FileWriter}

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
    println("doInit called in sbt tap plugin")
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
          // TODO: for exceptions, write stack trace to tap file.
        case TResult.Skipped =>
          // it doesn't look like this framework distinguishes between pending and ignored.
          writeTapFields("ok", testId.incrementAndGet(), "#", "skip", e.testName())
      }
    }
  }

  override def doComplete(finalResult: TestResult.Value) {
    writeTapFields("1.." + testId.get)
    fileWriter.close()
  }

  private def writeTapFields(s: Any*) { fileWriter.write(s.mkString("",  " ", "\n")) }

  def endGroup(name: String, t: Throwable) { }

  def endGroup(name: String, result: TestResult.Value) { }
}
