package pl.metastack.metaweb.tag

import pl.metastack.metaweb.tree

/**
 * The <em>HTML Section Element</em> (<code>&lt;section&gt;</code>) represents a generic section of a document, i.e., a thematic grouping of content, typically with a heading. Each <code>&lt;section&gt;</code> should be identified, typically by including a heading (<a href="/en-US/docs/Web/HTML/Element/h1" title="Heading elements implement six levels of document headings, <h1> is the most important and <h6> is the least. A heading element briefly describes the topic of the section it introduces. Heading information may be used by user agents, for example, to construct a table of contents for a document automatically."><code>&lt;h1&gt;</code></a>-<a href="/en-US/docs/Web/HTML/Element/h6" title="Heading elements implement six levels of document headings, <h1> is the most important and <h6> is the least. A heading element briefly describes the topic of the section it introduces. Heading information may be used by user agents, for example, to construct a table of contents for a document automatically."><code>&lt;h6&gt;</code></a> element) as a child of the <code>&lt;section&gt;</code> element.
<p><em>Usage notes :</em></p> 
<ul> 
 <li>If it makes sense to separately syndicate the content of a <a href="/en-US/docs/Web/HTML/Element/section" title="The HTML Section Element (<section>) represents a generic section of a document, i.e., a thematic grouping of content, typically with a heading. Each <section> should be identified, typically by including a heading (<h1>-<h6> element) as a child of the <section> element."><code>&lt;section&gt;</code></a> element, use an <a href="/en-US/docs/Web/HTML/Element/article" title="The HTML Article Element (<article>) represents a self-contained composition in a document, page, application, or site, which is intended to be independently distributable or reusable, e.g., in syndication. This could be a forum post, a magazine or newspaper article, a blog entry, or any other independent item of content. Each <article> should be identified, typically by including a heading (h1-h6 element) as a child of the <article> element."><code>&lt;article&gt;</code></a> element instead.</li> 
 <li>Do not use the <a href="/en-US/docs/Web/HTML/Element/section" title="The HTML Section Element (<section>) represents a generic section of a document, i.e., a thematic grouping of content, typically with a heading. Each <section> should be identified, typically by including a heading (<h1>-<h6> element) as a child of the <section> element."><code>&lt;section&gt;</code></a> element as a generic container; this is what <a href="/en-US/docs/Web/HTML/Element/div" title="The HTML <div> element (or HTML Document Division Element) is the generic container for flow content, which does not inherently represent anything. It can be used to group elements for styling purposes (using the class or id attributes), or because they share attribute values, such as lang. It should be used only when no other semantic element (such as <article> or <nav>) is appropriate."><code>&lt;div&gt;</code></a> is for, especially when the sectioning is only for styling purposes. A rule of thumb is that a section should logically appear in the outline of a document.</li> 
</ul>
 */
case class Section(attributes: Predef.Map[String, Any] = Predef.Map.empty, children: Seq[tree.Node] = Seq.empty) extends tree.Tag with HTMLTag {
  override def tagName = "section"
  override def copy(attributes: Predef.Map[String, Any] = attributes, children: Seq[tree.Node] = children): Section = Section(attributes, children)
}
