package com.littlebug.kdi

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.littlebug.kdi.annotation.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty

@Suppress("UNCHECKED_CAST")
class DependencyGraph {
    private val providers = mutableMapOf<KClass<*>, () -> Any>()
    private val instances = mutableMapOf<KClass<*>, Any>()
    private val activityScopes = mutableMapOf<Any, MutableMap<KClass<*>, Any>>()
    private val fragmentScopes = mutableMapOf<Any, MutableMap<KClass<*>, Any>>()

    fun <T : Any> register(clazz: KClass<T>, provider: () -> T) {
        providers[clazz] = provider
    }

    fun <T : Any> resolve(clazz: KClass<T>, scope: Any? = null): T {
        return when (scope) {
            is AppCompatActivity -> resolveInScope(clazz, scope, activityScopes)
            is Fragment -> resolveInScope(clazz, scope, fragmentScopes)
            else -> instances[clazz] as? T
                ?: providers[clazz]?.invoke() as? T
                ?: throw IllegalArgumentException("No provider found for ${clazz.simpleName}")
        }
    }

    private fun <T : Any> resolveInScope(
        clazz: KClass<T>,
        scope: Any,
        scopeMap: MutableMap<Any, MutableMap<KClass<*>, Any>>
    ): T {
        val scopedInstances = scopeMap.getOrPut(scope) { mutableMapOf() }
        return scopedInstances[clazz] as? T
            ?: providers[clazz]?.invoke()
                ?.also { scopedInstances[clazz] = it } as? T
            ?: throw IllegalArgumentException("No provider found for ${clazz.simpleName}")
    }

    fun <T : Any> inject(target: T, scope: Any? = null) {
        val targetClass = target::class
        targetClass.constructors.firstOrNull { it.parameters.isNotEmpty() }?.let { constructor ->
            val parameters = constructor.parameters.map { parameter ->
                resolve(parameter.type.classifier as KClass<*>, scope)
            }
            constructor.call(*parameters.toTypedArray())
        }
        targetClass.members.filterIsInstance<KMutableProperty<*>>().forEach { property ->
            property.annotations.filterIsInstance<Inject>().forEach {
                val dependency = resolve(property.returnType.classifier as KClass<*>, scope)
                property.setter.call(target, dependency)
            }
        }
    }

    fun clearActivityScope(activity: AppCompatActivity) {
        activityScopes.remove(activity)
    }

    fun clearFragmentScope(fragment: Fragment) {
        fragmentScopes.remove(fragment)
    }
}