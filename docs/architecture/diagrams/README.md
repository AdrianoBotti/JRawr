# Diagram Sources

Most diagrams in this handbook are Mermaid blocks embedded in the Markdown, so they render directly
on GitHub and in Mermaid-aware editors. No separate image generation is required.

If a diagram needs a layout that Mermaid cannot express cleanly, add a Graphviz `.dot` source here
with the same base name and, optionally, a generated `.svg`. Keep the source file; the SVG is a
build artifact of the documentation, not the source of truth.
