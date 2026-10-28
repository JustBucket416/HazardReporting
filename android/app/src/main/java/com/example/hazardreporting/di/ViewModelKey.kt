package com.example.hazardreporting.di

import androidx.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * Map key for binding a [ViewModel] into the multibound map that [ViewModelFactory] creates ViewModels from
 *
 * @property value class of the bound ViewModel
 *
 * @author Roman Pliskin on 29.10.2026
 */
@MapKey
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewModelKey(val value: KClass<out ViewModel>)
