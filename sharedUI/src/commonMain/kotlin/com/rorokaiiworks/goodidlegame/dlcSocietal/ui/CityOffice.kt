package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun CityOfficeScreen(
    city: City = koinInject(),
    i18n: I18n = koinInject()
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = i18n.tr("If you think you've made a little mistake, just click the button below to start over! No worries—this is just a tiny hiccup on your way to greatness."))

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            onClick = { showDialog = true },
        ) {
            Text(text = i18n.tr("Start Over"))
        }
    }

    if (showDialog) {
        GameDialog(
            title = i18n.tr("Are you sure?"),
            onDismissRequest = { showDialog = false },
            confirmEnabled = true,
            onConfirmation = {
                city.startOver()
                showDialog = false
            }
        ) {
            Text(i18n.tr("Are you sure?"))
        }
    }
}