package com.swordfish.lemuroid.app.tv.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.gemrating.GemShelfPresentation
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.ControllerSettingsActivity
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.mobile.feature.main.OPEN_SAFESAVE_EXTRA
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.tv.folderpicker.TVFolderPickerLauncher
import com.swordfish.lemuroid.app.tv.main.MainTVActivity
import com.swordfish.lemuroid.app.tv.settings.TVSettingsActivity
import com.swordfish.lemuroid.app.tv.shared.GamePresenter
import com.swordfish.lemuroid.app.tv.shared.TVHelper
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TVHomeFragment : BrowseSupportFragment() {
    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    @Inject
    lateinit var gameInteractor: GameInteractor

    @Inject
    lateinit var saveSyncManager: SaveSyncManager

    private var shelfSections = emptyList<TVShelfSection>()
    private var gemRatings = emptyMap<String, Int>()
    private var shelfFocus: TVShelfFocus? = null
    private var pendingShelfFocus: TVShelfFocus? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                when (item) {
                    is Game -> (activity as? MainTVActivity)?.showCartridge(item, gameInteractor::onGamePlay)
                    is MetaSystemInfo -> {
                        val action =
                            TVHomeFragmentDirections.actionNavigationSystemsToNavigationGames(
                                item.metaSystem.name,
                            )
                        findNavController().navigate(action)
                    }
                    is TVSetting -> {
                        when (item.type) {
                            TVSettingType.STOP_RESCAN ->
                                LibraryIndexScheduler.cancelLibrarySync(
                                    requireContext().applicationContext,
                                )
                            TVSettingType.RESCAN ->
                                LibraryIndexScheduler.scheduleLibrarySync(
                                    requireContext().applicationContext,
                                )
                            TVSettingType.CHOOSE_DIRECTORY -> launchFolderPicker()
                            TVSettingType.SETTINGS -> launchTVSettings()
                            TVSettingType.GEM ->
                                startActivity(
                                    Intent(requireContext(), MainActivity::class.java)
                                        .putExtra("cartdeck.open_gem", true),
                                )
                            TVSettingType.SAFESAVE ->
                                startActivity(
                                    Intent(requireContext(), MainActivity::class.java)
                                        .putExtra(OPEN_SAFESAVE_EXTRA, true),
                                )
                            TVSettingType.CONTROLLERS ->
                                startActivity(Intent(requireContext(), ControllerSettingsActivity::class.java))
                            TVSettingType.SHOW_ALL_FAVORITES -> launchFavorites()
                            TVSettingType.SAVE_SYNC ->
                                SaveSyncWork.enqueueManualWork(
                                    requireContext().applicationContext,
                                )
                        }
                    }
                }
            }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        pendingShelfFocus =
            shelfFocus ?: savedInstanceState?.getString(FOCUSED_ROW)?.let {
                TVShelfFocus(it, savedInstanceState.getString(FOCUSED_CARTRIDGE))
            }
        onItemViewSelectedListener =
            OnItemViewSelectedListener { _, item, _, row ->
                if (pendingShelfFocus == null && row is ListRow) {
                    val rowKey = tvShelfRowKey(row.headerItem.id, shelfSections)
                    if (rowKey != null) shelfFocus = TVShelfFocus(rowKey, (item as? Game)?.cartridgeId)
                }
            }

        setTitle("CARTDECK")
        setBadgeDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.cartdeck_wordmark_native))
        setBrandColor(Color.rgb(16, 28, 35))
        setSearchAffordanceColor(Color.rgb(80, 210, 194))

        recreateAdapter(
            includeFavorites = false,
            includeRecentGames = false,
            includeSystems = false,
        )
        setOnSearchClickedListener {
            findNavController().navigate(R.id.navigation_search)
        }

        val factory = TVHomeViewModel.Factory(retrogradeDb, requireContext().applicationContext)
        val homeViewModel = ViewModelProvider(this, factory)[TVHomeViewModel::class.java]

        launchOnState(Lifecycle.State.RESUMED) {
            homeViewModel.getViewStates()
                .combine(homeViewModel.shelfSections) { state, sections -> state to sections }
                .collect { (state, sections) -> update(state, sections) }
        }
        launchOnState(Lifecycle.State.RESUMED) {
            GemShelfPresentation.revision.collect {
                val context = requireContext().applicationContext
                val ratings =
                    withContext(Dispatchers.IO) {
                        runCatching { GemShelfPresentation.ratings(context) }.getOrDefault(emptyMap())
                    }
                if (gemRatings != ratings) {
                    gemRatings = ratings
                    val rowIds = listOf(FAVORITES_ADAPTER, RECENTS_ADAPTER) + shelfSections.indices.map(::shelfRowId)
                    rowIds.forEach { id ->
                        findAdapterById<ArrayObjectAdapter>(id)?.let { row ->
                            row.notifyArrayItemRangeChanged(0, row.size())
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (pendingShelfFocus ?: shelfFocus)?.let {
            outState.putString(FOCUSED_ROW, it.rowKey)
            outState.putString(FOCUSED_CARTRIDGE, it.cartridgeId)
        }
    }

    private fun update(
        viewState: TVHomeViewModel.HomeViewState,
        sections: List<TVShelfSection>,
    ) {
        val sectionsChanged = shelfSections.map { it.id to it.title } != sections.map { it.id to it.title }
        shelfSections = sections
        val adapterHasFavorites = findAdapterById<ObjectAdapter>(FAVORITES_ADAPTER) != null
        val adapterHasGames = findAdapterById<ObjectAdapter>(RECENTS_ADAPTER) != null
        val adapterHasSystems = findAdapterById<ObjectAdapter>(SYSTEM_ADAPTER) != null

        val favoritesChanged = adapterHasFavorites != viewState.favoritesGames.isNotEmpty()
        val recentsChanged = adapterHasGames != viewState.recentGames.isNotEmpty()
        val systemsChanged = adapterHasSystems != viewState.metaSystems.isNotEmpty()

        val rowsChanged = favoritesChanged || recentsChanged || systemsChanged || sectionsChanged
        val focusToRestore = pendingShelfFocus ?: shelfFocus.takeIf { rowsChanged }
        if (rowsChanged) {
            recreateAdapter(
                viewState.favoritesGames.isNotEmpty(),
                viewState.recentGames.isNotEmpty(),
                viewState.metaSystems.isNotEmpty(),
            )
        }

        findAdapterById<ArrayObjectAdapter>(FAVORITES_ADAPTER)?.apply {
            if (viewState.favoritesGames.size <= TVHomeViewModel.CAROUSEL_MAX_ITEMS) {
                setItems(viewState.favoritesGames, LEANBACK_MULTI_DIFF_CALLBACK)
            } else {
                val allItems =
                    viewState.favoritesGames.subList(0, TVHomeViewModel.CAROUSEL_MAX_ITEMS) +
                        listOf(TVSetting(TVSettingType.SHOW_ALL_FAVORITES))
                setItems(allItems, LEANBACK_MULTI_DIFF_CALLBACK)
            }
        }

        findAdapterById<ArrayObjectAdapter>(RECENTS_ADAPTER)
            ?.setItems(viewState.recentGames, LEANBACK_GAME_DIFF_CALLBACK)

        findAdapterById<ArrayObjectAdapter>(SYSTEM_ADAPTER)
            ?.setItems(viewState.metaSystems, LEANBACK_SYSTEM_DIFF_CALLBACK)

        shelfSections.forEachIndexed { index, section ->
            findAdapterById<ArrayObjectAdapter>(shelfRowId(index))
                ?.setItems(section.games, LEANBACK_GAME_DIFF_CALLBACK)
        }

        findAdapterById<ArrayObjectAdapter>(SETTINGS_ADAPTER)?.setItems(
            buildSettingsRowItems(viewState.indexInProgress, viewState.scanInProgress),
            LEANBACK_SETTING_DIFF_CALLBACK,
        )

        pendingShelfFocus = null
        focusToRestore?.let { focus ->
            locateTVShelfFocus(adapter, shelfSections, focus)?.let { (rowIndex, itemIndex) ->
                if (itemIndex == null) {
                    setSelectedPosition(rowIndex, false)
                } else {
                    setSelectedPosition(rowIndex, false, ListRowPresenter.SelectItemViewHolderTask(itemIndex))
                }
            }
        }
    }

    private fun <T> findAdapterById(id: Long): T? {
        for (i: Int in 0 until adapter.size()) {
            val listRow = adapter.get(i) as ListRow
            if (listRow.headerItem.id == id) {
                return listRow.adapter as T
            }
        }
        return null
    }

    private fun gemGamePresenter(cardSize: Int): GamePresenter =
        GamePresenter(cardSize, gameInteractor) { game -> gemRatings[game.cartridgeId] }

    private fun recreateAdapter(
        includeFavorites: Boolean,
        includeRecentGames: Boolean,
        includeSystems: Boolean,
    ) {
        val result = ArrayObjectAdapter(ListRowPresenter())
        val cardSize = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.card_size)
        val systemsCardPadding =
            resources.getDimensionPixelSize(
                com.swordfish.lemuroid.lib.R.dimen.systems_card_padding,
            )
        val settingsCardPadding =
            resources.getDimensionPixelSize(
                com.swordfish.lemuroid.lib.R.dimen.settings_card_padding,
            )

        if (includeFavorites) {
            val presenter = ClassPresenterSelector()
            presenter.addClassPresenter(Game::class.java, gemGamePresenter(cardSize))
            presenter.addClassPresenter(TVSetting::class.java, SettingPresenter(cardSize, settingsCardPadding))
            val favouritesItems = ArrayObjectAdapter(presenter)
            val title = resources.getString(R.string.tv_home_section_favorites)
            result.add(ListRow(HeaderItem(FAVORITES_ADAPTER, title), favouritesItems))
        }

        if (includeRecentGames) {
            val recentItems = ArrayObjectAdapter(gemGamePresenter(cardSize))
            val title = resources.getString(R.string.tv_home_section_recents)
            result.add(ListRow(HeaderItem(RECENTS_ADAPTER, title), recentItems))
        }

        if (includeSystems) {
            val systemItems = ArrayObjectAdapter(SystemPresenter(cardSize, systemsCardPadding))
            val title = resources.getString(R.string.tv_home_section_systems)
            result.add(ListRow(HeaderItem(SYSTEM_ADAPTER, title), systemItems))
        }

        shelfSections.forEachIndexed { index, section ->
            val items = ArrayObjectAdapter(gemGamePresenter(cardSize))
            items.setItems(section.games, LEANBACK_GAME_DIFF_CALLBACK)
            result.add(ListRow(HeaderItem(shelfRowId(index), section.title), items))
        }

        val gemItems = ArrayObjectAdapter(SettingPresenter(cardSize, settingsCardPadding))
        gemItems.add(TVSetting(TVSettingType.GEM))
        result.add(ListRow(HeaderItem(GEM_ADAPTER, "GEM"), gemItems))

        val saveItems = ArrayObjectAdapter(SettingPresenter(cardSize, settingsCardPadding))
        saveItems.add(TVSetting(TVSettingType.SAFESAVE))
        result.add(ListRow(HeaderItem(SAFESAVE_ADAPTER, getString(R.string.cartdeck_safesave_title)), saveItems))

        val settingsItems = ArrayObjectAdapter(SettingPresenter(cardSize, settingsCardPadding))
        settingsItems.setItems(
            buildSettingsRowItems(indexInProgress = false, scanInProgress = false),
            LEANBACK_SETTING_DIFF_CALLBACK,
        )
        val settingsTitle = resources.getString(R.string.tv_home_section_settings)
        result.add(ListRow(HeaderItem(SETTINGS_ADAPTER, settingsTitle), settingsItems))

        adapter = result
    }

    private fun buildSettingsRowItems(
        indexInProgress: Boolean,
        scanInProgress: Boolean,
    ): List<TVSetting> {
        return mutableListOf<TVSetting>().apply {
            if (saveSyncManager.isSupported() && saveSyncManager.isConfigured()) {
                add(TVSetting(TVSettingType.SAVE_SYNC, !indexInProgress))
            }
            if (scanInProgress) {
                add(TVSetting(TVSettingType.STOP_RESCAN, true))
            } else {
                add(TVSetting(TVSettingType.RESCAN, !indexInProgress))
            }

            add(TVSetting(TVSettingType.CHOOSE_DIRECTORY, !indexInProgress))
            add(TVSetting(TVSettingType.CONTROLLERS))
            add(TVSetting(TVSettingType.SETTINGS, !indexInProgress))
        }
    }

    private fun launchFolderPicker() {
        if (TVHelper.isSAFSupported(requireContext())) {
            StorageFrameworkPickerLauncher.pickFolder(requireContext())
        } else {
            TVFolderPickerLauncher.pickFolder(requireContext())
        }
    }

    private fun launchTVSettings() {
        startActivity(Intent(requireContext(), TVSettingsActivity::class.java))
    }

    private fun launchFavorites() {
        findNavController().navigate(R.id.navigation_favorites)
    }

    companion object {
        const val RECENTS_ADAPTER = 1L
        const val SYSTEM_ADAPTER = 2L
        const val SETTINGS_ADAPTER = 3L
        const val GEM_ADAPTER = 5L
        const val SAFESAVE_ADAPTER = 6L
        const val FAVORITES_ADAPTER = 4L
        private const val FOCUSED_ROW = "cartdeck.tv.focusedRow"
        private const val FOCUSED_CARTRIDGE = "cartdeck.tv.focusedCartridge"

        private fun shelfRowId(index: Int) = -1L - index

        val LEANBACK_MULTI_DIFF_CALLBACK =
            object : DiffCallback<Any>() {
                override fun areContentsTheSame(
                    oldItem: Any,
                    newItem: Any,
                ): Boolean {
                    return when {
                        (oldItem is Game && newItem is Game) -> {
                            LEANBACK_GAME_DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        }
                        (oldItem is TVSetting && newItem is TVSetting) -> {
                            LEANBACK_SETTING_DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        }
                        else -> false
                    }
                }

                override fun areItemsTheSame(
                    oldItem: Any,
                    newItem: Any,
                ): Boolean {
                    return when {
                        (oldItem is Game && newItem is Game) -> {
                            LEANBACK_GAME_DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
                        }
                        (oldItem is TVSetting && newItem is TVSetting) -> {
                            LEANBACK_SETTING_DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
                        }
                        else -> false
                    }
                }
            }

        val LEANBACK_GAME_DIFF_CALLBACK =
            object : DiffCallback<Game>() {
                override fun areContentsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areItemsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem.id == newItem.id
                }
            }

        val LEANBACK_SYSTEM_DIFF_CALLBACK =
            object : DiffCallback<MetaSystemInfo>() {
                override fun areContentsTheSame(
                    oldInfo: MetaSystemInfo,
                    newInfo: MetaSystemInfo,
                ): Boolean {
                    return oldInfo == newInfo
                }

                override fun areItemsTheSame(
                    oldInfo: MetaSystemInfo,
                    newInfo: MetaSystemInfo,
                ): Boolean {
                    return oldInfo.metaSystem.name == newInfo.metaSystem.name
                }
            }

        val LEANBACK_SETTING_DIFF_CALLBACK =
            object : DiffCallback<TVSetting>() {
                override fun areContentsTheSame(
                    oldItem: TVSetting,
                    newItem: TVSetting,
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areItemsTheSame(
                    oldItem: TVSetting,
                    newItem: TVSetting,
                ): Boolean {
                    return oldItem.type == newItem.type
                }
            }
    }

    @dagger.Module
    class Module
}
