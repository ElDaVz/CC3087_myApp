package com.uvg.cc3087.myapp.ui.navigation

import android.os.Bundle
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/** Each account gets fresh ViewModels and its own saved-state registry. */
internal class SessionViewModelOwner(restored: Bundle? = null) : SavedStateRegistryOwner,
    ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val viewModelStore = ViewModelStore()
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = controller.savedStateRegistry
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() = SavedStateViewModelFactory(null, this)
    override val defaultViewModelCreationExtras: CreationExtras get() = MutableCreationExtras().apply {
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@SessionViewModelOwner)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@SessionViewModelOwner)
    }
    init {
        controller.performAttach()
        enableSavedStateHandles()
        controller.performRestore(restored)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    fun save(): Bundle = Bundle().also { controller.performSave(it) }
    fun clear() { registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY); viewModelStore.clear() }
}
