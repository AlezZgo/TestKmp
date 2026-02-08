package org.example.project.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.example.project.createcard.CreateCardComponent

@Composable
fun CreateCardContent(
    component: CreateCardComponent,
    modifier: Modifier = Modifier,
) {
    val model by component.model.subscribeAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Новая карточка",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = model.firstName,
            onValueChange = component::onFirstNameChanged,
            singleLine = true,
            label = { Text("Имя") },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = model.lastName,
            onValueChange = component::onLastNameChanged,
            singleLine = true,
            label = { Text("Фамилия") },
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = component::onBackClicked) {
                Text("Назад")
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = component::onCreateClicked,
                enabled = model.isValid,
            ) {
                Text("Создать")
            }
        }
    }
}

