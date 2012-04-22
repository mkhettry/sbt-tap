Provides reporting of test success and failure for tests run by
[simple build tool](https://github.com/harrah/xsbt)
in [TAP](http://en.wikipedia.org/wiki/Test_Anything_Protocol) format

All the test results will be generated in one file: test-results/test.tap

To use

1. Add this plugin to your sbt project. Create project/project/Plugins.scala that looks like this:

        import sbt._
        // sets up other project dependencies when building our root project
        object Plugins extends Build {
          lazy val root = Project("root", file(".")) dependsOn(tapListener)
          lazy val tapListener = RootProject(uri("git://github.com/mkhettry/sbt-tap.git"))
        }

2. In your build.sbt, add the SbtTapListener to the sequence of Test Listeners.

        testListeners += SbtTapReporting.tapListener

