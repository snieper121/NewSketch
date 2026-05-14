package my.company.ai.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import my.company.ai.data.model.ProjectFile

/**
 * Сворачиваемое дерево файлов проекта.
 *
 * Фиксит баги предшественника:
 *  * показываются только раскрытые узлы (раньше разворачивалось всё сразу);
 *  * стабильный ключ `relativePath` (корень — специальная строка);
 *  * клик по папке раскрывает/сворачивает, клик по файлу открывает его;
 *  * нередактируемые файлы отображаются с пониженной непрозрачностью.
 */
@Composable
fun FileTreePanel(
    tree: ProjectFile,
    openedFile: String?,
    expandedPaths: Set<String>,
    onToggleExpand: (String) -> Unit,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(tree, expandedPaths) { flattenVisible(tree, expandedPaths) }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(rows, key = { it.node.relativePath.ifEmpty { "\u0000root" } }) { row ->
            TreeRow(
                row = row,
                selected = row.node.relativePath == openedFile,
                isExpanded = row.node.relativePath in expandedPaths,
                onClick = {
                    if (row.node.isDirectory) {
                        onToggleExpand(row.node.relativePath)
                    } else if (row.node.isEditable) {
                        onFileClick(row.node.relativePath)
                    }
                },
            )
        }
    }
}

private data class TreeRow(val node: ProjectFile, val depth: Int)

/** Обходим только раскрытые поддеревья. Корень всегда видим. */
private fun flattenVisible(
    root: ProjectFile,
    expanded: Set<String>,
): List<TreeRow> = buildList {
    fun walk(node: ProjectFile, depth: Int) {
        add(TreeRow(node, depth))
        if (node.isDirectory && node.relativePath in expanded) {
            node.children.forEach { walk(it, depth + 1) }
        }
    }
    walk(root, 0)
}

@Composable
private fun TreeRow(
    row: TreeRow,
    selected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val node = row.node
    val clickable = node.isDirectory || node.isEditable
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = (8 + row.depth * 16).dp,
                top = 6.dp,
                bottom = 6.dp,
                end = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (node.isDirectory) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandMore
                else Icons.Default.ChevronRight,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                modifier = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (node.isDirectory) Icons.Default.Folder
            else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = node.name.ifEmpty { "/" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(if (clickable) 1f else 0.5f),
        )
    }
}
