package xitrum.view

import xitrum.{Action, Config}

/**
 * This class is intended for use only by Xitrum. Apps that want to create
 * additional Scalate template engine instances can use [[ScalateEngine]].
 */
class Scalate extends ScalateEngine(
  ScalateEngine.DEV_TEMPLATE_DIR_URI,
  !Config.productionMode,
  Config.xitrum.config.getString("template.\"" + classOf[Scalate].getName + "\".defaultType")
) {
  override def start(): Unit = {
    // Scalate takes several seconds to initialize => Warm it up here

    val dummyAction = new Action {
      def execute(): Unit = {}
    }

    renderJadeString("")(dummyAction)
    renderMustacheString("")(dummyAction)
    renderScamlString("")(dummyAction)
    renderSspString("")(dummyAction)

    // Can't warmup Scalate.renderTemplateFile:
    // https://github.com/xitrum-framework/xitrum-scalate/issues/6
  }
}
