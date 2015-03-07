`Xitrum <http://xitrum-framework.github.io/>`_ allows you to choose template engines.
This library is a template engine for Xitrum.
It wraps `Scalate <http://scalate.fusesource.org/>`_.

See CHANGELOG to know which version of xitrum-scalate to use for which version
of Xitrum.

Config your Xitrum project to use this template engine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Edit your Xitrum project's project/plugins.sbt:

::

  // For precompiling Scalate templates
  addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")

Edit build.sbt:

::

  // Template engine for Xitrum
  libraryDependencies += "tv.cntt" %% "xitrum-scalate" % "2.3"

  // Precompile Scalate templates
  seq(scalateSettings:_*)

  ScalateKeys.scalateTemplateConfig in Compile := Seq(TemplateConfig(
    baseDirectory.value / "src" / "main" / "scalate",
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

"type" option
~~~~~~~~~~~~~

In xitrum.conf, you config "defaultType" (see above).

When calling Xitrum's renderView method, if you want to use template type other
than the defaultType, set the last argument (options) like this:

::

   renderView(Map("type" -> "mustache")

"date" option
~~~~~~~~~~~~~

If you don't specify `DateFormat <http://docs.oracle.com/javase/7/docs/api/java/text/DateFormat.html>`_,
``java.text.DateFormat.getDateInstance(DateFormat.DEFAULT, lo)`` will be used,
where ``lo`` is ``java.util.Locale.forLanguageTag(currentAction.language)``.

::

  renderView(Map("date" -> myDateFormat)

"number" option
~~~~~~~~~~~~~~~

If you don't specify `NumberFormat <http://docs.oracle.com/javase/7/docs/api/java/text/NumberFormat.html>`_,
``java.text.NumberFormat.getInstance(lo)`` will be used,
where ``lo`` is ``java.util.Locale.forLanguageTag(currentAction.language)``.

::

  renderView(Map("date" -> myNumberFormat)

Other utility methods
~~~~~~~~~~~~~~~~~~~~~

xitrum.view.Scalate also provides some utility methods so that you can easily
use Scalate features. See the `API doc <http://xitrum-framework.github.io/xitrum-scalate/>`_.

Ex:

::

  import xitrum.Config
  import xitrum.view.Scalate

  // In your action:
  val scalate  = Config.xitrum.template.get.asInstanceOf[Scalate]
  val template = "p This Jade template is from a string, not from a file."
  val string   = scalate.renderJadeString(template)
  respondInlineView(string)

Log
~~~

To avoid unnecessary log, you should add these to config/logback.xml:

::

  <logger name="org.fusesource.scalate" level="INFO"/>
  <logger name="org.fusesource.scalate.util.ClassPathBuilder" level="ERROR"/>
