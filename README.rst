Xitrum allows you to choose template engines.
This library is a template engine for Xitrum.
It wraps `Scalate <http://scalate.fusesource.org/>`_.

See CHANGELOG to know which version of xitrum-scalate to use for which version
of Xitrum.

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
  libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "1.8"

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
      "xitrum.view.Scalate" {
        defaultType = jade  # jade, mustache, scaml, or ssp
      }
    }
    ...
  }

defaultType
~~~~~~~~~~~

In xitrum.conf, you config "defaultType" (see above).

When calling Xitrum's renderView method, if you want to use template type other
than the defaultType, set the last argument (options) like this:

::

   renderView(Map("type" -> "mustache")

Other utility methods
~~~~~~~~~~~~~~~~~~~~~

xitrum.view.Scalate also provides some utility methods so that you can easily
use Scalate features. See the `API doc <http://ngocdaothanh.github.io/xitrum-scalate/>`_.

Ex:

::

  import xitrum.Config
  import xitrum.view.Scalate

  // In your action:
  val scalate  = Config.xitrum.template.get.asInstanceOf[Scalate]
  val template = "p This Jade template is from a string, not from a file."
  val string   = scalate.renderJadeString(template)
  respondInlineView(string)
