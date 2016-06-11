package pl.metastack.metaweb.macros

import scala.language.experimental.macros
import scala.language.reflectiveCalls

import scala.collection.mutable

import scala.reflect.macros.blackbox.Context

import scala.xml.XML

import pl.metastack.metaweb.tree

object InlineHtml {
  trait Implicit {
    implicit class HtmlString(sc: StringContext) {
      def html(args: Any*): tree.Tag = macro HtmlImpl
    }
  }

  def iter(c: Context)(node: scala.xml.Node,
                       args: Seq[c.Expr[Any]],
                       root: Boolean): Seq[c.Expr[Seq[tree.Node]]] = {
    import c.universe._

    (node.label, Option(node.prefix)) match {
      case ("#PCDATA", _) =>
        // TODO Find a better solution
        val parts = node.text.replaceAll("""\$\{\d+\}""", "_$0_").split("_").toSeq

        parts.map { v =>
          if (!v.startsWith("${") || !v.endsWith("}"))
            c.Expr(q"Seq(pl.metastack.metaweb.tree.Text($v))")
          else {
            val index = v.drop(2).init.toInt

            args(index) match {
              // TODO Do proper type checking
              case n: c.Expr[tree.Node]
                if n.tree.tpe.toString == "pl.metastack.metaweb.tree.Node" =>
                c.Expr(q"Seq($n)")
              case n: c.Expr[tree.Tag]
                if n.tree.tpe.toString == "pl.metastack.metaweb.tree.Tag" =>
                c.Expr(q"Seq($n)")

              case n: c.Expr[Seq[tree.Node]]
                if n.tree.tpe.toString == "Seq[pl.metastack.metaweb.tree.Node]" =>
                n
              case n: c.Expr[Seq[tree.Tag]]
                if n.tree.tpe.toString == "Seq[pl.metastack.metaweb.tree.Tag]" =>
                n

              case n: c.Expr[String]
                if n.tree.tpe.toString == "String" =>
                  c.Expr(q"Seq(pl.metastack.metaweb.tree.Text($n))")

              case n =>
                c.error(c.enclosingPosition, s"Type ${n.tree.tpe} (${n.tree.symbol}) not supported")
                null
            }
          }
        }

      case (tag, prefix) =>
        val tagName = prefix.map(pfx => s"$pfx:$tag").getOrElse(tag)
        val rootAttributes: Map[String, String] =
          if (root) Helpers.namespaceBinding(node.scope)
          else Map.empty

        val tagType = TypeName(tag.capitalize)
        val tagAttrs = mutable.ArrayBuffer.empty[c.Expr[Option[(String, Any)]]]
        val tagEvents = mutable.ArrayBuffer.empty[c.Expr[(String, Seq[Any] => Unit)]]

        (node.attributes.asAttrMap ++ rootAttributes).foreach { case (k, v) =>
          if (!v.startsWith("${") || !v.endsWith("}")) {
            if (k.startsWith("on")) tagEvents += c.Expr(q"${k.drop(2)} -> ((_: Any) => { $v; () })")
            else tagAttrs += c.Expr(q"Some($k -> $v)")
          } else {
            val index = v.drop(2).init.toInt

            if (k.startsWith("on")) {
              if (args(index).actualType.toString == "Any => Unit")  // TODO Don't rely on string comparison
                tagEvents += c.Expr(q"${k.drop(2)} -> ${args(index)}")
              else  // Enforce lazy evaluation
                tagEvents += c.Expr(q"${k.drop(2)} -> ((_: Any) => { ${args(index)}; () })")
            } else {
              args(index) match {
                case a: c.Expr[String]
                  if a.tree.tpe.toString == "String" =>
                  tagAttrs += c.Expr(q"Some($k -> $a)")

                case a: c.Expr[Option[String]]
                  if a.tree.tpe.toString == "Option[String]" ||
                     a.tree.tpe.toString == "Some[String]" ||
                     a.tree.tpe.toString == "None.type" =>
                  tagAttrs += c.Expr(q"$a.map(v => $k -> v)")

                case a =>
                  c.error(c.enclosingPosition, s"Type of $a not supported")
                  null
              }
            }
          }
        }

        val tagChildren = node.child.flatMap(n => iter(c)(n, args, root = false))
        val qAttrs = q"Seq[Option[(String, String)]](..$tagAttrs).collect { case Some(x) => x }.toMap"

        try {
          c.typecheck(q"val x: pl.metastack.metaweb.tag.$tagType")
          Seq(c.Expr(q"Seq(new pl.metastack.metaweb.tag.$tagType($qAttrs, Seq(..$tagChildren).flatten))"))
        } catch { case t: Throwable =>
          Seq(c.Expr(q"Seq(pl.metastack.metaweb.tag.HTMLTag.fromTag($tagName, $qAttrs, Seq(..$tagChildren).flatten))"))
        }
    }
  }

  def insertPlaceholders(c: Context)(parts: Seq[c.universe.Tree]): String = {
    parts.zipWithIndex.map { case (tree, i) =>
      val p = Helpers.literalValueTree[String](c)(tree)

      if (i == parts.length - 1) p
      else if (p.lastOption.contains('=')) p + "\"${" + i + "}\""
      else p + "${" + i + "}"
    }.mkString
  }

  def convert(c: Context)(parts: Seq[c.universe.Tree],
                          args: Seq[c.Expr[Any]]): c.Expr[tree.Tag] = {
    import c.universe._
    val html = insertPlaceholders(c)(parts)
    val xml = XML.loadString(html)
    val nodes = iter(c)(xml, args, root = true).head
    c.Expr(q"$nodes.head").asInstanceOf[c.Expr[tree.Tag]]
  }

  def HtmlImpl(c: Context)(args: c.Expr[Any]*): c.Expr[tree.Tag] = {
    import c.universe._

    c.prefix.tree match {
      case Apply(_, List(Apply(_, parts))) =>
        convert(c)(parts, args)
    }
  }
}
