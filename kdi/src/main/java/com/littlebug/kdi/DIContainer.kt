package com.littlebug.kdi

import kotlin.reflect.KClass

object DIContainer {
    private val singletonInstances = mutableMapOf<KClass<*>, Any>()
    private val activityInstances = mutableMapOf<KClass<*>, Any>()
    private val fragmentInstances = mutableMapOf<KClass<*>, Any>()

    inline fun <T : Any> get(clazz: KClass<T>, scope: Any? = null): T {
        val instanceMap = getInstanceMapForScope(clazz, scope)
        return instanceMap[clazz] as? T ?: createInstance(clazz, scope).also {
            instanceMap[clazz] = it
        }
    }

    private fun <T : Any> createInstance(clazz: KClass<T>, scope: Any? = null): T {
        val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException("No primary constructor found for ${clazz.simpleName}")
        val parameters = constructor.parameters.map {
            get(it.type.classifier as KClass<*>, scope)
        }
        return constructor.call(*parameters.toTypedArray())
    }

    private fun getInstanceMapForScope(clazz: KClass<*>, scope: Any?): MutableMap<KClass<*>, Any> {
        return when {
            clazz.findAnnotation<Singleton>() != null -> singletonInstances
            clazz.findAnnotation<ActivityScope>() != null -> activityInstances
            clazz.findAnnotation<FragmentScope>() != null -> fragmentInstances
            else -> singletonInstances // Default to singleton if no scope is provided
        }
    }

    fun clearActivityScope() {
        activityInstances.clear()
    }

    fun clearFragmentScope() {
        fragmentInstances.clear()
    }
}