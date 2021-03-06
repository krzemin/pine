package pine.macros

import scala.collection.mutable

import scala.language.reflectiveCalls
import scala.language.experimental.macros

import scala.reflect.macros.blackbox.Context

import pine._
import pine.internal.HtmlParser

object InlineHtml {
  trait Implicit {
    implicit class HtmlString(sc: StringContext) {
      def html(args: Any*): Tag[Singleton] = macro HtmlImpl
    }
  }

  def iter(c: Context)(node: Node,
                       args: Seq[c.Expr[Any]],
                       root: Boolean): Seq[c.Expr[Seq[Node]]] = {
    import c.universe._

    val integerType = definitions.IntTpe
    val booleanType = definitions.BooleanTpe
    val stringType = definitions.StringClass.toType
    val optionStringType =
      appliedType(definitions.OptionClass, List(stringType))
    val nodeType = c.mirror.staticClass("pine.Node")
       .toType
    val seqType = c.mirror.staticClass("scala.collection.Seq")
    val seqNodeType = appliedType(seqType, List(nodeType))

    node match {
      case Text(text) =>
        // TODO Find a better solution
        val parts = text.replaceAll("""\$\{\d+\}""", "_$0_").split("_").toSeq

        parts.map { v =>
          if (!v.startsWith("${") || !v.endsWith("}"))
            c.Expr(q"Seq(pine.Text($v))")
          else {
            val index = v.drop(2).init.toInt

            args(index) match {
              case n if n.tree.tpe <:< nodeType => c.Expr(q"Seq($n)")
              case n if n.tree.tpe <:< seqNodeType =>
                n.asInstanceOf[c.Expr[Seq[Node]]]
              case n if n.tree.tpe =:= integerType ||
                        n.tree.tpe =:= booleanType =>
                c.Expr(q"Seq(pine.Text($n.toString))")
              case n if n.tree.tpe =:= stringType =>
                c.Expr(q"Seq(pine.Text($n))")
              case n =>
                c.error(c.enclosingPosition, s"Type ${n.tree.tpe} (${n.tree.symbol}) not supported")
                null
            }
          }
        }

      case tag @ Tag(_, _, _) =>
        val tagAttrs = mutable.ArrayBuffer.empty[c.Expr[Option[(String, Any)]]]

        tag.attributes.mapValues(_.toString).foreach { case (k, v) =>
          if (!v.startsWith("${") || !v.endsWith("}"))
            tagAttrs += c.Expr(q"Some($k -> $v)")
          else {
            val index = v.drop(2).init.toInt

            args(index) match {
              case a if a.tree.tpe =:= stringType =>
                tagAttrs += c.Expr(q"Some($k -> $a)")
              case a if a.tree.tpe <:< optionStringType =>
                tagAttrs += c.Expr(q"$a.map($k -> _)")
              case a =>
                c.error(c.enclosingPosition, s"Type ${a.tree.tpe} (${a.tree.symbol}) not supported")
                null
            }
          }
        }

        val tagChildren = tag.children.flatMap(n => iter(c)(n, args, root = false))
        val qAttrs = q"Seq[Option[(String, String)]](..$tagAttrs).collect { case Some(x) => x }.toMap"
        Seq(c.Expr(q"Seq(pine.Tag(${tag.tagName}, $qAttrs, Seq(..$tagChildren).flatten))"))
    }
  }

  def insertPlaceholders(c: Context)(parts: Seq[c.universe.Tree]): String =
    parts.zipWithIndex.map { case (tree, i) =>
      val p = Helpers.literalValueTree[String](c)(tree)

      if (i == parts.length - 1) p
      else if (p.lastOption.contains('=')) p + "\"${" + i + "}\""
      else p + "${" + i + "}"
    }.mkString

  def convert(c: Context)(parts: Seq[c.universe.Tree],
                          args: Seq[c.Expr[Any]]): c.Expr[Tag[Singleton]] = {
    import c.universe._
    val html = insertPlaceholders(c)(parts)
    val node = HtmlParser.fromString(html)
    val nodes = iter(c)(node, args, root = true).head
    c.Expr(q"$nodes.head.asInstanceOf[pine.Tag[Singleton]]")
  }

  def HtmlImpl(c: Context)(args: c.Expr[Any]*): c.Expr[Tag[Singleton]] = {
    import c.universe._

    c.prefix.tree match {
      case Apply(_, List(Apply(_, parts))) =>
        convert(c)(parts, args)
    }
  }
}
