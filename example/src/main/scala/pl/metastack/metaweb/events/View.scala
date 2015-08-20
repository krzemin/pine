package pl.metastack.metaweb.events

import org.scalajs.dom

import pl.metastack.metarx.Var

import pl.metastack.metaweb
import pl.metastack.metaweb._

class View extends metaweb.View {
  def replace(event: dom.MouseEvent, v: Var[String]) {
    v := "Coordinates: " + (event.clientX, event.clientY)
  }

  val v = Var("")

  val view =
    html"""
       <div>
         <h1>MetaWeb example</h1>
         <input type="text" value=$v />
         <button onclick="${(e: dom.MouseEvent) => replace(e, v)}">Replace</button><br />
         <a href="#/numberguess">Number guess</a>
       </div>
      """
}