package pine

import org.scalatest.FunSuite

class TagRefSpec extends FunSuite {
  test("Use DSL to set boolean attribute to true") {
    val node = tag.Input.id("test")

    val tagRef = TagRef["input"]("test")

    assert(!node.checked)

    val updated = node.update(implicit ctx => tagRef.checked.update(!_))
    assert(updated == node.checked(true))
  }

  test("Use DSL to set boolean attribute to false") {
    val node = tag.Input
      .id("test")
      .checked(true)

    val tagRef = TagRef[tag.Input]("test")

    assert(node.checked)

    val updated = node.update(implicit ctx => tagRef.checked.update(!_))
    assert(updated == node.checked(false))
  }

  test("Use DSL to set CSS tag") {
    val node = tag.Input
      .id("test")
      .`type`("checkbox")

    val tagRef = TagRef["input"]("test")

    val updated = node.update(implicit ctx => tagRef.css(true, "a"))
    assert(updated == node.`class`("a"))
  }

  test("Use DSL to remove CSS tag") {
    val node = tag.Input
      .id("test")
      .`type`("checkbox")
      .`class`("a b c")

    val tagRef = TagRef["input"]("test")

    val updated = node.update(implicit ctx => tagRef.css(false, "b"))
    assert(updated == node.`class`("a c"))
  }

  test("Match by tag") {
    val node = tag.Div.set(tag.Span)
    val ref = TagRef["span"]
    val updated = node.update(implicit ctx => ref := tag.B)

    assert(updated == tag.Div.set(tag.Span.set(tag.B)))
  }

  test("Replace node") {
    val node = tag.Div.set(tag.Span)
    val ref = TagRef["span"]
    val updated = node.update(implicit ctx => ref.replace(tag.B))

    assert(updated == tag.Div.set(tag.B))
  }

  test("Resolve child references") {
    val itemRef = TagRef["div"]("item", "0")
    assert(itemRef == TagRef["div"]("item0"))
  }

  test("Invalid reference") {
    val node = tag.Div
    val ref = TagRef["span"]

    assertThrows[Exception] {
      node.update(implicit ctx => ref.replace(tag.B))
    }
  }
}
