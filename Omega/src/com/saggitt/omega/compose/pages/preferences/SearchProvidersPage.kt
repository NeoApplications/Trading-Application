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
package com.saggitt.omega.compose.pages.preferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.saggitt.omega.compose.components.BaseDialog
import com.saggitt.omega.compose.components.ListItemWithIcon
import com.saggitt.omega.compose.components.ViewWithActionBar
import com.saggitt.omega.compose.components.move
import com.saggitt.omega.compose.icons.Phosphor
import com.saggitt.omega.compose.icons.phosphor.Plus
import com.saggitt.omega.data.SearchProviderRepository
import com.saggitt.omega.theme.GroupItemShape
import com.saulhdev.neolauncher.search.SearchProviderController
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchProvidersPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openDialog = remember { mutableStateOf(false) }
    var selectedProvider by remember {
        mutableLongStateOf(0L)
    }

    // TODO pulling directly from DB (fixes reactivity of add/delete/rename)
    val searchProviders = Utilities.getNeoPrefs(context).searchProviders
    val allItems = SearchProviderController.getSearchProviders()
    val (enabled, disabled) = allItems
        .toList()
        .partition {
            it.first in searchProviders.getAll()
        }
    val enabledMap = enabled.associateBy { it.first }
    val enabledSorted = searchProviders.getAll().mapNotNull { enabledMap[it] }
    val enabledItems = remember { enabledSorted.toMutableStateList() }
    val disabledItems = remember { disabled.toMutableStateList() }

    val saveList = {
        val enabledKeys = enabledItems.map { it.first }
        searchProviders.setAll(enabledKeys)
    }

    val lazyListState = rememberLazyListState()
    val reorderableListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = enabledItems.indexOfFirst { it.first == from.key }
        val toIndex = enabledItems.indexOfFirst { it.first == to.key }

        enabledItems.move(fromIndex, toIndex)
    }

    ViewWithActionBar(
        title = stringResource(id = R.string.search_provider),
        onBackAction = saveList,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    Icon(
                        imageVector = Phosphor.Plus,
                        contentDescription = stringResource(id = R.string.add_search_provider)
                    )
                },
                text = {
                    Text(text = stringResource(R.string.add_search_provider))
                },
                onClick = {
                    scope.launch {
                        selectedProvider = getKoin().get<SearchProviderRepository>().insertNew()
                        openDialog.value = true
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 8.dp),
            state = lazyListState,
        ) {
            stickyHeader {
                Text(
                    text = stringResource(id = R.string.enabled_events),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            itemsIndexed(enabledItems, key = { _, it -> it.first }) { subIndex, item ->
                ReorderableItem(
                    reorderableListState,
                    key = item.first,
                ) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 16.dp else 0.dp,
                        label = "elevation",
                    )
                    val bgColor by animateColorAsState(
                        if (isDragging) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer,
                        label = "bgColor",
                    )

                    ListItemWithIcon(
                        modifier = Modifier
                            .longPressDraggableHandle()
                            .shadow(elevation)
                            .clip(GroupItemShape(subIndex, enabledItems.size - 1))
                            .combinedClickable(
                                onClick = {
                                    enabledItems.remove(item)
                                    disabledItems.add(0, item)
                                },
                                onLongClick = {
                                    selectedProvider = item.first
                                    openDialog.value = true
                                }
                            ),
                        containerColor = bgColor,
                        title = item.second.name,
                        startIcon = {
                            Image(
                                painter = painterResource(id = item.second.iconId),
                                contentDescription = item.second.name,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        endCheckbox = {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    enabledItems.remove(item)
                                    disabledItems.add(0, item)
                                }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_drag_handle),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                    )
                }
            }

            stickyHeader {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.tap_to_enable),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
            itemsIndexed(disabledItems, key = { _, it -> it.first }) { subIndex, item ->
                ListItemWithIcon(
                    modifier = Modifier
                        .clip(GroupItemShape(subIndex, disabledItems.size - 1))
                        .combinedClickable(
                            onClick = {
                                disabledItems.remove(item)
                                enabledItems.add(item)
                            },
                            onLongClick = {
                                selectedProvider = item.first
                                openDialog.value = true
                            }
                        ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    title = item.second.name,
                    startIcon = {
                        Image(
                            painter = painterResource(id = item.second.iconId),
                            contentDescription = item.second.name,
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    endCheckbox = {
                        Spacer(modifier = Modifier.height(32.dp))
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (openDialog.value) {
        BaseDialog(openDialogCustom = openDialog) {
            SearchProviderDialogUI(
                repositoryId = selectedProvider,
                openDialogCustom = openDialog,
                onDelete = {
                    getKoin().get<SearchProviderRepository>().delete(it)

                },
                onSave = { getKoin().get<SearchProviderRepository>().update(it) }
            )
        }
    }

    DisposableEffect(key1 = null) {
        onDispose { saveList() }
    }
}