package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rorokaiiworks.goodidlegame.IdleGameTheme

@Composable
fun SubCategoryBtn(
    modifier: Modifier = Modifier,
    subCategory: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = subCategory) }
    )
}

@Composable
@Preview
private fun SubCategoryBtnPreview() {
    IdleGameTheme {
        Column {
            SubCategoryBtn(
                subCategory = "SubCategory",
                isSelected = true,
                onClick = {}
            )

            SubCategoryBtn(
                subCategory = "SubCategory",
                isSelected = false,
                onClick = {}
            )
        }
    }
}
