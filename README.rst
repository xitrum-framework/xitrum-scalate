Xitrum allows you to choose template engines.
This library is a template engine for Xitrum.
It wraps Scalate.

Config your Xitrum project to use this template engine
------------------------------------------------------

Edit your Xitrum project's project/plugins.sbt:

::

  // For compiling Scalate templates
  addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.4.2")

Edit build.sbt to use the above plugin and this template engine:

::

  // "import" must be at top of build.sbt, or SBT will complain
  import ScalateKeys._

  // Template engine for Xitrum
  libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "1.0"

  // Precompile Scalate
  seq(scalateSettings:_*)

  scalateTemplateConfig in Compile := Seq(TemplateConfig(
    file("src") / "main" / "scalate",  // See config/scalate.conf
    Seq(),
    Seq(Binding("helper", "xitrum.Controller", true))
  ))

Add config/scalate.conf file:

::

  scalate {
    defaultType = jade              # jade, mustache, scaml, or ssp
    dir         = src/main/scalate  # Only used in development mode
  }

Edit config/application.conf to include the file above:

::

  include "scalate"

Edit xitrum.conf to use this template engine:

::

  templateEngine = xitrum.view.ScalateTemplateEngine

defaultType
-----------

In scalate.conf, you config "defaultType".

When calling Xitrum's renderView method, if you want to use template type other
than defaultType, set the last argument (options) like this:

::

   renderView(Map("type" -> "mustache")
