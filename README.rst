Xitrum allows you to choose template engines.
This library is a template engine for Xitrum.
It wraps `Scalate <http://scalate.fusesource.org/>`_.

Config your Xitrum project to use this template engine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Edit your Xitrum project's project/plugins.sbt:

::

  // For compiling Scalate templates
  addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

Edit build.sbt:

::

  // Scalate template engine config for Xitrum
  // "import" must be at top of build.sbt, or SBT will complain
  import ScalateKeys._

  // Template engine for Xitrum
  libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "1.3"

  // Precompile Scalate
  seq(scalateSettings:_*)

  scalateTemplateConfig in Compile := Seq(TemplateConfig(
    file("src") / "main" / "scalate",
    Seq(),
    Seq(Binding("helper", "xitrum.Controller", true))
  ))

Edit xitrum.conf:

::

  xitrum {
    ...
    template {
      engine = xitrum.view.Scalate
      scalateDefaultType = jade  # jade, mustache, scaml, or ssp
    }
    ...
  }

scalateDefaultType
~~~~~~~~~~~~~~~~~~

In xitrum.conf, you config "scalateDefaultType" (see above).

When calling Xitrum's renderView method, if you want to use template type other
than scalateDefaultType, set the last argument (options) like this:

::

   renderView(Map("type" -> "mustache")
