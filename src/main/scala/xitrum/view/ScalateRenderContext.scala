package xitrum.view

import java.io.{ PrintWriter, StringWriter, Writer }
import java.util.Locale

import org.fusesource.scalate.{ DefaultRenderContext, TemplateEngine => STE }

import xitrum.Action

class ScalateRenderContext(_requestUri: String,
                           override val engine: STE,
                           currentAction: Action,
                           templateType: String,
                           val buffer: Writer = new StringWriter)
    extends DefaultRenderContext(_requestUri, engine, new PrintWriter(buffer)) {
  override def locale: Locale = currentAction.locale

  override def view(model: AnyRef, viewName: String = "index"): Unit = {
    if (model == null) {
      throw new NullPointerException("No model object given!")
    }

    val template = ScalateEngine.viewTemplate(model, engine, viewName, templateType)
    using(model) {
      withUri(template.getClass.getName) {
        template.render(this)
      }
    }
  }
}
