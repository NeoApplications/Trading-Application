/*
 * This file is part of Neo Launcher
 * Copyright (c) 2022   Neo Launcher Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.saggitt.omega.groups.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.android.launcher3.util.ComponentKey
import com.saggitt.omega.compose.components.BaseDialog
import com.saggitt.omega.compose.components.preferences.BasePreference
import com.saggitt.omega.compose.pages.AppSelectionPage
import com.saggitt.omega.compose.pages.ColorSelectionDialog
import com.saggitt.omega.flowerpot.Flowerpot
import com.saggitt.omega.groups.AppGroupsManager
import com.saggitt.omega.preferences.NLPrefs
import com.saggitt.omega.util.Config
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    category: AppGroupsManager.Category,
    onClose: (Int) -> Unit,
) {

    val context = LocalContext.current
    val prefs = NLPrefs.getInstance(context)
    var title by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val flowerpotManager = Flowerpot.Manager.getInstance(context)
    val openDialog = remember { mutableStateOf(false) }
    val colorPicker = remember { mutableStateOf(false) }
    var isHidden by remember { mutableStateOf(false) }
    var flowerpotCategory by remember { mutableStateOf("PERSONALIZATION") }
    val selectedApps = remember { mutableSetOf<ComponentKey>(*(emptyArray())) }
    val coroutineScope = rememberCoroutineScope()

    var color by remember { mutableStateOf(prefs.profileAccentColor.getValue()) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Divider(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12F),
                textColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
            }),
            shape = MaterialTheme.shapes.large,
            label = { Text(text = stringResource(id = R.string.name)) },
            isError = title.isEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (category == AppGroupsManager.Category.FLOWERPOT) {
            BasePreference(
                titleId = R.string.pref_appcategorization_flowerpot_title,
                summary = flowerpotManager.getAllPots()
                    .find { it.name == flowerpotCategory }!!.displayName,
                startWidget = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                endWidget = {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            ) { openDialog.value = true }
            Spacer(modifier = Modifier.height(4.dp))

            if (openDialog.value) {
                BaseDialog(openDialogCustom = openDialog) {
                    CategorySelectionDialogUI(selectedCategory = flowerpotCategory) {
                        flowerpotCategory = it
                        openDialog.value = false
                    }
                }
            }
        } else {
            val summary = context.resources.getQuantityString(
                R.plurals.tab_apps_count,
                selectedApps.size,
                selectedApps.size
            )

            BasePreference(
                titleId = R.string.tab_manage_apps,
                summary = summary,
                startWidget = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_apps),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                endWidget = {
                    Icon(
                        painter = painterResource(id = R.drawable.chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            ) { openDialog.value = true }
            Spacer(modifier = Modifier.height(4.dp))

            if (openDialog.value) {
                BaseDialog(openDialogCustom = openDialog) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(8.dp),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        GroupAppSelection(
                            selectedApps = selectedApps.map { it.toString() }.toSet(),
                        ) {
                            val componentsSet =
                                it.mapNotNull { ck -> ComponentKey.fromString(ck) }.toMutableSet()
                            selectedApps.clear()
                            selectedApps.addAll(componentsSet)
                        }
                    }
                }
            }
        }

        BasePreference(
            titleId = R.string.tab_hide_from_main,
            startWidget = {
                Icon(
                    painter = painterResource(id = R.drawable.tab_hide_from_main),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            endWidget = {
                Switch(
                    modifier = Modifier
                        .height(24.dp),
                    checked = isHidden,
                    onCheckedChange = {
                        isHidden = it
                    }
                )
            },
            onClick = { isHidden = !isHidden }
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (category != AppGroupsManager.Category.FOLDER) {

            BasePreference(
                titleId = R.string.tab_color,
                startWidget = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_color_donut),
                        contentDescription = "",
                        modifier = Modifier.size(30.dp),
                        tint = Color(color)
                    )
                }
            ) {
                colorPicker.value = true
            }
            if (colorPicker.value) {
                BaseDialog(openDialogCustom = colorPicker) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(8.dp),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                    ) {
                        ColorSelectionDialog(
                            defaultColor = color
                        ) {
                            color = it
                            colorPicker.value = false
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    onClose(Config.BS_SELECT_TAB_TYPE)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
            Spacer(modifier = Modifier.width(16.dp))

            OutlinedButton(
                onClick = {
                    onClose(Config.BS_SELECT_TAB_TYPE)

                    coroutineScope.launch {
                        val group = JSONObject("{}")

                        if (category.key == AppGroupsManager.Category.TAB.key) {
                            group.apply {
                                put("title", title)
                                put("hideFromAllApps", isHidden)
                                put("type", 2)
                                put("color", color)
                                put("apps", selectedApps.map { it.toString() }.toSet())
                            }
                            prefs.drawerAppGroupsManager.drawerTabs.saveToJson(group)
                        }
                        if (category.key == AppGroupsManager.Category.FOLDER.key) {

                            group.apply {
                                put("title", title)
                                put("hideFromAllApps", isHidden)
                                put("type", 1)
                                put("apps", selectedApps.map { it.toString() }.toSet())
                            }
                            prefs.drawerAppGroupsManager.drawerFolders.saveToJson(group)
                        }
                    }

                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35F),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65F)),
            ) {
                Text(text = stringResource(id = R.string.tab_bottom_sheet_save))
            }
        }
    }
}

@Composable
fun GroupAppSelection(
    selectedApps: Set<String>,
    onSave: (Set<String>) -> Unit,
) {
    var selected: Set<String> by remember {
        mutableStateOf(selectedApps)
    }
    val pageTitle = stringResource(id = R.string.selected_apps, selected.size)
    AppSelectionPage(
        pageTitle = pageTitle,
        selectedApps = selected,
        pluralTitleId = R.string.selected_apps,
        onSave = {
            selected = it
            onSave(it)
        }
    )
}