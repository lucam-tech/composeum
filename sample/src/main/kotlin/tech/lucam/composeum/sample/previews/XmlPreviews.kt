package tech.lucam.composeum.sample.previews

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import tech.lucam.composeum.annotation.ViewPreview
import tech.lucam.composeum.sample.R
import tech.lucam.composeum.sample.SampleGroup

/** Inflates the info-card XML layout from resources. */
@ViewPreview(
    name = "Info Card (XML)",
    group = SampleGroup.XmlLayouts::class,
    description = "View-based info card inflated from res/layout/view_info_card.xml.",
    tags = ["xml", "view", "card"],
)
fun infoCardXmlPreview(context: Context): View =
    LayoutInflater.from(context).inflate(R.layout.view_info_card, null, false)

/** Demonstrates a @ViewPreview with custom runtime data applied to the inflated view. */
@ViewPreview(
    name = "Info Card — Custom Text (XML)",
    group = SampleGroup.XmlLayouts::class,
    description = "Same layout with title and subtitle populated programmatically after inflation.",
    tags = ["xml", "view", "card", "data"],
)
fun infoCardCustomTextPreview(context: Context): View {
    val root = LayoutInflater.from(context).inflate(R.layout.view_info_card, null, false)
    root.findViewById<TextView>(R.id.title).text = "Hello from ViewPreview"
    root.findViewById<TextView>(R.id.subtitle).text =
        "Text bound after inflation — the same pattern you would use in a real Fragment or Activity."
    return root
}

/** Demonstrates a @ViewPreview where the view is built entirely in code (no XML). */
@ViewPreview(
    name = "Programmatic View",
    group = SampleGroup.XmlLayouts::class,
    description = "A View built entirely in code via a context parameter — no XML file needed.",
    tags = ["xml", "view", "programmatic"],
)
fun programmaticViewPreview(context: Context): View =
    LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        // White background makes the view visible as a thumbnail (transparent would blend
        // into the Card surface and appear blank at the scaled-down thumbnail size).
        setBackgroundColor(android.graphics.Color.WHITE)
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setPadding(48, 48, 48, 48)
        addView(TextView(context).apply {
            text = "Programmatic View"
            textSize = 24f
        })
        addView(TextView(context).apply {
            text = "No LayoutInflater needed."
            textSize = 16f
            setTextColor(android.graphics.Color.GRAY)
        })
    }
