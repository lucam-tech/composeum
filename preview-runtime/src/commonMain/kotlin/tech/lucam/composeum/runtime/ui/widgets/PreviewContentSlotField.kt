package tech.lucam.composeum.runtime.ui.widgets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

/**
 * Dropdown widget for a content slot preview parameter.
 *
 * Delegates to [PreviewDropdownField] with the option names derived from the
 * [ContentSlotValue][tech.lucam.composeum.runtime.ContentSlotValue] stored in state.
 *
 * @param label         Label displayed inside the text field.
 * @param optionNames   Display names for each composable option.
 * @param selectedIndex Index of the currently selected option.
 * @param onIndex       Called with the newly selected index when the user picks one.
 * @param description   Optional helper text shown below the dropdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewContentSlotField(
    label: String,
    optionNames: List<String>,
    selectedIndex: Int,
    onIndex: (Int) -> Unit,
    description: String = "",
) {
    val safeIndex = selectedIndex.coerceIn(optionNames.indices)
    PreviewDropdownField(
        label = label,
        value = optionNames.getOrElse(safeIndex) { "" },
        options = optionNames,
        description = description,
        onValue = { name -> onIndex(optionNames.indexOf(name).coerceAtLeast(0)) },
    )
}
